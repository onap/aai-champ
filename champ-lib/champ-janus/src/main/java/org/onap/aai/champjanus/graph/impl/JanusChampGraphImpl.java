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

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.*;
import org.onap.aai.champcore.Formatter;
import org.janusgraph.core.schema.JanusGraphIndex;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.SchemaAction;
import org.janusgraph.core.schema.SchemaStatus;
import org.janusgraph.graphdb.database.management.ManagementSystem;
import org.onap.aai.champcore.ChampCapabilities;
import org.onap.aai.champcore.FormatMapper;
import org.onap.aai.champcore.exceptions.ChampIndexNotExistsException;
import org.onap.aai.champcore.exceptions.ChampSchemaViolationException;
import org.onap.aai.champcore.graph.impl.AbstractTinkerpopChampGraph;
import org.onap.aai.champcore.model.*;
import org.onap.aai.champcore.schema.ChampSchemaEnforcer;
import org.onap.aai.champcore.schema.DefaultChampSchemaEnforcer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class JanusChampGraphImpl extends AbstractTinkerpopChampGraph {
  private static final Logger LOGGER = LoggerFactory.getLogger(JanusChampGraphImpl.class);
  private static final String JANUS_CASSANDRA_KEYSPACE = "storage.cassandra.keyspace";
  private static final String JANUS_HBASE_TABLE = "storage.hbase.table";
  private static final String JANUS_UNIQUE_SUFFIX = "graph.unique-instance-id-suffix";
  private static final ChampSchemaEnforcer SCHEMA_ENFORCER = new DefaultChampSchemaEnforcer();
  private static final int REGISTER_OBJECT_INDEX_TIMEOUT_SECS = 30;

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

  private final JanusGraph graph;

  public JanusChampGraphImpl(Builder builder) {
    super(builder.graphConfiguration);
    final JanusGraphFactory.Builder janusGraphBuilder = JanusGraphFactory.build();

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
    } else if ("hbase".equals(storageBackend)) {
      janusGraphBuilder.set(JANUS_HBASE_TABLE, builder.graphName);
    } else if ("berkleyje".equals(storageBackend)) {
      throw new RuntimeException("storage.backend=berkleyje cannot handle multiple graphs on a single DB, not usable");
    } else if ("inmemory".equals(storageBackend)) {
    } else {
      throw new RuntimeException("Unknown storage.backend=" + storageBackend);
    }
    
    LOGGER.info("Instantiated data access layer for Janus graph data store with backend: " + storageBackend);
    this.graph = janusGraphBuilder.open();
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
    final PropertyKey pk = createIndexMgmt.getOrCreatePropertyKey(index.getField().getName());

    if (createIndexMgmt.getGraphIndex(index.getName()) != null) {
      createIndexMgmt.rollback();
      return; //Ignore, index already exists
    }

    createIndexMgmt.buildIndex(index.getName(), Vertex.class).addKey(pk).buildCompositeIndex();

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

    return Optional.of(ChampObjectIndex.create()
        .ofName(indexName)
        .onType(ChampObject.ReservedTypes.ANY.toString())
        .forField(index.getFieldKeys()[0].name())
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

          next = ChampObjectIndex.create()
              .ofName(index.name())
              .onType(ChampObject.ReservedTypes.ANY.toString())
              .forField(index.getFieldKeys()[0].name())
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
        LOGGER.warn("Object index was created, but timed out while waiting for it to be registered");
        return;
      }
    } catch (InterruptedException e) {
      LOGGER.warn("Interrupted while waiting for object index creation status");
      return;
    }

    //Reindex the existing data

    try {
      final JanusGraphManagement updateIndexMgmt = graph.openManagement();
      updateIndexMgmt.updateIndex(updateIndexMgmt.getGraphIndex(indexName), SchemaAction.REINDEX).get();
      updateIndexMgmt.commit();
    } catch (InterruptedException e) {
      LOGGER.warn("Interrupted while reindexing for object index");
      return;
    } catch (ExecutionException e) {
      LOGGER.warn("Exception occurred during reindexing procedure for creating object index " + indexName, e);
    }

    try {
      ManagementSystem.awaitGraphIndexStatus(graph, indexName)
          .status(SchemaStatus.ENABLED)
          .timeout(10, ChronoUnit.MINUTES)
          .call();
    } catch (InterruptedException e) {
      LOGGER.warn("Interrupted while waiting for index to transition to ENABLED state");
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
  
	public GraphTraversal<?, ?> hasLabel(GraphTraversal<?, ?> query, Object type) {
		return query.hasLabel(type);
	}
}