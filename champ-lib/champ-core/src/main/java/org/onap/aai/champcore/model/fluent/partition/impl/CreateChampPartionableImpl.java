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
package org.onap.aai.champcore.model.fluent.partition.impl;

import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampPartition;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.fluent.partition.CreateChampPartitionable;

public final class CreateChampPartionableImpl implements CreateChampPartitionable {

	private final ChampPartition.Builder builder;

	public CreateChampPartionableImpl() {
		this.builder = new ChampPartition.Builder();
	}

	@Override
	public CreateChampPartitionable withObject(ChampObject object) {
		builder.object(object);
		return this;
	}

	@Override
	public CreateChampPartitionable withRelationship(ChampRelationship relationship) {
		builder.relationship(relationship);
		return this;
	}

	@Override
	public ChampPartition build() {
		return builder.build();
	}
}
