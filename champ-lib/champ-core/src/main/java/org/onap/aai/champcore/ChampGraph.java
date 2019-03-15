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
package org.onap.aai.champcore;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.onap.aai.champcore.exceptions.ChampIndexNotExistsException;
import org.onap.aai.champcore.exceptions.ChampMarshallingException;
import org.onap.aai.champcore.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champcore.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champcore.exceptions.ChampSchemaViolationException;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampObjectConstraint;
import org.onap.aai.champcore.model.ChampObjectIndex;
import org.onap.aai.champcore.model.ChampPartition;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.ChampRelationshipConstraint;
import org.onap.aai.champcore.model.ChampRelationshipIndex;
import org.onap.aai.champcore.model.ChampSchema;

public interface ChampGraph {
  
  /**
   * Opens a transaction within the graph data store.
   * 
   * @return - A transaction object.
   */
  public ChampTransaction openTransaction();
  
  /**
   * Attempts to commit the supplied open transaction.
   * 
   * @param transaction - The transaction to be committed.
   * 
   * @throws ChampTransactionException - If an attempt to commit or rollback the transaction failed.
   */
  public void commitTransaction(ChampTransaction transaction) throws ChampTransactionException;
  
  /**
   * Attempts to roll back the supplied open transaction.
   * 
   * @param transaction - The transaction to be committed.
   * 
   * @throws ChampTransactionException - If an attempt to commit or rollback the transaction failed.
   */
  public void rollbackTransaction(ChampTransaction transaction) throws ChampTransactionException;
  
  /**
   * Create/Update an object.  
   * <p>
   * If the ChampObject key is present, an update will be attempted,
   * otherwise a create will be attempted.  Each implementation has different guarantees on
   * validation - see the specific implementation for more details on this.
   * 
   * @param object      - The ChampObject that you wish to store in the graph
   * 
   * @return The ChampObject as it was stored
   * 
   * @throws ChampMarshallingException If the {@code object} is not able to be marshalled into the backend representation
   * @throws ChampSchemaViolationException If the {@code object} violates the constraints specifed by {@link ChampGraph#retrieveSchema}
   * @throws ChampObjectNotExistsException If {@link org.onap.aai.champcore.model.ChampObject#getKey}.isPresent() but the object cannot be found in the graph
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   */
  public ChampObject storeObject(ChampObject object) throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException;

