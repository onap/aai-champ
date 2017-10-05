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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.onap.aai.champ.model.fluent.partition.CreateChampPartitionable;
import org.onap.aai.champ.model.fluent.partition.impl.CreateChampPartionableImpl;

public final class ChampPartition {

	private final Set<ChampObject> champObjects;
	private final Set<ChampRelationship> champRelationships;

	private ChampPartition() {
		throw new RuntimeException("Cannot call ChampGraph() constructor");
	}
	
	private ChampPartition(Builder builder) {
		this.champObjects = builder.champObjects;
		this.champRelationships = builder.champRelationships;
	}

	public static CreateChampPartitionable create() {
		return new CreateChampPartionableImpl();
	}

	public Set<ChampObject> getChampObjects() { return champObjects; }
	public Set<ChampRelationship> getChampRelationships() { return champRelationships; }

	public Set<ChampRelationship> getIncidentRelationships(ChampObject source) {
		final Set<ChampRelationship> incidentRelationships = new HashSet<ChampRelationship> ();

		for (ChampRelationship relationship : getChampRelationships()) {
			if (relationship.getSource().equals(source) ||
				relationship.getTarget().equals(source)) {
				incidentRelationships.add(relationship);
			}
		}

		return incidentRelationships;
	}

	public Map<String, Set<ChampRelationship>> getIncidentRelationshipsByType(ChampObject source) {
		final Map<String, Set<ChampRelationship>> incidentRelationships = new HashMap<String, Set<ChampRelationship>> ();

		for (ChampRelationship relationship : getChampRelationships()) {
			if (relationship.getSource().equals(source) ||
				relationship.getTarget().equals(source)) {
				if (!incidentRelationships.containsKey(relationship.getType())) {
					incidentRelationships.put(relationship.getType(), new HashSet<ChampRelationship> ());
				}

				incidentRelationships.get(relationship.getType()).add(relationship);
			}
		}

		return incidentRelationships;
	}

	public static class Builder {
		private final Set<ChampObject> champObjects;
		private final Set<ChampRelationship> champRelationships;

		public Builder() {
			this.champObjects = new HashSet<ChampObject> ();
			this.champRelationships = new HashSet<ChampRelationship> ();
		}

		public Builder object(ChampObject object) {
			champObjects.add(object);
			return this;
		}

		public Builder relationship(ChampRelationship relationship) {
			champRelationships.add(relationship);
			return this;
		}

		public Builder objects(Set<ChampObject> objects) {
			champObjects.addAll(objects);
			return this;
		}
		
		public Builder relationships(Set<ChampRelationship> relationships) {
			champRelationships.addAll(relationships);
			return this;
		}

		public ChampPartition build() {
			return new ChampPartition(this);
		}
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append("{objects: [");

		for (ChampObject object : champObjects) {
			sb.append(object.toString());
			sb.append(",");
		}

		if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1); //Delete last comma

		sb.append("], relationships: [");

		for (ChampRelationship relationship : champRelationships) {
			sb.append(relationship.toString());
			sb.append(",");
		}

		if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1); //Delete last comma

		sb.append("]}");

		return sb.toString();
	}
}
