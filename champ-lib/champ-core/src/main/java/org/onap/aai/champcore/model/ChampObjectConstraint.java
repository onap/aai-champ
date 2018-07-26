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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ChampObjectConstraint.Builder.class)
public final class ChampObjectConstraint {

	private final String type;
	private final Map<String, ChampPropertyConstraint> propertyConstraints;
	
	private ChampObjectConstraint() {
		throw new RuntimeException("Cannot call ChampObjectConstraint() constructor");
	}
	
	private ChampObjectConstraint(Builder builder) {
		this.type = builder.type;
		this.propertyConstraints = builder.propertyConstraints;
	}
	
	public String getType() { return type; }
	public Set<ChampPropertyConstraint> getPropertyConstraints() { return new HashSet<ChampPropertyConstraint> (propertyConstraints.values()); }
	
	public Optional<ChampPropertyConstraint> getPropertyConstraint(String fieldName) {
		if (!propertyConstraints.containsKey(fieldName)) return Optional.empty();

		return Optional.of(propertyConstraints.get(fieldName));
	}

	@JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
	public static class Builder {
		private final String type;
		private final Map<String, ChampPropertyConstraint> propertyConstraints;
		
		public Builder(@JsonProperty("type") String type) {
			this.type = type;
			this.propertyConstraints = new HashMap<String, ChampPropertyConstraint> ();
		}

		@JsonProperty("propertyConstraints")
		public Builder constraints(Set<ChampPropertyConstraint> propertyConstraints) {

			for (ChampPropertyConstraint propConstraint : propertyConstraints) {
				constraint(propConstraint);
			}

			return this;
		}

		@JsonIgnore
		public Builder constraint(ChampPropertyConstraint propConstraint) {
			propertyConstraints.put(propConstraint.getField().getName(), propConstraint);
			return this;
		}

		public ChampObjectConstraint build() {
			return new ChampObjectConstraint(this);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (o instanceof ChampObjectConstraint) {
			final ChampObjectConstraint objectConstraint = (ChampObjectConstraint) o;

			if (objectConstraint.getType().equals(getType())) return true;
		}
		
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getType());
	}

	@Override
	public String toString() {
		return "{type: " + getType() +
				", propertyConstraints: " + getPropertyConstraints() +
				"}";
	}
}
