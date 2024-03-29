/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ===================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 */
package org.onap.champ;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champcore.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.champ.async.ChampAsyncRequestProcessor;
import org.onap.champ.entity.ChampBulkPayload;
import org.onap.champ.entity.ChampBulkResponse;
import org.onap.champ.entity.ChampObjectDeserializer;
import org.onap.champ.entity.ChampObjectSerializer;
import org.onap.champ.entity.ChampRelationshipDeserializer;
import org.onap.champ.entity.ChampRelationshipSerializer;
import org.onap.champ.exception.ChampServiceException;
import org.onap.champ.service.ChampDataService;
import org.onap.champ.service.logging.ChampMsgs;
import org.onap.champ.service.logging.LoggingUtil;
import org.onap.champ.util.ChampProperties;
import org.onap.champ.util.ChampServiceConstants;
import org.onap.champ.util.HttpHeadersValidator;
import org.onap.champ.util.etag.EtagGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Path(value = "/services/champ-service/v1/")
public class ChampRESTAPI {

  private ObjectMapper mapper;

  private ChampDataService champDataService;
  private EtagGenerator etagGenerator;
  private HttpHeadersValidator httpHeadersValidator;
  private String TRANSACTION_METHOD = "method";
  private Timer timer;

  private Logger logger = LoggerFactory.getInstance().getLogger(ChampRESTAPI.class);
  Logger auditLogger = LoggerFactory.getInstance().getAuditLogger(ChampRESTAPI.class.getName());
  private static Logger metricsLogger = LoggerFactory.getInstance().getMetricsLogger(ChampRESTAPI.class.getName());
  private static final Pattern QUERY_OBJECT_ID_URL_MATCH = Pattern.compile("_reserved_(.*)");

  public ChampRESTAPI(ChampDataService champDataService, ChampAsyncRequestProcessor champAsyncRequestProcessor) throws NoSuchAlgorithmException {
    this.champDataService = champDataService;

    // Async request handling is optional.
    if (champAsyncRequestProcessor != null) {
      timer = new Timer("ChampAsyncRequestProcessor-1");
      timer.schedule(champAsyncRequestProcessor, champAsyncRequestProcessor.getRequestPollingTimeSeconds(),
          champAsyncRequestProcessor.getRequestPollingTimeSeconds());
    }

    mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addSerializer(ChampObject.class, new ChampObjectSerializer());
    module.addDeserializer(ChampObject.class, new ChampObjectDeserializer());
    module.addSerializer(ChampRelationship.class, new ChampRelationshipSerializer());
    module.addDeserializer(ChampRelationship.class, new ChampRelationshipDeserializer());
    mapper.registerModule(module);

    etagGenerator = new EtagGenerator();
    httpHeadersValidator = new HttpHeadersValidator();
  }

  @GET
  @Path("echo")
  @Produces(MediaType.TEXT_PLAIN)
  public Response echo() {
    return Response.ok().entity("alive").build();
  }

  @GET
  @Path("objects/{objectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getObject(@PathParam("objectId") String objectId, @QueryParam("transactionId") String tId,
      @Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    logger.info(ChampMsgs.INCOMING_REQUEST, tId, objectId);

    Response response = null;
    ChampObject retrieved;

    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      ChampTransaction transaction = champDataService.getTransaction(tId);

      if (tId != null && transaction == null) {
        throw new ChampServiceException("transactionId not found", Status.BAD_REQUEST);
      }
      retrieved = champDataService.getObject(objectId, Optional.ofNullable(transaction));
      if (retrieved == null) {
        response = Response.status(Status.NOT_FOUND).entity(objectId + " not found").build();
      } else {
        EntityTag etag = new EntityTag(etagGenerator.computeHashForChampObject(retrieved));
        response = Response.status(Status.OK).entity(mapper.writeValueAsString(retrieved)).tag(etag).build();
      }

    } catch (JsonProcessingException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (ChampServiceException ce) {
      response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
    } catch (Exception e) {
        response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        LoggingUtil.logInternalError(logger, e);
    } finally {
      if (response != null) {
        logger.debug(response.getEntity().toString());
      }
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "GET", Long.toString(System.currentTimeMillis() - startTimeInMs));
    }

