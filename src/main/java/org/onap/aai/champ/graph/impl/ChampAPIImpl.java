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

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.onap.aai.champ.ChampAPI;
import org.onap.aai.champ.ChampGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChampAPIImpl implements ChampAPI {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChampAPIImpl.class);

	private final AtomicBoolean shutdown;
	private final ChampGraph.Type type;
	private final ConcurrentHashMap<String, ChampGraph> graphs;

	public ChampAPIImpl(ChampGraph.Type type) {
		this.type = type;
		this.graphs = new ConcurrentHashMap<String, ChampGraph> ();
		this.shutdown = new AtomicBoolean(false);
	}

	private ConcurrentHashMap<String, ChampGraph> getGraphs() {
		return graphs;
	}

	@Override
	public ChampGraph getGraph(String graphName) {
		if (shutdown.get()) throw new IllegalStateException("Cannot call getGraph() after shutdown() has been initiated");

		if (getGraphs().containsKey(graphName)) return getGraphs().get(graphName);

		final ChampGraph graph = ChampGraph.Factory.newInstance(type, graphName);
		
		final ChampGraph existingGraph = getGraphs().putIfAbsent(graphName, graph);
		
		if (existingGraph == null) return graph;
		
		return existingGraph;
	}

	@Override
	public void shutdown() {
		if (shutdown.compareAndSet(false, true)) {
			for (Entry<String, ChampGraph> graphEntry : graphs.entrySet()) {
				LOGGER.info("Shutting down graph {}", graphEntry.getKey());
				
				try {
					graphEntry.getValue().shutdown();
					LOGGER.info("Graph {} shutdown successfully", graphEntry.getKey());
				} catch (Throwable t) {
					LOGGER.warn("Caught exception while shutting down graph " + graphEntry.getKey(), t);
				}
			}
		}
	}

	@Override
	public ChampGraph.Type getType() {
		return type;
	}

}