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

import org.openecomp.aai.champ.model.fluent.object.CreateChampObjectable;
import org.openecomp.aai.champ.model.fluent.object.impl.CreateChampObjectableImpl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ChampObject implements ChampElement {

	private final String type;
	private final Optional<Object> key;
	private final Map<String, Object> properties;

	public static CreateChampObjectable create() {
		return new CreateChampObjectableImpl();
	}

	private ChampObject() {
		throw new RuntimeException("Attempted to call private AAIObject() constructor");
	} //Not instantiable
	
	private ChampObject(Builder builder) {
		this.type = builder.type;
		this.key = builder.key;
		this.properties = builder.properties;
	}

	@SuppressWarnings("unchecked")
	public <T> Optional<T> getProperty(String key) {
		if (!properties.containsKey(key)) return Optional.empty();

		return Optional.of((T) properties.get(key));
	}

	public String getType() { return type; }
	
	@JsonIgnore
	public Optional<Object> getKey() { return key; }
	public Map<String, Object> getProperties() { return properties; }

	@JsonProperty("key")
	public Object getKeyValue() {
	  return key.orElse("");
	  
	}
	
	public static class Builder {
		private final String type;
		private final Map<String, Object> properties = new HashMap<String, Object> ();
		
		private Optional<Object> key = Optional.empty();

		public Builder(String type) {
			if (type == null) throw new IllegalArgumentException("Type cannot be null");

			this.type = type;
		}

		public Builder(ChampObject object) {
			type = object.getType();
			key = object.getKey();
			properties(object.getProperties());
		}

		public Builder key(Object key) {
			if (key == null) throw new IllegalArgumentException("Key cannot be set to null");

			this.key = Optional.of(key);
			return this;
		}

		public Builder property(String key, Object value) {
			if (key == null) throw new IllegalArgumentException("Property key cannot be null");
			if (value == null) throw new IllegalArgumentException("Property value cannot be null");

			if (ReservedPropertyKeys.contains(key)) throw new IllegalArgumentException("Property key " + key + " is reserved");

			properties.put(key, value);
			return this;
		}

		public Builder properties(Map<String, Object> properties) {
			for (Entry<String, Object> property : properties.entrySet()) {
				property(property.getKey(), property.getValue());
			}
			
			return this;
		}
		
		public ChampObject build() {
			return new ChampObject(this);
		}
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) return true;

		if (object instanceof ChampObject) {
			final ChampObject champObj = (ChampObject) object;

			if (getKey().isPresent() && champObj.getKey().isPresent()) {

				if (getKey().get().equals(champObj.getKey().get())) return true;

			} else if (!getKey().isPresent() && !champObj.getKey().isPresent()) {
				if (getType().equals(champObj.getType()) &&
					getProperties().equals(champObj.getProperties())) return true;
			}
		}
		
		return false;
	}

	@Override
	public int hashCode() {
		if (getKey().isPresent()) return getKey().get().hashCode();

		final int returnValue = 31 * (getType().hashCode() + getProperties().hashCode());
		return returnValue;
	}

	@Override
	public String toString() {
		return "{key: " + (getKey().isPresent() ? getKey().get() : "")
			+ ", type: " + getType()
			+ ", properties: " + getProperties() + "}";
	}
	
	public enum ReservedPropertyKeys {
		CHAMP_OBJECT_TYPE ("aai_node_type"),
		CHAMP_OBJECT_KEY ("key");

		private final String text;

		private ReservedPropertyKeys(final String text) {
			this.text = text;
		}

		@Override
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

	public enum IgnoreOnReadPropertyKeys {
		CHAMP_IMPORT_ASSIGNED_ID ("importAssignedId");
		
		private final String text;

		private IgnoreOnReadPropertyKeys(final String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}

		public static boolean contains(String key) {
			for (IgnoreOnReadPropertyKeys choice : IgnoreOnReadPropertyKeys.values()) {
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

		@Override
		public String toString() {
			return text;
		}
	}

	@Override
	public boolean isObject() {
		return true;
	}

	@Override
	public ChampObject asObject() {
		return this;
	}

	@Override
	public boolean isRelationship() {
		return false;
	}

	@Override
	public ChampRelationship asRelationship() {
		throw new UnsupportedOperationException("Cannot call asRelationship() on ChampObject");
	}
}
