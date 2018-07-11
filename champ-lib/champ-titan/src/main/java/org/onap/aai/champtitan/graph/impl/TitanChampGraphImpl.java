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
package org.onap.aai.champtitan.graph.impl;

import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.onap.aai.champcore.ChampCapabilities;
import org.onap.aai.champcore.exceptions.ChampIndexNotExistsException;
import org.onap.aai.champcore.exceptions.ChampSchemaViolationException;
import org.onap.aai.champcore.graph.impl.AbstractTinkerpopChampGraph;
import org.onap.aai.champcore.model.ChampCardinality;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thinkaurelius.titan.core.Cardinality;
import com.thinkaurelius.titan.core.EdgeLabel;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.SchemaViolationException;
import com.thinkaurelius.titan.core.TitanEdge;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanVertex;
import com.thinkaurelius.titan.core.schema.SchemaAction;
import com.thinkaurelius.titan.core.schema.SchemaStatus;
import com.thinkaurelius.titan.core.schema.TitanGraphIndex;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem;

public final class TitanChampGraphImpl extends AbstractTinkerpopChampGraph {

  private static final Logger LOGGER = LoggerFactory.getLogger(TitanChampGraphImpl.class);
  private static final String TITAN_UNIQUE_SUFFIX = "graph.unique-instance-id-suffix";
  private static final String TITAN_CASSANDRA_KEYSPACE = "storage.cassandra.keyspace";
  private static final String TITAN_HBASE_TABLE = "storage.hbase.table";
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

  private final TitanGraph graph;

  public TitanChampGraphImpl(Builder builder) {
    super(builder.graphConfiguration);
    final TitanFactory.Builder titanGraphBuilder = TitanFactory.build();

    for (Entry<String, Object> titanGraphProperty : builder.graphConfiguration.entrySet()) {
      titanGraphBuilder.set(titanGraphProperty.getKey(), titanGraphProperty.getValue());
    }

    titanGraphBuilder.set(TITAN_UNIQUE_SUFFIX, ((short) new SecureRandom().nextInt(Short.MAX_VALUE)+""));
    
    final Object storageBackend = builder.graphConfiguration.get("storage.backend");

    if ("cassandra".equals(storageBackend) ||
        "cassandrathrift".equals(storageBackend) ||
        "astyanax".equals(storageBackend) ||
        "embeddedcassandra".equals(storageBackend)) {
      titanGraphBuilder.set(TITAN_CASSANDRA_KEYSPACE, builder.graphName);
    } else if ("hbase".equals(storageBackend)) {
      titanGraphBuilder.set(TITAN_HBASE_TABLE, builder.graphName);
    } else if ("berkleyje".equals(storageBackend)) {
      throw new RuntimeException("storage.backend=berkleyje cannot handle multiple graphs on a single DB, not usable");
    } else if ("inmemory".equals(storageBackend)) {
    } else {
      throw new RuntimeException("Unknown storage.backend=" + storageBackend);
    }
    
    LOGGER.info("Instantiated data access layer for Titan graph data store with backend: " + storageBackend);

    this.graph = titanGraphBuilder.open();
  }

  public static class Builder {
    private final String graphName;

    private final Map<String, Object> graphConfiguration = new HashMap<String, Object> ();

    public Builder(String graphName) {
      this.graphName = graphName;
    }
    
    public Builder(String graphName, Map<String, Object> properties) {
        this.graphName = graphName;
        properties(properties);
    }

    public Builder properties(Map<String, Object> properties) {
      if (properties.containsKey(TITAN_CASSANDRA_KEYSPACE))
        throw new IllegalArgumentException("Cannot use path " + TITAN_CASSANDRA_KEYSPACE
            + " in initial configuration - this path is used"
            + " to specify graph names");

      this.graphConfiguration.putAll(properties);
      return this;
    }

