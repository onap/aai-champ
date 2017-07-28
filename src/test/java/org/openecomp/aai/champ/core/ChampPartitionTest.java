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
package org.openecomp.aai.champ.core;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.openecomp.aai.champ.ChampAPI;
import org.openecomp.aai.champ.ChampGraph;
import org.openecomp.aai.champ.exceptions.ChampMarshallingException;
import org.openecomp.aai.champ.exceptions.ChampObjectNotExistsException;
import org.openecomp.aai.champ.exceptions.ChampRelationshipNotExistsException;
import org.openecomp.aai.champ.exceptions.ChampSchemaViolationException;
import org.openecomp.aai.champ.exceptions.ChampUnmarshallingException;
import org.openecomp.aai.champ.model.ChampObject;
import org.openecomp.aai.champ.model.ChampPartition;
import org.openecomp.aai.champ.model.ChampRelationship;

public class ChampPartitionTest extends BaseChampAPITest {

	@Test
	public void runTests() {
		for (ChampGraph.Type apiType : ChampGraph.Type.values()) {
			final ChampAPI api = ChampAPI.Factory.newInstance(apiType);
			final String graphName = ChampPartitionTest.class.getSimpleName();

			switch (apiType) {
			case IN_MEMORY:
			break;
			case TITAN:
				cleanUp(graphName);
			break;
			default:
			break;
			}

			ChampPartitionTest.testChampPartitionCrud(api.getGraph(graphName));
			api.shutdown();
		}
	}

	@Test
	public void testHashCode() {

		final ChampObject foo = ChampObject.create()
												.ofType("foo")
												.withoutKey()
												.build();
		final ChampObject bar = ChampObject.create()
												.ofType("bar")
												.withoutKey()
												.build();
		final ChampRelationship baz = ChampRelationship.create()
														.ofType("baz")
														.withoutKey()
														.withSource()
															.from(foo)
															.build()
														.withTarget()
															.from(bar)
															.build()
														.build();

		final ChampPartition partition = ChampPartition.create()
														.withObject(foo)
														.withObject(bar)
														.withRelationship(baz)
														.build();

		assertTrue(partition.getChampObjects().contains(foo));
		assertTrue(partition.getChampObjects().contains(bar));
		assertTrue(partition.getChampRelationships().contains(baz));
	}

	@Test
	public void testBuilder() {
		final ChampObject foo = new ChampObject.Builder("foo").build();
		final ChampObject bar = new ChampObject.Builder("bar").build();
		final ChampRelationship uses = new ChampRelationship.Builder(foo, bar, "uses")
																.build();
		final ChampPartition a = new ChampPartition.Builder()
													.object(foo)
													.objects(Collections.singleton(bar))
													.relationship(uses)
													.relationships(Collections.singleton(uses))
													.build();
		assertTrue(a.getChampObjects().size() == 2);
		assertTrue(a.getChampObjects().contains(foo));
		assertTrue(a.getChampObjects().contains(bar));

		assertTrue(a.getChampRelationships().size() == 1);
		assertTrue(a.getChampRelationships().contains(uses));
	}

	public static void testChampPartitionCrud(ChampGraph graph) {

		final ChampObject foo = ChampObject.create()
											.ofType("foo")
											.withoutKey()
											.withProperty("prop1", "value1")
											.build();
		final ChampObject bar = ChampObject.create()
											.ofType("bar")
											.withoutKey()
											.withProperty("prop2", "value2")
											.build();

		final ChampRelationship baz = ChampRelationship.create()
														.ofType("baz")
														.withoutKey()
														.withSource()
															.from(foo)
															.build()
														.withTarget()
															.from(bar)
															.build()
														.withProperty("prop3", "value3")
														.build();

		final ChampPartition partition = ChampPartition.create()
														.withObject(foo)
														.withObject(bar)
														.withRelationship(baz)
														.build();

		assertTrue(partition.getIncidentRelationships(foo).contains(baz));
		assertTrue(partition.getIncidentRelationships(bar).contains(baz));
		assertTrue(partition.getIncidentRelationshipsByType(foo).get("baz").contains(baz));

		try {
			final ChampPartition storedPartition = graph.storePartition(partition);

			ChampPartitionTest.retrievePartitionElements(graph, storedPartition, true);

			graph.deletePartition(storedPartition);

			ChampPartitionTest.retrievePartitionElements(graph, storedPartition, false);

		} catch (ChampMarshallingException e) {
			throw new AssertionError(e);
		} catch (ChampObjectNotExistsException e) {
			throw new AssertionError(e);
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError(e);
		} catch (ChampRelationshipNotExistsException e) {
			throw new AssertionError(e);
		}
	}

	private static void retrievePartitionElements(ChampGraph graph, ChampPartition partition, boolean expectFound) {
		for (ChampObject object : partition.getChampObjects()) {
			try {
				final Optional<ChampObject> retrievedObject = graph.retrieveObject(object.getKey().get());
				
				if (!expectFound && retrievedObject.isPresent()) throw new AssertionError("Expected object to not be found, but it was found");
				if (expectFound && !retrievedObject.isPresent()) throw new AssertionError("Expected object to be found, but it was not found");
			} catch (ChampUnmarshallingException e) {
				throw new AssertionError(e);
			}
		}

		for (ChampRelationship relationship : partition.getChampRelationships()) {
			try {
				final Optional<ChampRelationship> retrievedRelationship = graph.retrieveRelationship(relationship.getKey().get());
				
				if (!expectFound && retrievedRelationship.isPresent()) throw new AssertionError("Expected relationship to not be found, but it was found");
				if (expectFound && !retrievedRelationship.isPresent()) throw new AssertionError("Expected relationship to be found, but it was not found");
			} catch (ChampUnmarshallingException e) {
				throw new AssertionError(e);
			}
		}
	}
}