    return response;
  }

  @DELETE
  @Path("objects/{objectId}")
  public Response deleteObject(@PathParam("objectId") String objectId, @QueryParam("transactionId") String tId,
      @Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    logger.info(ChampMsgs.INCOMING_REQUEST, tId, objectId);
    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      ChampTransaction transaction = champDataService.getTransaction(tId);

      if (tId != null && transaction == null) {
        throw new ChampServiceException("transactionId not found", Status.BAD_REQUEST);
      }
      champDataService.deleteObject(objectId, Optional.ofNullable(transaction));

      response = Response.status(Status.OK).build();
    } catch (ChampObjectNotExistsException e) {
      response = Response.status(Status.NOT_FOUND).entity(objectId + " not found").build();
    } catch (ChampServiceException ce) {
      response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
    } catch (ChampTransactionException | ChampUnmarshallingException e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "DELETE",
          Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @POST
  @Path("objects")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postObject(String champObj, @QueryParam("transactionId") String tId, @Context HttpHeaders headers,
      @Context UriInfo uriInfo, @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    logger.info(ChampMsgs.INCOMING_REQUEST, tId, champObj);
    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      ChampTransaction transaction = champDataService.getTransaction(tId);
      if (tId != null && transaction == null) {
        throw new ChampServiceException("transactionId not found", Status.BAD_REQUEST);
      }
      ChampObject champObject = mapper.readValue(champObj, ChampObject.class);

      ChampObject created = champDataService.storeObject(champObject, Optional.ofNullable(transaction));
      EntityTag eTag = new EntityTag(etagGenerator.computeHashForChampObject(created));
      response = Response.status(Status.CREATED).entity(mapper.writeValueAsString(created)).tag(eTag).build();
    } catch (IOException e) {
      response = Response.status(Status.BAD_REQUEST).entity("Unable to parse the payload").build();
    } catch (ChampServiceException ce) {
      response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
    } catch (IllegalArgumentException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LoggingUtil.logInternalError(logger, e);
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "POST",
          Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @PUT
  @Path("objects/{objectId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response putObject(@PathParam("objectId") String objectId, String champObj,
      @QueryParam("transactionId") String tId, @Context HttpHeaders headers, @Context UriInfo uriInfo,
      @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    logger.info(ChampMsgs.INCOMING_REQUEST, tId, objectId + " " + champObj);

    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      ChampTransaction transaction = champDataService.getTransaction(tId);
      if (tId != null && transaction == null) {
        throw new ChampServiceException("transactionId not found", Status.BAD_REQUEST);
      }

      ChampObject co = mapper.readValue(champObj, ChampObject.class);
      // check if key is present or if it equals the key that is in the URI
      ChampObject updated = champDataService.replaceObject(co, objectId, Optional.ofNullable(transaction));
      EntityTag eTag = new EntityTag(etagGenerator.computeHashForChampObject(updated));
      response = Response.status(Status.OK).entity(mapper.writeValueAsString(updated)).tag(eTag).build();
    } catch (IOException e) {
      response = Response.status(Status.BAD_REQUEST).entity("Unable to parse the payload").build();
    } catch (ChampServiceException ce) {
      response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
    } catch (IllegalArgumentException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LoggingUtil.logInternalError(logger, e);
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "PUT", Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @GET
  @Path("objects/relationships/{oId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEdges(@PathParam("oId") String oId, @QueryParam("transactionId") String tId,
      @Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    List<ChampRelationship> retrieved;
    Response response = null;
    logger.info(ChampMsgs.INCOMING_REQUEST, tId, oId);
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      ChampTransaction transaction = tId == null ? null : champDataService.getTransaction(tId);

      if (tId != null && transaction == null) {
        throw new ChampServiceException("No transaction found for transaction ID: " + tId, Status.BAD_REQUEST);
      }
      retrieved = champDataService.getRelationshipsByObject(oId, Optional.ofNullable(transaction));
      if (retrieved == null) {
        response = Response.status(Status.NOT_FOUND).entity(oId + " not found").build();
        return response;
      }
      EntityTag eTag = new EntityTag(etagGenerator.computeHashForChampRelationships(retrieved));
      response = Response.status(Status.OK).entity(mapper.writeValueAsString(retrieved)).tag(eTag).build();
    } catch (JsonProcessingException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (ChampServiceException ce) {
      response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LoggingUtil.logInternalError(logger, e);
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "GET", Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @GET
  @Path("objects/filter/")
  @Produces(MediaType.APPLICATION_JSON)
  public Response filterObject(@Context HttpHeaders headers, @Context UriInfo uriInfo,
      @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    String propertiesKey = ChampProperties.get(ChampServiceConstants.CHAMP_COLLECTION_PROPERTIES_KEY);
    List<ChampObject> champObjects;
    Map<String, Object> filter = new HashMap<>();

    for (Map.Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
      if ((!e.getKey().equals(propertiesKey)) && !reservedKeyMatcher ( QUERY_OBJECT_ID_URL_MATCH, e.getKey () )) {
        filter.put(e.getKey(), e.getValue().get(0));
      }
    }

    HashSet<String> properties;
    if (uriInfo.getQueryParameters().containsKey(propertiesKey)) {
      properties = new HashSet<>(uriInfo.getQueryParameters().get(propertiesKey));
    } else {
      properties = new HashSet<>();
    }

    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      champObjects = champDataService.queryObjects(filter, properties);
      EntityTag eTag = new EntityTag(etagGenerator.computeHashForChampObjects(champObjects));
      response = Response.status(Status.OK).type(MediaType.APPLICATION_JSON).tag(eTag).entity(mapper.writeValueAsString(champObjects))
          .build();
    } catch (JsonProcessingException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (ChampServiceException e1) {
      response = Response.status(e1.getHttpStatus()).entity(e1.getMessage()).build();
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LoggingUtil.logInternalError(logger, e);
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "GET", Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @GET
  @Path("relationships/{rId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRelationship(@PathParam("rId") String rId, @QueryParam("transactionId") String tId,
      @Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    logger.info(ChampMsgs.INCOMING_REQUEST, tId, rId);
    ChampRelationship retrieved;
    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      ChampTransaction transaction = champDataService.getTransaction(tId);

      if (tId != null && transaction == null) {
        throw new ChampServiceException("transactionId not found", Status.BAD_REQUEST);
      }
      retrieved = champDataService.getRelationship(rId, Optional.ofNullable(transaction));
      if (retrieved == null) {
        response = Response.status(Status.NOT_FOUND).entity(rId + " not found").build();
        return response;
      }
      EntityTag eTag = new EntityTag(etagGenerator.computeHashForChampRelationship(retrieved));
      response = Response.status(Status.OK).entity(mapper.writeValueAsString(retrieved)).tag(eTag).build();

    } catch (IOException e) {
      response = Response.status(Status.BAD_REQUEST).entity("Unable to parse the payload").build();
    } catch (ChampServiceException ce) {
      response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LoggingUtil.logInternalError(logger, e);
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "GET", Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @POST
  @Path("relationships")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postRelationships(String relationship, @QueryParam("transactionId") String tId,
      @Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    logger.info(ChampMsgs.INCOMING_REQUEST, tId, relationship);
    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      ChampTransaction transaction = champDataService.getTransaction(tId);
      if (tId != null && transaction == null) {
        throw new ChampServiceException("transactionId not found", Status.BAD_REQUEST);
      }
      ChampRelationship r = mapper.readValue(relationship, ChampRelationship.class);

      ChampRelationship created = champDataService.storeRelationship(r, Optional.ofNullable(transaction));
      EntityTag eTag = new EntityTag(etagGenerator.computeHashForChampRelationship(created));
      response = Response.status(Status.CREATED).entity(mapper.writeValueAsString(created)).tag(eTag).build();
    } catch (IOException e) {
      response = Response.status(Status.BAD_REQUEST).entity("Unable to parse the payload").build();
    } catch (ChampServiceException ce) {
      response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
    } catch (IllegalArgumentException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LoggingUtil.logInternalError(logger, e);
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "POST",
          Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @PUT
  @Path("relationships/{rId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateRelationship(@PathParam("rId") String rId, String relationship,
      @QueryParam("transactionId") String tId, @Context HttpHeaders headers, @Context UriInfo uriInfo,
      @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    logger.info(ChampMsgs.INCOMING_REQUEST, tId, relationship);

    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      ChampTransaction transaction = champDataService.getTransaction(tId);
      if (tId != null && transaction == null) {
        throw new ChampServiceException("transactionId not found", Status.BAD_REQUEST);
      }
      ChampRelationship r = mapper.readValue(relationship, ChampRelationship.class);
      ChampRelationship updated = champDataService.updateRelationship(r, rId, Optional.ofNullable(transaction));
      EntityTag eTag = new EntityTag(etagGenerator.computeHashForChampRelationship(updated));
      response = Response.status(Status.OK).entity(mapper.writeValueAsString(updated)).tag(eTag).build();
    } catch (IOException e) {
      response = Response.status(Status.BAD_REQUEST).entity("Unable to parse the payload").build();
    } catch (ChampServiceException ce) {
      response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
    } catch (IllegalArgumentException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LoggingUtil.logInternalError(logger, e);
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "PUT", Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @DELETE
  @Path("relationships/{relationshipId}")
  public Response deleteRelationship(@PathParam("relationshipId") String relationshipId,
      @QueryParam("transactionId") String tId, @Context HttpHeaders headers, @Context UriInfo uriInfo,
      @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    logger.info(ChampMsgs.INCOMING_REQUEST, tId, relationshipId);

    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      ChampTransaction transaction = champDataService.getTransaction(tId);
      if (tId != null && transaction == null) {
        throw new ChampServiceException("transactionId not found", Status.BAD_REQUEST);
      }
      champDataService.deleteRelationship(relationshipId, Optional.ofNullable(transaction));
      response = Response.status(Status.OK).build();

    } catch (ChampRelationshipNotExistsException e) {
      response = Response.status(Status.NOT_FOUND).entity(relationshipId + " not found").build();
    } catch (ChampServiceException ce) {
      response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
    } catch (ChampTransactionException | ChampUnmarshallingException e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "DELETE",
          Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @GET
  @Path("relationships/filter/")
  @Produces(MediaType.APPLICATION_JSON)
  public Response filterMethod(@Context HttpHeaders headers, @Context UriInfo uriInfo,
      @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    List<ChampRelationship> champRelationshipList;
    Map<String, Object> filter = new HashMap<>();
    for (Map.Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
      if (!reservedKeyMatcher ( QUERY_OBJECT_ID_URL_MATCH, e.getKey () )) {
        filter.put ( e.getKey (), e.getValue ().get ( 0 ) );
      }
    }
    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      champRelationshipList = champDataService.queryRelationships(filter);
      EntityTag eTag = new EntityTag(etagGenerator.computeHashForChampRelationships(champRelationshipList));
      response = Response.status(Status.OK).type(MediaType.APPLICATION_JSON).tag(eTag).entity(mapper.writeValueAsString(champRelationshipList))
          .build();
    } catch (JsonProcessingException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (ChampServiceException e1) {
      response = Response.status(e1.getHttpStatus()).entity(e1.getMessage()).build();
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LoggingUtil.logInternalError(logger, e);
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "GET", Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @POST
  @Path("transaction")
  @Produces(MediaType.TEXT_PLAIN)
  public Response openTransaction(@Context HttpHeaders headers, @Context UriInfo uriInfo,
      @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      String transaction = champDataService.openTransaction();
      Status s = Status.OK;
      response = Response.status(s).entity(transaction).build();
      logger.info(ChampMsgs.PROCESS_EVENT, "Opened Transaction with ID: " + transaction, s.toString());
    } catch (ChampServiceException e) {
      response = Response.status(e.getHttpStatus()).entity(e.getMessage()).build();
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "POST", Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @GET
  @Path("transaction/{tId}")
  public Response getSpecificTransaction(@PathParam("tId") String tId, @Context HttpHeaders headers,
      @Context UriInfo uriInfo, @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();

    Response response = null;
    ChampTransaction transaction = champDataService.getTransaction(tId);
    if (transaction == null) {
      response = Response.status(Status.NOT_FOUND).entity("transaction " + tId + " not found").build();
      return response;
    }

    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      response = Response.status(Status.OK).entity(mapper.writeValueAsString(tId + " is OPEN")).build();
    } catch (JsonProcessingException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (ChampServiceException e) {
        response = Response.status(e.getHttpStatus()).entity(e.getMessage()).build();
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LoggingUtil.logInternalError(logger, e);
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "GET", Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @PUT
  @Path("transaction/{tId}")
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateTransaction(String t, @PathParam("tId") String tId, @Context HttpHeaders headers,
      @Context UriInfo uriInfo, @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    logger.info(ChampMsgs.INCOMING_REQUEST, tId, "COMMIT/ROLLBACK");

    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      JSONObject jsonObj = new JSONObject(t);
      String method = jsonObj.getString(this.TRANSACTION_METHOD);

      if (method.equals("commit")) {
        champDataService.commitTransaction(tId);
        response = Response.status(Status.OK).entity("COMMITTED").build();

      } else if (method.equals("rollback")) {
        champDataService.rollbackTransaction(tId);
        response = Response.status(Status.OK).entity("ROLLED BACK").build();
      } else {
        response = Response.status(Status.BAD_REQUEST).entity("Invalid Method: " + method).build();
        return response;
      }

    } catch (ChampTransactionException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (JSONException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (ChampServiceException e) {
      response = Response.status(e.getHttpStatus()).entity(e.getMessage()).build();
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LoggingUtil.logInternalError(logger, e);
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "PUT", Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    return response;
  }

  @POST
  @Path("bulk")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postBulk(String bulkPayload, @Context HttpHeaders headers,
      @Context UriInfo uriInfo, @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);
    long startTimeInMs = System.currentTimeMillis();
    logger.info(ChampMsgs.INCOMING_REQUEST, "null", bulkPayload);
    Response response = null;
    try {
      httpHeadersValidator.validateRequestHeaders(headers);
      ChampBulkPayload bulkRequest = ChampBulkPayload.fromJson(bulkPayload);
      ChampBulkResponse bulkResponse = champDataService.processBulkRequest(bulkRequest);
      
      response = Response.status(Status.OK).entity(bulkResponse.toJson()).build();
    } catch (ChampServiceException ce) {
      response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
    } catch (IllegalArgumentException e) {
      response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LoggingUtil.logInternalError(logger, e);
    } finally {
      LoggingUtil.logRestRequest(logger, auditLogger, req, response);
      metricsLogger.info(ChampMsgs.PROCESSED_REQUEST, "POST",
          Long.toString(System.currentTimeMillis() - startTimeInMs));
    }
    
    return response;
  }
  
  private boolean reservedKeyMatcher(Pattern p, String key) {
    Matcher m = p.matcher ( key );
    if (m.matches()) {
      return true;
    } else {
      return false;
    }
  }

}
