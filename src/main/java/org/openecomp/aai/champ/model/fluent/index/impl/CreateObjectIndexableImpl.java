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
package org.openecomp.aai.champ.model.fluent.index.impl;

import org.openecomp.aai.champ.model.ChampField;
import org.openecomp.aai.champ.model.ChampObject;
import org.openecomp.aai.champ.model.ChampObjectIndex;
import org.openecomp.aai.champ.model.fluent.BuildStep;
import org.openecomp.aai.champ.model.fluent.index.CreateObjectIndexable;
import org.openecomp.aai.champ.model.fluent.index.ObjectIndexFieldStep;
import org.openecomp.aai.champ.model.fluent.index.ObjectIndexTypeStep;

public final class CreateObjectIndexableImpl implements CreateObjectIndexable {

	@Override
	public ObjectIndexTypeStep ofName(String name) {
		return new ObjectIndexTypeStep() {

			@Override
			public ObjectIndexFieldStep onType(String objectType) {
				return new ObjectIndexFieldStep() {

					@Override
					public BuildStep<ChampObjectIndex> forField(String fieldName) {
						return new BuildStep<ChampObjectIndex> () {

							@Override
							public ChampObjectIndex build() {
								return new ChampObjectIndex.Builder(
									name, objectType, new ChampField.Builder(fieldName).build()
								).build();
							}
						};
					}
				};
			}

			@Override
			public ObjectIndexFieldStep onAnyType() {
				return new ObjectIndexFieldStep() {

					@Override
					public BuildStep<ChampObjectIndex> forField(String fieldName) {
						return new BuildStep<ChampObjectIndex> () {

							@Override
							public ChampObjectIndex build() {
								return new ChampObjectIndex.Builder(
									name, ChampObject.ReservedTypes.ANY.toString(), new ChampField.Builder(fieldName).build()
								).build();
							}
						};
					}
				};
			}
		};
	}
}
