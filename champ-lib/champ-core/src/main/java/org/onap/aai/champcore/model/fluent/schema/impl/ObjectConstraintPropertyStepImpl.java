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

import org.onap.aai.champcore.model.ChampObjectConstraint;
import org.onap.aai.champcore.model.ChampSchema;
import org.onap.aai.champcore.model.fluent.schema.CreateChampSchemable;
import org.onap.aai.champcore.model.fluent.schema.ObjectConstraintFieldStep;
import org.onap.aai.champcore.model.fluent.schema.ObjectConstraintPropertyStep;

public class ObjectConstraintPropertyStepImpl implements ObjectConstraintPropertyStep {

	private final ChampObjectConstraint.Builder constraintBuilder;
	private final ChampSchema.Builder schemaBuilder;

	public ObjectConstraintPropertyStepImpl(ChampSchema.Builder schemaBuilder, ChampObjectConstraint.Builder constraintBuilder) {
		this.constraintBuilder = constraintBuilder;
		this.schemaBuilder = schemaBuilder;
	}

	@Override
	public CreateChampSchemable build() {
		return new CreateChampSchemableImpl(schemaBuilder.constraint(constraintBuilder.build()));
	}
	
	@Override
	public ObjectConstraintFieldStep withPropertyConstraint() {
		return new ObjectConstraintFieldStepImpl(schemaBuilder, constraintBuilder);
	}

}
