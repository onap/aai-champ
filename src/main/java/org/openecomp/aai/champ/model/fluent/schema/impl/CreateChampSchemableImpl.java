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

import org.openecomp.aai.champ.model.ChampObjectConstraint;
import org.openecomp.aai.champ.model.ChampRelationshipConstraint;
import org.openecomp.aai.champ.model.ChampSchema;
import org.openecomp.aai.champ.model.fluent.schema.CreateChampSchemable;
import org.openecomp.aai.champ.model.fluent.schema.ObjectConstraintPropertyStep;
import org.openecomp.aai.champ.model.fluent.schema.ObjectConstraintTypeStep;
import org.openecomp.aai.champ.model.fluent.schema.RelationshipConstraintSubStep;
import org.openecomp.aai.champ.model.fluent.schema.RelationshipConstraintTypeStep;

public class CreateChampSchemableImpl implements CreateChampSchemable {

	private final ChampSchema.Builder schemaBuilder;
	
	public CreateChampSchemableImpl() {
		this.schemaBuilder = new ChampSchema.Builder();
	}

	public CreateChampSchemableImpl(ChampSchema.Builder schemaBuilder) {
		this.schemaBuilder = schemaBuilder;
	}

	@Override
	public ChampSchema build() {
		return schemaBuilder.build();
	}

	@Override
	public ObjectConstraintTypeStep withObjectConstraint() {
		return new ObjectConstraintTypeStep() {

			@Override
			public ObjectConstraintPropertyStep onType(String type) {
				return new ObjectConstraintPropertyStepImpl(schemaBuilder, 
															new ChampObjectConstraint.Builder(type));
			}
		};
	}

	@Override
	public RelationshipConstraintTypeStep withRelationshipConstraint() {
		return new RelationshipConstraintTypeStep() {

			@Override
			public RelationshipConstraintSubStep onType(String type) {
				return new RelationshipConstraintSubStepImpl(schemaBuilder,
															 new ChampRelationshipConstraint.Builder(type));
			}
		};
	}

}