  /**
    * Create/Update an object.  
    * <p>
    * If the ChampObject key is present, an update will be attempted,
    * otherwise a create will be attempted.  Each implementation has different guarantees on
    * validation - see the specific implementation for more details on this.
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion.
    * 
    * @param object      - The ChampObject that you wish to store in the graph
    * @param transaction - Optional transaction context to perform the operation in.
    * 
    * @return The ChampObject as it was stored
    * 
    * @throws ChampMarshallingException If the {@code object} is not able to be marshalled into the backend representation
    * @throws ChampSchemaViolationException If the {@code object} violates the constraints specifed by {@link ChampGraph#retrieveSchema}
    * @throws ChampObjectNotExistsException If {@link org.onap.aai.champcore.model.ChampObject#getKey}.isPresent() but the object cannot be found in the graph
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    */
  public ChampObject storeObject(ChampObject object, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException;
	
  /**
   * Replace an object.  ChampObject key is mandatory
   * <p>
   * Each implementation has different guarantees on validation - see the specific implementation
   * for more details on this.
   * 
   * @param object - The ChampObject that you wish to replace in the graph
   *
   * @return The ChampObject as it was stored
   * 
   * @throws ChampMarshallingException If the {@code object} is not able to be marshalled into the backend representation
   * @throws ChampSchemaViolationException If the {@code object} violates the constraints specifed by {@link ChampGraph#retrieveSchema}
   * @throws ChampObjectNotExistsException If {@link org.onap.aai.champcore.model.ChampObject#getKey} is not present or object not found in the graph
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   */
  public ChampObject replaceObject(ChampObject object) throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException;

  /**
    * Replace an object.  ChampObject key is mandatory
    * <p>
    * Each implementation has different guarantees on validation - see the specific implementation
    * for more details on this.
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion.
    * 
	* @param object - The ChampObject that you wish to replace in the graph
	* @param transaction - Optional transaction context to perform the operation in.
    *
	* @return The ChampObject as it was stored
	* 
    * @throws ChampMarshallingException If the {@code object} is not able to be marshalled into the backend representation
    * @throws ChampSchemaViolationException If the {@code object} violates the constraints specifed by {@link ChampGraph#retrieveSchema}
    * @throws ChampObjectNotExistsException If {@link org.onap.aai.champcore.model.ChampObject#getKey} is not present or object not found in the graph
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    */
  public ChampObject replaceObject(ChampObject object, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampSchemaViolationException, ChampObjectNotExistsException, ChampTransactionException;

  /**
   * Retrieve an object by its key.
   * 
   * @param key         - The key of the ChampObject in the graph {@link org.onap.aai.champcore.model.ChampObject#getKey()}
   *  
   * @return The {@link org.onap.aai.champcore.model.ChampObject} if it was present, otherwise {@link Optional#empty()}
   * 
   * @throws ChampUnmarshallingException If the object was found, but could not be unmarshalled
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   */
  public Optional<ChampObject> retrieveObject(Object key) throws ChampUnmarshallingException, ChampTransactionException;

 
  /**
    * Retrieve an object by its key.
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion.
    * 
    * @param key         - The key of the ChampObject in the graph {@link org.onap.aai.champcore.model.ChampObject#getKey()}
    * @param transaction - Optional transaction context to perform the operation in.
    *  
    * @return The {@link org.onap.aai.champcore.model.ChampObject} if it was present, otherwise {@link Optional#empty()}
    * 
    * @throws ChampUnmarshallingException If the object was found, but could not be unmarshalled
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    */
  public Optional<ChampObject> retrieveObject(Object key, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampTransactionException;
    
  /**
   * Delete an object by its key.
   * 
   * @param key         - The key of the ChampObject in the graph {@link ChampObject#getKey}
   *
   * @throws ChampObjectNotExistsException If the object did not exist in the graph
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   */
  public void deleteObject(Object key) throws ChampObjectNotExistsException, ChampTransactionException;
 
  /**
    * Delete an object by its key.
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion.
    * 
    * @param key         - The key of the ChampObject in the graph {@link ChampObject#getKey}
    * @param transaction - Optional transaction context to perform the operation in.
    *
    * @throws ChampObjectNotExistsException If the object did not exist in the graph
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    */
  public void deleteObject(Object key, Optional<ChampTransaction> transaction) throws ChampObjectNotExistsException, ChampTransactionException;

  /**
   * Retrieve all the objects whose properties match the given {@code queryParams}
   * 
   * @param queryParams - The key/value pairs which are found in {@link ChampObject#getProperties}
   * 
   * @return - A {@link Stream} where each {@link ChampObject#getProperties} contains the {@code queryParams}
   *
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   */
  public Stream<ChampObject> queryObjects(Map<String, Object> queryParams) throws ChampTransactionException;

 
  /**
    * Retrieve all the objects whose properties match the given {@code queryParams}
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion.
    * 
    * @param queryParams - The key/value pairs which are found in {@link ChampObject#getProperties}
    * @param transaction - Optional transaction context to perform the operation in.
    * 
    * @return - A {@link Stream} where each {@link ChampObject#getProperties} contains the {@code queryParams}
    * 
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    */
  public Stream<ChampObject> queryObjects(Map<String, Object> queryParams, Optional<ChampTransaction> transaction) throws ChampTransactionException;

  /**
   * Create/Update a relationship.  
   * <p>
   * If the ChampRelationship key is present, an update will be attempted,
   * otherwise a create will be attempted.  Each implementation has different guarantees on
   * validation - see the specific implementation for more details on this.
   * 
   * @param relationship - The ChampRelationship that you wish to store in the graph
   * 
   * @return The ChampRelationship as it was stored
   * 
   * @throws ChampMarshallingException If the {@code relationship} is not able to be marshalled into the backend representation
   * @throws ChampSchemaViolationException If the {@code relationship} violates the constraints specifed by {@link ChampGraph#retrieveSchema}
   * @throws ChampObjectNotExistsException If either the source or target object referenced by this relationship does not exist in the graph
   * @throws ChampRelationshipNotExistsException If {@link org.onap.aai.champcore.model.ChampRelationship#getKey}.isPresent() but the object cannot be found in the graph
   * @throws ChampUnmarshallingException If the edge which was created could not be unmarshalled into a ChampRelationship
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   */
  public ChampRelationship storeRelationship(ChampRelationship relationship) throws ChampMarshallingException, ChampObjectNotExistsException, ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampUnmarshallingException, ChampTransactionException;

 
  /**
    * Create/Update a relationship.  
    * <p>
    * If the ChampRelationship key is present, an update will be attempted,
    * otherwise a create will be attempted.  Each implementation has different guarantees on
    * validation - see the specific implementation for more details on this.
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion.
    * 
    * @param relationship - The ChampRelationship that you wish to store in the graph
    * @param transaction  - Optional transaction context to perform the operation in.
    * 
    * @return The ChampRelationship as it was stored
    * 
    * @throws ChampMarshallingException If the {@code relationship} is not able to be marshalled into the backend representation
    * @throws ChampSchemaViolationException If the {@code relationship} violates the constraints specifed by {@link ChampGraph#retrieveSchema}
    * @throws ChampObjectNotExistsException If either the source or target object referenced by this relationship does not exist in the graph
    * @throws ChampRelationshipNotExistsException If {@link org.onap.aai.champcore.model.ChampRelationship#getKey}.isPresent() but the object cannot be found in the graph
    * @throws ChampUnmarshallingException If the edge which was created could not be unmarshalled into a ChampRelationship
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    */
  public ChampRelationship storeRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampObjectNotExistsException, ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampUnmarshallingException, ChampTransactionException;
	
  /**
   * Replace a relationship. 
   * <p>
   * ChampRelationship key is mandatory.  The main purpose of this method is to replace the
   * entire properties of an existing relationship.  Source/Target can't be updated with this method.
   * <p>
   * Each implementation has different guarantees on validation - see the specific implementation 
   * for more details on this.
   * 
   * @param relationship - The ChampRelationship that you wish to replace in the graph
   * 
   * @return The ChampRelationship as it was stored
   * 
   * @throws ChampMarshallingException If the {@code relationship} is not able to be marshalled into the backend representation
   * @throws ChampSchemaViolationException If the {@code relationship} violates the constraints specifed by {@link ChampGraph#retrieveSchema}
   * @throws ChampRelationshipNotExistsException If {@link org.onap.aai.champcore.model.ChampRelationship#getKey} is not present or object not found in the graph
   * @throws ChampUnmarshallingException If the edge which was created could not be unmarshalled into a ChampRelationship
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed. 
   */
  public ChampRelationship replaceRelationship(ChampRelationship relationship) throws ChampMarshallingException, ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampUnmarshallingException, ChampTransactionException;   

  /**
    * Replace a relationship. 
    * <p>
    * ChampRelationship key is mandatory.  The main purpose of this method is to replace the
    * entire properties of an existing relationship.  Source/Target can't be updated with this method.
    * <p>
    * Each implementation has different guarantees on validation - see the specific implementation 
    * for more details on this.
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion.
    * 
    * @param relationship - The ChampRelationship that you wish to replace in the graph
    * @param transaction  - Optional transaction context to perform the operation in.
    * 
    * @return The ChampRelationship as it was stored
    * 
    * @throws ChampMarshallingException If the {@code relationship} is not able to be marshalled into the backend representation
    * @throws ChampSchemaViolationException If the {@code relationship} violates the constraints specifed by {@link ChampGraph#retrieveSchema}
    * @throws ChampRelationshipNotExistsException If {@link org.onap.aai.champcore.model.ChampRelationship#getKey} is not present or object not found in the graph
    * @throws ChampUnmarshallingException If the edge which was created could not be unmarshalled into a ChampRelationship
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    */
  public ChampRelationship replaceRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampUnmarshallingException, ChampTransactionException;	

  /**
   * Retrieve a relationship by its key.
   *  
   * @param key          - The key of the ChampRelationship in the graph 
   *                       {@link org.onap.aai.champcore.model.ChampRelationship#getKey()}
   * 
   * @return The {@link org.onap.aai.champcore.model.ChampRelationship} if it was present, otherwise {@link Optional#empty()}
   * 
   * @throws ChampUnmarshallingException If the relationship was found, but could not be unmarshalled
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   */
  public Optional<ChampRelationship> retrieveRelationship(Object key) throws ChampUnmarshallingException, ChampTransactionException;

 
  /**
    * Retrieve a relationship by its key.
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion.
    *  
    * @param key          - The key of the ChampRelationship in the graph 
    *                       {@link org.onap.aai.champcore.model.ChampRelationship#getKey()}
    * @param transaction  - Optional transaction context to perform the operation in. 
    * 
    * @return The {@link org.onap.aai.champcore.model.ChampRelationship} if it was present, otherwise {@link Optional#empty()}
    * 
    * @throws ChampUnmarshallingException If the relationship was found, but could not be unmarshalled
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    */
  public Optional<ChampRelationship> retrieveRelationship(Object key, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampTransactionException;

  /**
   * Delete a relationship by its key.
   * 
   * @param relationship - The ChampRelationship in the graph ({@link ChampRelationship#getKey must be present})
   * 
   * @throws ChampRelationshipNotExistsException If the object did not exist in the graph
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   */
  public void deleteRelationship(ChampRelationship relationship) throws ChampRelationshipNotExistsException, ChampTransactionException;

  /**
    * Delete a relationship by its key.
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion.
    * 
    * @param relationship - The ChampRelationship in the graph ({@link ChampRelationship#getKey must be present})
    * @param transaction  - Optional transaction context to perform the operation in.
    * 
    * @throws ChampRelationshipNotExistsException If the object did not exist in the graph
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    */
  public void deleteRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) throws ChampRelationshipNotExistsException, ChampTransactionException;

  /**
   * Retrieve the relationships which are incident to the {@code object}
   * 
   * @param object       - The object you wish to find incident relationships for
   * 
   * @return A {@link Stream} where each {@link ChampRelationship} has this {@code object} as either a source or target object
   * 
   * @throws ChampUnmarshallingException If any of the ChampRelationship objects could not be unmarshalled
   * @throws ChampObjectNotExistsException If the {@code object} does not exist in this graph
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   */
  public Stream<ChampRelationship> retrieveRelationships(ChampObject object) throws ChampUnmarshallingException, ChampObjectNotExistsException, ChampTransactionException;

 
  /**
    * Retrieve the relationships which are incident to the {@code object}
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion.
    * 
    * @param object       - The object you wish to find incident relationships for
    * @param transaction  - Optional transaction context to perform the operation in.
    * 
    * @return A {@link Stream} where each {@link ChampRelationship} has this {@code object} as either a source or target object
    * 
    * @throws ChampUnmarshallingException If any of the ChampRelationship objects could not be unmarshalled
    * @throws ChampObjectNotExistsException If the {@code object} does not exist in this graph
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    */
  public Stream<ChampRelationship> retrieveRelationships(ChampObject object, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampObjectNotExistsException, ChampTransactionException;

  /**
   * Retrieve the relationships whose properties match the given {@code queryParams}
   * 
   * @param queryParams - The key/value pairs to search for in the {@link ChampRelationship#getProperties}
   * 
   * @return A {@link Stream} where each {@link ChampRelationship#getProperties} contains the {@code queryParams}
   * 
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   */
  public Stream<ChampRelationship> queryRelationships(Map<String, Object> queryParams) throws ChampTransactionException;

  /**
    * Retrieve the relationships whose properties match the given {@code queryParams}
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion. 
    * 
    * @param queryParams - The key/value pairs to search for in the {@link ChampRelationship#getProperties}
    * @param transaction - Optional transaction context to perform the operation in.
    * 
    * @return A {@link Stream} where each {@link ChampRelationship#getProperties} contains the {@code queryParams}
    * 
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed. 
    */
  public Stream<ChampRelationship> queryRelationships(Map<String, Object> queryParams, Optional<ChampTransaction> transaction) throws ChampTransactionException;

  /**
   * Create/Update a {@link ChampPartition}.  If any of the ChampObjects or ChampRelationships
   * present in this ChampPartition already exist, an update will be attempted, otherwise a create
   * will be attempted.  
   * <p>
   * Each implementation has different guarantees on validation -
   * see the specific implementation details for more information on this.
   *  
   * @param partition   - The ChampPartition you wish to store in this graph
   * 
   * @throws ChampMarshallingException If any of the objects or relationships contained in this
   *                                       partition could not be marshalled into its backed representation
   * @throws ChampObjectNotExistsException If any of the objects being updated do not exist, or if a relationship
   *                                           contain objects which do not exist in the graph.
   * @throws ChampSchemaViolationException If any of the objects or relationships violate the schema provided by {@link retrieveSchema}
   * @throws ChampRelationshipNotExistsException If any of the relationships which are being updated do not exist
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   * 
   * @return The ChampPartition as is was stored in the graph (contains keys for each newly created object)
   */
  public ChampPartition storePartition(ChampPartition partition) throws ChampMarshallingException, ChampObjectNotExistsException, ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampTransactionException;

  /**
    * Create/Update a {@link ChampPartition}.  If any of the ChampObjects or ChampRelationships
    * present in this ChampPartition already exist, an update will be attempted, otherwise a create
    * will be attempted.  
    * <p>
    * Each implementation has different guarantees on validation -
    * see the specific implementation details for more information on this.
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion. 
    *  
    * @param partition   - The ChampPartition you wish to store in this graph
    * @param transaction - Optional transaction context to perform the operation in.
    * 
    * @throws ChampMarshallingException If any of the objects or relationships contained in this
    * 										partition could not be marshalled into its backed representation
    * @throws ChampObjectNotExistsException If any of the objects being updated do not exist, or if a relationship
    * 											contain objects which do not exist in the graph.
    * @throws ChampSchemaViolationException If any of the objects or relationships violate the schema provided by {@link retrieveSchema}
    * @throws ChampRelationshipNotExistsException If any of the relationships which are being updated do not exist
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    * 
    * @return The ChampPartition as is was stored in the graph (contains keys for each newly created object)
    */
  public ChampPartition storePartition(ChampPartition partition, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampObjectNotExistsException, ChampSchemaViolationException, ChampRelationshipNotExistsException, ChampTransactionException;

  /**
   * Delete the {@code partition} from the graph.
   * 
   * @param partition   - The partition to delete from the graph
   * 
   * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
   */
  public void deletePartition(ChampPartition partition) throws ChampTransactionException;
 
  /**
    * Delete the {@code partition} from the graph.
    * <p>
    * If a transaction context is not provided, then a transaction will be automatically 
    * created and committed for this operation only, otherwise, the supplied transaction
    * will be used and it will be up to the caller to commit the transaction at its 
    * discretion.
    * 
    * @param partition   - The partition to delete from the graph
    * @param transaction - Optional transaction context to perform the operation in.
    * 
    * @throws ChampTransactionException If an attempt to commit or rollback the transaction failed.
    */
  public void deletePartition(ChampPartition partition, Optional<ChampTransaction> transaction) throws ChampTransactionException;

  /**
    * Create/Update an object index on the graph
    * @param index - The index to create on this {@code graph}
    */
  public void storeObjectIndex(ChampObjectIndex index);

	/**
	 * Retrieve an object index on the graph by its {@code indexName}
	 * @param indexName The name of the index to retrieve from the graph
	 * @return The {@link ChampObjectIndex} which matches the given @{code indexName} in the graph
	 */
	public Optional<ChampObjectIndex> retrieveObjectIndex(String indexName);

	/**
	 * Retrieve the object indices on the graph
	 * @return A {@link Stream} where each {@link ChampObjectIndex} exists in the graph
	 */
	public Stream<ChampObjectIndex> retrieveObjectIndices();

	/**
	 * Delete the object index on the graph by its {@code indexName}
	 * @param indexName The name of the index to delete from the graph
	 * @throws ChampIndexNotExistsException If an index does not exist with the given {@code indexName} in the graph
	 */
	public void deleteObjectIndex(String indexName) throws ChampIndexNotExistsException;

	/**
	 * Create/Update a relationship index on the graph
	 * @param index The relationship index to create on the graph
	 */
	public void storeRelationshipIndex(ChampRelationshipIndex index);

	/**
	 * Retrieve a relationship index from the graph
	 * @param indexName The name of the relationship index to retrieve from the graph
	 * @return The {@link ChampRelationshipIndex} which matches the given {@code indexName} in the graph
	 * 			or {@link Optional#empty} if no such relationship index exists
	 */
	public Optional<ChampRelationshipIndex> retrieveRelationshipIndex(String indexName);

	/**
	 * Retrieve the relationship indices from the graph
	 * @return A {@link Stream} where each {@link ChampRelationshipIndex} exists in the graph
	 */
	public Stream<ChampRelationshipIndex> retrieveRelationshipIndices();

	/**
	 * Delete a relationship index from the graph
	 * @param indexName THe name of the index to delete from the graph
	 * @throws ChampIndexNotExistsException If an index does not exist with the give {@code indexName} in the graph
	 */
	public void deleteRelationshipIndex(String indexName) throws ChampIndexNotExistsException;

	/**
	 * Create/Update the schema for a graph
	 * @param schema The {@link ChampSchema} to create or update on the graph
	 * @throws ChampSchemaViolationException If this schema update would violate the current schema
	 */
	public void storeSchema(ChampSchema schema) throws ChampSchemaViolationException;

	/**
	 * Retrieve the schema for a graph
	 * @return The {@link ChampSchema} for the graph
	 */
	public ChampSchema retrieveSchema();

	/**
	 * Create/Update an object constraint on a schema
	 * @param objectConstraint The object constraint you wish to create/update for the graph
	 * @throws ChampSchemaViolationException If this schema update would violate the current schema
	 */
	public void updateSchema(ChampObjectConstraint objectConstraint) throws ChampSchemaViolationException;

	/**
	 * Create/Update a relationship constraint on a schema
	 * @param schema The relationship constraint you wish to create/update for the graph
	 * @throws ChampSchemaViolationException If this schema update would violate the current schema
	 */
	public void updateSchema(ChampRelationshipConstraint schema) throws ChampSchemaViolationException;

	/**
	 * Delete the schema for a graph
	 */
	public void deleteSchema();

	/**
	 * Shutdown the ChampAPI. It is up to the caller to synchronize access to the ChampAPI
	 * so that shutting it down does not interfere with concurrent operations.
	 */
	public void shutdown();

	/**
	 * Used to determine what the outcome of certain ChampGraph operations will be.  For example,
	 * if this graph is not capable of deleting object indices, you can expect those calls to fail.
	 * @see ChampCapabilities
	 * @return What this graph is capable of performing
	 */
	 public ChampCapabilities capabilities();


   public void createDefaultIndexes();
}
