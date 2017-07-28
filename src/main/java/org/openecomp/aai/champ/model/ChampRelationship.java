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

import org.openecomp.aai.champ.model.fluent.relationship.CreateChampRelationshipable;
import org.openecomp.aai.champ.model.fluent.relationship.impl.CreateChampRelationshipableImpl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ChampRelationship implements ChampElement {
	private final String type; //AKA edge label
	private final Optional<Object> key;
	private final Map<String, Object> properties;
	private final ChampObject source;
	private final ChampObject target;

	public static CreateChampRelationshipable create() {
		return new CreateChampRelationshipableImpl();
	}

	private ChampRelationship() { //Not instantiable
		throw new RuntimeException("Cannot call ChampRelationship() constructor");
	}
	
	private ChampRelationship(Builder builder) {
		this.properties = builder.properties;
		this.source = builder.source;
		this.target = builder.target;
		this.type = builder.type;
		this.key = builder.key;
	}

	@JsonIgnore
	public Optional<Object> getKey() {
		return key;
	}

    @JsonProperty("key")
    public Object getKeyValue() {
      return key.orElse("");      
    }
	   
	public ChampObject getSource() {
		return source;
	}
	
	public ChampObject getTarget() {
		return target;
	}
	
	public String getType() {
		return type;
	}
	
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getProperty(String key) {
		if (!properties.containsKey(key)) return Optional.empty();
		
		return Optional.of((T) properties.get(key));
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public static class Builder {
		private final Map<String, Object> properties = new HashMap<String, Object> ();
		private final ChampObject source;
		private final ChampObject target;
		private final String type;

		private Optional<Object> key = Optional.empty();

		public Builder(ChampObject source, ChampObject target, String type) {
			this.source = source;
			this.target = target;
			this.type = type;
		}
		
		public Builder(ChampRelationship relationship) {
			this.source = relationship.source;
			this.target = relationship.target;
			this.type = relationship.type;

			properties.putAll(relationship.getProperties());
		}

		public Builder key(Object key) {
			this.key = Optional.of(key);
			return this;
		}

		public Builder properties(Map<String, Object> properties) {
			for (Entry<String, Object> property : properties.entrySet()) {
				property(property.getKey(), property.getValue());
			}

			return this;
		}

		public Builder property(String key, Object value) {
			if (ChampRelationship.ReservedPropertyKeys.contains(key)) throw new IllegalArgumentException("Cannot make use of reserved property key " + key);

			properties.put(key, value);
			return this;
		}
		
		public ChampRelationship build() {
			return new ChampRelationship(this);
		}
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (object instanceof ChampRelationship) {
			final ChampRelationship champRelationship = (ChampRelationship) object;

			if (getKey().isPresent() && champRelationship.getKey().isPresent()) {
				if (getKey().get().equals(champRelationship.getKey().get())) return true;
			}
		}
		
		return false;
	}

	@Override
	public String toString() {
		return "{key: " + (getKey().isPresent() ? getKey().get() : "") 
				+ ", type: " + getType()
				+ ", source: " + getSource()
				+ ", target: " + getTarget()
				+ ", properties: " + getProperties() + "}";
	}

	public enum ReservedPropertyKeys {
		CHAMP_RELATIONSHIP_TYPE ("relationshipType"),
		CHAMP_RELATIONSHIP_KEY ("key");

		private final String text;

		private ReservedPropertyKeys(final String text) {
			this.text = text;
		} 
		
		public String toString() {
			return text;
		}

		public static boolean contains(String key) {
			for (ReservedPropertyKeys choice : ReservedPropertyKeys.values()) {
				if (choice.toString().equals(key)) return true;
			}

			return false;
		}
	}

	public enum ReservedTypes {
		ANY ("ANY");

		private final String text;
		
		private ReservedTypes(final String text) {
			this.text = text;
		}
		
		public String toString() {
			return text;
		}
	}

	@Override
	public boolean isObject() {
		return false;
	}

	@Override
	public ChampObject asObject() {
		throw new UnsupportedOperationException("Cannot call asObject() on ChampRelationship");
	}

	@Override
	public boolean isRelationship() {
		return true;
	}

	@Override
	public ChampRelationship asRelationship() {
		return this;
	}
}
