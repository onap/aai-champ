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
package org.onap.aai.champcore.model.fluent.schema.impl;

import org.onap.aai.champcore.model.ChampField;
import org.onap.aai.champcore.model.ChampPropertyConstraint;
import org.onap.aai.champcore.model.ChampRelationshipConstraint;
import org.onap.aai.champcore.model.ChampSchema;
import org.onap.aai.champcore.model.fluent.schema.RelationshipConstraintBuildStep;
import org.onap.aai.champcore.model.fluent.schema.RelationshipConstraintPropertyOptionalsStep;
import org.onap.aai.champcore.model.fluent.schema.RelationshipConstraintRequiredOptionalStep;

public class RelationshipConstraintPropertyOptionalsStepImpl implements RelationshipConstraintPropertyOptionalsStep {

	private final ChampSchema.Builder schemaBuilder;
	private final ChampRelationshipConstraint.Builder relConstraintBuilder;
	private final ChampField.Builder fieldBuilder;

	public RelationshipConstraintPropertyOptionalsStepImpl(ChampSchema.Builder schemaBuilder,
			ChampRelationshipConstraint.Builder relConstraintBuilder,
			ChampField.Builder fieldBuilder) {
		this.schemaBuilder = schemaBuilder;
		this.relConstraintBuilder = relConstraintBuilder;
		this.fieldBuilder = fieldBuilder;
	}

	@Override
	public RelationshipConstraintRequiredOptionalStep ofType(ChampField.Type type) {
		return new RelationshipConstraintRequiredOptionalStepImpl(schemaBuilder, relConstraintBuilder, fieldBuilder.type(type));
	}
	
	@Override
	public RelationshipConstraintBuildStep required() {
		final ChampPropertyConstraint propConstraint = new ChampPropertyConstraint.Builder(fieldBuilder.build())
																					.required(true)
																					.build();

		return new RelationshipConstraintBuildStepImpl(schemaBuilder, relConstraintBuilder.constraint(propConstraint));
	}
	
	@Override
	public RelationshipConstraintBuildStep optional() {
		final ChampPropertyConstraint propConstraint = new ChampPropertyConstraint.Builder(fieldBuilder.build())
																					.required(false)
																					.build();

		return new RelationshipConstraintBuildStepImpl(schemaBuilder, relConstraintBuilder.constraint(propConstraint));
	}
}
