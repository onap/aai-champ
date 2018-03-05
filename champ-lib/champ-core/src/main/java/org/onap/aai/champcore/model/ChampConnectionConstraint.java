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
package org.onap.aai.champcore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ChampConnectionConstraint.Builder.class)
public final class ChampConnectionConstraint {

	private final String sourceType;
	private final String targetType;
	private final ChampConnectionMultiplicity cardinality;

	private ChampConnectionConstraint() {
		throw new RuntimeException("Cannot call ConnectionConstraint() constructor");
	}
	
	private ChampConnectionConstraint(Builder builder) {
		this.sourceType = builder.sourceType;
		this.targetType = builder.targetType;
		this.cardinality = builder.multiplicity;
	}
	
	public String getSourceType() { return sourceType; }
	public String getTargetType() { return targetType; }
	public ChampConnectionMultiplicity getMultiplicity() { return cardinality; }

	@JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
	public static class Builder {
		private final String sourceType;
		private final String targetType;

		private ChampConnectionMultiplicity multiplicity = ChampConnectionMultiplicity.MANY;
		
		public Builder(@JsonProperty("sourceType") String sourceType, @JsonProperty("targetType") String targetType) {
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		public Builder multiplicity(ChampConnectionMultiplicity multiplicity) {
			this.multiplicity = multiplicity;
			return this;
		}

		public ChampConnectionConstraint build() {
			return new ChampConnectionConstraint(this);
		}
	}

	@Override
	public int hashCode() {
		return 31 * (getSourceType().hashCode() + getTargetType().hashCode());
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ChampConnectionConstraint) {
			final ChampConnectionConstraint connConstraint = (ChampConnectionConstraint) o;

			if (connConstraint.getSourceType().equals(getSourceType()) &&
				connConstraint.getTargetType().equals(getTargetType()) &&
				connConstraint.getMultiplicity().equals(getMultiplicity())) return true;
		}

		return false;
	}

	@Override
	public String toString() {
		return "{sourceType: " + getSourceType() +
				", targetType: " + getTargetType() +
				", multiplicity: " + getMultiplicity()
				+ "}";
	}
}
