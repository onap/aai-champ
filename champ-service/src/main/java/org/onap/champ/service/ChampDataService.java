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
package org.onap.champ.service;

import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.exceptions.ChampMarshallingException;
import org.onap.aai.champcore.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champcore.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champcore.exceptions.ChampSchemaViolationException;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import org.onap.aai.champcore.model.ChampElement;
import org.onap.aai.champcore.model.ChampField;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampObjectIndex;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.fluent.object.ObjectBuildOrPropertiesStep;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.champ.exception.ChampServiceException;
import org.onap.champ.service.logging.ChampMsgs;
import org.onap.champ.util.ChampProperties;
import org.onap.champ.util.ChampServiceConstants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.Response.Status;

public class ChampDataService {
  private ChampUUIDService champUUIDService;

  private ChampGraph graphImpl;
  private ChampTransactionCache cache;
  private static final String KEY_NAME = ChampProperties.get(ChampServiceConstants.CHAMP_KEY_NAME);
  private static final String SOT_NAME = ChampProperties.get(ChampServiceConstants.CHAMP_SOT_NAME);
  private static final String CREATED_TS_NAME = ChampProperties.get(ChampServiceConstants.CHAMP_CREATED_TS_NAME);
  private static final String LAST_MOD_TS_NAME = ChampProperties.get(ChampServiceConstants.CHAMP_LAST_MOD_TS_NAME);
  private Logger logger = LoggerFactory.getInstance().getLogger(ChampDataService.class);


  public ChampDataService(ChampUUIDService champUUIDService, ChampGraph graphImpl, ChampTransactionCache cache) {

    this.champUUIDService = champUUIDService;
    this.graphImpl = graphImpl;

    ChampField field = new ChampField.Builder(ChampProperties.get("keyName"))
        .type(ChampField.Type.STRING)
        .build();
    ChampObjectIndex index = new ChampObjectIndex.Builder(ChampProperties.get("keyName"), "STRING", field).build();

    graphImpl.storeObjectIndex(index);

    this.cache = cache;
  }

