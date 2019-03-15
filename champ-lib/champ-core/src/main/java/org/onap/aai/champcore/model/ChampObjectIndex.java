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

import java.util.List;
import java.util.Objects;
import org.onap.aai.champcore.model.fluent.index.CreateObjectIndexable;
import org.onap.aai.champcore.model.fluent.index.impl.CreateObjectIndexableImpl;

public final class ChampObjectIndex {

	private final String name;
	private final String type;
	private final List<ChampField> fields;

	public static CreateObjectIndexable create() {
		return new CreateObjectIndexableImpl();
	}

	private ChampObjectIndex() {
		throw new RuntimeException("Cannot call ChampObjectIndex() constructor");
	}

	private ChampObjectIndex(Builder builder) {
		this.name = builder.name;
		this.type = builder.type;
		this.fields = builder.fields;
	}

	public String getName() { return name; }
	public String getType() { return type; }
	public List<ChampField> getFields() { return fields; }

	public static class Builder {
		private final String name;
		private final String type;
		private final List<ChampField> fields;

		public Builder(String name, String type, List<ChampField> fields) {
			this.name = name;
			this.type = type;
			this.fields = fields;
		}

		public ChampObjectIndex build() {
			return new ChampObjectIndex(this);
		}
	}

	@Override
	public String toString() {
		return "{name: " + getName()
			+ ", type: " + getType()
			+ ", fields: " + getFields() + "}";
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		
		if (object instanceof ChampObjectIndex) {
			final ChampObjectIndex objectIndex = (ChampObjectIndex) object;
			
			if ( getName().equals(objectIndex.getName()) && (getFields().hashCode() == objectIndex.getFields().hashCode()) ) {
			    return true;
			}
		}
		
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getFields().hashCode());
	}
}
