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

import org.onap.aai.champcore.model.ChampCardinality;
import org.onap.aai.champcore.model.ChampField;
import org.onap.aai.champcore.model.ChampObjectConstraint;
import org.onap.aai.champcore.model.ChampPropertyConstraint;
import org.onap.aai.champcore.model.ChampSchema;
import org.onap.aai.champcore.model.fluent.schema.ObjectConstraintBuildStep;
import org.onap.aai.champcore.model.fluent.schema.ObjectConstraintPropertyStep;
import org.onap.aai.champcore.model.fluent.schema.ObjectConstraintRequiredOptionalStep;
import org.onap.aai.champcore.model.fluent.schema.ObjectConstraintSubStep;

public class ObjectConstraintSubStepImpl implements ObjectConstraintSubStep {

	private final ChampSchema.Builder schemaBuilder;
	private final ChampObjectConstraint.Builder constraintBuilder;
	private final ChampField.Builder fieldBuilder;
	private final ChampPropertyConstraint.Builder propConstBuilder;

	public ObjectConstraintSubStepImpl(ChampSchema.Builder schemaBuilder,
			ChampObjectConstraint.Builder constraintBuilder,
			ChampField.Builder fieldBuilder) {
		this.schemaBuilder = schemaBuilder;
		this.constraintBuilder = constraintBuilder;
		this.fieldBuilder = fieldBuilder;
		this.propConstBuilder = new ChampPropertyConstraint.Builder(fieldBuilder.build());

	}

	@Override
	public ObjectConstraintRequiredOptionalStep ofType(ChampField.Type type) {
		return new ObjectConstraintRequiredOptionalStep() {
			final ChampPropertyConstraint.Builder propConstBuilder = new ChampPropertyConstraint.Builder(fieldBuilder.type(type).build());

			@Override
			public ObjectConstraintBuildStep required() {
				constraintBuilder.constraint(propConstBuilder.required(true).build());

				return new ObjectConstraintBuildStep() {

					@Override
					public ObjectConstraintPropertyStep build() {
						return new ObjectConstraintPropertyStepImpl(schemaBuilder, constraintBuilder);
					}
				};
			}

			@Override
			public ObjectConstraintBuildStep optional() {
				constraintBuilder.constraint(propConstBuilder.required(false).build());

				return new ObjectConstraintBuildStep() {

					@Override
					public ObjectConstraintPropertyStep build() {
						return new ObjectConstraintPropertyStepImpl(schemaBuilder, constraintBuilder);
					}
				};
			}

			@Override
			public ObjectConstraintRequiredOptionalStep cardinality(ChampCardinality cardinality) {
				propConstBuilder.cardinality(cardinality);
				return this;
			}
		};
	}
	
	@Override
	public ObjectConstraintBuildStep required() {
		constraintBuilder.constraint(propConstBuilder.required(true).build());

		return new ObjectConstraintBuildStepImpl(schemaBuilder, constraintBuilder);
	}
	
	@Override
	public ObjectConstraintBuildStep optional() {
		final ChampPropertyConstraint.Builder propConstBuilder = new ChampPropertyConstraint.Builder(fieldBuilder.build());

		constraintBuilder.constraint(propConstBuilder.required(false).build());

		return new ObjectConstraintBuildStepImpl(schemaBuilder, constraintBuilder);
	}

	@Override
	public ObjectConstraintRequiredOptionalStep cardinality(ChampCardinality cardinality) {
		propConstBuilder.cardinality(cardinality);
		return this;
	}
}
