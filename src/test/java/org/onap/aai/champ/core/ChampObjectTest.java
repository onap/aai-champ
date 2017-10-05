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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.onap.aai.champ.ChampAPI;
import org.onap.aai.champ.ChampGraph;
import org.onap.aai.champ.exceptions.ChampMarshallingException;
import org.onap.aai.champ.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champ.exceptions.ChampSchemaViolationException;
import org.onap.aai.champ.exceptions.ChampUnmarshallingException;
import org.onap.aai.champ.model.ChampCardinality;
import org.onap.aai.champ.model.ChampField;
import org.onap.aai.champ.model.ChampObject;
import org.onap.aai.champ.model.ChampSchema;

public class ChampObjectTest extends BaseChampAPITest {

	@Test
	public void testHashCode() {
		final ChampObject foo1 = ChampObject.create()
					.ofType("foo")
					.withoutKey()
					.withProperty("property", "value")
					.withProperty("prop", 1)
					.build();

		final ChampObject foo2 = ChampObject.create()
											.ofType("foo")
											.withoutKey()
											.withProperty("property", "value")
											.withProperty("prop", 1)
											.build();

		final ChampObject foo1Copy = ChampObject.create()
												.from(foo1)
												.withoutKey()
												.build();

		final ChampObject foo2Copy = ChampObject.create()
												.from(foo2)
												.withoutKey()
												.build();

		assertTrue(foo1.hashCode() == foo2.hashCode());
		assertTrue(foo1.hashCode() == foo1.hashCode());
		assertTrue(foo2.hashCode() == foo2.hashCode());
		assertTrue(foo1.hashCode() == foo1Copy.hashCode());
		assertTrue(foo2.hashCode() == foo2Copy.hashCode());
		
		assertTrue(Collections.singleton(foo1).contains(foo1));
		assertTrue(Collections.singleton(foo1).contains(foo1Copy));
	}

	@Test
	public void runTest() {
		for (ChampGraph.Type apiType : ChampGraph.Type.values()) {
			final String graphName = ChampObjectTest.class.getSimpleName();
			switch (apiType) {
				case TITAN:
					cleanUp(graphName);
					break;
				default:
				break;
			}

			final ChampAPI api = ChampAPI.Factory.newInstance(apiType);
			ChampObjectTest.testChampObjectCrud(api.getGraph(graphName));
			testChampObjectReservedProperties(api.getGraph(graphName));
			api.shutdown();
		}
	}
	
