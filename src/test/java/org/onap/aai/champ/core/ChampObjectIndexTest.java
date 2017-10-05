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
package org.onap.aai.champ.core;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.onap.aai.champ.ChampAPI;
import org.onap.aai.champ.ChampGraph;
import org.onap.aai.champ.exceptions.ChampIndexNotExistsException;
import org.onap.aai.champ.model.ChampObjectIndex;

public class ChampObjectIndexTest extends BaseChampAPITest {
	@Test
	public void runTest() {
		for (ChampGraph.Type apiType : ChampGraph.Type.values()) {
			final ChampAPI api = ChampAPI.Factory.newInstance(apiType);
			final String graphName = api.getClass().getSimpleName();

			switch (apiType) {
			case IN_MEMORY:
			break;
			case TITAN:
				cleanUp(graphName);
			break;
			default:
			break;
			}

			ChampObjectIndexTest.testChampObjectIndexCrud(api.getGraph(graphName));

			api.shutdown();
		}
	}

	private static void testChampObjectIndexCrud(ChampGraph graph) {
		
		final ChampObjectIndex objectIndex = ChampObjectIndex.create()
																.ofName("fooObjectIndex")
																.onType("foo")
																.forField("propertyName")
																.build();

		testChampObjectIndexStorage(graph, objectIndex);
		testChampObjectIndexDelete(graph, objectIndex);
	}

	private static void testChampObjectIndexDelete(ChampGraph graph, ChampObjectIndex objectIndex) {

		if (!graph.capabilities().canDeleteObjectIndices()) {
			try {
				graph.deleteObjectIndex("someindex");
				throw new AssertionError("Graph claims it does not support object index delete, but failed to throw UnsupportedOperationException");
			} catch (UnsupportedOperationException e) {
			} catch (ChampIndexNotExistsException e) {
				throw new AssertionError("Graph claims it does not support object index delete, but failed to throw UnsupportedOperationException");
			}
		} else {
			try {
				graph.deleteObjectIndex(objectIndex.getName());
				
				final Optional<ChampObjectIndex> retrievedObjectIndex = graph.retrieveObjectIndex(objectIndex.getName());
	
				if (retrievedObjectIndex.isPresent()) throw new AssertionError("Retrieved object index after deleting it");
	
				final Stream<ChampObjectIndex> retrievedObjectIndices = graph.retrieveObjectIndices();
				final Collection<ChampObjectIndex> allObjectIndices = retrievedObjectIndices.collect(Collectors.toList());
	
				if (allObjectIndices.contains(objectIndex)) throw new AssertionError("Retrieve all indices contained index previously deleted");
				if (allObjectIndices.size() != 0) throw new AssertionError("Wrong number of indices returned by retrieve all indices");
			
			} catch (ChampIndexNotExistsException e) {
				throw new AssertionError(e);
			}
	
			try {
				graph.deleteObjectIndex(objectIndex.getName());
				throw new AssertionError("Failed to throw exception on non-existent object index");
			} catch (ChampIndexNotExistsException e) {
				//Expected
			}
		}
	}

	private static void testChampObjectIndexStorage(ChampGraph graph, ChampObjectIndex objectIndex) {

		graph.storeObjectIndex(objectIndex);
		graph.storeObjectIndex(objectIndex); //Test storing an already existing object index

		assertTrue(!graph.retrieveRelationshipIndex(objectIndex.getName()).isPresent()); //Make sure this wasn't stored as an object index

		final Optional<ChampObjectIndex> retrieveObjectIndex = graph.retrieveObjectIndex(objectIndex.getName());
		
		if (!retrieveObjectIndex.isPresent()) throw new AssertionError("Failed to retrieve object index after storing it");
		if (!objectIndex.equals(retrieveObjectIndex.get())) throw new AssertionError("Non-equal object index returned from API after storing it");
		
		final Stream<ChampObjectIndex> retrievedObjectIndices = graph.retrieveObjectIndices();
		final Collection<ChampObjectIndex> allObjectIndices = retrievedObjectIndices.collect(Collectors.toList());

		if (!allObjectIndices.contains(objectIndex)) throw new AssertionError("Retrieve all indices did not contained index previously stored");
		if (allObjectIndices.size() != 1) throw new AssertionError("Wrong number of indices returned by retrieve all indices");

		assertTrue(!graph.retrieveObjectIndex("nonExistentIndexName").isPresent());
	}

	@Test
	public void testFluentRelationshipCreation() {
		final ChampObjectIndex objectIndex = ChampObjectIndex.create()
																.ofName("fooNameIndex")
																.onType("foo")
																.forField("name")
																.build();

		assertTrue(objectIndex.getName().equals("fooNameIndex"));
		assertTrue(objectIndex.getType().equals("foo"));
		assertTrue(objectIndex.getField().getName().equals("name"));
	}
}
