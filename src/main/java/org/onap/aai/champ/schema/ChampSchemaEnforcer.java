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
package org.onap.aai.champ.schema;

import org.onap.aai.champ.exceptions.ChampSchemaViolationException;
import org.onap.aai.champ.model.ChampObject;
import org.onap.aai.champ.model.ChampObjectConstraint;
import org.onap.aai.champ.model.ChampPartition;
import org.onap.aai.champ.model.ChampRelationship;
import org.onap.aai.champ.model.ChampRelationshipConstraint;
import org.onap.aai.champ.model.ChampSchema;

public interface ChampSchemaEnforcer {

	public void validate(ChampObject champObject, ChampObjectConstraint champObjectConstraint) throws ChampSchemaViolationException;
	public void validate(ChampRelationship champRelationship, ChampRelationshipConstraint champRelationshipConstraint) throws ChampSchemaViolationException;
	public void validate(ChampPartition champPartition, ChampSchema schema) throws ChampSchemaViolationException;
}