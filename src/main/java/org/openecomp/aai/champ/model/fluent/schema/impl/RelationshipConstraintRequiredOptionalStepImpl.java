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
import org.openecomp.aai.champ.model.ChampPropertyConstraint;
import org.openecomp.aai.champ.model.ChampRelationshipConstraint;
import org.openecomp.aai.champ.model.ChampSchema;
import org.openecomp.aai.champ.model.ChampSchema.Builder;
import org.openecomp.aai.champ.model.fluent.schema.RelationshipConstraintBuildStep;
import org.openecomp.aai.champ.model.fluent.schema.RelationshipConstraintRequiredOptionalStep;

public class RelationshipConstraintRequiredOptionalStepImpl implements RelationshipConstraintRequiredOptionalStep {

	private final ChampSchema.Builder schemaBuilder;
	private final ChampRelationshipConstraint.Builder relConstraintBuilder;
	private final ChampField.Builder fieldBuilder;

	public RelationshipConstraintRequiredOptionalStepImpl(Builder schemaBuilder,
															ChampRelationshipConstraint.Builder relConstraintBuilder,
															ChampField.Builder fieldBuilder) {
		this.schemaBuilder = schemaBuilder;
		this.relConstraintBuilder = relConstraintBuilder;
		this.fieldBuilder = fieldBuilder;
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
