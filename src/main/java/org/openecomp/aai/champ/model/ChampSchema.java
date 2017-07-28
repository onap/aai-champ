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
package org.openecomp.aai.champ.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.openecomp.aai.champ.model.fluent.schema.CreateChampSchemable;
import org.openecomp.aai.champ.model.fluent.schema.impl.CreateChampSchemableImpl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = ChampSchema.Builder.class)
public final class ChampSchema {

	private final Map<String, ChampObjectConstraint> objectConstraints;
	private final Map<String, ChampRelationshipConstraint> relationshipConstraints;

	private ChampSchema() {
		throw new RuntimeException("Cannot call ChampSchema() constructor");
	}
	
	private ChampSchema(Builder builder) {
		this.objectConstraints = builder.objectConstraints;
		this.relationshipConstraints = builder.relationshipConstraints;
	}

	public static CreateChampSchemable create() {
		return new CreateChampSchemableImpl();
	}

	public Optional<ChampObjectConstraint> getObjectConstraint(String type) {
		if (!getObjectConstraints().containsKey(type)) return Optional.empty();
		
		return Optional.of(getObjectConstraints().get(type));
	}
	
	public Optional<ChampRelationshipConstraint> getRelationshipConstraint(String type) {
		if (!getRelationshipConstraints().containsKey(type)) return Optional.empty();
		
		return Optional.of(getRelationshipConstraints().get(type));
	}

	public Map<String, ChampObjectConstraint> getObjectConstraints() {
		return objectConstraints;
	}

	public Map<String, ChampRelationshipConstraint> getRelationshipConstraints() {
		return relationshipConstraints;
	}

	@JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
	public static class Builder {
		private Map<String, ChampObjectConstraint> objectConstraints = new HashMap<String, ChampObjectConstraint> ();
		private Map<String, ChampRelationshipConstraint> relationshipConstraints = new HashMap<String, ChampRelationshipConstraint> ();
	
		public Builder() {}

		public Builder(ChampSchema schema) {
			objectConstraints.putAll(schema.getObjectConstraints());
			relationshipConstraints.putAll(schema.getRelationshipConstraints());
		}

		public Builder objectConstraints(Map<String, ChampObjectConstraint> objectConstraints) {
			this.objectConstraints.putAll(objectConstraints);
			return this;
		}

		public Builder relationshipConstraints(Map<String, ChampRelationshipConstraint> relationshipConstraints) {
			this.relationshipConstraints.putAll(relationshipConstraints);
			return this;
		}

		@JsonIgnore
		public Builder constraint(ChampObjectConstraint objConstraint) {
			objectConstraints.put(objConstraint.getType(), objConstraint);
			return this;
		}

		@JsonIgnore
		public Builder constraint(ChampRelationshipConstraint relConstraint) {
			relationshipConstraints.put(relConstraint.getType(), relConstraint);
			return this;
		}

		public ChampSchema build() {
			return new ChampSchema(this);
		}
	}

	@Override
	public boolean equals(Object schema) {
		if (schema instanceof ChampSchema) {

			for (Entry<String, ChampObjectConstraint> objConstraint : getObjectConstraints().entrySet()) {
				final Optional<ChampObjectConstraint> champObjConstraint = ((ChampSchema) schema).getObjectConstraint(objConstraint.getKey());
				
				if (!champObjConstraint.isPresent() ||
					!champObjConstraint.get().equals(objConstraint.getValue())) return false;
			}
			
			for (Entry<String, ChampRelationshipConstraint> relConstraint : getRelationshipConstraints().entrySet()) {
				final Optional<ChampRelationshipConstraint> champRelConstraint = ((ChampSchema) schema).getRelationshipConstraint(relConstraint.getKey());
				
				if (!champRelConstraint.isPresent() ||
					!champRelConstraint.get().equals(relConstraint.getValue())) return false;
			}
			
			return true;
		}
		
		return false;
	}

	@Override
	public String toString() {
		return "{objectConstraints: " + getObjectConstraints() +
				", relationshipConstraints: " + getRelationshipConstraints() +
				"}";
	}

	public static ChampSchema emptySchema() {
		return new ChampSchema.Builder().build();
	}
}
