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
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 */
package org.onap.aai.champjanus.graph.impl;

import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.EdgeLabel;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphEdge;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraphVertex;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.SchemaViolationException;
import org.janusgraph.core.schema.JanusGraphIndex;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.JanusGraphManagement.IndexBuilder;
import org.janusgraph.core.schema.SchemaAction;
import org.janusgraph.core.schema.SchemaStatus;
import org.janusgraph.graphdb.database.management.ManagementSystem;
import org.onap.aai.champcore.ChampCapabilities;
import org.onap.aai.champcore.exceptions.ChampIndexNotExistsException;
import org.onap.aai.champcore.exceptions.ChampSchemaViolationException;
import org.onap.aai.champcore.graph.impl.AbstractTinkerpopChampGraph;
import org.onap.aai.champcore.model.ChampCardinality;
import org.onap.aai.champcore.model.ChampField;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampObjectConstraint;
import org.onap.aai.champcore.model.ChampObjectIndex;
import org.onap.aai.champcore.model.ChampPropertyConstraint;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.ChampRelationshipConstraint;
import org.onap.aai.champcore.model.ChampRelationshipIndex;
import org.onap.aai.champcore.model.ChampSchema;
import org.onap.aai.champcore.schema.ChampSchemaEnforcer;
import org.onap.aai.champcore.schema.DefaultChampSchemaEnforcer;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;

public final class JanusChampGraphImpl extends AbstractTinkerpopChampGraph {
  private static final Logger LOGGER = LoggerFactory.getInstance().getLogger(JanusChampGraphImpl.class);
  private static final String JANUS_CASSANDRA_KEYSPACE = "storage.cassandra.keyspace";
  private static final String JANUS_CQL_KEYSPACE = "storage.cql.keyspace";
  private static final String JANUS_HBASE_TABLE = "storage.hbase.table";
  private static final String JANUS_UNIQUE_SUFFIX = "graph.unique-instance-id-suffix";
  private static final ChampSchemaEnforcer SCHEMA_ENFORCER = new DefaultChampSchemaEnforcer();
  private static final int REGISTER_OBJECT_INDEX_TIMEOUT_SECS = 45;

  private static final ChampCapabilities CAPABILITIES = new ChampCapabilities() {

    @Override
    public boolean canDeleteObjectIndices() {
      return false;
    }

    @Override
    public boolean canDeleteRelationshipIndices() {
      return false;
    }
  };

  private JanusGraph graph;
  private final JanusGraphFactory.Builder janusGraphBuilder;

  public JanusChampGraphImpl(Builder builder) {
    super(builder.graphConfiguration);
    janusGraphBuilder = JanusGraphFactory.build();

    for (Map.Entry<String, Object> janusGraphProperty : builder.graphConfiguration.entrySet()) {
      janusGraphBuilder.set(janusGraphProperty.getKey(), janusGraphProperty.getValue());
    }
    
    janusGraphBuilder.set(JANUS_UNIQUE_SUFFIX, ((short) new SecureRandom().nextInt(Short.MAX_VALUE)+""));

    final Object storageBackend = builder.graphConfiguration.get("storage.backend");

    if ("cassandra".equals(storageBackend) ||
        "cassandrathrift".equals(storageBackend) ||
        "astyanax".equals(storageBackend) ||
        "embeddedcassandra".equals(storageBackend)) {

      janusGraphBuilder.set(JANUS_CASSANDRA_KEYSPACE, builder.graphName);
    } else if("cql".equals(storageBackend)){
      janusGraphBuilder.set(JANUS_CQL_KEYSPACE, builder.graphName);
    } else if ("hbase".equals(storageBackend)) {
      janusGraphBuilder.set(JANUS_HBASE_TABLE, builder.graphName);
    } else if ("berkleyje".equals(storageBackend)) {
      throw new RuntimeException("storage.backend=berkleyje cannot handle multiple graphs on a single DB, not usable");
    } else if ("inmemory".equals(storageBackend)) {
    } else {
      throw new RuntimeException("Unknown storage.backend=" + storageBackend);
    }
    
    try {
      openGraph();
    }
    catch (Exception ex) {
      // Swallow exception.  Cassandra may not be reachable.  Will retry next time we need to use the graph.
      LOGGER.error(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_ERROR,
          "Error opening graph: " + ex.getMessage());
      return;
    }
    
    LOGGER.info(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_INFO,
        "Instantiated data access layer for Janus graph data store with backend: " + storageBackend);
  }

