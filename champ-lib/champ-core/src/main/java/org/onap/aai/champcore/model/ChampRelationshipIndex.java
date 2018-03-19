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
package org.onap.aai.champcore.model;

import org.onap.aai.champcore.model.fluent.index.CreateRelationshipIndexable;
import org.onap.aai.champcore.model.fluent.index.impl.CreateRelationshipIndexableImpl;

public final class ChampRelationshipIndex {

	private final String name;
	private final String type;
	private final ChampField field;

	public static CreateRelationshipIndexable create() {
		return new CreateRelationshipIndexableImpl();
	}

	private ChampRelationshipIndex() {
		throw new RuntimeException("Cannot call ChampRelationshipIndex() constructor");
	}

	private ChampRelationshipIndex(Builder builder) {
		this.name = builder.name;
		this.type = builder.type;
		this.field = builder.field;
	}

	public String getName() { return name; }
	public String getType() { return type; }
	public ChampField getField() { return field; }

	public static class Builder {
		private final String name;
		private final String type;
		private final ChampField field;

		public Builder(String name, String type, ChampField field) {
			this.name = name;
			this.type = type;
			this.field = field;
		}

		public ChampRelationshipIndex build() {
			return new ChampRelationshipIndex(this);
		}
	}

	@Override
	public String toString() {
		return "{name: " + getName()
			+ ", type: " + getType()
			+ ", field: " + getField() + "}";
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		
		if (object instanceof ChampRelationshipIndex) {
			final ChampRelationshipIndex relationshipIndex = (ChampRelationshipIndex) object;
			
			if (getName().equals(relationshipIndex.getName()) &&
				getField().getName().equals(relationshipIndex.getField().getName())) return true;
		}
		
		return false;
	}
}
