/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017 Amdocs
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.champcore.graph.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.onap.aai.champcore.ChampCapabilities;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.NoOpTinkerPopTransaction;
import org.onap.aai.champcore.exceptions.ChampIndexNotExistsException;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampObjectIndex;
import org.onap.aai.champcore.model.ChampRelationshipIndex;
import org.onap.aai.champcore.schema.ChampSchemaEnforcer;
import org.onap.aai.champcore.schema.DefaultChampSchemaEnforcer;

public final class InMemoryChampGraphImpl extends AbstractTinkerpopChampGraph {

	private static final ChampCapabilities CAPABILITIES = new ChampCapabilities() {

		@Override
		public boolean canDeleteObjectIndices() {
			return true;
		}

		@Override
		public boolean canDeleteRelationshipIndices() {
			return true;
		}
	};

	private final ConcurrentHashMap<String, ChampObjectIndex> objectIndices;
	private final ConcurrentHashMap<String, ChampRelationshipIndex> relationshipIndices;

	private final ChampSchemaEnforcer schemaEnforcer;
	private final TinkerGraph graph;

	private InMemoryChampGraphImpl(Builder builder) {
	    super(builder.graphConfiguration);
		this.graph = TinkerGraph.open();
	
		this.objectIndices = new ConcurrentHashMap<String, ChampObjectIndex> ();
		this.relationshipIndices = new ConcurrentHashMap<String, ChampRelationshipIndex> ();

		this.schemaEnforcer = builder.schemaEnforcer;
	}

	@Override
    public ChampTransaction getOrCreateTransactionInstance(Optional<ChampTransaction> transaction) {

	  return new NoOpTinkerPopTransaction(getGraph());

	}
	   
	public static class Builder {
	    private final Map<String, Object> graphConfiguration = new HashMap<String, Object> ();
		private ChampSchemaEnforcer schemaEnforcer = new DefaultChampSchemaEnforcer();

		public Builder() {}

		public Builder schemaEnforcer(ChampSchemaEnforcer schemaEnforcer) {
			this.schemaEnforcer = schemaEnforcer;
			return this;
		}

	      public Builder properties(Map<String, Object> properties) {
            
            this.graphConfiguration.putAll(properties);
            return this;
        }

        public Builder property(String path, Object value) {
           
            graphConfiguration.put(path, value);
            return this;
        }
        
		public InMemoryChampGraphImpl build() {
			return new InMemoryChampGraphImpl(this);
		}
	}

	protected ChampSchemaEnforcer getSchemaEnforcer() {
		return schemaEnforcer;
	}

	@Override
	protected TinkerGraph getGraph() {
		return graph;
	}

	
	private ConcurrentHashMap<String, ChampObjectIndex> getObjectIndices() {
		return objectIndices;
	}

	private ConcurrentHashMap<String, ChampRelationshipIndex> getRelationshipIndices() {
		return relationshipIndices;
	}

	@Override
	public void executeStoreObjectIndex(ChampObjectIndex index) {
	  
		if (isShutdown()) throw new IllegalStateException("Cannot call storeObjectIndex() after shutdown has been initiated");

		getGraph().createIndex(index.getField().getName(), Vertex.class);
		getObjectIndices().put(index.getName(), index);
	}

	@Override
	public Optional<ChampObjectIndex> retrieveObjectIndex(String indexName) {
		if (isShutdown()) throw new IllegalStateException("Cannot call retrieveObjectIndex() after shutdown has been initiated");

		if (getObjectIndices().containsKey(indexName))
			return Optional.of(getObjectIndices().get(indexName));
			
		return Optional.empty();
	}

	@Override
	public Stream<ChampObjectIndex> retrieveObjectIndices() {
		if (isShutdown()) throw new IllegalStateException("Cannot call retrieveObjectIndices() after shutdown has been initiated");

		return getObjectIndices().values().stream();
	}

	public void executeDeleteObjectIndex(String indexName) throws ChampIndexNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot call deleteObjectIndex() after shutdown has been initiated");

		final ChampObjectIndex objectIndex = getObjectIndices().remove(indexName);

		if (objectIndex == null) throw new ChampIndexNotExistsException();

		getGraph().dropIndex(objectIndex.getField().getName(), Vertex.class);
	}

	public void executeStoreRelationshipIndex(ChampRelationshipIndex index) {
		if (isShutdown()) throw new IllegalStateException("Cannot call storeRelationshipIndex() after shutdown has been initiated");

		getGraph().createIndex(index.getField().getName(), Edge.class);
		getRelationshipIndices().put(index.getName(), index);
	}

	@Override
	public Optional<ChampRelationshipIndex> retrieveRelationshipIndex(String indexName) {
		if (isShutdown()) throw new IllegalStateException("Cannot call retrieveRelationshipIndex() after shutdown has been initiated");

		if (getRelationshipIndices().containsKey(indexName)) {
			return Optional.of(getRelationshipIndices().get(indexName));
		}
		
		return Optional.empty();
	}

	@Override
	public Stream<ChampRelationshipIndex> retrieveRelationshipIndices() {
		if (isShutdown()) throw new IllegalStateException("Cannot call retrieveRelationshipIndices() after shutdown has been initiated");

		return getRelationshipIndices().values().stream();
	}

	public void executeDeleteRelationshipIndex(String indexName) throws ChampIndexNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot call deleteRelationshipIndex() after shutdown has been initiated");

		final ChampRelationshipIndex relationshipIndex = getRelationshipIndices().remove(indexName);

		if (relationshipIndex == null) throw new ChampIndexNotExistsException();
		
		getGraph().dropIndex(relationshipIndex.getField().getName(), Edge.class);
	}

	@Override
	public ChampCapabilities capabilities() {
		return CAPABILITIES;
	}

	@Override
	public GraphTraversal<?, ?> hasLabel(GraphTraversal<?, ?> query, Object type) {
		return query.hasLabel((String)type, (String)type);
	}
}