	public static void testChampObjectCrud(ChampGraph graph) {
		final ChampObject bookooObject = ChampObject.create()
													.ofType("foo")
													.withoutKey()
													.withProperty("property1", "value1")
													.withProperty("integer",  1)
													.withProperty("long", 1L)
													.withProperty("double", 1.2)
													.withProperty("float", 2.3F)
													.withProperty("string", "foo")
													.withProperty("boolean", true)
													.withProperty("list", Collections.singletonList("list"))
													.withProperty("set", Collections.singleton("set"))
													.build();

		final ChampObject storedBookooObject;

		try {

			graph.storeSchema(ChampSchema.create()
											.withObjectConstraint()
												.onType("foo")
												.withPropertyConstraint()
													.onField("list")
													.ofType(ChampField.Type.STRING)
													.cardinality(ChampCardinality.LIST)
													.optional()
													.build()
												.withPropertyConstraint()
													.onField("set")
													.ofType(ChampField.Type.STRING)
													.cardinality(ChampCardinality.SET)
													.optional()
													.build()
												.build()
											.build());

			storedBookooObject = graph.storeObject(bookooObject);

			assertTrue(storedBookooObject.getProperty("property1").get().equals("value1"));
			assertTrue(storedBookooObject.getProperty("integer").get().equals(1));
			assertTrue(storedBookooObject.getProperty("long").get().equals(1L));
			assertTrue(storedBookooObject.getProperty("double").get().equals(1.2));
			assertTrue(storedBookooObject.getProperty("float").get().equals(2.3F));
			assertTrue(storedBookooObject.getProperty("string").get().equals("foo"));
			assertTrue(storedBookooObject.getProperty("boolean").get().equals(true));
			assertTrue(storedBookooObject.getProperty("list").get().equals(Collections.singletonList("list")));
			assertTrue(storedBookooObject.getProperty("set").get().equals(Collections.singleton("set")));

			final Optional<ChampObject> retrievedBookooObject = graph.retrieveObject(storedBookooObject.getKey().get());
			final Stream<ChampObject> emptyStream = graph.queryObjects(new HashMap<String, Object> () {{
																				put(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString(), "foo");
																				put("long", 2L);
																			}});
			
			assertTrue(emptyStream.limit(1).count() == 0);

			final Stream<ChampObject> oneStream = graph.queryObjects(new HashMap<String, Object> () {{
																				put(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString(), "foo");
																				put("long", 1L);
																			}});
			final List<ChampObject> oneObject = oneStream.limit(2).collect(Collectors.toList());
			assertTrue(oneObject.size() == 1);
			assertTrue(oneObject.get(0).equals(storedBookooObject));

			final List<ChampObject> queryByKey = graph.queryObjects(Collections.singletonMap(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_KEY.toString(), storedBookooObject.getKey().get()))
														.limit(2)
														.collect(Collectors.toList());

			assertTrue(queryByKey.size() == 1);
			assertTrue(queryByKey.get(0).equals(storedBookooObject));

			if (!retrievedBookooObject.isPresent()) throw new AssertionError("Failed to retrieve stored object " + bookooObject);
			if (!storedBookooObject.equals(retrievedBookooObject.get())) throw new AssertionError("Retrieved object does not equal stored object");

			final ChampObject updatedBookoo = graph.storeObject(ChampObject.create()
													.from(storedBookooObject)
													.withKey(storedBookooObject.getKey().get())
													.withProperty("long", 2L)
													.build());

			final Optional<ChampObject> retrievedUpdBookooObject = graph.retrieveObject(updatedBookoo.getKey().get());

			assertTrue(updatedBookoo.getProperty("property1").get().equals("value1"));
			assertTrue(updatedBookoo.getProperty("integer").get().equals(1));
			assertTrue(updatedBookoo.getProperty("long").get().equals(2L));
			assertTrue(updatedBookoo.getProperty("double").get().equals(1.2));
			assertTrue(updatedBookoo.getProperty("float").get().equals(2.3F));
			assertTrue(updatedBookoo.getProperty("string").get().equals("foo"));
			assertTrue(updatedBookoo.getProperty("boolean").get().equals(true));
			assertTrue(updatedBookoo.getProperty("list").get().equals(Collections.singletonList("list")));
			assertTrue(updatedBookoo.getProperty("set").get().equals(Collections.singleton("set")));

			if (!retrievedUpdBookooObject.isPresent()) throw new AssertionError("Failed to retrieve stored object " + bookooObject);
			if (!updatedBookoo.equals(retrievedUpdBookooObject.get())) throw new AssertionError("Retrieved object does not equal stored object");
			
			//validate the replaceObject method
			final ChampObject replacedBookoo = graph.replaceObject(ChampObject.create()
					.ofType("foo")
					.withKey(storedBookooObject.getKey().get())
					.withProperty("property1", "value2")
					.withProperty("list", Collections.singletonList("list"))
					.withProperty("set", Collections.singleton("set"))
					.build());
			
			final Optional<ChampObject> retrievedReplacedBookooObject = graph.retrieveObject(replacedBookoo.getKey().get());
			
			assertTrue(replacedBookoo.getProperties().size()==3);
			assertTrue(replacedBookoo.getProperty("property1").get().equals("value2"));
			assertTrue(replacedBookoo.getProperty("list").get().equals(Collections.singletonList("list")));
			assertTrue(replacedBookoo.getProperty("set").get().equals(Collections.singleton("set")));
			
			
			if (!retrievedReplacedBookooObject.isPresent()) throw new AssertionError("Failed to retrieve stored object " + replacedBookoo);
			if (!replacedBookoo.equals(retrievedReplacedBookooObject.get())) throw new AssertionError("Retrieved object does not equal stored object");
			

			
			
			
			graph.deleteObject(storedBookooObject.getKey().get());
			if (graph.retrieveObject(storedBookooObject.getKey().get()).isPresent()) throw new AssertionError("Object not successfully deleted");

			assertTrue(graph.queryObjects(Collections.emptyMap()).count() == 0);
			assertTrue(graph.queryRelationships(Collections.emptyMap()).count() == 0);
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError("Schema mismatch while storing object", e);
		} catch (ChampMarshallingException e) {
			throw new AssertionError("Marshalling exception while storing object", e);
		} catch (ChampUnmarshallingException e) {
			throw new AssertionError("Unmarshalling exception while retrieving object", e);
		}catch (ChampObjectNotExistsException e) {
			throw new AssertionError("Missing object on delete/update", e);
		}

		try {
			graph.deleteObject(storedBookooObject.getKey().get());
			throw new AssertionError("Delete succeeded when it should have failed");
		} catch (ChampObjectNotExistsException e) {
			//Expected
		}

		try {
			graph.storeObject(ChampObject.create()
											.ofType("foo")
											.withKey("non-existent object key")
											.build());
			throw new AssertionError("Expected ChampObjectNotExistsException but object was successfully stored");
		} catch (ChampObjectNotExistsException e) {
			//Expected
		} catch (ChampMarshallingException e) {
			throw new AssertionError(e);
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError(e);
		}
		
		try {
			// validate the replaceObject method when Object key is not passed
			graph.replaceObject(
					ChampObject.create().ofType("foo").withoutKey().withProperty("property1", "value2").build());
		} catch (ChampObjectNotExistsException e) {
			// Expected
		} catch (ChampMarshallingException e) {
			throw new AssertionError(e);
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError(e);
		}
		
	}

