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
package org.openecomp.aai.champ.model.fluent.schema.impl;

import org.openecomp.aai.champ.model.ChampField;
import org.openecomp.aai.champ.model.ChampObjectConstraint;
import org.openecomp.aai.champ.model.ChampSchema;
import org.openecomp.aai.champ.model.fluent.schema.ObjectConstraintFieldStep;
import org.openecomp.aai.champ.model.fluent.schema.ObjectConstraintSubStep;

public class ObjectConstraintFieldStepImpl implements ObjectConstraintFieldStep {

	private final ChampSchema.Builder schemaBuilder;
	private final ChampObjectConstraint.Builder constraintBuilder;

	public ObjectConstraintFieldStepImpl(ChampSchema.Builder schemaBuilder, ChampObjectConstraint.Builder constraintBuilder) {
		this.schemaBuilder = schemaBuilder;
		this.constraintBuilder = constraintBuilder;
	}

	@Override
	public ObjectConstraintSubStep onField(String name) {
		return new ObjectConstraintSubStepImpl(schemaBuilder, constraintBuilder, new ChampField.Builder(name));
	}

}
