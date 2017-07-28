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
package org.openecomp.aai.champ.perf;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openecomp.aai.champ.ChampGraph;
import org.openecomp.aai.champ.exceptions.ChampMarshallingException;
import org.openecomp.aai.champ.exceptions.ChampObjectNotExistsException;
import org.openecomp.aai.champ.exceptions.ChampRelationshipNotExistsException;
import org.openecomp.aai.champ.exceptions.ChampSchemaViolationException;
import org.openecomp.aai.champ.exceptions.ChampUnmarshallingException;
import org.openecomp.aai.champ.graph.impl.InMemoryChampGraphImpl;
import org.openecomp.aai.champ.graph.impl.TitanChampGraphImpl;
import org.openecomp.aai.champ.model.ChampField;
import org.openecomp.aai.champ.model.ChampObject;
import org.openecomp.aai.champ.model.ChampObjectIndex;
import org.openecomp.aai.champ.model.ChampRelationship;
import org.openecomp.aai.champ.model.ChampRelationshipIndex;
import org.openecomp.aai.champ.model.ChampSchema;
import org.openecomp.aai.champ.schema.ChampSchemaEnforcer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanFactory.Builder;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.util.TitanCleanup;

public class ChampAPIPerformanceTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChampAPIPerformanceTest.class);

	private static final int NUM_OBJECTS = 1000;
	private static final int NUM_RELATIONSHIPS = 1000;
	private static final String GRAPH_NAME = ChampAPIPerformanceTest.class.getSimpleName();

	private static final String getGraphName() {
		return GRAPH_NAME;
	}

	private static void cleanUp(String graphName, Map<String, String> settings) {
		LOGGER.debug("Cleaning up graph {}", graphName);

		try {
			final Builder graphBuilder = TitanFactory.build();
			
			for (Entry<String, String> setting : settings.entrySet()) {
				graphBuilder.set(setting.getKey(), setting.getValue());
			}

			final String storageBackend = settings.getOrDefault("storage.backend", "inmemory");

			if (storageBackend.equals("cassandra") ||
				storageBackend.equals("cassandrathrift") ||
				storageBackend.equals("astyanax") ||
				storageBackend.equals("embeddedcassandra")) {
				graphBuilder.set("storage.cassandra.keyspace", graphName);
			} else if (storageBackend.equals("hbase")) {
				graphBuilder.set("storage.hbase.table", graphName);
			}

			final TitanGraph graph = graphBuilder.open();

			graph.close();
			TitanCleanup.clear(graph);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Could not clean up graph - unable to instantiate");
		}
	}

	public static void main(String[] args) {

		if (args.length < 1 || !args[0].startsWith("--champ.graph.type=")) {
			throw new RuntimeException("Must provide --champ.graph.type=" + ChampGraph.Type.values() + " as first parameter");
		}

		final ChampGraph.Type graphType = ChampGraph.Type.valueOf(args[0].split("=")[1]);

		final Map<String, String> settings = new HashMap<String, String> ();

		for (int i = 1; i < args.length; i++) {
			if (!args[i].startsWith("--")) throw new RuntimeException("Bad command line argument: " + args[i]);

			final String[] keyValue = args[i].replaceFirst("--", "").split("=");

			if (keyValue.length != 2) throw new RuntimeException("Bad command line argument: " + args[i]);

			settings.put(keyValue[0], keyValue[1]);
		}

		LOGGER.info("Provided graph settings: " + settings);

		if (graphType == ChampGraph.Type.TITAN)	cleanUp(getGraphName(), settings);

		LOGGER.info("Graph cleaned, instantiating ChampGraph");

		final ChampGraph graph;

		switch (graphType) {
		case IN_MEMORY:
			final InMemoryChampGraphImpl.Builder inMemGraphBuilder = new InMemoryChampGraphImpl.Builder();

			if (settings.containsKey("champ.schema.enforcer")) {
				final String schemaEnforcerClassStr = settings.get("champ.schema.enforcer");

				try {
					final Class<?> schemaEnforcer = Class.forName(schemaEnforcerClassStr);

					if (!schemaEnforcer.isAssignableFrom(ChampSchemaEnforcer.class)) throw new RuntimeException("Unknown ChampSchemaEnforcer " + schemaEnforcer);
					
					inMemGraphBuilder.schemaEnforcer((ChampSchemaEnforcer) schemaEnforcer.newInstance());
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}

			graph = inMemGraphBuilder.build();
		break;
		case TITAN:		
			final TitanChampGraphImpl.Builder graphBuilder = new TitanChampGraphImpl.Builder(getGraphName());
			
			for (Entry<String, String> setting : settings.entrySet()) {
				graphBuilder.property(setting.getKey(), setting.getValue());
			}
	
			graph = graphBuilder.build();
		break;
		default:
			throw new RuntimeException("Unknown ChampGraph.Type " + graphType);
		}

		if (graph.queryObjects(Collections.emptyMap()).limit(1).count() > 0) {
			graph.shutdown();
			throw new RuntimeException("Expected empty graph");
		}

		LOGGER.info("Graph instantiated, warming up JVM");
		warmUp(graph);

		LOGGER.info("Warm up complete, starting to record performance measurements");

		LOGGER.info("Performance without indexing/schema");

		storeObjects(graph, false);
		storeRelationships(graph, false);
		retrieveIndividualObjects(graph, false);
		retrieveBulkRelationships(graph, false);
		retrieveIndividualRelationships(graph, false);

		LOGGER.info("Storing indices + schema");

		storeIndices(graph, false);
		storeSchema(graph, false);

		LOGGER.info("Stored indices + schema");

		LOGGER.info("Performance with indexing + schema");

		storeObjects(graph, false);
		storeRelationships(graph, false);
		retrieveIndividualObjects(graph, false);
		retrieveBulkRelationships(graph, false);
		retrieveIndividualRelationships(graph, false);

		LOGGER.info("Performance test complete, shutting down graph");

		graph.shutdown();

		LOGGER.info("Graph shutdown, JVM exiting");
	}

	private static void storeSchema(ChampGraph graph, boolean warmUp) {
		try {
			graph.storeSchema(
				ChampSchema.create()
							.withObjectConstraint()
								.onType("foo")
								.withPropertyConstraint()
									.onField("fooObjectNumber")
									.optional()
									.build()
								.build()
							.withRelationshipConstraint()
								.onType("bar")
								.withPropertyConstraint()
									.onField("barObjectNumber")
									.ofType(ChampField.Type.INTEGER)
									.optional()
									.build()
								.build()
							.build()
			);
		} catch (ChampSchemaViolationException e) {
			throw new AssertionError(e);
		}
	}

	private static void storeIndices(ChampGraph graph, boolean warmUp) {
		graph.storeObjectIndex(
			ChampObjectIndex.create()
							.ofName("objectNumberIndex")
							.onType("foo")
							.forField("objectNumber")
							.build()
		);

		graph.storeRelationshipIndex(ChampRelationshipIndex.create()
															.ofName("relationshipNumberIndex")
															.onType("bazz")
															.forField("relationshipNumber")
															.build()
		);
	}

	private static void warmUp(ChampGraph graph) {
		storeObjects(graph, false);
		storeRelationships(graph, false);
		retrieveIndividualObjects(graph, false);
		retrieveBulkRelationships(graph, false);
		retrieveIndividualRelationships(graph, false);
	}

	private static void retrieveIndividualRelationships(ChampGraph graph, boolean warmUp) {
		final double[] latencies = new double[NUM_RELATIONSHIPS];
		final long totalStartTime = System.nanoTime();

		for (int i = 0; i < NUM_RELATIONSHIPS; i++) {
			final long startTime = System.nanoTime();

			final Stream<ChampRelationship> objects = graph.queryRelationships(Collections.singletonMap("relationshipNumber", i));
			objects.findFirst().get();
			final double elapsedMs = (System.nanoTime() - startTime) / 1000.0 / 1000.0;
			latencies[i] = elapsedMs;
		}

		final double totalElapsedTimeSecs = (System.nanoTime() - totalStartTime) / 1000.0 / 1000.0 / 1000.0;
		LOGGER.info("Individually read " + NUM_RELATIONSHIPS + " relationships in " + totalElapsedTimeSecs + "s (" + NUM_RELATIONSHIPS / totalElapsedTimeSecs + " relationships/s)");

		Arrays.sort(latencies);

		if (!warmUp) {
			LOGGER.info("Retrieve individual relationship latencies");
			LOGGER.info("\t50th percentile: " + latencies[(int) (NUM_RELATIONSHIPS * 0.50)]);
			LOGGER.info("\t75th percentile: " + latencies[(int) (NUM_RELATIONSHIPS * 0.75)]);
			LOGGER.info("\t90th percentile: " + latencies[(int) (NUM_RELATIONSHIPS * 0.90)]);
			LOGGER.info("\t99th percentile: " + latencies[(int) (NUM_RELATIONSHIPS * 0.99)]);
		}
	}

	private static void retrieveIndividualObjects(ChampGraph graph, boolean warmUp) {
	
		final double[] latencies = new double[NUM_OBJECTS];
		final long totalStartTime = System.nanoTime();

		for (int i = 0; i < NUM_OBJECTS; i++) {
			final long startTime = System.nanoTime();
			final Stream<ChampObject> objects = graph.queryObjects(Collections.singletonMap("objectNumber", i));
			
			objects.findFirst().get();

			final double elapsedMs = (System.nanoTime() - startTime) / 1000.0 / 1000.0;

			latencies[i] = elapsedMs;
		}

		final double totalElapsedTimeSecs = (System.nanoTime() - totalStartTime) / 1000.0 / 1000.0 / 1000.0;

		LOGGER.info("Individually read " + NUM_OBJECTS + " objects in " + totalElapsedTimeSecs + "s (" + NUM_OBJECTS / totalElapsedTimeSecs + " objects/s)");
		Arrays.sort(latencies);

		if (!warmUp) {
			LOGGER.info("Retrieve individual object latencies");
			LOGGER.info("\t50th percentile: " + latencies[(int) (NUM_OBJECTS * 0.50)]);
			LOGGER.info("\t75th percentile: " + latencies[(int) (NUM_OBJECTS * 0.75)]);
			LOGGER.info("\t90th percentile: " + latencies[(int) (NUM_OBJECTS * 0.90)]);
			LOGGER.info("\t99th percentile: " + latencies[(int) (NUM_OBJECTS * 0.99)]);
		} 
	}

	private static List<ChampObject> retrieveBulkObjects(ChampGraph graph, boolean warmUp) {

		final long startTime = System.nanoTime();
		final Stream<ChampObject> objects = graph.queryObjects( 
							Collections.singletonMap(
								ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString(), "foo"
							)
						);
		
		final List<ChampObject> objectsAsList = objects.collect(Collectors.toList());
		final double elapsedSecs = (System.nanoTime() - startTime) / 1000.0 / 1000.0 / 1000.0;
		
		if (!warmUp) LOGGER.info("Bulk read " + objectsAsList.size() + " objects in " + elapsedSecs + "s (" + objectsAsList.size() / elapsedSecs + " objects/s)");

		return objectsAsList;
	}

	private static List<ChampRelationship> retrieveBulkRelationships(ChampGraph graph, boolean warmUp) {
		final long startTime = System.nanoTime();
		final Stream<ChampRelationship> relationships = graph.queryRelationships(
							Collections.singletonMap(
								ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString(), "bazz"
							)
						);
						
		final List<ChampRelationship> relationshipsAsList = relationships.collect(Collectors.toList());
		final double elapsedSecs = (System.nanoTime() - startTime) / 1000.0 / 1000.0 / 1000.0;
		
		if (!warmUp) LOGGER.info("Bulk read " + relationshipsAsList.size() + " relationships in " + elapsedSecs + "s (" + relationshipsAsList.size() / elapsedSecs + " relationships/s)");

		return relationshipsAsList;
	}

	private static void storeObjects(ChampGraph graph, boolean warmUp) {
		final double[] latencies = new double[NUM_OBJECTS];
		final long totalStartTime = System.nanoTime();

		for (int i = 0; i < NUM_OBJECTS; i++) {
			try {
				final long startTime = System.nanoTime();

				graph.storeObject(
					ChampObject.create()
								.ofType("foo")
								.withoutKey()
								.withProperty("objectNumber", i)
								.build()
				);

				final double elapsedMs = (System.nanoTime() - startTime) / 1000.0 / 1000.0;
				latencies[i] = elapsedMs;
			} catch (ChampMarshallingException e) {
				throw new RuntimeException(e);
			} catch (ChampSchemaViolationException e) {
				//Ignore, no schema set
			} catch (ChampObjectNotExistsException e) {
				//Ignore, not an update
			}
		}

		final double totalElapsedTimeSecs = (System.nanoTime() - totalStartTime) / 1000.0 / 1000.0 / 1000.0;
		LOGGER.info("Wrote " + NUM_OBJECTS + " objects in " + totalElapsedTimeSecs + "s (" + NUM_OBJECTS / totalElapsedTimeSecs + " objects/s)");

		Arrays.sort(latencies);

		if (!warmUp) {
			LOGGER.info("Store object latencies");
			LOGGER.info("\t50th percentile: " + latencies[(int) (NUM_OBJECTS * 0.50)]);
			LOGGER.info("\t75th percentile: " + latencies[(int) (NUM_OBJECTS * 0.75)]);
			LOGGER.info("\t90th percentile: " + latencies[(int) (NUM_OBJECTS * 0.90)]);
			LOGGER.info("\t99th percentile: " + latencies[(int) (NUM_OBJECTS * 0.99)]);
		} 
	}

	private static void storeRelationships(ChampGraph graph, boolean warmUp) {
		final List<ChampObject> objects = retrieveBulkObjects(graph, warmUp);
		final double[] latencies = new double[NUM_RELATIONSHIPS];
		final long totalStartTime = System.nanoTime();

		for (int i = 0; i < NUM_RELATIONSHIPS; i++) {
			try {
				final long startTime = System.nanoTime();

				graph.storeRelationship(
					new ChampRelationship.Builder(
						objects.get(i % objects.size()), objects.get((i + 1) % objects.size()), "bazz"
					).property("relationshipNumber", i)
					.build()
				);

				final double elapsedMs = (System.nanoTime() - startTime) / 1000.0 / 1000.0;

				latencies[i] = elapsedMs;
			} catch (ChampMarshallingException e) {
				throw new RuntimeException(e);
			} catch (ChampObjectNotExistsException e) {
				throw new RuntimeException(e);
			} catch (ChampSchemaViolationException e) {
				throw new RuntimeException(e);
			} catch (ChampRelationshipNotExistsException e) {
				throw new RuntimeException(e);
			} catch (ChampUnmarshallingException e) {
				throw new RuntimeException(e);
			}
		}

		final double totalElapsedTimeSecs = (System.nanoTime() - totalStartTime) / 1000.0 / 1000.0 / 1000.0;
		LOGGER.info("Wrote " + NUM_RELATIONSHIPS + " relationships in " + totalElapsedTimeSecs + "s (" + NUM_RELATIONSHIPS / totalElapsedTimeSecs + " relationships/s)");

		Arrays.sort(latencies);

		if (!warmUp) {
			LOGGER.info("Store relationship latencies");
			LOGGER.info("\t50th percentile: " + latencies[(int) (NUM_RELATIONSHIPS * 0.50)]);
			LOGGER.info("\t75th percentile: " + latencies[(int) (NUM_RELATIONSHIPS * 0.75)]);
			LOGGER.info("\t90th percentile: " + latencies[(int) (NUM_RELATIONSHIPS * 0.90)]);
			LOGGER.info("\t99th percentile: " + latencies[(int) (NUM_RELATIONSHIPS * 0.99)]);
		} 
	}
}
