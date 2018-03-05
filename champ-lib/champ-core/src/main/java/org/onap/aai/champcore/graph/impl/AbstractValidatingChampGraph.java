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
package org.onap.aai.champcore.graph.impl;

import java.util.Map;
import java.util.Optional;

import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.event.AbstractLoggingChampGraph;
import org.onap.aai.champcore.exceptions.ChampMarshallingException;
import org.onap.aai.champcore.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champcore.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champcore.exceptions.ChampSchemaViolationException;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampObjectConstraint;
import org.onap.aai.champcore.model.ChampPartition;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.ChampRelationshipConstraint;
import org.onap.aai.champcore.model.ChampSchema;
import org.onap.aai.champcore.schema.ChampSchemaEnforcer;

public abstract class AbstractValidatingChampGraph extends AbstractLoggingChampGraph {

	private ChampSchema schema = ChampSchema.emptySchema();

	protected abstract ChampSchemaEnforcer getSchemaEnforcer();
	protected abstract boolean isShutdown();

	/**
	 * Updates an existing vertex in the graph data store.
	 * <p>
     * If a transaction context is not provided, then a transaction will be automatically 
     * created and committed for this operation only, otherwise, the supplied transaction
     * will be used and it will be up to the caller to commit the transaction at its 
     * discretion.
     * <p>
     * 
	 * @param object      - The vertex to be updated in the graph data store.
	 * @param transaction - Optional transaction context to perform the operation in.
	 * 
	 * @return - The updated vertex, marshaled as a {@link ChampObject}
	 * 
	 * @throws ChampMarshallingException     - If the {@code relationship} is not able to be 
     *                                         marshalled into the backend representation
	 * @throws ChampObjectNotExistsException - If {@link org.onap.aai.champcore.model.ChampObject#getKey} 
     *                                         is not present or object not found in the graph
	 */
	protected abstract ChampObject doReplaceObject(ChampObject object, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampObjectNotExistsException, ChampTransactionException, ChampTransactionException;
	
    /** 
     * Creates or updates a vertex in the graph data store.
     * <p>
     * If a transaction context is not provided, then a transaction will be automatically 
     * created and committed for this operation only, otherwise, the supplied transaction
     * will be used and it will be up to the caller to commit the transaction at its 
     * discretion.
     * 
	 * @param object      - The vertex to be stored in the graph data store.
	 * @param transaction - Optional transaction context to perform the operation in.
	 * 
	 * @return - The vertex which was created, marshaled as a {@link ChampObject}
	 * 
     * @throws ChampMarshallingException     - If the {@code relationship} is not able to be 
     *                                         marshaled into the back end representation
     * @throws ChampObjectNotExistsException - If {@link org.onap.aai.champcore.model.ChampObject#getKey} 
     *                                         is not present or object not found in the graph
	 */
	protected abstract ChampObject doStoreObject(ChampObject object, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampObjectNotExistsException, ChampTransactionException;
	
	/**
	 * Replaces an edge in the graph data store.
     * <p>
     * If a transaction context is not provided, then a transaction will be automatically 
     * created and committed for this operation only, otherwise, the supplied transaction
     * will be used and it will be up to the caller to commit the transaction at its 
     * discretion.
     * 
	 * @param relationship - The edge to be replaced in the graph data store.
	 * @param transaction  - Optional transaction context to perform the operation in.
	 * 
	 * @return - The edge as it was replaced, marshaled as a {@link ChampRelationship}
	 * 
	 * @throws ChampUnmarshallingException         - If the edge which was created could not be 
     *                                               unmarshaled into a ChampObject
	 * @throws ChampRelationshipNotExistsException - If {@link org.onap.aai.champcore.model.ChampRelationship#getKey}.isPresent() 
     *                                               but the object cannot be found in the graph
	 * @throws ChampMarshallingException           - If the {@code relationship} is not able to be 
     *                                               marshaled into the back end representation
	 */
	protected abstract ChampRelationship doReplaceRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampRelationshipNotExistsException, ChampMarshallingException, ChampTransactionException;
	
	/**
	 * Creates or updates a relationship in the graph data store.
     * <p>
     * If a transaction context is not provided, then a transaction will be automatically 
     * created and committed for this operation only, otherwise, the supplied transaction
     * will be used and it will be up to the caller to commit the transaction at its 
     * discretion.
     *  
	 * @param relationship - The relationship to be stored in the graph data store.
	 * @param transaction  - Optional transaction context to perform the operation in.
	 * 
	 * @return - The relationship that was stored.
	 * 
	 * @throws ChampUnmarshallingException         - If the edge which was created could not be 
     *                                               unmarshalled into a ChampObject
	 * @throws ChampObjectNotExistsException       - If {@link org.onap.aai.champcore.model.ChampObject#getKey} 
     *                                               is not present or object not found in the graph
	 * @throws ChampRelationshipNotExistsException - If {@link org.onap.aai.champcore.model.ChampRelationship#getKey}.isPresent() 
   *                                                 but the object cannot be found in the graph
	 * @throws ChampMarshallingException           - If the {@code relationship} is not able to be 
     *                                               marshalled into the backend representation
	 */
	protected abstract ChampRelationship doStoreRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampObjectNotExistsException, ChampRelationshipNotExistsException, ChampMarshallingException, ChampTransactionException;
		
	/**
	 * Creates or updates a partition in the graph data store.
	 * 
	 * @param partition   - The partition to be stored in the graph data store.
	 * @param transaction - Optional transaction context to perform the operation in.
	 * 
	 * @return - The partition that was stored.
	 * 
	 * @throws ChampRelationshipNotExistsException - If {@link org.onap.aai.champcore.model.ChampRelationship#getKey}.isPresent() 
   *                                                 but the object cannot be found in the graph
	 * @throws ChampMarshallingException           - If the {@code relationship} is not able to be 
     *                                               marshalled into the backend representation
	 * @throws ChampObjectNotExistsException       - If {@link org.onap.aai.champcore.model.ChampObject#getKey} 
     *                                               is not present or object not found in the graph
	 */
	protected abstract ChampPartition doStorePartition(ChampPartition partition, Optional<ChampTransaction> transaction) throws ChampRelationshipNotExistsException, ChampMarshallingException, ChampObjectNotExistsException, ChampTransactionException;

	protected AbstractValidatingChampGraph(Map<String, Object> properties) {
	  super(properties);
	}
	
	@Override
	public ChampObject executeStoreObject(ChampObject object, Optional<ChampTransaction> transaction)
			throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException {
	  
	  if (isShutdown()) {
	    throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
	  }

      validate(object);

      return doStoreObject(object, transaction);
	}
	  

	@Override
	public ChampObject executeReplaceObject(ChampObject object, Optional<ChampTransaction> transaction)
			throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException {
	  
		if (isShutdown()) {
		  throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
		}

		validate(object);

		return doReplaceObject(object, transaction);
	}

	@Override
	public ChampRelationship executeStoreRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction)
			throws ChampUnmarshallingException, ChampMarshallingException, ChampObjectNotExistsException, ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampTransactionException {	
		
	  if (isShutdown()) {
	    throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
	  }

	  validate(relationship);

	  return doStoreRelationship(relationship, transaction);
	}
	
	@Override
	public ChampRelationship executeReplaceRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction)
			throws ChampUnmarshallingException, ChampMarshallingException, ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampTransactionException {	
		
	  if (isShutdown()) {
	    throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
	  }

	  validate(relationship);

	  return doReplaceRelationship(relationship, transaction);
	}

	@Override
	public ChampPartition executeStorePartition(ChampPartition partition, Optional<ChampTransaction> transaction) throws ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampMarshallingException, ChampObjectNotExistsException, ChampTransactionException {

	  if (isShutdown()) {
	    throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
	  }

	  validate(partition);

	  return doStorePartition(partition, transaction);
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
