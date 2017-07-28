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
package org.openecomp.aai.champ.graph.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.openecomp.aai.champ.exceptions.ChampMarshallingException;
import org.openecomp.aai.champ.exceptions.ChampObjectNotExistsException;
import org.openecomp.aai.champ.exceptions.ChampRelationshipNotExistsException;
import org.openecomp.aai.champ.exceptions.ChampUnmarshallingException;
import org.openecomp.aai.champ.graph.impl.TitanChampGraphImpl.Builder;
import org.openecomp.aai.champ.model.ChampElement;
import org.openecomp.aai.champ.model.ChampObject;
import org.openecomp.aai.champ.model.ChampPartition;
import org.openecomp.aai.champ.model.ChampRelationship;
import org.openecomp.aai.champ.model.fluent.partition.CreateChampPartitionable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGremlinChampGraph extends AbstractValidatingChampGraph {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGremlinChampGraph.class);

	protected abstract GraphTraversalSource startTraversal();
	protected abstract Stream<ChampElement> runTraversal(GraphTraversal<?, ?> traversal);

	protected AbstractGremlinChampGraph(Map<String, Object> properties) {
	  super(properties);
	}
	
	@Override
	public Optional<ChampObject> retrieveObject(Object key) throws ChampUnmarshallingException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		final Stream<ChampElement> elements = runTraversal(startTraversal().V(key).limit(1));

		if (elements.count() == 0) {
			return Optional.empty();
		}

		return Optional.of(elements.findFirst().get().asObject());
	}

	public void executeDeleteObject(Object key) throws ChampObjectNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		final Stream<ChampElement> elements = runTraversal(startTraversal().V(key).limit(1));

		if (elements.count() == 0) {
			throw new ChampObjectNotExistsException();
		}

		runTraversal(startTraversal().V(key).drop());
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

		final GraphTraversal<?, Vertex> traversal = startTraversal().V();

		for (Entry<String, Object> filter : queryParams.entrySet()) {
			if (filter.getKey().equals(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString())) {
				continue; //For performance reasons, the label is the last thing to be added
			} else {
				traversal.has(filter.getKey(), filter.getValue());
			}
		}

		if (queryParams.containsKey(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString())) {
			traversal.hasLabel((String) queryParams.get(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString()));
		}


		return runTraversal(traversal).map(element -> {
			return element.asObject(); //Safe, since we're only operating on vertices
		});
	}

	@Override
	public Optional<ChampRelationship> retrieveRelationship(Object key) throws ChampUnmarshallingException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		final Stream<ChampElement> elements = runTraversal(startTraversal().E(key).limit(1));

		if (elements.count() == 0) return Optional.empty();

		return Optional.of(elements.findFirst().get().asRelationship());
	}

	public void executeDeleteRelationship(ChampRelationship relationship) throws ChampRelationshipNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
		if (!relationship.getKey().isPresent()) throw new IllegalArgumentException("Key must be provided when deleting a relationship");

		final Stream<ChampElement> elements = runTraversal(startTraversal().E(relationship.getKey().get()).limit(1));

		if (elements.count() == 0) {
			throw new ChampRelationshipNotExistsException();
		}

		runTraversal(startTraversal().E(relationship.getKey().get()).drop());
	}

	@Override
	public Stream<ChampRelationship> retrieveRelationships(ChampObject object)
			throws ChampUnmarshallingException, ChampObjectNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		final Stream<ChampElement> elements = runTraversal(startTraversal().V(object.getKey().get()).limit(1).bothE());

		return elements.map(element -> {
			return element.asRelationship(); //Safe, since we're only operating on edges
		});
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

		final GraphTraversal<Edge, Edge> traversal = startTraversal().E();

		for (Entry<String, Object> filter : queryParams.entrySet()) {
			if (filter.getKey().equals(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString())) {
				continue; //Add the label last for performance reasons
			} else {
				traversal.has(filter.getKey(), filter.getValue());
			}
		}

		if (queryParams.containsKey(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString())) {
			traversal.hasLabel((String) queryParams.get(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString()));
		}

		return runTraversal(traversal).map(element -> {
			return element.asRelationship(); //Safe, since we are only operating on edges
		});
	}

	@Override
	public void executeDeletePartition(ChampPartition graph) {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		final Object[] objectIds = graph.getChampObjects()
				.stream()
				.filter(o -> o.getKey().isPresent())
				.map(o -> { return o.getKey().get(); })
				.collect(Collectors.toList())
				.toArray();

		final Object[] relationshipIds = graph.getChampRelationships()
				.stream()
				.filter(o -> o.getKey().isPresent())
				.map(o -> { return o.getKey().get(); })
				.collect(Collectors.toList())
				.toArray();

		runTraversal(startTraversal().V(objectIds).drop());
		runTraversal(startTraversal().E(relationshipIds).drop());
	}

	@Override
	protected ChampObject doStoreObject(ChampObject object)
			throws ChampMarshallingException, ChampObjectNotExistsException {
		final GraphTraversal<Vertex, Vertex> traversal;

		if (object.getKey().isPresent()) {
			traversal = startTraversal().V(object.getKey().get());
		} else {
			traversal = startTraversal().addV(object.getType());
		}

		for (Entry<String, Object> property : object.getProperties().entrySet()) {

			if (property.getValue() instanceof List) {
				for (Object subPropertyValue : (List<?>) property.getValue()) {
					traversal.property(VertexProperty.Cardinality.list, property.getKey(), subPropertyValue);
				}
			} else if (property.getValue() instanceof Set) {
				for (Object subPropertyValue : (Set<?>) property.getValue()) {
					traversal.property(VertexProperty.Cardinality.set, property.getKey(), subPropertyValue);
				}
			} else {
				traversal.property(property.getKey(), property.getValue());
			}
		}

		return runTraversal(traversal).findFirst().get().asObject(); //TODO check if this return the updated vertices
	}
	
	@Override
	protected ChampObject doReplaceObject(ChampObject object)
			throws ChampMarshallingException, ChampObjectNotExistsException {
		//TODO: implement the replace method when required
		throw new UnsupportedOperationException("Method not implemented");
	}
	
	@Override
	protected ChampRelationship doReplaceRelationship(ChampRelationship relationship) throws ChampRelationshipNotExistsException, ChampMarshallingException {
		//TODO: implement the replace method when required
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	protected ChampRelationship doStoreRelationship(ChampRelationship relationship) throws ChampObjectNotExistsException, ChampRelationshipNotExistsException, ChampMarshallingException {

		/* FIXME: Only compatible with Tinkerpop 3.2.3 (Titan uses 3.0.1-incubating).

		final GraphTraversal<?, Vertex> sourceBuilder;

		if (relationship.getSource().getKey().isPresent()) {
			sourceBuilder = startTraversal().V(relationship.getSource().getKey().get()).as("source");
		} else {
			sourceBuilder = startTraversal().addV(relationship.getSource().getType());
		}

		for (Entry<String, Object> sourceProperty : relationship.getSource().getProperties().entrySet()) {
			sourceBuilder.property(sourceProperty.getKey(), sourceProperty.getValue());
		}

		final GraphTraversal<?, Vertex> targetBuilder;

		if (relationship.getTarget().getKey().isPresent()) {
			targetBuilder = sourceBuilder.V(relationship.getTarget().getKey().get()).as("target");
		} else {
			targetBuilder = sourceBuilder.addV(relationship.getTarget().getType());
		}

		for (Entry<String, Object> targetProperty : relationship.getTarget().getProperties().entrySet()) {
			targetBuilder.property(targetProperty.getKey(), targetProperty.getValue());
		}

		final GraphTraversal<?, Edge> edgeBuilder = targetBuilder.addE(relationship.getType()).from("source");

		for (Entry<String, Object> property : relationship.getProperties().entrySet()) {
			edgeBuilder.property(property.getKey(), property.getValue());
		}

		return runTraversal(edgeBuilder).filter(e -> e.isRelationship()).findFirst().get().asRelationship();
		*/

		throw new UnsupportedOperationException("Cannot store relationships because of project setup (Incompatible Tinkerpop version in use)");
	}

	@Override
	protected ChampPartition doStorePartition(ChampPartition submittedPartition) throws ChampObjectNotExistsException, ChampMarshallingException, ChampRelationshipNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		try {
			final HashMap<ChampObject, ChampObject> objectsWithKeys = new HashMap<ChampObject, ChampObject> ();
			final CreateChampPartitionable storedPartition = ChampPartition.create();

			for (ChampObject champObject : submittedPartition.getChampObjects()) {

				final ChampObject objectWithKey = doStoreObject(champObject);
				objectsWithKeys.put(champObject, objectWithKey);
			}

			for (ChampRelationship champRelationship : submittedPartition.getChampRelationships()) {
				if (!objectsWithKeys.containsKey(champRelationship.getSource())) {
					final ChampObject objectWithKey = doStoreObject(champRelationship.getSource());

					objectsWithKeys.put(champRelationship.getSource(), objectWithKey);
				}

				if (!objectsWithKeys.containsKey(champRelationship.getTarget())) {
					final ChampObject objectWithKey = doStoreObject(champRelationship.getTarget());

					objectsWithKeys.put(champRelationship.getTarget(), objectWithKey);
				}

				final ChampRelationship.Builder relWithKeysBuilder = new ChampRelationship.Builder(objectsWithKeys.get(champRelationship.getSource()),
																							objectsWithKeys.get(champRelationship.getTarget()),
																							champRelationship.getType());

				if (champRelationship.getKey().isPresent()) relWithKeysBuilder.key(champRelationship.getKey().get());
				
				relWithKeysBuilder.properties(champRelationship.getProperties());

				final ChampRelationship relationship = doStoreRelationship(relWithKeysBuilder.build());

				storedPartition.withRelationship(relationship);
			}

			for (ChampObject object : objectsWithKeys.values()) {
				storedPartition.withObject(object);
			}

			return storedPartition.build();
		} catch (ChampObjectNotExistsException | ChampMarshallingException e) {
			throw e;
		}
	}
}
