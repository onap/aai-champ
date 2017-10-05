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
package org.onap.aai.champ.ie;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.onap.aai.champ.ChampAPI;
import org.onap.aai.champ.ChampGraph;
import org.onap.aai.champ.ie.GraphMLImporterExporter;

public class ImportTest {

	private final String GRAPH_NAME = "unit-test";

	@Test
	public void testGraphMLImport() {

		final GraphMLImporterExporter importer = new GraphMLImporterExporter();
		final ChampAPI api = ChampAPI.Factory.newInstance(ChampGraph.Type.IN_MEMORY);

		importer.importData(api, getClass().getClassLoader().getResourceAsStream("import-test.graphml"));

		final ChampGraph graph = api.getGraph(GRAPH_NAME);

		graph.queryObjects(Collections.emptyMap()).forEach(object -> {
			final Optional<String> nameOpt = object.getProperty("name");
			final Optional<Boolean> studentOpt = object.getProperty("student");
			final Optional<Long> worthOpt = object.getProperty("worth");
			final Optional<Integer> ageOpt = object.getProperty("age");
			final Optional<Float> heightOpt = object.getProperty("height");
			final Optional<Double> weightOpt = object.getProperty("weight");
			final Optional<String> favoriteColorOpt = object.getProperty("favoriteColor");

			final String name = nameOpt.get();

			if (name.equals("Champ")) {
				assertTrue(!studentOpt.isPresent());
				assertTrue(!ageOpt.isPresent());
				assertTrue(!worthOpt.isPresent());
				assertTrue(!heightOpt.isPresent());
				assertTrue(!weightOpt.isPresent());
				assertTrue(favoriteColorOpt.get().equals("green"));
			} else if (name.equals("Max")) {
				assertTrue(!studentOpt.isPresent());
				assertTrue(!ageOpt.isPresent());
				assertTrue(!worthOpt.isPresent());
				assertTrue(!heightOpt.isPresent());
				assertTrue(!weightOpt.isPresent());
				assertTrue(favoriteColorOpt.get().equals("red"));
			} else if (name.equals("Ace")) {
				assertTrue(studentOpt.get());
				assertTrue(worthOpt.get().equals(50000L));
				assertTrue(ageOpt.get().equals(21));
				assertTrue(heightOpt.get().equals(72.5f));
				assertTrue(weightOpt.get().equals(180.5d));
				assertTrue(favoriteColorOpt.get().equals("yellow"));
			} else if (name.equals("Fido")) {
				assertTrue(!studentOpt.isPresent());
				assertTrue(!ageOpt.isPresent());
				assertTrue(!worthOpt.isPresent());
				assertTrue(!heightOpt.isPresent());
				assertTrue(!weightOpt.isPresent());
				assertTrue(favoriteColorOpt.get().equals("blue"));
			} else {
				throw new AssertionError("Unknown object " + name + " - update unit test");
			}
		});

		api.shutdown();
	}
}
