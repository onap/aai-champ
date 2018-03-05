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

import org.junit.Test;
import org.onap.aai.champcore.model.ChampCardinality;
import org.onap.aai.champcore.model.ChampField;
import org.onap.aai.champcore.model.ChampPropertyConstraint;

public class ChampPropertyConstraintTest {

	@Test
	public void testChampPropertyConstraint() {
		final ChampField z = new ChampField.Builder("z").build();
		final ChampField y = new ChampField.Builder("y").build();

		final ChampPropertyConstraint a = new ChampPropertyConstraint.Builder(z)
																		.cardinality(ChampCardinality.SINGLE)
																		.required(false)
																		.build();
		final ChampPropertyConstraint aEquivalent = new ChampPropertyConstraint.Builder(z)
																				.build();

		final ChampPropertyConstraint b = new ChampPropertyConstraint.Builder(y)
																		.build();
		assertTrue(a.equals(aEquivalent));
		assertTrue(!a.equals(b));
		assertTrue(!a.equals(new Object()));

		assertTrue(a.toString().equals(aEquivalent.toString()));
		assertTrue(!a.toString().equals(b.toString()));

		assertTrue(a.compareTo(aEquivalent) == 0);
		assertTrue(a.compareTo(b) != 0);
	}
}
