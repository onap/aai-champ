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
package org.openecomp.aai.champ.schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.openecomp.aai.champ.exceptions.ChampSchemaViolationException;
import org.openecomp.aai.champ.model.ChampConnectionConstraint;
import org.openecomp.aai.champ.model.ChampField;
import org.openecomp.aai.champ.model.ChampObject;
import org.openecomp.aai.champ.model.ChampObjectConstraint;
import org.openecomp.aai.champ.model.ChampPartition;
import org.openecomp.aai.champ.model.ChampPropertyConstraint;
import org.openecomp.aai.champ.model.ChampRelationship;
import org.openecomp.aai.champ.model.ChampRelationshipConstraint;
import org.openecomp.aai.champ.model.ChampSchema;

public final class DefaultChampSchemaEnforcer implements ChampSchemaEnforcer {

	@Override
	public void validate(ChampObject champObject, ChampObjectConstraint champObjectConstraint) throws ChampSchemaViolationException {
		for (ChampPropertyConstraint pc : champObjectConstraint.getPropertyConstraints()) {
			final ChampField field = pc.getField();
			final Optional<Object> property = champObject.getProperty(field.getName());
			
			if (pc.isRequired() && !property.isPresent()) {
				throw new ChampSchemaViolationException("Required property " + pc.getField().getName() + " is not present");
			}

			if (property.isPresent()) {
				switch (pc.getCardinality()) {
				case SINGLE:
					if (!pc.getField().getJavaType().isInstance(property.get())) {
						throw new ChampSchemaViolationException("Expected type " + pc.getField().getType() + " for type " + pc.getField().getName());
					}
					break;
				case LIST:
					if (!(property.get() instanceof List)) throw new ChampSchemaViolationException("Expected List type for ChampCardinality." + pc.getCardinality());
					break;
				case SET:
					if (!(property.get() instanceof Set)) throw new ChampSchemaViolationException("Expected Set type for ChampCardinality." + pc.getCardinality());
					break;
				default:
					throw new RuntimeException("Unknown property constraint cardinality " + pc.getCardinality());
				}
			}
		}
	}

	@Override
	public void validate(ChampRelationship champRelationship,
			ChampRelationshipConstraint champRelationshipConstraint) throws ChampSchemaViolationException {

		for (ChampPropertyConstraint pc : champRelationshipConstraint.getPropertyConstraints()) {
			final ChampField field = pc.getField();
			final Optional<Object> property = champRelationship.getProperty(field.getName());
			
			if (pc.isRequired() && !property.isPresent()) {
				throw new ChampSchemaViolationException("Required property " + pc.getField().getName() + " is not present");
			}

			if (property.isPresent() && !pc.getField().getJavaType().isInstance(property.get())) {
				throw new ChampSchemaViolationException("Expected type " + pc.getField().getType() + " for type " + pc.getField().getName());
			}
		}
	}

	@Override
	public void validate(ChampPartition champPartition, ChampSchema schema) throws ChampSchemaViolationException {
	
		for (ChampObject object : champPartition.getChampObjects()) {
			final Optional<ChampObjectConstraint> objConstraint = schema.getObjectConstraint(object.getType());

			if (!objConstraint.isPresent()) continue;
			
			validate(object, objConstraint.get());

			final Map<String, Set<ChampRelationship>> incidentRelationshipsByType = champPartition.getIncidentRelationshipsByType(object);

			for (Map.Entry<String, Set<ChampRelationship>> incidentRelationshipsOfType : incidentRelationshipsByType.entrySet()) {
				final Optional<ChampRelationshipConstraint> relConstraint = schema.getRelationshipConstraint(incidentRelationshipsOfType.getKey());

				if (relConstraint.isPresent()) {
					final ChampRelationshipConstraint relationshipConstraint = relConstraint.get();
					final Map<ChampConnectionConstraint, AtomicInteger> connectionCounts = new HashMap<ChampConnectionConstraint, AtomicInteger> ();

					for (ChampRelationship incidentRelationship : incidentRelationshipsOfType.getValue()) {
						final Optional<ChampConnectionConstraint> connectionConstraint = relationshipConstraint.getConnectionConstraint(incidentRelationship);

						validate(incidentRelationship, relationshipConstraint);

						if (connectionConstraint.isPresent()) {

							if (!connectionCounts.containsKey(connectionConstraint.get())) {
								connectionCounts.put(connectionConstraint.get(), new AtomicInteger(0));
							}

							final int connectionCount = connectionCounts.get(connectionConstraint.get()).incrementAndGet();

							switch (connectionConstraint.get().getMultiplicity()) {
							case MANY:
								//Always valid
							break;
							case NONE:
								if (connectionCount > 0) throw new ChampSchemaViolationException("Violated connection constraint " + connectionConstraint.get());
							break;
							case ONE:
								if (connectionCount > 1) throw new ChampSchemaViolationException("Violated connection constraint " + connectionConstraint.get());
							break;
							default:
							break;
							}
						}
					}
				}
				
			}
		}
	}
}
