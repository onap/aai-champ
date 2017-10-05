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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ChampPropertyConstraint.Builder.class)
public final class ChampPropertyConstraint implements Comparable<ChampPropertyConstraint> {

	private final ChampField field;
	private final boolean required;
	private final ChampCardinality cardinality;

	private ChampPropertyConstraint() {
		throw new RuntimeException("Cannot call ChampPropertyConstraint() constructor");
	}
	
	private ChampPropertyConstraint(Builder builder) {
		this.field = builder.field;
		this.required = builder.required;
		this.cardinality = builder.cardinality;
	}

	public ChampField getField() { return field; }
	public boolean isRequired() { return required; }
	public ChampCardinality getCardinality() { return cardinality; }

	@JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
	public static class Builder {
		private final ChampField field;

		private boolean required = false;
		private ChampCardinality cardinality = ChampCardinality.SINGLE;

		public Builder(@JsonProperty("field") ChampField field) {
			this.field = field;
		}

		public Builder required(boolean required) {
			this.required = required;
			return this;
		}

		public Builder cardinality(ChampCardinality cardinality) {
			this.cardinality = cardinality;
			return this;
		}

		public ChampPropertyConstraint build() {
			return new ChampPropertyConstraint(this);
		}
	}

	@Override
	public int hashCode() {
		return 31 * (getField().hashCode());
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ChampPropertyConstraint) {
			final ChampPropertyConstraint propertyConstraint = (ChampPropertyConstraint) o;

			if (propertyConstraint.getField().equals(getField()))
				return true;
		}

		return false;
	}

	@Override
	public String toString() {
		return "{field: " + getField() +
				", required: " + isRequired() +
				", cardinality: " + getCardinality() +
				"}";
	}

	@Override
	public int compareTo(ChampPropertyConstraint o) {
		final int fieldComparison = o.getField().compareTo(getField());

		if (fieldComparison == 0) {
			return o.getCardinality().compareTo(getCardinality());
		}

		return fieldComparison;
	}
}
