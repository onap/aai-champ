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
package org.onap.aai.champ.graph.impl;

import java.util.Map;
import java.util.Optional;

import org.onap.aai.champ.ChampGraph;
import org.onap.aai.champ.event.AbstractLoggingChampGraph;
import org.onap.aai.champ.event.ChampEvent;
import org.onap.aai.champ.event.ChampEvent.ChampOperation;
import org.onap.aai.champ.exceptions.ChampMarshallingException;
import org.onap.aai.champ.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champ.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champ.exceptions.ChampSchemaViolationException;
import org.onap.aai.champ.exceptions.ChampUnmarshallingException;
import org.onap.aai.champ.graph.impl.TitanChampGraphImpl.Builder;
import org.onap.aai.champ.model.ChampObject;
import org.onap.aai.champ.model.ChampObjectConstraint;
import org.onap.aai.champ.model.ChampPartition;
import org.onap.aai.champ.model.ChampRelationship;
import org.onap.aai.champ.model.ChampRelationshipConstraint;
import org.onap.aai.champ.model.ChampSchema;
import org.onap.aai.champ.schema.ChampSchemaEnforcer;

public abstract class AbstractValidatingChampGraph extends AbstractLoggingChampGraph {

	private ChampSchema schema = ChampSchema.emptySchema();

	protected abstract ChampSchemaEnforcer getSchemaEnforcer();
	protected abstract boolean isShutdown();

	protected abstract ChampObject doReplaceObject(ChampObject object) throws ChampMarshallingException, ChampObjectNotExistsException;
	protected abstract ChampObject doStoreObject(ChampObject object) throws ChampMarshallingException, ChampObjectNotExistsException;
	protected abstract ChampRelationship doReplaceRelationship(ChampRelationship relationship) throws ChampUnmarshallingException, ChampRelationshipNotExistsException, ChampMarshallingException;
	protected abstract ChampRelationship doStoreRelationship(ChampRelationship relationship) throws ChampUnmarshallingException, ChampObjectNotExistsException, ChampRelationshipNotExistsException, ChampMarshallingException;
	protected abstract ChampPartition doStorePartition(ChampPartition partition) throws ChampRelationshipNotExistsException, ChampMarshallingException, ChampObjectNotExistsException;

	protected AbstractValidatingChampGraph(Map<String, Object> properties) {
	  super(properties);
	}
	
	public ChampObject executeStoreObject(ChampObject object)
			throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		validate(object);

		return doStoreObject(object);
	}
	
	public ChampObject executeReplaceObject(ChampObject object)
			throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		validate(object);

		return doReplaceObject(object);
	}

	public ChampRelationship executeStoreRelationship(ChampRelationship relationship)
			throws ChampUnmarshallingException, ChampMarshallingException, ChampObjectNotExistsException, ChampSchemaViolationException, ChampRelationshipNotExistsException {	
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		validate(relationship);

		return doStoreRelationship(relationship);
	}
	
	public ChampRelationship executeReplaceRelationship(ChampRelationship relationship)
			throws ChampUnmarshallingException, ChampMarshallingException, ChampSchemaViolationException, ChampRelationshipNotExistsException {	
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		validate(relationship);

		return doReplaceRelationship(relationship);
	}

	public ChampPartition executeStorePartition(ChampPartition partition) throws ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampMarshallingException, ChampObjectNotExistsException {
		if (isShutdown()) throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");

		validate(partition);

		return doStorePartition(partition);
	}

	protected void validate(ChampObject object) throws ChampSchemaViolationException {
		final Optional<ChampObjectConstraint> objectConstraint = retrieveSchema().getObjectConstraint(object.getType());

 		if (objectConstraint.isPresent()) getSchemaEnforcer().validate(object, objectConstraint.get());
	}

	protected void validate(ChampRelationship relationship) throws ChampSchemaViolationException {
		final ChampSchema graphSchema = retrieveSchema();
		final Optional<ChampRelationshipConstraint> relationshipConstraint = graphSchema.getRelationshipConstraint(relationship.getType());
		final Optional<ChampObjectConstraint> sourceObjConstraint = graphSchema.getObjectConstraint(relationship.getSource().getType());
 		final Optional<ChampObjectConstraint> targetObjConstraint = graphSchema.getObjectConstraint(relationship.getTarget().getType());

 		if (relationshipConstraint.isPresent()) getSchemaEnforcer().validate(relationship, relationshipConstraint.get());
		if (sourceObjConstraint.isPresent()) getSchemaEnforcer().validate(relationship.getSource(), sourceObjConstraint.get());
		if (targetObjConstraint.isPresent()) getSchemaEnforcer().validate(relationship.getTarget(), targetObjConstraint.get());
	}

	protected void validate(ChampPartition partition) throws ChampSchemaViolationException {
		for (ChampObject object : partition.getChampObjects()) {
			validate(object);
		}

		for (ChampRelationship relationship : partition.getChampRelationships()) {
			validate(relationship);
		}
	}

	@Override
	public void storeSchema(ChampSchema schema) throws ChampSchemaViolationException {
		if (isShutdown()) throw new IllegalStateException("Cannot call storeSchema() after shutdown has been initiated");

		this.schema = schema;
	}

	@Override
	public ChampSchema retrieveSchema() {
		if (isShutdown()) throw new IllegalStateException("Cannot call retrieveSchema() after shutdown has been initiated");

		return schema;
	}

	@Override
	public void updateSchema(ChampObjectConstraint objectConstraint) throws ChampSchemaViolationException {
		if (isShutdown()) throw new IllegalStateException("Cannot call updateSchema() after shutdown has been initiated");

		final ChampSchema currentSchema = retrieveSchema();
		final ChampSchema updatedSchema = new ChampSchema.Builder(currentSchema)
												.constraint(objectConstraint)
												.build();
		
		storeSchema(updatedSchema);
	}

	@Override
	public void updateSchema(ChampRelationshipConstraint relationshipConstraint) throws ChampSchemaViolationException {
		if (isShutdown()) throw new IllegalStateException("Cannot call updateSchema() after shutdown has been initiated");

		final ChampSchema currentSchema = retrieveSchema();
		final ChampSchema updatedSchema = new ChampSchema.Builder(currentSchema)
												.constraint(relationshipConstraint)
												.build();
		
		storeSchema(updatedSchema);
	}

	@Override
	public void deleteSchema() {
		if (isShutdown()) throw new IllegalStateException("Cannot call deleteSchema() after shutdown has been initiated");
		this.schema = ChampSchema.emptySchema();
	}
}
