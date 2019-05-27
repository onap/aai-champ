/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
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
 */
package org.onap.aai.champcore.graph.impl;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.onap.aai.champcore.ChampAPI;
import org.onap.aai.champcore.ChampCoreMsgs;
import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;


public class ChampAPIImpl implements ChampAPI {

	private static final Logger LOGGER = LoggerFactory.getInstance().getLogger(ChampAPIImpl.class);

	private final AtomicBoolean shutdown;
	private final String type;
	private final ConcurrentHashMap<String, ChampGraph> graphs;

	public ChampAPIImpl(String type) {
		this.type = type;
		this.graphs = new ConcurrentHashMap<String, ChampGraph> ();
		this.shutdown = new AtomicBoolean(false);
	}

	private ConcurrentHashMap<String, ChampGraph> getGraphs() {
	  return graphs;
	}

	@Override
	public ChampGraph getGraph(String graphName) {
		if (shutdown.get()) {
			throw new IllegalStateException("Cannot call getGraph() after shutdown() has been initiated");
		}

		if (getGraphs().containsKey(graphName)) {
			return getGraphs().get(graphName);
		}

		// At this point, we know a graph with this name doesn't exist. Create and return it.
		final ChampGraph graph = new InMemoryChampGraphImpl.Builder().build();
		graphs.put(graphName, graph);
		return graph;
	}

	@Override
	public void shutdown() {
		if (shutdown.compareAndSet(false, true)) {
			for (Entry<String, ChampGraph> graphEntry : graphs.entrySet()) {
				LOGGER.info(ChampCoreMsgs.CHAMPCORE_CHAMP_API_IMPL_INFO, 
				    String.format("Shutting down graph %s", graphEntry.getKey()));
				
				try {
					graphEntry.getValue().shutdown();
					LOGGER.info(ChampCoreMsgs.CHAMPCORE_CHAMP_API_IMPL_INFO,
					    String.format("Graph %s shutdown successfully", graphEntry.getKey()));
				} catch (Throwable t) {
					LOGGER.warn(ChampCoreMsgs.CHAMPCORE_CHAMP_API_IMPL_WARN,
					    String.format("Caught exception while shutting down graph %s: %s", graphEntry.getKey(), t.getMessage()));
				}
			}
		}
	}

	@Override
	public String getType() {
		return type;
	}

}
