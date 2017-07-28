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

import org.junit.Test;
import org.openecomp.aai.champ.model.ChampField;

public class ChampFieldTest {

	@Test
	public void testChampField() {
		final ChampField a = new ChampField.Builder("a")
											.type(ChampField.Type.STRING)
											.build();

		final ChampField aEquivalent = new ChampField.Builder("a")
														.build();

		final ChampField b = new ChampField.Builder("b")
											.build();

		assertTrue(a.equals(aEquivalent));
		assertTrue(!a.equals(new Object()));
		assertTrue(!a.equals(b));

		assertTrue(a.compareTo(aEquivalent) == 0);
		assertTrue(a.compareTo(b) != 0);

		assertTrue(a.toString().equals(aEquivalent.toString()));
		assertTrue(!a.toString().equals(b.toString()));
	}
}
