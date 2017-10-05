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
package org.onap.aai.champ.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ChampField.Builder.class)
public final class ChampField implements Comparable<ChampField> {

	private final String name;
	private final ChampField.Type type;

	private ChampField() {
		throw new RuntimeException("Cannot use ChampField() constructor");
	}
	
	public String getName() { return name; }
	public ChampField.Type getType() { return type; }

	@JsonIgnore
	public Class<?> getJavaType() {
		switch (type) {
		case BOOLEAN:
			return Boolean.class;
		case DOUBLE:
			return Double.class;
		case FLOAT:
			return Float.class;
		case INTEGER:
			return Integer.class;
		case LONG:
			return Long.class;
		case STRING:
			return String.class;
		default:
			throw new RuntimeException("Unknown ChampField.Type " + type);
		}
	}

	private ChampField(Builder builder) {
		this.name = builder.name;
		this.type = builder.type;
	}

	public static enum Type {
		STRING,
		INTEGER,
		LONG,
		DOUBLE,
		FLOAT,
		BOOLEAN
	}

	@JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
	public static class Builder {
		private final String name;

		private ChampField.Type type = ChampField.Type.STRING;

		public Builder(@JsonProperty("name") String name) {
			this.name = name;
		}
		
		public Builder type(ChampField.Type type) {
			this.type = type;
			return this;
		}

		public ChampField build() {
			return new ChampField(this);
		}
	}

	@Override
	public int hashCode() {
		return 31 * (getName().hashCode());
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ChampField) {
			final ChampField champField = (ChampField) object;

			if (champField.getName().equals(getName())) return true;
		}
		
		return false;
	}

	@Override
	public String toString() {
		return "{name: " + getName() +
				", type: " + getType() +
				"}";
	}

	@Override
	public int compareTo(ChampField o) {
		final int nameComparison = getName().compareTo(o.getName());

		if (nameComparison == 0) {
			return getType().compareTo(o.getType());
		}

		return nameComparison;
	}
}
