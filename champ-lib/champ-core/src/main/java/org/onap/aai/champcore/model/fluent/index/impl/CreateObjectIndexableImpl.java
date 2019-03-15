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

import java.util.ArrayList;
import java.util.List;

import org.onap.aai.champcore.model.ChampField;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampObjectIndex;
import org.onap.aai.champcore.model.fluent.BuildStep;
import org.onap.aai.champcore.model.fluent.index.CreateObjectIndexable;
import org.onap.aai.champcore.model.fluent.index.ObjectIndexFieldStep;
import org.onap.aai.champcore.model.fluent.index.ObjectIndexTypeStep;

public final class CreateObjectIndexableImpl implements CreateObjectIndexable {

	@Override
	public ObjectIndexTypeStep ofName(String name) {
		return new ObjectIndexTypeStep() {

			@Override
			public ObjectIndexFieldStep onType(String objectType) {
				return new ObjectIndexFieldStep() {

					@Override
					public BuildStep<ChampObjectIndex> forFields(List<String> fieldNames) {
						return new BuildStep<ChampObjectIndex> () {

							@Override
							public ChampObjectIndex build() {
							    List<ChampField> fields = new ArrayList<ChampField>();
							    for (String fn : fieldNames) {
							        fields.add(new ChampField.Builder(fn).build());
							    }
								return new ChampObjectIndex.Builder(
									name, objectType, fields).build();
							}
						};
					}
				};
			}

			@Override
			public ObjectIndexFieldStep onAnyType() {
				return new ObjectIndexFieldStep() {

					@Override
					public BuildStep<ChampObjectIndex> forFields(List<String> fieldNames) {
						return new BuildStep<ChampObjectIndex> () {

							@Override
							public ChampObjectIndex build() {
							    List<ChampField> fields = new ArrayList<ChampField>();
                                for (String fn : fieldNames) {
                                    fields.add(new ChampField.Builder(fn).build());
                                }
								return new ChampObjectIndex.Builder(
									name, ChampObject.ReservedTypes.ANY.toString(), fields).build();
							}
						};
					}
				};
			}
		};
	}
}
