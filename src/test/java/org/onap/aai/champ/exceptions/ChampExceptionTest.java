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
package org.onap.aai.champ.exceptions;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onap.aai.champ.exceptions.ChampIndexNotExistsException;
import org.onap.aai.champ.exceptions.ChampMarshallingException;
import org.onap.aai.champ.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champ.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champ.exceptions.ChampSchemaViolationException;
import org.onap.aai.champ.exceptions.ChampUnmarshallingException;

public class ChampExceptionTest {

	@Test
	public void testChampIndexNotExistsException() {
		final ChampIndexNotExistsException e1 = new ChampIndexNotExistsException();

		assertTrue(e1.getMessage() == null);

		final ChampIndexNotExistsException e2 = new ChampIndexNotExistsException("foo");

		assertTrue(e2.getMessage().equals("foo"));

		final ChampIndexNotExistsException e3 = new ChampIndexNotExistsException(e2);

		assertTrue(e3.getCause().equals(e2));

		final ChampIndexNotExistsException e4 = new ChampIndexNotExistsException("foo", e3);

		assertTrue(e4.getMessage().equals("foo"));
		assertTrue(e4.getCause().equals(e3));
	}

	@Test
	public void testChampMarshallingException() {
		final ChampMarshallingException e1 = new ChampMarshallingException();

		assertTrue(e1.getMessage() == null);

		final ChampMarshallingException e2 = new ChampMarshallingException("foo");

		assertTrue(e2.getMessage().equals("foo"));

		final ChampIndexNotExistsException e3 = new ChampIndexNotExistsException(e2);

		assertTrue(e3.getCause().equals(e2));

		final ChampMarshallingException e4 = new ChampMarshallingException("foo", e3);

		assertTrue(e4.getMessage().equals("foo"));
		assertTrue(e4.getCause().equals(e3));
	}

	@Test
	public void testChampObjectNotExistsException() {
		final ChampObjectNotExistsException e1 = new ChampObjectNotExistsException();

		assertTrue(e1.getMessage() == null);

		final ChampObjectNotExistsException e2 = new ChampObjectNotExistsException("foo");

		assertTrue(e2.getMessage().equals("foo"));

		final ChampIndexNotExistsException e3 = new ChampIndexNotExistsException(e2);

		assertTrue(e3.getCause().equals(e2));

		final ChampObjectNotExistsException e4 = new ChampObjectNotExistsException("foo", e3);

		assertTrue(e4.getMessage().equals("foo"));
		assertTrue(e4.getCause().equals(e3));
	}

	@Test
	public void testChampRelationshipNotExistsException() {
		final ChampRelationshipNotExistsException e1 = new ChampRelationshipNotExistsException();

		assertTrue(e1.getMessage() == null);

		final ChampRelationshipNotExistsException e2 = new ChampRelationshipNotExistsException("foo");

		assertTrue(e2.getMessage().equals("foo"));

		final ChampIndexNotExistsException e3 = new ChampIndexNotExistsException(e2);

		assertTrue(e3.getCause().equals(e2));

		final ChampRelationshipNotExistsException e4 = new ChampRelationshipNotExistsException("foo", e3);

		assertTrue(e4.getMessage().equals("foo"));
		assertTrue(e4.getCause().equals(e3));
	}

	@Test
	public void testChampSchemaViolationException() {
		final ChampSchemaViolationException e1 = new ChampSchemaViolationException();

		assertTrue(e1.getMessage() == null);

		final ChampSchemaViolationException e2 = new ChampSchemaViolationException("foo");

		assertTrue(e2.getMessage().equals("foo"));

		final ChampIndexNotExistsException e3 = new ChampIndexNotExistsException(e2);

		assertTrue(e3.getCause().equals(e2));

		final ChampSchemaViolationException e4 = new ChampSchemaViolationException("foo", e3);

		assertTrue(e4.getMessage().equals("foo"));
		assertTrue(e4.getCause().equals(e3));
	}

	@Test
	public void testChampUnmarshallingException() {
		final ChampUnmarshallingException e1 = new ChampUnmarshallingException();

		assertTrue(e1.getMessage() == null);

		final ChampUnmarshallingException e2 = new ChampUnmarshallingException("foo");

		assertTrue(e2.getMessage().equals("foo"));

		final ChampIndexNotExistsException e3 = new ChampIndexNotExistsException(e2);

		assertTrue(e3.getCause().equals(e2));

		final ChampUnmarshallingException e4 = new ChampUnmarshallingException("foo", e3);

		assertTrue(e4.getMessage().equals("foo"));
		assertTrue(e4.getCause().equals(e3));
	}

}