  public ChampObject getObject(String id, Optional<ChampTransaction> transaction) throws ChampServiceException {

    Optional<ChampObject> retrieved = Optional.empty();
    try {
      retrieved = champUUIDService.getObjectbyUUID(id, transaction.orElse(null));
    } catch (ChampUnmarshallingException | ChampTransactionException e) {
      throw new ChampServiceException("Error: " + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
    }
    if (retrieved.isPresent()) {
      return (ChampObject) champUUIDService.populateUUIDKey(retrieved.get());
    } else {
      return null;
    }
  }

  public ChampObject storeObject(ChampObject object, Optional<ChampTransaction> transaction)
      throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException,
      ChampTransactionException, ChampServiceException {

    if (object.getProperty(KEY_NAME).isPresent() || object.getKey().isPresent()) {
      throw new ChampServiceException(KEY_NAME + " can't be updated", Status.BAD_REQUEST);
    }

    champUUIDService.populateUUIDProperty(object, java.util.UUID.randomUUID().toString());
    addTimestamps(object, null);
    ChampObject created = graphImpl.storeObject(object, transaction);
    return (ChampObject) champUUIDService.populateUUIDKey(created);
  }

  public ChampObject replaceObject(ChampObject object, String objectId, Optional<ChampTransaction> transaction)
      throws ChampServiceException, ChampUnmarshallingException, ChampTransactionException, ChampMarshallingException,
      ChampSchemaViolationException, ChampObjectNotExistsException {
    if (object.getKey().isPresent() && (!object.getKeyValue().equals(objectId))) {
      throw new ChampServiceException("Object Id in the URI doesn't match the body.", Status.BAD_REQUEST);
    }

    if (object.getProperty(KEY_NAME).isPresent() && !object.getProperty(KEY_NAME).get().toString().equals(objectId)) {
      throw new ChampServiceException(KEY_NAME + " can't be updated", Status.BAD_REQUEST);
    }

    Optional<ChampObject> retrieved = champUUIDService.getObjectbyUUID(objectId, transaction.orElse(null));
    if (!retrieved.isPresent()) {
      throw new ChampServiceException(objectId + " not found", Status.NOT_FOUND);
    }
    ObjectBuildOrPropertiesStep payloadBuilder = ChampObject.create().from(object).withKey(retrieved.get().getKey().get())
        .withProperty(KEY_NAME, objectId);
    if (retrieved.get().getProperty(SOT_NAME).isPresent()){
      payloadBuilder = payloadBuilder.withProperty(SOT_NAME, retrieved.get().getProperty(SOT_NAME).get());
    }

    if (object.getProperty(CREATED_TS_NAME).isPresent() && retrieved.get().getProperty(CREATED_TS_NAME).isPresent()) {
      // the timestamps in object are parsed as strings regardless of how the input json is. Convert retrieved to string for easy comparison
      if (!retrieved.get().getProperty(CREATED_TS_NAME).get().toString().equals(object.getProperty(CREATED_TS_NAME).get())) {
        throw new ChampServiceException(CREATED_TS_NAME + " can't be updated", Status.BAD_REQUEST);
      }
    }

    if (object.getProperty(LAST_MOD_TS_NAME).isPresent() && retrieved.get().getProperty(LAST_MOD_TS_NAME).isPresent()) {
      if (!retrieved.get().getProperty(LAST_MOD_TS_NAME).get().toString().equals(object.getProperty(LAST_MOD_TS_NAME).get())) {
        throw new ChampServiceException(LAST_MOD_TS_NAME + " can't be updated", Status.BAD_REQUEST);
      }
    }

    ChampObject payload = payloadBuilder.build();
    addTimestamps(payload, (Long)retrieved.get().getProperty(CREATED_TS_NAME).orElse(null));
    ChampObject updated = graphImpl.replaceObject(payload, transaction);
    return (ChampObject) champUUIDService.populateUUIDKey(updated);
  }

  public void deleteObject(String objectId, Optional<ChampTransaction> transaction) throws ChampServiceException,
      ChampObjectNotExistsException, ChampTransactionException, ChampUnmarshallingException {
    Optional<ChampObject> retrieved = champUUIDService.getObjectbyUUID(objectId, transaction.orElse(null));
    if (!retrieved.isPresent()) {
      throw new ChampServiceException(objectId + " not found", Status.NOT_FOUND);
    }
    Stream<ChampRelationship> relationships = graphImpl.retrieveRelationships(retrieved.get(), transaction);

    if (relationships.count() > 0) {
      throw new ChampServiceException("Attempt to delete vertex with id " + objectId + " which has incident edges.",
          Status.BAD_REQUEST);
    }
    graphImpl.deleteObject(retrieved.get().getKey().get(), transaction);

  }

  public ChampRelationship storeRelationship(ChampRelationship r, Optional<ChampTransaction> transaction)
      throws ChampMarshallingException, ChampObjectNotExistsException, ChampSchemaViolationException,
      ChampRelationshipNotExistsException, ChampUnmarshallingException, ChampTransactionException,
      ChampServiceException {

    if (r.getSource() == null || !r.getSource().getKey().isPresent() || r.getTarget() == null
        || !r.getTarget().getKey().isPresent()) {
      logger.error(ChampMsgs.CHAMP_DATA_SERVICE_ERROR, "Source/Target Object key must be provided");
      throw new ChampServiceException("Source/Target Object key must be provided", Status.BAD_REQUEST);
    }

    if (r.getProperty(KEY_NAME).isPresent() || r.getKey().isPresent()) {
      logger.error(ChampMsgs.CHAMP_DATA_SERVICE_ERROR, "key or " + KEY_NAME + " not allowed while creating new Objects");
      throw new ChampServiceException("key or " + KEY_NAME + " not allowed while creating new Objects", Status.BAD_REQUEST);

    }

    Optional<ChampObject> source = champUUIDService.getObjectbyUUID(r.getSource().getKey().get().toString(),
        transaction.orElse(null));
    Optional<ChampObject> target = champUUIDService.getObjectbyUUID(r.getTarget().getKey().get().toString(),
        transaction.orElse(null));

    if (!source.isPresent() || !target.isPresent()) {
      logger.error(ChampMsgs.CHAMP_DATA_SERVICE_ERROR, "Source/Target object not found");
      throw new ChampServiceException("Source/Target object not found", Status.BAD_REQUEST);
    }

    champUUIDService.populateUUIDProperty(r, java.util.UUID.randomUUID().toString());

    ChampRelationship payload = new ChampRelationship.Builder(source.get(), target.get(), r.getType())
        .properties(r.getProperties()).build();
    addTimestamps(payload, null);
    ChampRelationship created = graphImpl.storeRelationship(payload, transaction);
    return (ChampRelationship) champUUIDService.populateUUIDKey(created);
  }

  public ChampRelationship updateRelationship(ChampRelationship r, String rId, Optional<ChampTransaction> transaction)
      throws ChampServiceException, ChampUnmarshallingException, ChampTransactionException, ChampMarshallingException,
      ChampSchemaViolationException, ChampRelationshipNotExistsException {
    if (r.getKey().isPresent() && (!r.getKeyValue().equals(rId))) {

      throw new ChampServiceException("Relationship Id in the URI \"" + rId + "\" doesn't match the URI in the body"
          + " \"" + r.getKeyValue() + "\"", Status.BAD_REQUEST);

    }

    if (r.getProperty(KEY_NAME).isPresent() && !r.getProperty(KEY_NAME).get().toString().equals(rId)) {
      throw new ChampServiceException(KEY_NAME + " can't be updated", Status.BAD_REQUEST);
    }

    Optional<ChampRelationship> retrieved = champUUIDService.getRelationshipbyUUID(rId, transaction.orElse(null));
    if (!retrieved.isPresent()) {
      throw new ChampServiceException(rId + " not found", Status.NOT_FOUND);
    }
    // check if key is present or if it equals the key that is in the URI
    if (r.getSource() == null || !r.getSource().getKey().isPresent() || r.getTarget() == null
        || !r.getTarget().getKey().isPresent()) {
      throw new ChampServiceException("Source/Target Object key must be provided", Status.BAD_REQUEST);
    }
    ChampObject source = retrieved.get().getSource();
    ChampObject target = retrieved.get().getTarget();

    if (!source.getProperty(KEY_NAME).get().toString().equals(r.getSource().getKey().get().toString())
        || !target.getProperty(KEY_NAME).get().toString().equals(r.getTarget().getKey().get().toString())) {
      throw new ChampServiceException("Source/Target cannot be updated", Status.BAD_REQUEST);
    }

    if (r.getProperty(CREATED_TS_NAME).isPresent() && retrieved.get().getProperty(CREATED_TS_NAME).isPresent()) {
      if (!retrieved.get().getProperty(CREATED_TS_NAME).get().toString().equals(r.getProperty(CREATED_TS_NAME).get())) {
        throw new ChampServiceException(CREATED_TS_NAME + " can't be updated", Status.BAD_REQUEST);
      }
    }

    if (r.getProperty(LAST_MOD_TS_NAME).isPresent() && retrieved.get().getProperty(LAST_MOD_TS_NAME).isPresent()) {
      if (!retrieved.get().getProperty(LAST_MOD_TS_NAME).get().toString().equals(r.getProperty(LAST_MOD_TS_NAME).get())) {
        throw new ChampServiceException(LAST_MOD_TS_NAME + " can't be updated", Status.BAD_REQUEST);
      }
    }

    ChampRelationship payload = new ChampRelationship.Builder(source, target, r.getType())
        .key(retrieved.get().getKey().get()).properties(r.getProperties()).property(KEY_NAME, rId).build();
    addTimestamps(payload, (Long)retrieved.get().getProperty(CREATED_TS_NAME).orElse(null));
    ChampRelationship updated = graphImpl.replaceRelationship(payload, transaction);
    return (ChampRelationship) champUUIDService.populateUUIDKey(updated);
  }

  public void deleteRelationship(String relationshipId, Optional<ChampTransaction> transaction)
      throws ChampServiceException, ChampRelationshipNotExistsException, ChampTransactionException,
      ChampUnmarshallingException {
    Optional<ChampRelationship> retrieved = champUUIDService.getRelationshipbyUUID(relationshipId,
        transaction.orElse(null));
    if (!retrieved.isPresent()) {
      throw new ChampServiceException(relationshipId + " not found", Status.NOT_FOUND);
    }

    graphImpl.deleteRelationship(retrieved.get(), transaction);

  }


  public List<ChampRelationship> getRelationshipsByObject(String objectId, Optional<ChampTransaction> transaction)
      throws ChampServiceException {
    try {
      Optional<ChampObject> retrievedObject = champUUIDService.getObjectbyUUID(objectId, transaction.orElse(null));
      if (!retrievedObject.isPresent()) {
        throw new ChampServiceException(objectId + " not found", Status.NOT_FOUND);
      }
      List<ChampRelationship> relations = new ArrayList<ChampRelationship>();

      Stream<ChampRelationship> retrieved = graphImpl.retrieveRelationships(retrievedObject.get(), transaction);
      relations = champUUIDService.populateUUIDKey(retrieved.collect(Collectors.toList()));
      return relations;
    } catch (ChampObjectNotExistsException e) {
      throw new ChampServiceException(" obj not found", Status.NOT_FOUND);
    } catch (ChampUnmarshallingException | ChampTransactionException e) {
      throw new ChampServiceException("Internal Error", Status.INTERNAL_SERVER_ERROR);
    }

  }

  /**
   * Gets the ChampObjects that pass filter
   * @param filter key/value pairs that must be present in the returned objects
   * @param properties properties that will show up in the object
   * @return
   * @throws ChampServiceException
   */
  public List<ChampObject> queryObjects(Map<String, Object> filter, HashSet<String> properties) throws ChampServiceException {
    try {

      Stream<ChampObject> retrieved = graphImpl.queryObjects(filter);
      List<ChampObject> objects = champUUIDService.populateUUIDKey(retrieved.collect(Collectors.toList()));

      if (!properties.contains("all")) {
        for (ChampObject champObject : objects) {
          champObject.dropProperties(properties);
        }
      }

      return objects;
    } catch (ChampTransactionException e) {
      throw new ChampServiceException("Internal Error", Status.INTERNAL_SERVER_ERROR);
    }
  }

  public List<ChampRelationship> queryRelationships(Map<String, Object> filter) throws ChampServiceException {
    try {
      List<ChampRelationship> relations = new ArrayList<ChampRelationship>();
      Stream<ChampRelationship> retrieved;

      retrieved = graphImpl.queryRelationships(filter);

      relations = champUUIDService.populateUUIDKey(retrieved.collect(Collectors.toList()));
      return relations;
    } catch (ChampTransactionException e) {
      throw new ChampServiceException("Internal Error", Status.INTERNAL_SERVER_ERROR);
    }
  }

  public ChampRelationship getRelationship(String id, Optional<ChampTransaction> transaction)
      throws ChampServiceException {

    Optional<ChampRelationship> retrieved = Optional.empty();
    try {
      retrieved = champUUIDService.getRelationshipbyUUID(id, transaction.orElse(null));
    } catch (ChampUnmarshallingException | ChampTransactionException e) {
      throw new ChampServiceException("Error: " + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
    }
    if (retrieved.isPresent()) {
      return (ChampRelationship) champUUIDService.populateUUIDKey(retrieved.get());
    } else {
      return null;
    }
  }

  public String openTransaction() {
    ChampTransaction transaction = graphImpl.openTransaction();
    String transacId = transaction.id();
    cache.put(transacId, transaction);
    return transacId;

  }

  public void commitTransaction(String tId) throws ChampServiceException, ChampTransactionException {
    ChampTransaction transaction = cache.get(tId);
    if (transaction == null) {
      throw new ChampServiceException("Transaction Not found: " + tId, Status.NOT_FOUND);
    }
    graphImpl.commitTransaction(transaction);
    cache.invalidate(tId);
    cache.invalidate(transaction.id());

  }

  public void rollbackTransaction(String tId) throws ChampServiceException, ChampTransactionException {
    ChampTransaction transaction = cache.get(tId);
    if (transaction == null) {
      throw new ChampServiceException("Transaction Not found: " + tId, Status.NOT_FOUND);
    }
    graphImpl.rollbackTransaction(transaction);
    cache.invalidate(tId);
    cache.invalidate(transaction.id());

  }

  public ChampTransaction getTransaction(String id) {
    return cache.get(id);
  }

  private void addTimestamps(ChampElement e, Long oldCreated) {
    Long timestamp = System.currentTimeMillis();

    if (oldCreated == null) {
      e.getProperties().put(CREATED_TS_NAME, timestamp);
    } else {
      e.getProperties().put(CREATED_TS_NAME, oldCreated);
    }

    e.getProperties().put(LAST_MOD_TS_NAME, timestamp);
  }
}
