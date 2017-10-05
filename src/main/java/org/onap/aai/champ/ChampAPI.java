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
package org.onap.aai.champ;

import org.onap.aai.champ.graph.impl.ChampAPIImpl;

public interface ChampAPI {

	/**
	 * A factory for constructing basic ChampAPI implementations (minimal).
	 * If finer control is needed, you should consider accessing an implementation's
	 * constructors/builders.
	 */
	public static final class Factory {
		private Factory() { throw new RuntimeException("Cannot instantiate ChampAPI.Factory"); }

		public static ChampAPI newInstance(ChampGraph.Type type) {
			return new ChampAPIImpl(type);
		}
	}

	public ChampGraph getGraph(String graphName);

	public ChampGraph.Type getType();

	/**
	 * Shutdown the ChampAPI. It is up to the caller to synchronize access to the ChampAPI
	 * so that shutting it down does not interfere with concurrent operations.
	 */
	public void shutdown();
}

