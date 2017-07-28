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

import org.openecomp.aai.champ.model.ChampConnectionConstraint;
import org.openecomp.aai.champ.model.ChampField;
import org.openecomp.aai.champ.model.ChampObject;
import org.openecomp.aai.champ.model.ChampRelationshipConstraint;
import org.openecomp.aai.champ.model.ChampConnectionMultiplicity;
import org.openecomp.aai.champ.model.ChampSchema;
import org.openecomp.aai.champ.model.fluent.BuildStep;
import org.openecomp.aai.champ.model.fluent.schema.CreateChampSchemable;
import org.openecomp.aai.champ.model.fluent.schema.RelationshipConstraintFieldStep;
import org.openecomp.aai.champ.model.fluent.schema.RelationshipConstraintMultiplicityStep;
import org.openecomp.aai.champ.model.fluent.schema.RelationshipConstraintPropertyOptionalsStep;
import org.openecomp.aai.champ.model.fluent.schema.RelationshipConstraintSourceStep;
import org.openecomp.aai.champ.model.fluent.schema.RelationshipConstraintSubStep;
import org.openecomp.aai.champ.model.fluent.schema.RelationshipConstraintTargetStep;

public class RelationshipConstraintSubStepImpl implements RelationshipConstraintSubStep {

	private final ChampSchema.Builder schemaBuilder;
	private final ChampRelationshipConstraint.Builder relConstraintBuilder;

	public RelationshipConstraintSubStepImpl(ChampSchema.Builder schemaBuilder,
				ChampRelationshipConstraint.Builder relConstraintBuilder) {
		this.schemaBuilder = schemaBuilder;
		this.relConstraintBuilder = relConstraintBuilder;
	}
	
	@Override
	public CreateChampSchemable build() {
		return new CreateChampSchemableImpl(schemaBuilder.constraint(relConstraintBuilder.build()));
	}

	@Override
	public RelationshipConstraintFieldStep withPropertyConstraint() {
		return new RelationshipConstraintFieldStep() {

			@Override
			public RelationshipConstraintPropertyOptionalsStep onField(String name) {
				return new RelationshipConstraintPropertyOptionalsStepImpl(schemaBuilder, relConstraintBuilder, new ChampField.Builder(name));
			}
		};
	}
	
	@Override
	public RelationshipConstraintSourceStep withConnectionConstraint() {
		return new RelationshipConstraintSourceStep() {

			@Override
			public RelationshipConstraintTargetStep sourcedFrom(String sourceType) {
				
				return new RelationshipConstraintTargetStep() {
					
					@Override
					public RelationshipConstraintMultiplicityStep targetedTo(String targetType) {
						final ChampConnectionConstraint.Builder connectionConstraint = new ChampConnectionConstraint.Builder(sourceType, targetType);

						return new RelationshipConstraintMultiplicityStep() {
		
							@Override
							public RelationshipConstraintSubStep build() {
								relConstraintBuilder.constraint(connectionConstraint.build());
							
								return RelationshipConstraintSubStepImpl.this;
							}
		
							@Override
							public BuildStep<RelationshipConstraintSubStep> withMultiplicity(
									ChampConnectionMultiplicity multiplicity) {
								connectionConstraint.multiplicity(multiplicity);
								return new BuildStep<RelationshipConstraintSubStep> () {

									@Override
									public RelationshipConstraintSubStep build() {
										relConstraintBuilder.constraint(connectionConstraint.build());

										return RelationshipConstraintSubStepImpl.this;
									}
								};
							}
						};
					}

					@Override
					public RelationshipConstraintMultiplicityStep targetedToAny() {
						final ChampConnectionConstraint.Builder connectionConstraint = new ChampConnectionConstraint.Builder(sourceType, ChampObject.ReservedTypes.ANY.toString());

						return new RelationshipConstraintMultiplicityStep() {
		
							@Override
							public RelationshipConstraintSubStep build() {
								relConstraintBuilder.constraint(connectionConstraint.build());
							
								return RelationshipConstraintSubStepImpl.this;
							}
		
							@Override
							public BuildStep<RelationshipConstraintSubStep> withMultiplicity(
									ChampConnectionMultiplicity multiplicity) {
								connectionConstraint.multiplicity(multiplicity);
								return new BuildStep<RelationshipConstraintSubStep> () {

									@Override
									public RelationshipConstraintSubStep build() {
										relConstraintBuilder.constraint(connectionConstraint.build());

										return RelationshipConstraintSubStepImpl.this;
									}
								};
							}
						};
					}
				};
			}

			@Override
			public RelationshipConstraintTargetStep sourcedFromAny() {
				return new RelationshipConstraintTargetStep() {
					
					@Override
					public RelationshipConstraintMultiplicityStep targetedTo(String targetType) {
						final ChampConnectionConstraint.Builder connectionConstraint = new ChampConnectionConstraint.Builder(ChampObject.ReservedTypes.ANY.toString(), targetType);

						return new RelationshipConstraintMultiplicityStep() {
		
							@Override
							public RelationshipConstraintSubStep build() {
								relConstraintBuilder.constraint(connectionConstraint.build());
							
								return RelationshipConstraintSubStepImpl.this;
							}
		
							@Override
							public BuildStep<RelationshipConstraintSubStep> withMultiplicity(
									ChampConnectionMultiplicity multiplicity) {
								connectionConstraint.multiplicity(multiplicity);
								return new BuildStep<RelationshipConstraintSubStep> () {

									@Override
									public RelationshipConstraintSubStep build() {
										relConstraintBuilder.constraint(connectionConstraint.build());

										return RelationshipConstraintSubStepImpl.this;
									}
								};
							}
						};
					}

					@Override
					public RelationshipConstraintMultiplicityStep targetedToAny() {
						final ChampConnectionConstraint.Builder connectionConstraint = new ChampConnectionConstraint.Builder(ChampObject.ReservedTypes.ANY.toString(), ChampObject.ReservedTypes.ANY.toString());

						return new RelationshipConstraintMultiplicityStep() {
		
							@Override
							public RelationshipConstraintSubStep build() {
								relConstraintBuilder.constraint(connectionConstraint.build());
							
								return RelationshipConstraintSubStepImpl.this;
							}
		
							@Override
							public BuildStep<RelationshipConstraintSubStep> withMultiplicity(
									ChampConnectionMultiplicity multiplicity) {
								connectionConstraint.multiplicity(multiplicity);
								return new BuildStep<RelationshipConstraintSubStep> () {

									@Override
									public RelationshipConstraintSubStep build() {
										relConstraintBuilder.constraint(connectionConstraint.build());

										return RelationshipConstraintSubStepImpl.this;
									}
								};
							}
						};
					}
				};
			}
		};
	}

}