	public void testChampObjectReservedProperties(ChampGraph graph) {

		for (ChampObject.ReservedPropertyKeys key : ChampObject.ReservedPropertyKeys.values()) {
			try {
				ChampObject.create()
							.ofType(ChampObject.ReservedTypes.ANY.toString())
							.withoutKey()
							.withProperty(key.toString(), "")
							.build();
				throw new AssertionError("Allowed reserved property key to be used during object creation");
			} catch (IllegalArgumentException e) {
				//Expected
			}
		}
	}

	@Test
	public void testFluentObjectCreation() {
		final Object value1 = new Object();
		final String value2 = "value2";
		final float value3 = 0.0f;

		final ChampObject champObject1 = ChampObject.create()
													.ofType("foo")
													.withoutKey()
													.withProperty("key1", value1)
													.withProperty("key2", value2)
													.withProperty("key3", value3)
													.build();

		assertTrue(champObject1.getKey().equals(Optional.empty()));
		assertTrue(champObject1.getKey().isPresent() == false);
		assertTrue(champObject1.getType().equals("foo"));
		assertTrue(champObject1.getProperty("key1").get() instanceof Object);
		assertTrue(champObject1.getProperty("key1").get().equals(value1));
		assertTrue(champObject1.getProperty("key2").get() instanceof String);
		assertTrue(champObject1.getProperty("key2").get().equals(value2));
		assertTrue(champObject1.getProperty("key3").get() instanceof Float);
		assertTrue(champObject1.getProperty("key3").get().equals(value3));

		final ChampObject champObject2 = ChampObject.create()
													.ofType("foo")
													.withKey(1)
													.withProperty("key1", value1)
													.withProperty("key2", value2)
													.withProperty("key3", value3)
													.build();

		assertTrue(champObject2.getType().equals("foo"));
		assertTrue(champObject2.getKey().isPresent() == true);
		assertTrue(champObject2.getKey().get() instanceof Integer);
		assertTrue(champObject2.getKey().get().equals(1));
		assertTrue(champObject2.getProperty("key1").get() instanceof Object);
		assertTrue(champObject2.getProperty("key1").get().equals(value1));
		assertTrue(champObject2.getProperty("key2").get() instanceof String);
		assertTrue(champObject2.getProperty("key2").get().equals(value2));
		assertTrue(champObject2.getProperty("key3").get() instanceof Float);
		assertTrue(champObject2.getProperty("key3").get().equals(value3));
	}
}