  public static class Builder {
    private final String graphName;

    private final Map<String, Object> graphConfiguration = new HashMap<String, Object>();

    public Builder(String graphName) {
      this.graphName = graphName;
    }
    
    public Builder(String graphName, Map<String, Object> properties) {
        this.graphName = graphName;
        properties(properties);
    }

    public Builder properties(Map<String, Object> properties) {
      if (properties.containsKey(JANUS_CASSANDRA_KEYSPACE)) {
        throw new IllegalArgumentException("Cannot use path " + JANUS_CASSANDRA_KEYSPACE
            + " in initial configuration - this path is used"
            + " to specify graph names");
      }

      this.graphConfiguration.putAll(properties);
      return this;
    }

    public Builder property(String path, Object value) {
      if (path.equals(JANUS_CASSANDRA_KEYSPACE)) {
        throw new IllegalArgumentException("Cannot use path " + JANUS_CASSANDRA_KEYSPACE
            + " in initial configuration - this path is used"
            + " to specify graph names");
      }
      graphConfiguration.put(path, value);
      return this;
    }

    public JanusChampGraphImpl build() {
      return new JanusChampGraphImpl(this);
    }
  }

  @Override
  protected JanusGraph getGraph() {
    if (graph == null) {
      openGraph();
    }
    return graph;
  }

 
  @Override
  protected ChampSchemaEnforcer getSchemaEnforcer() {
    return SCHEMA_ENFORCER;
  }

  @Override
  public void executeStoreObjectIndex(ChampObjectIndex index) {
    if (isShutdown()) {
      throw new IllegalStateException("Cannot call storeObjectIndex() after shutdown has been initiated");
    }

    final JanusGraph graph = getGraph();
    final JanusGraphManagement createIndexMgmt = graph.openManagement();

    if (createIndexMgmt.getGraphIndex(index.getName()) != null) {
      createIndexMgmt.rollback();
      LOGGER.info(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_INFO,
          "Index " + index.getName() + " already exists");
      return; //Ignore, index already exists
    }

    LOGGER.info(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_INFO,
        "Create index " + index.getName());
    IndexBuilder ib = createIndexMgmt.buildIndex(index.getName(), Vertex.class);
    for (ChampField field : index.getFields()) {
      PropertyKey pk = createIndexMgmt.getOrCreatePropertyKey(field.getName());
      ib = ib.addKey(pk);
    }
    ib.buildCompositeIndex();

    createIndexMgmt.commit();
    graph.tx().commit();

    awaitIndexCreation(index.getName());
  }

  @Override
  public Optional<ChampObjectIndex> retrieveObjectIndex(String indexName) {
    if (isShutdown()) {
      throw new IllegalStateException("Cannot call retrieveObjectIndex() after shutdown has been initiated");
    }

    final JanusGraphManagement retrieveIndexMgmt = getGraph().openManagement();
    final JanusGraphIndex index = retrieveIndexMgmt.getGraphIndex(indexName);

    if (index == null) {
      return Optional.empty();
    }
    if (index.getIndexedElement() != JanusGraphVertex.class) {
      return Optional.empty();
    }

    List<String> fieldNames = new ArrayList<String>();
    for (int i = 0; i < index.getFieldKeys().length; i++) {
      fieldNames.add(index.getFieldKeys()[i].name());
    }
    
    return Optional.of(ChampObjectIndex.create()
        .ofName(indexName)
        .onType(ChampObject.ReservedTypes.ANY.toString())
        .forFields(fieldNames)
        .build());
  }

