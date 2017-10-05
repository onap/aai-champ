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
package org.onap.aai.champ.graph.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.onap.aai.champ.event.ChampEvent;
import org.onap.aai.champ.event.ChampEvent.ChampOperation;
import org.onap.aai.champ.exceptions.ChampMarshallingException;
import org.onap.aai.champ.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champ.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champ.exceptions.ChampSchemaViolationException;
import org.onap.aai.champ.exceptions.ChampUnmarshallingException;
import org.onap.aai.champ.graph.impl.TitanChampGraphImpl.Builder;
import org.onap.aai.champ.model.ChampObject;
import org.onap.aai.champ.model.ChampPartition;
import org.onap.aai.champ.model.ChampRelationship;
import org.onap.aai.champ.model.ChampSchema;
import org.onap.aai.champ.model.fluent.partition.CreateChampPartitionable;
import org.onap.aai.champ.transform.TinkerpopChampformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractTinkerpopChampGraph extends AbstractValidatingChampGraph {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTinkerpopChampGraph.class);
	private static final TinkerpopChampformer TINKERPOP_CHAMPFORMER = new TinkerpopChampformer();
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final int COMMIT_RETRY_COUNT = 3;

	private volatile AtomicBoolean isShutdown;

	protected AbstractTinkerpopChampGraph(Map<String, Object> properties) {
	  super(properties);
	  
	  isShutdown = new AtomicBoolean(false);
      Runtime.getRuntime().addShutdownHook(shutdownHook);
	}
	
	private static final TinkerpopChampformer getChampformer() {
		return TINKERPOP_CHAMPFORMER;
	}

	private static final ObjectMapper getObjectMapper() {
		return OBJECT_MAPPER;
	}

	private Vertex writeVertex(ChampObject object) throws ChampObjectNotExistsException, ChampMarshallingException {
		final Vertex vertex;
		
		if (object.getKey().isPresent()) {
			final Iterator<Vertex> vertexIter = getGraph().vertices(object.getKey().get());

			if (vertexIter.hasNext()) {
				vertex = vertexIter.next();
			} else throw new ChampObjectNotExistsException();
		} else {
			vertex = getGraph().addVertex(object.getType());
		}

		for (Entry<String, Object> property : object.getProperties().entrySet()) {

			if (property.getValue() instanceof List) {
				for (Object subPropertyValue : (List<?>) property.getValue()) {
					vertex.property(VertexProperty.Cardinality.list, property.getKey(), subPropertyValue);
				}
			} else if (property.getValue() instanceof Set) {
				for (Object subPropertyValue : (Set<?>) property.getValue()) {
					vertex.property(VertexProperty.Cardinality.set, property.getKey(), subPropertyValue);
				}
			} else {
				vertex.property(property.getKey(), property.getValue());
			}
		}

		return vertex;
	}
	
	private Vertex replaceVertex(ChampObject object) throws ChampObjectNotExistsException, ChampMarshallingException {
		Vertex vertex;
		
		if (object.getKey().isPresent()) {
			final Iterator<Vertex> vertexIter = getGraph().vertices(object.getKey().get());

			if (vertexIter.hasNext()) {
				vertex = vertexIter.next();
			} else throw new ChampObjectNotExistsException();
		} else {
			throw new ChampObjectNotExistsException();
		}

		//clear all the existing properties
		Iterator<VertexProperty<Object>> it = vertex.properties();
		while (it.hasNext()) {
			it.next().remove();
		}
		
		for (Entry<String, Object> property : object.getProperties().entrySet()) {

			if (property.getValue() instanceof List) {
				for (Object subPropertyValue : (List<?>) property.getValue()) {
					vertex.property(VertexProperty.Cardinality.list, property.getKey(), subPropertyValue);
				}
			} else if (property.getValue() instanceof Set) {
				for (Object subPropertyValue : (Set<?>) property.getValue()) {
					vertex.property(VertexProperty.Cardinality.set, property.getKey(), subPropertyValue);
				}
			} else {
				vertex.property(property.getKey(), property.getValue());				
			}
		}

		return vertex;
	}

	private Edge writeEdge(ChampRelationship relationship) throws ChampObjectNotExistsException, ChampRelationshipNotExistsException, ChampMarshallingException {

		final Vertex source = writeVertex(relationship.getSource());
		final Vertex target = writeVertex(relationship.getTarget());
		final Edge edge;

		if (relationship.getKey().isPresent()) {
			final Iterator<Edge> edgeIter = getGraph().edges(relationship.getKey().get());

			if (edgeIter.hasNext()) {
				edge = edgeIter.next();
			} else throw new ChampRelationshipNotExistsException();
		} else {
			edge = source.addEdge(relationship.getType(), target);
		}

		for (Entry<String, Object> property : relationship.getProperties().entrySet()) {
			edge.property(property.getKey(), property.getValue());
		}

		return edge;
	}
	
	private Edge replaceEdge(ChampRelationship relationship) throws  ChampRelationshipNotExistsException, ChampMarshallingException {
		final Edge edge;
		
		if(!relationship.getSource().getKey().isPresent() || !relationship.getTarget().getKey().isPresent()){
			throw new IllegalArgumentException("Invalid source/target");
		}
		
		if (relationship.getKey().isPresent()) {
			final Iterator<Edge> edgeIter = getGraph().edges(relationship.getKey().get());

			if (edgeIter.hasNext()) {
				edge = edgeIter.next();
				//validate if the source/target are the same as before. Throw error if not the same
				if (!edge.outVertex().id().equals(relationship.getSource().getKey().get())
						|| !edge.inVertex().id().equals(relationship.getTarget().getKey().get())) {
					throw new IllegalArgumentException("source/target can't be updated");
				}

			} else throw new ChampRelationshipNotExistsException();
		} else {
			throw new ChampRelationshipNotExistsException();
		}
		
		// clear all the existing properties
		Iterator<Property<Object>> it = edge.properties();
		while (it.hasNext()) {
			it.next().remove();
		}
				
		for (Entry<String, Object> property : relationship.getProperties().entrySet()) {
			edge.property(property.getKey(), property.getValue());
		}

		return edge;
	}

	private void tryRollback() {
		if (getGraph().features().graph().supportsTransactions()) {
			getGraph().tx().rollback();
		}
	}

	private void tryCommit() {

		if (getGraph().features().graph().supportsTransactions()) {

			final long initialBackoff = (int) (Math.random() * 50);

			for (int i = 0; i < COMMIT_RETRY_COUNT; i++) {
				try {
					getGraph().tx().commit();
					return;
				} catch (Throwable e) {
					if (i == COMMIT_RETRY_COUNT - 1) {
						LOGGER.error("Maxed out commit attempt retries, client must handle exception and retry", e);
						getGraph().tx().rollback();
						throw e;
					}

					final long backoff = (long) Math.pow(2, i) * initialBackoff;
					LOGGER.warn("Caught exception while retrying transaction commit, retrying in " + backoff + " ms");
					
					try {
						Thread.sleep(backoff);
					} catch (InterruptedException ie) {
						LOGGER.info("Interrupted while backing off on transaction commit");
						return;
					}
				}
			}
		}
	}

	protected abstract Graph getGraph();

	private Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			try {
				shutdown();
			} catch (IllegalStateException e) {
				//Suppress, because shutdown() has already been called
			}
		}
	};

	protected boolean isShutdown() {
		return isShutdown.get();
	}

	@Override
	public Stream<ChampObject> queryObjects(Map<String, Object> queryParams) {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
	
		//If they provided the object key, do this the quick way rather than creating a traversal
		if (queryParams.containsKey(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_KEY.toString())) {
			try {
				final Optional<ChampObject> object = retrieveObject(queryParams.get(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_KEY.toString()));
			
				if (object.isPresent()) return Stream.of(object.get());
				else return Stream.empty();
			} catch (ChampUnmarshallingException e) {
				LOGGER.warn("Failed to unmarshall object", e);
				return Stream.empty();
			}
		}

		final GraphTraversal<Vertex, Vertex> query = getGraph().traversal().V();

		for (Entry<String, Object> filter : queryParams.entrySet()) {
			if (filter.getKey().equals(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString())) {
				continue; //For performance reasons, the label is the last thing to be added
			} else {
				query.has(filter.getKey(), filter.getValue());
			}
		}

		if (queryParams.containsKey(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString())) {
			query.hasLabel((String) queryParams.get(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString()));
		}

		final Iterator<ChampObject> objIter = new Iterator<ChampObject> () {
	
			private ChampObject next;


			@Override
			public boolean hasNext() {
				while (query.hasNext()) {
					try {
						next = getChampformer().unmarshallObject(query.next());
						return true;
					} catch (ChampUnmarshallingException e) {
						LOGGER.warn("Failed to unmarshall tinkerpop vertex during query, returning partial results", e);
					}					
				}

				tryCommit(); //Danger ahead if this iterator is not completely consumed
														//then the transaction cache will hold stale values

				next = null;
				return false;
			}

			@Override
			public ChampObject next() {
				if (next == null) throw new NoSuchElementException();
				
				return next;
			}
		};

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                objIter, Spliterator.ORDERED | Spliterator.NONNULL), false);
	}

	@Override
	public Optional<ChampObject> retrieveObject(Object key) throws ChampUnmarshallingException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		final Iterator<Vertex> vertices = getGraph().vertices(key);
		final Optional<ChampObject> optionalObject;

		if (!vertices.hasNext()) optionalObject = Optional.empty();
		else optionalObject = Optional.of(getChampformer().unmarshallObject(vertices.next()));

		tryCommit();

		return optionalObject;
	}

	@Override
	public Stream<ChampRelationship> retrieveRelationships(ChampObject source) throws ChampUnmarshallingException, ChampObjectNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
		
		final Vertex sourceVertex;

		try {
			sourceVertex = getGraph().vertices(source.getKey().get()).next();
		} catch (NoSuchElementException e) {			
			tryRollback();

			throw new ChampObjectNotExistsException();
		}

		final Iterator<Edge> edges = sourceVertex.edges(Direction.BOTH);
		final Iterator<ChampRelationship> relIter = new Iterator<ChampRelationship> () {

			private ChampRelationship next;

			@Override
			public boolean hasNext() {
				while (edges.hasNext()) {
					try {
						next = getChampformer().unmarshallRelationship(edges.next());
						return true;
					} catch (ChampUnmarshallingException e) {
						LOGGER.warn("Failed to unmarshall tinkerpop edge during query, returning partial results", e);
					}					
				}

				tryCommit();//Danger ahead if this iterator is not completely
									 //consumed, then the transaction cache will be stale
				next = null;
				return false;
			}

			@Override
			public ChampRelationship next() {
				if (next == null) throw new NoSuchElementException();
				
				return next;
			}
		};

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                relIter, Spliterator.ORDERED | Spliterator.NONNULL), false);
	}

	@Override
	public ChampObject doStoreObject(ChampObject object) throws ChampMarshallingException, ChampObjectNotExistsException {

		try {
			final Vertex vertex = writeVertex(object);

			tryCommit();
			
			return ChampObject.create()
                                .from(object)
                                .withKey(vertex.id())
                                .build();
			
		} catch (ChampObjectNotExistsException e) {
			tryRollback();

			throw e;
		}
	}
	
	@Override
	public ChampObject doReplaceObject(ChampObject object) throws ChampMarshallingException, ChampObjectNotExistsException {

		try {
			final Vertex vertex = replaceVertex(object);

			tryCommit();
			
			return ChampObject.create()
                                  .from(object)
                                  .withKey(vertex.id())
                                  .build();
			
		} catch (ChampObjectNotExistsException e) {
			tryRollback();

			throw e;
		}
	}

	public void executeDeleteObject(Object key) throws ChampObjectNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		final Iterator<Vertex> vertex = getGraph().vertices(key);

		if (!vertex.hasNext()) {
			tryRollback();

			throw new ChampObjectNotExistsException();
		}

		vertex.next().remove();
		
		tryCommit();
	}

	@Override
	public ChampRelationship doStoreRelationship(ChampRelationship relationship)
			throws ChampUnmarshallingException, ChampObjectNotExistsException, ChampRelationshipNotExistsException, ChampMarshallingException  {

		try {
			final Edge edge = writeEdge(relationship);

			tryCommit();
	
			return getChampformer().unmarshallRelationship(edge);
			
		} catch (ChampObjectNotExistsException | ChampRelationshipNotExistsException | ChampUnmarshallingException | ChampMarshallingException e) {
			tryRollback();

			throw e;
		}
	}
	
	@Override
	public ChampRelationship doReplaceRelationship(ChampRelationship relationship)
			throws ChampUnmarshallingException, ChampRelationshipNotExistsException, ChampMarshallingException  {

		try {
			final Edge edge = replaceEdge(relationship);

			tryCommit();
	
			return getChampformer().unmarshallRelationship(edge);
			
		} catch ( ChampRelationshipNotExistsException | ChampUnmarshallingException | ChampMarshallingException e) {
			tryRollback();

			throw e;
		}
	}

	@Override
	public Stream<ChampRelationship> queryRelationships(Map<String, Object> queryParams) {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		//If they provided the relationship key, do this the quick way rather than creating a traversal
		if (queryParams.containsKey(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_KEY.toString())) {
			try {
				final Optional<ChampRelationship> relationship = retrieveRelationship(queryParams.get(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_KEY.toString()));
			
				if (relationship.isPresent()) return Stream.of(relationship.get());
				else return Stream.empty();
			} catch (ChampUnmarshallingException e) {
				LOGGER.warn("Failed to unmarshall relationship", e);
				return Stream.empty();
			}
		}
	 
		final GraphTraversal<Edge, Edge> query = getGraph().traversal().E();

		for (Entry<String, Object> filter : queryParams.entrySet()) {
			if (filter.getKey().equals(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString())) {
				continue; //Add the label last for performance reasons
			} else {
				query.has(filter.getKey(), filter.getValue());
			}
		}

		if (queryParams.containsKey(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString())) {
			query.hasLabel((String) queryParams.get(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString()));
		}

		final Iterator<ChampRelationship> objIter = new Iterator<ChampRelationship> () {
	
			private ChampRelationship next;


			@Override
			public boolean hasNext() {
				while (query.hasNext()) {
					try {
						next = getChampformer().unmarshallRelationship(query.next());
						return true;
					} catch (ChampUnmarshallingException e) {
						LOGGER.warn("Failed to unmarshall tinkerpop vertex during query, returning partial results", e);
					}					
				}

				tryCommit(); //Danger ahead if this iterator is not completely
									  //consumed, then the transaction cache will be stale
					
				next = null;
				return false;
			}

			@Override
			public ChampRelationship next() {
				if (next == null) throw new NoSuchElementException();
				
				return next;
			}
		};

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                objIter, Spliterator.ORDERED | Spliterator.NONNULL), false);
	}

	@Override
	public Optional<ChampRelationship> retrieveRelationship(Object key)
			throws ChampUnmarshallingException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		final Iterator<Edge> edge = getGraph().edges(key);
		final Optional<ChampRelationship> optionalRelationship;

		if (!edge.hasNext()) optionalRelationship = Optional.empty();
		else optionalRelationship = Optional.of(getChampformer().unmarshallRelationship(edge.next()));

		tryCommit();

		return optionalRelationship;
	}

	public void executeDeleteRelationship(ChampRelationship relationship) throws ChampRelationshipNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
		if (!relationship.getKey().isPresent()) throw new IllegalArgumentException("Key must be provided when deleting a relationship");

		final Iterator<Edge> edge = getGraph().edges(relationship.getKey().get());
		
		if (!edge.hasNext()) {
			tryRollback();

			throw new ChampRelationshipNotExistsException();
		}
		
		edge.next().remove();

		tryCommit();
	}

	@Override
	public ChampPartition doStorePartition(ChampPartition submittedPartition) throws ChampMarshallingException, ChampObjectNotExistsException, ChampRelationshipNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		try {
			final HashMap<ChampObject, ChampObject> objectsWithKeys = new HashMap<ChampObject, ChampObject> ();
			final CreateChampPartitionable storedPartition = ChampPartition.create();

			for (ChampObject champObject : submittedPartition.getChampObjects()) {
				final Vertex vertex = writeVertex(champObject);
				objectsWithKeys.put(champObject, ChampObject.create()
															.from(champObject)
															.withKey(vertex.id())
															.build());
			}

			for (ChampRelationship champRelationship : submittedPartition.getChampRelationships()) {
				if (!objectsWithKeys.containsKey(champRelationship.getSource())) {
					final Vertex vertex = writeVertex(champRelationship.getSource());

					objectsWithKeys.put(champRelationship.getSource(), ChampObject.create()
														.from(champRelationship.getSource())
														.withKey(vertex.id())
														.build());
				}

				if (!objectsWithKeys.containsKey(champRelationship.getTarget())) {
					final Vertex vertex = writeVertex(champRelationship.getTarget());

					objectsWithKeys.put(champRelationship.getTarget(), ChampObject.create()
														.from(champRelationship.getTarget())
														.withKey(vertex.id())
														.build());
				}

				final ChampRelationship.Builder relWithKeysBuilder = new ChampRelationship.Builder(objectsWithKeys.get(champRelationship.getSource()),
																							objectsWithKeys.get(champRelationship.getTarget()),
																							champRelationship.getType());

				if (champRelationship.getKey().isPresent()) relWithKeysBuilder.key(champRelationship.getKey().get());
				
				relWithKeysBuilder.properties(champRelationship.getProperties());

				final Edge edge = writeEdge(relWithKeysBuilder.build());

				storedPartition.withRelationship(ChampRelationship.create()
																	.from(champRelationship)
																	.withKey(edge.id())
																	.build());
			}

			for (ChampObject object : objectsWithKeys.values()) {
				storedPartition.withObject(object);
			}

			tryCommit();
            
			return storedPartition.build();
			
		} catch (ChampObjectNotExistsException | ChampMarshallingException e) {
			tryRollback();

			throw e;
		}
	}

	public void executeDeletePartition(ChampPartition graph) {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		for (ChampObject champObject : graph.getChampObjects()) {
			try {
				final Object vertexId = champObject.getKey().get();
				final Iterator<Vertex> vertex = getGraph().vertices(vertexId);
	
				if (vertex.hasNext()) {
					vertex.next().remove();
				}
			} catch (NoSuchElementException e) {
				tryRollback();

				throw new IllegalArgumentException("Must pass a key to delete an object");
			}
		}

		for (ChampRelationship champRelationship : graph.getChampRelationships()) {
			try {
				final Iterator<Edge> edge = getGraph().edges(champRelationship.getKey().get());
		
				if (edge.hasNext()) {
					edge.next().remove();
				}
			} catch (NoSuchElementException e) {
				tryRollback();

				throw new IllegalArgumentException("Must pass a key to delete a relationship");
			}
		}

		tryCommit();

	}

	@Override
	public void shutdown() {

		if (isShutdown.compareAndSet(false, true)) {
		  super.shutdown();
			try {
				getGraph().close();
			} catch (Throwable t) {
				LOGGER.error("Exception while shutting down graph", t);
			}
		} else {
			throw new IllegalStateException("Cannot call shutdown() after shutdown() was already initiated");
		}
	}

	@Override
	public void storeSchema(ChampSchema schema) throws ChampSchemaViolationException {
		if (isShutdown()) throw new IllegalStateException("Cannot call storeSchema() after shutdown has been initiated");

		if (getGraph().features().graph().variables().supportsVariables()) {
			try {
				getGraph().variables().set("schema", getObjectMapper().writeValueAsBytes(schema));
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		} else {
			super.storeSchema(schema);
		}
	}

	@Override
	public ChampSchema retrieveSchema() {
		if (isShutdown()) throw new IllegalStateException("Cannot call retrieveSchema() after shutdown has been initiated");

		if (getGraph().features().graph().variables().supportsVariables()) {
			final Optional<byte[]> schema = getGraph().variables().get("schema");

			if (schema.isPresent()) {
				try {
					return getObjectMapper().readValue(schema.get(), ChampSchema.class);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		return super.retrieveSchema();
	}

	@Override
	public void deleteSchema() {
		if (isShutdown()) throw new IllegalStateException("Cannot call deleteSchema() after shutdown has been initiated");

		if (getGraph().features().graph().variables().supportsVariables()) {
			getGraph().variables().remove("schema");
		} else {
			super.deleteSchema();
		}
	}
}