    public Builder property(String path, Object value) {
      if (path.equals(TITAN_CASSANDRA_KEYSPACE))
        throw new IllegalArgumentException("Cannot use path " + TITAN_CASSANDRA_KEYSPACE
            + " in initial configuration - this path is used"
            + " to specify graph names");
      graphConfiguration.put(path, value);
      return this;
    }

    public TitanChampGraphImpl build() {
      return new TitanChampGraphImpl(this);
    }
  }

  @Override
  protected TitanGraph getGraph() {
    return graph;
  }

  @Override
  protected ChampSchemaEnforcer getSchemaEnforcer() {
    return SCHEMA_ENFORCER;
  }

  public void executeStoreObjectIndex(ChampObjectIndex index) {
    if (isShutdown()) throw new IllegalStateException("Cannot call storeObjectIndex() after shutdown has been initiated");

    final TitanGraph graph = getGraph();
    final TitanManagement createIndexMgmt = graph.openManagement();
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
    if (isShutdown()) throw new IllegalStateException("Cannot call retrieveObjectIndex() after shutdown has been initiated");

    final TitanManagement retrieveIndexMgmt = getGraph().openManagement();
    final TitanGraphIndex index = retrieveIndexMgmt.getGraphIndex(indexName);

    if (index == null) return Optional.empty();
    if (index.getIndexedElement() != TitanVertex.class) return Optional.empty();

    return Optional.of(ChampObjectIndex.create()
        .ofName(indexName)
        .onType(ChampObject.ReservedTypes.ANY.toString())
        .forField(index.getFieldKeys()[0].name())
        .build());
  }