  @Override
  public Stream<ChampObjectIndex> retrieveObjectIndices() {
    if (isShutdown()) {
      throw new IllegalStateException("Cannot call retrieveObjectIndices() after shutdown has been initiated");
    }

    final JanusGraphManagement createIndexMgmt = getGraph().openManagement();
    final Iterator<JanusGraphIndex> indices = createIndexMgmt.getGraphIndexes(Vertex.class).iterator();

    final Iterator<ChampObjectIndex> objIter = new Iterator<ChampObjectIndex>() {

      private ChampObjectIndex next;

      @Override
      public boolean hasNext() {
        if (indices.hasNext()) {
          final JanusGraphIndex index = indices.next();

          List<String> fieldNames = new ArrayList<String>();
          for (int i = 0; i < index.getFieldKeys().length; i++) {
            fieldNames.add(index.getFieldKeys()[i].name());
          }
          
          next = ChampObjectIndex.create()
              .ofName(index.name())
              .onType(ChampObject.ReservedTypes.ANY.toString())
              .forFields(fieldNames)
              .build();
          return true;
        }

        next = null;
        return false;
      }

      @Override
      public ChampObjectIndex next() {
        if (next == null) {
          throw new NoSuchElementException();
        }

        return next;
      }
    };

    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
        objIter, Spliterator.ORDERED | Spliterator.NONNULL), false);
  }

  @Override
  public void executeDeleteObjectIndex(String indexName) throws ChampIndexNotExistsException {
    if (isShutdown()) {
      throw new IllegalStateException("Cannot call deleteObjectIndex() after shutdown has been initiated");
    }

    throw new UnsupportedOperationException("Cannot delete indices using the JanusChampImpl");
  }

  @Override
  public void executeStoreRelationshipIndex(ChampRelationshipIndex index) {
    if (isShutdown()) {
      throw new IllegalStateException("Cannot call storeRelationshipIndex() after shutdown has been initiated");
    }

    final JanusGraph graph = getGraph();
    final JanusGraphManagement createIndexMgmt = graph.openManagement();
    final PropertyKey pk = createIndexMgmt.getOrCreatePropertyKey(index.getField().getName());

    if (createIndexMgmt.getGraphIndex(index.getName()) != null) {
      return; //Ignore, index already exists
    }
    
    LOGGER.info(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_INFO,
        "Create edge index " + index.getName());
    createIndexMgmt.buildIndex(index.getName(), Edge.class).addKey(pk).buildCompositeIndex();

    createIndexMgmt.commit();
    graph.tx().commit();

    awaitIndexCreation(index.getName());
  }

  @Override
  public Optional<ChampRelationshipIndex> retrieveRelationshipIndex(String indexName) {
    if (isShutdown()) {
      throw new IllegalStateException("Cannot call retrieveRelationshipIndex() after shutdown has been initiated");
    }

    final JanusGraphManagement retrieveIndexMgmt = getGraph().openManagement();
    final JanusGraphIndex index = retrieveIndexMgmt.getGraphIndex(indexName);

    if (index == null) {
      return Optional.empty();
    }
    if (index.getIndexedElement() != JanusGraphEdge.class) {
      return Optional.empty();
    }

    return Optional.of(ChampRelationshipIndex.create()
        .ofName(indexName)
        .onType(ChampObject.ReservedTypes.ANY.toString())
        .forField(index.getFieldKeys()[0].name())
        .build());
  }

  @Override
  public Stream<ChampRelationshipIndex> retrieveRelationshipIndices() {
    if (isShutdown()) {
      throw new IllegalStateException("Cannot call retrieveRelationshipIndices() after shutdown has been initiated");
    }

    final JanusGraphManagement createIndexMgmt = getGraph().openManagement();
    final Iterator<JanusGraphIndex> indices = createIndexMgmt.getGraphIndexes(Edge.class).iterator();

    final Iterator<ChampRelationshipIndex> objIter = new Iterator<ChampRelationshipIndex>() {

      private ChampRelationshipIndex next;

      @Override
      public boolean hasNext() {
        if (indices.hasNext()) {
          final JanusGraphIndex index = indices.next();

          next = ChampRelationshipIndex.create()
              .ofName(index.name())
              .onType(ChampRelationship.ReservedTypes.ANY.toString())
              .forField(index.getFieldKeys()[0].name())
              .build();
          return true;
        }

        next = null;
        return false;
      }

      @Override
      public ChampRelationshipIndex next() {
        if (next == null) {
          throw new NoSuchElementException();
        }

        return next;
      }
    };

    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
        objIter, Spliterator.ORDERED | Spliterator.NONNULL), false);
  }

  @Override
  public void executeDeleteRelationshipIndex(String indexName) throws ChampIndexNotExistsException {
    if (isShutdown()) {
      throw new IllegalStateException("Cannot call deleteRelationshipIndex() after shutdown has been initiated");
    }

    throw new UnsupportedOperationException("Cannot delete indices using the JanusChampImpl");
  }

  private Cardinality getJanusCardinality(ChampCardinality cardinality) {
    switch (cardinality) {
      case LIST:
        return Cardinality.LIST;
      case SET:
        return Cardinality.SET;
      case SINGLE:
        return Cardinality.SINGLE;
      default:
        throw new RuntimeException("Unknown ChampCardinality " + cardinality);
    }
  }

  private void awaitIndexCreation(String indexName) {
    //Wait for the index to become available
    try {
      if (ManagementSystem.awaitGraphIndexStatus(graph, indexName)
          .status(SchemaStatus.ENABLED)
          .timeout(1, ChronoUnit.SECONDS)
          .call()
          .getSucceeded()) {
        return; //Empty graphs immediately ENABLE indices
      }

      if (!ManagementSystem.awaitGraphIndexStatus(graph, indexName)
          .status(SchemaStatus.REGISTERED)
          .timeout(REGISTER_OBJECT_INDEX_TIMEOUT_SECS, ChronoUnit.SECONDS)
          .call()
          .getSucceeded()) {
        LOGGER.warn(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_WARN,
            "Object index was created, but timed out while waiting for it to be registered");
        return;
      }
    } catch (InterruptedException e) {
      LOGGER.warn(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_WARN,
          "Interrupted while waiting for object index creation status");
      Thread.currentThread().interrupt();
      return;
    }

    //Reindex the existing data

    try {
      final JanusGraphManagement updateIndexMgmt = graph.openManagement();
      updateIndexMgmt.updateIndex(updateIndexMgmt.getGraphIndex(indexName), SchemaAction.REINDEX).get();
      updateIndexMgmt.commit();
    } catch (InterruptedException e) {
      LOGGER.warn(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_WARN,
          "Interrupted while reindexing for object index");
      Thread.currentThread().interrupt();
      return;
    } catch (ExecutionException e) {
      LOGGER.warn(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_WARN,
          "Exception occurred during reindexing procedure for creating object index " + indexName + ". " + e.getMessage());
    }

    try {
      ManagementSystem.awaitGraphIndexStatus(graph, indexName)
          .status(SchemaStatus.ENABLED)
          .timeout(2, ChronoUnit.MINUTES)
          .call();
    } catch (InterruptedException e) {
      LOGGER.warn(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_WARN,
          "Interrupted while waiting for index to transition to ENABLED state");
      Thread.currentThread().interrupt();
      return;
    }
  }

  @Override
  public ChampCapabilities capabilities() {
    return CAPABILITIES;
  }

  public void storeSchema(ChampSchema schema) throws ChampSchemaViolationException {
    if (isShutdown()) throw new IllegalStateException("Cannot call storeSchema() after shutdown has been initiated");

    final ChampSchema currentSchema = retrieveSchema();
    final JanusGraphManagement mgmt = getGraph().openManagement();

    try {
      for (ChampObjectConstraint objConstraint : schema.getObjectConstraints().values()) {
        for (ChampPropertyConstraint propConstraint : objConstraint.getPropertyConstraints()) {
          final Optional<ChampObjectConstraint> currentObjConstraint = currentSchema.getObjectConstraint(objConstraint.getType());

          if (currentObjConstraint.isPresent()) {
            final Optional<ChampPropertyConstraint> currentPropConstraint = currentObjConstraint.get().getPropertyConstraint(propConstraint.getField().getName());

            if (currentPropConstraint.isPresent() && currentPropConstraint.get().compareTo(propConstraint) != 0) {
              throw new ChampSchemaViolationException("Cannot update already existing property on object type " + objConstraint.getType() + ": " + propConstraint);
            }
          }

          final String newPropertyKeyName = propConstraint.getField().getName();

          if (mgmt.getPropertyKey(newPropertyKeyName) != null) continue; //Check Janus to see if another node created this property key

          mgmt.makePropertyKey(newPropertyKeyName)
              .dataType(propConstraint.getField().getJavaType())
              .cardinality(getJanusCardinality(propConstraint.getCardinality()))
              .make();
        }
      }

      for (ChampRelationshipConstraint relConstraint : schema.getRelationshipConstraints().values()) {

        final Optional<ChampRelationshipConstraint> currentRelConstraint = currentSchema.getRelationshipConstraint(relConstraint.getType());

        for (ChampPropertyConstraint propConstraint : relConstraint.getPropertyConstraints()) {

          if (currentRelConstraint.isPresent()) {
            final Optional<ChampPropertyConstraint> currentPropConstraint = currentRelConstraint.get().getPropertyConstraint(propConstraint.getField().getName());

            if (currentPropConstraint.isPresent() && currentPropConstraint.get().compareTo(propConstraint) != 0) {
              throw new ChampSchemaViolationException("Cannot update already existing property on relationship type " + relConstraint.getType());
            }
          }

          final String newPropertyKeyName = propConstraint.getField().getName();

          if (mgmt.getPropertyKey(newPropertyKeyName) != null) continue; //Check Janus to see if another node created this property key

          mgmt.makePropertyKey(newPropertyKeyName)
              .dataType(propConstraint.getField().getJavaType())
              .cardinality(getJanusCardinality(propConstraint.getCardinality()))
              .make();
        }

        final EdgeLabel edgeLabel = mgmt.getEdgeLabel(relConstraint.getType());

        if (edgeLabel != null) {
          mgmt.makeEdgeLabel(relConstraint.getType())
              .directed()
              .make();
        }
      }

      mgmt.commit();

      super.storeSchema(schema);
    } catch (SchemaViolationException | ChampSchemaViolationException e) {
      mgmt.rollback();
      throw new ChampSchemaViolationException(e);
    }
  }
  
  private synchronized void openGraph() {
    if (graph == null) {
      graph = janusGraphBuilder.open();
    }
  }
  
  public GraphTraversal<?, ?> hasLabel(GraphTraversal<?, ?> query, Object type) {
    return query.hasLabel(type);
  }


  @Override
  public void createDefaultIndexes() {
    if (isShutdown()) {
      throw new IllegalStateException("Cannot call storeObjectIndex() after shutdown has been initiated");
    }

    final String EDGE_IX_NAME = "rel-key-uuid";
    
    final JanusGraph graph = getGraph();
    JanusGraphManagement createIndexMgmt = graph.openManagement();
    
    boolean vertexIndexExists = (createIndexMgmt.getGraphIndex(KEY_PROPERTY_NAME) != null);
    boolean edgeIndexExists = (createIndexMgmt.getGraphIndex(EDGE_IX_NAME) != null);
    boolean nodeTypeIndexExists = (createIndexMgmt.getGraphIndex(NODE_TYPE_PROPERTY_NAME) != null);
    
    if (!vertexIndexExists || !edgeIndexExists) {
      PropertyKey pk = createIndexMgmt.getOrCreatePropertyKey(KEY_PROPERTY_NAME);
      
      if (!vertexIndexExists) {
        LOGGER.info(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_INFO,
            "Create Index " + KEY_PROPERTY_NAME);
        createIndexMgmt.buildIndex(KEY_PROPERTY_NAME, Vertex.class).addKey(pk).buildCompositeIndex();
      }
      if (!edgeIndexExists) {
        LOGGER.info(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_INFO,
            "Create Index " + EDGE_IX_NAME);
        createIndexMgmt.buildIndex(EDGE_IX_NAME, Edge.class).addKey(pk).buildCompositeIndex();
      }
      createIndexMgmt.commit();

      if (!vertexIndexExists) {
        awaitIndexCreation(KEY_PROPERTY_NAME);
      }
      if (!edgeIndexExists) {
        awaitIndexCreation(EDGE_IX_NAME);
      }
    }
    else {
      createIndexMgmt.rollback();
      LOGGER.info(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_INFO,
          "Index " + KEY_PROPERTY_NAME + " and " + EDGE_IX_NAME + " already exist");
    }
    
    
    
    if (!nodeTypeIndexExists) {
      LOGGER.info(ChampJanusMsgs.JANUS_CHAMP_GRAPH_IMPL_INFO,
          "Create Index " + NODE_TYPE_PROPERTY_NAME);
      createIndexMgmt = graph.openManagement();
      PropertyKey pk = createIndexMgmt.getOrCreatePropertyKey(NODE_TYPE_PROPERTY_NAME);
      createIndexMgmt.buildIndex(NODE_TYPE_PROPERTY_NAME, Vertex.class).addKey(pk).buildCompositeIndex();
      createIndexMgmt.commit();
      awaitIndexCreation(NODE_TYPE_PROPERTY_NAME);
    }    
  }
}