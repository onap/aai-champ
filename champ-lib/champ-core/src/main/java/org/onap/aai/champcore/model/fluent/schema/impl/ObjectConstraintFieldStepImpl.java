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
package org.onap.aai.champcore.model.fluent.schema.impl;

import org.onap.aai.champcore.model.ChampField;
import org.onap.aai.champcore.model.ChampObjectConstraint;
import org.onap.aai.champcore.model.ChampSchema;
import org.onap.aai.champcore.model.fluent.schema.ObjectConstraintFieldStep;
import org.onap.aai.champcore.model.fluent.schema.ObjectConstraintSubStep;

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
