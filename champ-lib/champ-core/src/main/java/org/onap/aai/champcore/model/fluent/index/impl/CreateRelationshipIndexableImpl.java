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
package org.onap.aai.champcore.model.fluent.index.impl;

import org.onap.aai.champcore.model.ChampField;
import org.onap.aai.champcore.model.ChampRelationshipIndex;
import org.onap.aai.champcore.model.fluent.BuildStep;
import org.onap.aai.champcore.model.fluent.index.CreateRelationshipIndexable;
import org.onap.aai.champcore.model.fluent.index.RelationshipIndexFieldStep;
import org.onap.aai.champcore.model.fluent.index.RelationshipIndexTypeStep;

public final class CreateRelationshipIndexableImpl implements CreateRelationshipIndexable {

	@Override
	public RelationshipIndexTypeStep ofName(String name) {
		return new RelationshipIndexTypeStep() {

			@Override
			public RelationshipIndexFieldStep onType(String relationshipType) {
				return new RelationshipIndexFieldStep() {

					@Override
					public BuildStep<ChampRelationshipIndex> forField(String fieldName) {
						return new BuildStep<ChampRelationshipIndex> () {

							@Override
							public ChampRelationshipIndex build() {
								return new ChampRelationshipIndex.Builder(
									name, relationshipType, new ChampField.Builder(fieldName).build()
								).build();
							}
						};
					}
				};
			}
		};
	}

}
