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

@JsonDeserialize(builder = ChampRelationshipConstraint.Builder.class)
public final class ChampRelationshipConstraint {

	private final String type;
	private final Map<String, ChampPropertyConstraint> propertyConstraints;
	private final Set<ChampConnectionConstraint> connectionConstraints;

	private ChampRelationshipConstraint() {
		throw new RuntimeException("Cannot call ChampObjectConstraint() constructor");
	}
	
	private ChampRelationshipConstraint(Builder builder) {
		this.type = builder.type;
		this.propertyConstraints = builder.propertyConstraints;
		this.connectionConstraints = builder.connectionConstraints;
	}
	
	public String getType() { return type; }
	public Set<ChampPropertyConstraint> getPropertyConstraints() { return new HashSet<ChampPropertyConstraint> (propertyConstraints.values()); }
	
	public Optional<ChampPropertyConstraint> getPropertyConstraint(String fieldName) {
		if (!propertyConstraints.containsKey(fieldName)) return Optional.empty();

		return Optional.of(propertyConstraints.get(fieldName));
	}

	public Set<ChampConnectionConstraint> getConnectionConstraints() { return connectionConstraints; }

	public Optional<ChampConnectionConstraint> getConnectionConstraint(ChampRelationship incidentRelationship) {
		if (!incidentRelationship.getType().equals(getType())) return Optional.empty();

		final String sourceType = incidentRelationship.getSource().getType();
		final String targetType = incidentRelationship.getTarget().getType();

		for (ChampConnectionConstraint connConstraint : getConnectionConstraints()) {
			if (connConstraint.getSourceType().equals(sourceType) &&
				connConstraint.getTargetType().equals(targetType)) return Optional.of(connConstraint);
		}

		return Optional.empty();
	}

	@JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
	public static class Builder {
		private final String type;
		private final Map<String, ChampPropertyConstraint> propertyConstraints;
		private final Set<ChampConnectionConstraint> connectionConstraints;

		public Builder(@JsonProperty("type") String type) {
			this.type = type;
			this.propertyConstraints = new HashMap<String, ChampPropertyConstraint> ();
			this.connectionConstraints = new HashSet<ChampConnectionConstraint> ();
		}

		public Builder propertyConstraints(Set<ChampPropertyConstraint> propertyConstraints) {

			for (ChampPropertyConstraint propConstraint : propertyConstraints) {
				constraint(propConstraint);
			}

			return this;
		}

		public Builder connectionConstraints(Set<ChampConnectionConstraint> connectionConstraints) {
			this.connectionConstraints.addAll(connectionConstraints);
			return this;
		}

		@JsonIgnore
		public Builder constraint(ChampPropertyConstraint propConstraint) {
			propertyConstraints.put(propConstraint.getField().getName(), propConstraint);
			return this;
		}

		@JsonIgnore
		public Builder constraint(ChampConnectionConstraint connConstraint) {
			connectionConstraints.add(connConstraint);
			return this;
		}
		public ChampRelationshipConstraint build() {
			return new ChampRelationshipConstraint(this);
		}
	}

	@Override
	public String toString() {
		return "{type: " + getType() +
				", connectionConstraints: " + getConnectionConstraints() +
				", propertyConstraints: " + getPropertyConstraints() +
				"}";
	}
 
	@Override
	public boolean equals(Object o) {
		if (o instanceof ChampRelationshipConstraint) {
			final ChampRelationshipConstraint relConstraint = (ChampRelationshipConstraint) o;

			if (relConstraint.getType().equals(getType()) &&
				relConstraint.getConnectionConstraints().equals(getConnectionConstraints()) &&
				relConstraint.getPropertyConstraints().equals(getPropertyConstraints())) return true;
		}
		
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getType(), getConnectionConstraints(), getPropertyConstraints());
	}
}