  @Override
  public Stream<ChampObjectIndex> retrieveObjectIndices() {
    if (isShutdown()) throw new IllegalStateException("Cannot call retrieveObjectIndices() after shutdown has been initiated");

    final TitanManagement createIndexMgmt = getGraph().openManagement();
    final Iterator<TitanGraphIndex> indices = createIndexMgmt.getGraphIndexes(Vertex.class).iterator();

    final Iterator<ChampObjectIndex> objIter = new Iterator<ChampObjectIndex> () {

      private ChampObjectIndex next;

      @Override
      public boolean hasNext() {
        if (indices.hasNext()) {
          final TitanGraphIndex index = indices.next();

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
        if (next == null) throw new NoSuchElementException();

        return next;
      }
    };

    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
        objIter, Spliterator.ORDERED | Spliterator.NONNULL), false);
  }

  public void executeDeleteObjectIndex(String indexName) throws ChampIndexNotExistsException {
    if (isShutdown()) throw new IllegalStateException("Cannot call deleteObjectIndex() after shutdown has been initiated");

    throw new UnsupportedOperationException("Cannot delete indices using the TitanChampImpl");
  }

  public void executeStoreRelationshipIndex(ChampRelationshipIndex index) {
    if (isShutdown()) throw new IllegalStateException("Cannot call storeRelationshipIndex() after shutdown has been initiated");

    final TitanGraph graph = getGraph();
    final TitanManagement createIndexMgmt = graph.openManagement();
    final PropertyKey pk = createIndexMgmt.getOrCreatePropertyKey(index.getField().getName());

    if (createIndexMgmt.getGraphIndex(index.getName()) != null) return; //Ignore, index already exists
    createIndexMgmt.buildIndex(index.getName(), Edge.class).addKey(pk).buildCompositeIndex();

    createIndexMgmt.commit();
    graph.tx().commit();

    awaitIndexCreation(index.getName());
  }

  @Override
  public Optional<ChampRelationshipIndex> retrieveRelationshipIndex(String indexName) {
    if (isShutdown()) throw new IllegalStateException("Cannot call retrieveRelationshipIndex() after shutdown has been initiated");

    final TitanManagement retrieveIndexMgmt = getGraph().openManagement();
    final TitanGraphIndex index = retrieveIndexMgmt.getGraphIndex(indexName);

    if (index == null) return Optional.empty();
    if (index.getIndexedElement() != TitanEdge.class) return Optional.empty();

    return Optional.of(ChampRelationshipIndex.create()
        .ofName(indexName)
        .onType(ChampObject.ReservedTypes.ANY.toString())
        .forField(index.getFieldKeys()[0].name())
        .build());
  }

  @Override
  public Stream<ChampRelationshipIndex> retrieveRelationshipIndices() {
    if (isShutdown()) throw new IllegalStateException("Cannot call retrieveRelationshipIndices() after shutdown has been initiated");

    final TitanManagement createIndexMgmt = getGraph().openManagement();
    final Iterator<TitanGraphIndex> indices = createIndexMgmt.getGraphIndexes(Edge.class).iterator();

    final Iterator<ChampRelationshipIndex> objIter = new Iterator<ChampRelationshipIndex> () {

      private ChampRelationshipIndex next;

      @Override
      public boolean hasNext() {
        if (indices.hasNext()) {
          final TitanGraphIndex index = indices.next();

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
        if (next == null) throw new NoSuchElementException();

        return next;
      }
    };

    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
        objIter, Spliterator.ORDERED | Spliterator.NONNULL), false);
  }

  public void executeDeleteRelationshipIndex(String indexName) throws ChampIndexNotExistsException {
    if (isShutdown()) throw new IllegalStateException("Cannot call deleteRelationshipIndex() after shutdown has been initiated");

    throw new UnsupportedOperationException("Cannot delete indices using the TitanChampImpl");
  }

  private Cardinality getTitanCardinality(ChampCardinality cardinality) {
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
      Thread.currentThread().interrupt();
      return;
    }

    //Reindex the existing data

    try {
      final TitanManagement updateIndexMgmt = graph.openManagement();
      updateIndexMgmt.updateIndex(updateIndexMgmt.getGraphIndex(indexName),SchemaAction.REINDEX).get();
      updateIndexMgmt.commit();
    } catch (InterruptedException e) {
      LOGGER.warn("Interrupted while reindexing for object index");
      Thread.currentThread().interrupt();
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
      Thread.currentThread().interrupt();
      return;
    }
  }

  @Override
  public ChampCapabilities capabilities() {
    return CAPABILITIES;
  }

  @Override
  public void storeSchema(ChampSchema schema) throws ChampSchemaViolationException {
    if (isShutdown()) throw new IllegalStateException("Cannot call storeSchema() after shutdown has been initiated");

    final ChampSchema currentSchema = retrieveSchema();
    final TitanManagement mgmt = getGraph().openManagement();

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

          if (mgmt.getPropertyKey(newPropertyKeyName) != null) continue; //Check Titan to see if another node created this property key

          mgmt.makePropertyKey(newPropertyKeyName)
              .dataType(propConstraint.getField().getJavaType())
              .cardinality(getTitanCardinality(propConstraint.getCardinality()))
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

          if (mgmt.getPropertyKey(newPropertyKeyName) != null) continue; //Check Titan to see if another node created this property key

          mgmt.makePropertyKey(newPropertyKeyName)
              .dataType(propConstraint.getField().getJavaType())
              .cardinality(getTitanCardinality(propConstraint.getCardinality()))
              .make();
        }

        final EdgeLabel edgeLabel = mgmt.getEdgeLabel(relConstraint.getType());

        if (edgeLabel != null) mgmt.makeEdgeLabel(relConstraint.getType())
            .directed()
            .make();
      }

      mgmt.commit();

      super.storeSchema(schema);
    } catch (SchemaViolationException | ChampSchemaViolationException e) {
      mgmt.rollback();
      throw new ChampSchemaViolationException(e);
    }
  }

	@Override
	public GraphTraversal<?, ?> hasLabel(GraphTraversal<?, ?> query, Object type) {
		return query.hasLabel((String) type);
	}
}
