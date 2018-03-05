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
package org.onap.aai.champcore.core;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.onap.aai.champcore.ChampAPI;
import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.champcore.exceptions.ChampMarshallingException;
import org.onap.aai.champcore.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champcore.exceptions.ChampSchemaViolationException;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.model.ChampConnectionConstraint;
import org.onap.aai.champcore.model.ChampConnectionMultiplicity;
import org.onap.aai.champcore.model.ChampField;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampObject.ReservedTypes;
import org.onap.aai.champcore.model.ChampObjectConstraint;
import org.onap.aai.champcore.model.ChampPartition;
import org.onap.aai.champcore.model.ChampPropertyConstraint;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.ChampRelationshipConstraint;
import org.onap.aai.champcore.model.ChampSchema;
import org.onap.aai.champcore.schema.AlwaysValidChampSchemaEnforcer;
import org.onap.aai.champcore.schema.ChampSchemaEnforcer;
import org.onap.aai.champcore.schema.DefaultChampSchemaEnforcer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ChampSchemaTest extends BaseChampAPITest {

	@Test
	public void runInMemoryTest() {
		runTest("IN_MEMORY");
	}

	public void runTest(String apiType) {
			final String graphName = ChampSchemaTest.class.getSimpleName();

			final ChampAPI api = ChampAPI.Factory.newInstance(apiType);

			try {
				ChampSchemaTest.testChampSchemaCrud(api.getGraph(graphName));
			} catch (Throwable t) {
				throw new AssertionError(apiType + " unit test failed", t);
			}

			api.shutdown();
	}

	public static void testChampSchemaCrud(ChampGraph graph) {

		final ChampSchema schema = ChampSchema.create()
					.withObjectConstraint()
						.onType("foo")
						.withPropertyConstraint()
							.onField("property1")
							.required()
							.build()
						.withPropertyConstraint()
							.onField("property2")
							.optional()
							.build()
						.build()
					.withRelationshipConstraint()
						.onType("bar")
						.withPropertyConstraint()
							.onField("at")
							.ofType(ChampField.Type.STRING)
							.optional()
							.build()
						.withConnectionConstraint()
							.sourcedFrom("foo")
							.targetedToAny()
							.build()
						.build()
					.build();

		try {
			graph.storeSchema(schema);
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError(e);
		}

		final ChampObject emptyFoo = ChampObject.create()
												.ofType("foo")
												.withoutKey()
												.build();

		try {
			graph.storeObject(emptyFoo, Optional.empty());
		} catch (ChampMarshallingException e1) {
			throw new AssertionError(e1);
		} catch (ChampSchemaViolationException e1) {
			//Expected, since it does not have the required property "property1"
		} catch (ChampObjectNotExistsException e) {
			throw new AssertionError(e);
		} catch (ChampTransactionException e) {
		  throw new AssertionError(e);
        }

		final ChampSchema retrievedSchema = graph.retrieveSchema();

		if (!schema.equals(retrievedSchema)) throw new AssertionError("Retrieved schema is not the same as the schema that was previously stored");

		try {
			graph.updateSchema(new ChampRelationshipConstraint.Builder("bard").build());
			assertTrue(graph.retrieveSchema().getRelationshipConstraint("bard").isPresent());

			graph.updateSchema(new ChampObjectConstraint.Builder("baz").build());
			assertTrue(graph.retrieveSchema().getObjectConstraint("baz").isPresent());
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError(e);
		}

		final ChampSchema updatedSchema = graph.retrieveSchema();

		if (!updatedSchema.getObjectConstraint("baz").isPresent()) throw new AssertionError("Updated schema and retrieved, but retrieved schema did not contain updates");
		if (!updatedSchema.getRelationshipConstraint("bard").isPresent()) throw new AssertionError("Updated schema and retrieved, but retrieved schema did not contain updates");

		try {
			graph.updateSchema(new ChampObjectConstraint.Builder("foo")
														.constraint(
															new ChampPropertyConstraint.Builder(
																new ChampField.Builder("property2")
																				.build()
															)
															.required(false)
															.build()
														)
														.build());

			final ChampObject storedEmptyFoo = graph.storeObject(emptyFoo, Optional.empty());
			
			graph.deleteObject(storedEmptyFoo.getKey().get(), Optional.empty());
		} catch (ChampMarshallingException e) {
			throw new AssertionError(e);
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError(e);
		} catch (ChampObjectNotExistsException e) {
			throw new AssertionError(e);
		} catch (ChampTransactionException e) {
		  throw new AssertionError(e);
        }
		
		graph.deleteSchema();
		assertTrue(graph.retrieveSchema().equals(ChampSchema.emptySchema()));
	}

	@Test
	public void testChampSchemaFluentApi() {
		final ChampSchema schema = ChampSchema.create()
					.withObjectConstraint()
						.onType("foo")
						.withPropertyConstraint()
							.onField("bar")
							.ofType(ChampField.Type.STRING)
							.required()
							.build()
						.withPropertyConstraint()
							.onField("baz")
							.ofType(ChampField.Type.BOOLEAN)
							.optional()
							.build()
						.build()
					.withRelationshipConstraint()
						.onType("eats")
						.withPropertyConstraint()
							.onField("at")
							.ofType(ChampField.Type.STRING)
							.required()
							.build()
						.withPropertyConstraint()
							.onField("for")
							.optional()
							.build()
						.withConnectionConstraint()
							.sourcedFrom("foo")
							.targetedTo("foo")
							.withMultiplicity(ChampConnectionMultiplicity.ONE)
							.build()
						.withConnectionConstraint()
							.sourcedFrom("bar")
							.targetedTo("bar")
							.build()
					.build()
				.build();

		assertTrue(schema.getObjectConstraint("foo").get().getType().equals("foo"));
		
		for (ChampPropertyConstraint propConst : schema.getObjectConstraint("foo").get().getPropertyConstraints()) {
			if (propConst.getField().getName().equals("bar")) {
				assertTrue(propConst.getField().getJavaType().equals(String.class));
				assertTrue(propConst.isRequired());
			} else if (propConst.getField().getName().equals("baz")) {
				assertTrue(propConst.getField().getJavaType().equals(Boolean.class));
				assertTrue(!propConst.isRequired());
			} else {
				throw new AssertionError("Unknown property constraint found: " + propConst);
			}
		}

		assertTrue(schema.getRelationshipConstraint("eats").get().getType().equals("eats"));

		for (ChampPropertyConstraint propConst : schema.getRelationshipConstraint("eats").get().getPropertyConstraints()) {
			if (propConst.getField().getName().equals("at")) {
				assertTrue(propConst.getField().getJavaType().equals(String.class));
				assertTrue(propConst.isRequired());
			} else if (propConst.getField().getName().equals("for")) {
				assertTrue(propConst.getField().getJavaType().equals(String.class));
				assertTrue(!propConst.isRequired());
			} else {
				throw new AssertionError("Unknown property constraint found: " + propConst);
			}
		}

		for (ChampConnectionConstraint connConst : schema.getRelationshipConstraint("eats").get().getConnectionConstraints()) {
			if (connConst.getSourceType().equals("foo")) {
				assertTrue(connConst.getTargetType().equals("foo"));
				assertTrue(connConst.getMultiplicity() == ChampConnectionMultiplicity.ONE);
			} else if (connConst.getSourceType().equals("bar")) {
				assertTrue(connConst.getTargetType().equals("bar"));
				assertTrue(connConst.getMultiplicity() == ChampConnectionMultiplicity.MANY);
			} else {
				throw new AssertionError("Unknown connection constraint found: " + connConst);
			}
		}
	}

	@Test
	public void testDefaultChampSchemaEnforcer() {

		final ChampSchemaEnforcer schemaEnforcer = new DefaultChampSchemaEnforcer();
		final ChampSchema champSchema = ChampSchema.create()
											.withObjectConstraint()
												.onType("foo")
												.withPropertyConstraint()
													.onField("bar")
													.ofType(ChampField.Type.STRING)
													.required()
													.build()
												.build()
											.withRelationshipConstraint()
												.onType("makes")
												.withPropertyConstraint()
													.onField("bar")
													.required()
													.build()
												.withConnectionConstraint()
													.sourcedFrom("foo")
													.targetedTo("fiz")
													.withMultiplicity(ChampConnectionMultiplicity.ONE)
													.build()
												.build()
											.build();

		try {
			schemaEnforcer.validate(ChampObject.create()
												.ofType("foo")
												.withoutKey()
												.withProperty("bar", "true")
												.build(),
									champSchema.getObjectConstraint("foo").get());
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError(e);
		}

		try {
			schemaEnforcer.validate(ChampObject.create()
												.ofType("foo")
												.withoutKey()
												.build(),
									champSchema.getObjectConstraint("foo").get());
			throw new AssertionError("Failed to enforce required property constraint on object");
		} catch (ChampSchemaViolationException e) {
			//Expected
		}

		try {
			schemaEnforcer.validate(ChampObject.create()
												.ofType("foo")
												.withoutKey()
												.withProperty("bar", true)
												.build(),
									champSchema.getObjectConstraint("foo").get());
			throw new AssertionError("Failed to enforce property type constraint on object");
		} catch (ChampSchemaViolationException e) {
			//Expected
		}

		try {
			schemaEnforcer.validate(ChampRelationship.create()
												.ofType("makes")
												.withoutKey()
												.withSource()
													.ofType("foo")
													.withoutKey()
													.build()
												.withTarget()
													.ofType("fiz")
													.withoutKey()
													.build()
												.withProperty("bar", "true")
												.build(),
									champSchema.getRelationshipConstraint("makes").get()
								);
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError(e);
		}

		try {
			schemaEnforcer.validate(ChampRelationship.create()
														.ofType("makes")
														.withoutKey()
														.withSource()
															.ofType("foo")
															.withoutKey()
															.build()
														.withTarget()
															.ofType("fiz")
															.withoutKey()
															.build()
														.build(),
									champSchema.getRelationshipConstraint("makes").get()
								);
			throw new AssertionError("Failed to enforce required property constraint on relationship");
		} catch (ChampSchemaViolationException e) {
			//Expected
		}

		try {
			schemaEnforcer.validate(ChampPartition.create()
												.withObject(
													ChampObject.create()
																.ofType("foo")
																.withoutKey()
																.withProperty("bar", "true")
																.build()
												)
												.withObject(
													ChampObject.create()
																.ofType("fiz")
																.withoutKey()
																.build()
												)
												.withRelationship(
													ChampRelationship.create()
																		.ofType("makes")
																		.withoutKey()
																		.withSource()
																			.ofType("foo")
																			.withoutKey()
																			.withProperty("bar", "true")
																			.build()
																		.withTarget()
																			.ofType("fiz")
																			.withoutKey()
																			.build()
																		.withProperty("bar",  "true")
																		.build()
												)
												.withRelationship(
													ChampRelationship.create()
																		.ofType("makes")
																		.withoutKey()
																		.withSource()
																			.ofType("fiz")
																			.withoutKey()
																			.build()
																		.withTarget()
																			.ofType("foo")
																			.withoutKey()
																			.withProperty("bar", "true")
																			.build()
																		.withProperty("bar", "true")
																		.build()
												)
												.build(),
												champSchema					
								);
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError(e);
		}

		try {
			schemaEnforcer.validate(ChampPartition.create()
												.withObject(
													ChampObject.create()
																.ofType("foo")
																.withoutKey()
																.withProperty("bar", "true")
																.build()
												)
												.withObject(
													ChampObject.create()
																.ofType("fiz")
																.withoutKey()
																.build()
												)
												.withRelationship(
													ChampRelationship.create()
																		.ofType("makes")
																		.withoutKey()
																		.withSource()
																			.ofType("foo")
																			.withoutKey()
																			.withProperty("bar", "true")
																			.build()
																		.withTarget()
																			.ofType("fiz")
																			.withoutKey()
																			.build()
																		.withProperty("bar",  "true")
																		.build()
												)
												.withRelationship(
													ChampRelationship.create()
																		.ofType("makes")
																		.withoutKey()
																		.withSource()
																			.ofType("foo")
																			.withoutKey()
																			.withProperty("bar", "true")
																			.build()
																		.withTarget()
																			.ofType("fiz")
																			.withoutKey()
																			.build()
																		.withProperty("bar", "true")
																		.build()
												)
												.build(),
												champSchema					
								);
			throw new AssertionError("Failed to enforce connection constraint on relationship type 'makes'");
		} catch (ChampSchemaViolationException e) {
			//Expected
		}
	}

	@Test
	public void testAlwaysValidChampSchemaEnforcer() {

		final ChampSchemaEnforcer schemaEnforcer = new AlwaysValidChampSchemaEnforcer();

		try {
			schemaEnforcer.validate(ChampObject.create()
												.ofType("foo")
												.withoutKey()
												.withProperty("bar", true)
												.build(),
									new ChampObjectConstraint.Builder("foo")
																.constraint(
																	new ChampPropertyConstraint.Builder(
																		new ChampField.Builder("bar")
																						.type(ChampField.Type.STRING)
																						.build()
																	)
																	.required(true)
																	.build()
																)
																.build()
								);

			schemaEnforcer.validate(ChampRelationship.create()
												.ofType("foo")
												.withoutKey()
												.withSource()
													.ofType("foo")
													.withoutKey()
													.build()
												.withTarget()
													.ofType("fiz")
													.withoutKey()
													.build()
												.withProperty("bar", true)
												.build(),
									new ChampRelationshipConstraint.Builder("bar")
																.constraint(
																	new ChampPropertyConstraint.Builder(
																		new ChampField.Builder("bar")
																						.type(ChampField.Type.STRING)
																						.build()
																	)
																	.required(true)
																	.build()
																)
																.build()
								);

			schemaEnforcer.validate(ChampPartition.create()
												.withObject(
													ChampObject.create()
																.ofType("foo")
																.withoutKey()
																.withProperty("bar", true)
																.build()
												)
												.withObject(
													ChampObject.create()
																.ofType("fiz")
																.withoutKey()
																.withProperty("bar", true)
																.build()
												)
												.withRelationship(
													ChampRelationship.create()
																		.ofType("makes")
																		.withoutKey()
																		.withSource()
																			.ofType("foo")
																			.withoutKey()
																			.build()
																		.withTarget()
																			.ofType("fiz")
																			.withoutKey()
																			.build()
																		.build()
												)
												.withRelationship(
													ChampRelationship.create()
																		.ofType("makes")
																		.withoutKey()
																		.withSource()
																			.ofType("foo")
																			.withoutKey()
																			.build()
																		.withTarget()
																			.ofType("fiz")
																			.withoutKey()
																			.build()
																		.withProperty("bar", true)
																		.build()
												)
												.build(),
								ChampSchema.create()
											.withObjectConstraint()
												.onType("foo")
												.withPropertyConstraint()
													.onField("bar")
													.required()
													.build()
												.build()
											.withRelationshipConstraint()
												.onType("makes")
												.withPropertyConstraint()
													.onField("bar")
													.required()
													.build()
												.withConnectionConstraint()
													.sourcedFrom("foo")
													.targetedTo("fiz")
													.withMultiplicity(ChampConnectionMultiplicity.ONE)
													.build()
												.build()
											.withRelationshipConstraint()
												.onType("uses")
												.withConnectionConstraint()
													.sourcedFromAny()
													.targetedTo("computer")
													.build()
												.build()
											.withRelationshipConstraint()
												.onType("destroys")
												.withConnectionConstraint()
													.sourcedFrom("computer")
													.targetedToAny()
													.build()
												.build()
											.build()
													
								);
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError(e);
		}
	}

	@Test
	public void testFluentSchemaApi() {
		final ChampSchema schema = ChampSchema.create()
												.withObjectConstraint()
													.onType("a")
													.withPropertyConstraint()
														.onField("z")
														.ofType(ChampField.Type.STRING)
														.optional()
														.build()
													.build()
												.withObjectConstraint()
													.onType("b")
													.withPropertyConstraint()
														.onField("y")
														.ofType(ChampField.Type.LONG)
														.required()
														.build()
													.build()
												.withRelationshipConstraint()
													.onType("one")
													.withPropertyConstraint()
														.onField("nine")
														.ofType(ChampField.Type.INTEGER)
														.optional()
														.build()
													.withConnectionConstraint()
														.sourcedFrom("a")
														.targetedTo("b")
														.withMultiplicity(ChampConnectionMultiplicity.NONE)
														.build()
													.withConnectionConstraint()
														.sourcedFrom("a")
														.targetedToAny()
														.withMultiplicity(ChampConnectionMultiplicity.ONE)
														.build()
													.withConnectionConstraint()
														.sourcedFromAny()
														.targetedTo("b")
														.withMultiplicity(ChampConnectionMultiplicity.MANY)
														.build()
													.withConnectionConstraint()
														.sourcedFromAny()
														.targetedToAny()
														.withMultiplicity(ChampConnectionMultiplicity.MANY)
														.build()
													.build()
												.build();

		final ChampObjectConstraint aObjConstraint = schema.getObjectConstraint("a").get();

		assertTrue(aObjConstraint.getType().equals("a"));

		final ChampPropertyConstraint zPropertyConstraint = aObjConstraint.getPropertyConstraint("z").get();

		assertTrue(zPropertyConstraint.getField().getName().equals("z"));
		assertTrue(zPropertyConstraint.getField().getJavaType().equals(String.class));
		assertTrue(!zPropertyConstraint.isRequired());

		final ChampObjectConstraint bObjConstraint = schema.getObjectConstraint("b").get();

		assertTrue(bObjConstraint.getType().equals("b"));

		final ChampPropertyConstraint yPropertyConstraint = bObjConstraint.getPropertyConstraint("y").get();

		assertTrue(yPropertyConstraint.getField().getName().equals("y"));
		assertTrue(yPropertyConstraint.getField().getJavaType().equals(Long.class));
		assertTrue(yPropertyConstraint.isRequired());

		final ChampRelationshipConstraint oneRelConstraint = schema.getRelationshipConstraint("one").get();

		assertTrue(oneRelConstraint.getType().equals("one"));

		final ChampPropertyConstraint ninePropertyConstraint = oneRelConstraint.getPropertyConstraint("nine").get();

		assertTrue(ninePropertyConstraint.getField().getName().equals("nine"));
		assertTrue(ninePropertyConstraint.getField().getJavaType().equals(Integer.class));
		assertTrue(!ninePropertyConstraint.isRequired());

		final Set<ChampConnectionConstraint> connectionConstraints = oneRelConstraint.getConnectionConstraints();

		for (ChampConnectionConstraint cc : connectionConstraints) {
			if (cc.getSourceType().equals("a") && cc.getTargetType().equals("b")) {
				assertTrue(cc.getMultiplicity() == ChampConnectionMultiplicity.NONE);
			} else if (cc.getSourceType().equals(ReservedTypes.ANY.toString()) && cc.getTargetType().equals("b")) {
				assertTrue(cc.getMultiplicity() == ChampConnectionMultiplicity.MANY);
			} else if (cc.getSourceType().equals(ReservedTypes.ANY.toString()) && cc.getTargetType().equals(ReservedTypes.ANY.toString())) {
				assertTrue(cc.getMultiplicity() == ChampConnectionMultiplicity.MANY);
			} else if (cc.getSourceType().equals("a") && cc.getTargetType().equals(ReservedTypes.ANY.toString())) {
				assertTrue(cc.getMultiplicity() == ChampConnectionMultiplicity.ONE);
			} else {
				throw new AssertionError("Found unspecified connection constraint " + cc);
			}
		}	
	}

	@Test
	public void testJacksonObjectMapping() {
		final ChampSchema schema = ChampSchema.create()
												.withObjectConstraint()
													.onType("a")
													.withPropertyConstraint()
														.onField("z")
														.ofType(ChampField.Type.STRING)
														.optional()
														.build()
													.build()
												.withObjectConstraint()
													.onType("b")
													.withPropertyConstraint()
														.onField("y")
														.ofType(ChampField.Type.LONG)
														.required()
														.build()
													.build()
												.withRelationshipConstraint()
													.onType("one")
													.withPropertyConstraint()
														.onField("nine")
														.ofType(ChampField.Type.INTEGER)
														.optional()
														.build()
													.withConnectionConstraint()
														.sourcedFrom("a")
														.targetedTo("b")
														.withMultiplicity(ChampConnectionMultiplicity.NONE)
														.build()
													.withConnectionConstraint()
														.sourcedFrom("a")
														.targetedToAny()
														.withMultiplicity(ChampConnectionMultiplicity.ONE)
														.build()
													.withConnectionConstraint()
														.sourcedFromAny()
														.targetedTo("b")
														.withMultiplicity(ChampConnectionMultiplicity.MANY)
														.build()
													.withConnectionConstraint()
														.sourcedFromAny()
														.targetedToAny()
														.withMultiplicity(ChampConnectionMultiplicity.MANY)
														.build()
													.build()
												.build();

		final ObjectMapper om = new ObjectMapper();

		try {
			final byte[] serialized = om.writeValueAsBytes(schema);
			System.out.println(new String(serialized, "UTF-8"));
			final ChampSchema deserialized = om.readValue(serialized, ChampSchema.class);
			assert schema.equals(deserialized);
		} catch (IOException e) {
			throw new AssertionError(e);
		}

	}
}
