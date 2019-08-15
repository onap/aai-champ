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
package org.onap.aai.champcore.graph.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.onap.aai.champcore.ChampCoreMsgs;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.exceptions.ChampMarshallingException;
import org.onap.aai.champcore.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champcore.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champcore.exceptions.ChampSchemaViolationException;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampPartition;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.ChampSchema;
import org.onap.aai.champcore.model.fluent.partition.CreateChampPartitionable;
import org.onap.aai.champcore.transform.TinkerpopChampformer;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractTinkerpopChampGraph extends AbstractValidatingChampGraph {
	
	private static final Logger LOGGER = LoggerFactory.getInstance().getLogger(AbstractTinkerpopChampGraph.class);
	private static final TinkerpopChampformer TINKERPOP_CHAMPFORMER = new TinkerpopChampformer();
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private volatile AtomicBoolean isShutdown;

	protected AbstractTinkerpopChampGraph(Map<String, Object> properties) {
	  super(properties);
	  
	  isShutdown = new AtomicBoolean(false);
      Runtime.getRuntime().addShutdownHook(shutdownHook);
	}
	
	private static final TinkerpopChampformer getChampformer() {
		return TINKERPOP_CHAMPFORMER;
	}

	private static final ObjectMapper getObjectMapper() {
		return OBJECT_MAPPER;
	}

	public abstract GraphTraversal<?, ?> hasLabel(GraphTraversal<?, ?> query, Object type);
	
	public ChampTransaction openTransaction() {
	  
	  return new TinkerpopTransaction(getGraph());
	}	  
	
	private Vertex writeVertex(ChampObject object, ChampTransaction transaction) throws ChampObjectNotExistsException, ChampMarshallingException {
		final Vertex vertex;
		
		Graph graphInstance = ((TinkerpopTransaction)transaction).getGraphInstance();
		
		if (object.getKey().isPresent()) {
			final Iterator<Vertex> vertexIter = graphInstance.vertices(object.getKey().get());

			if (vertexIter.hasNext()) {
				vertex = vertexIter.next();
			} else throw new ChampObjectNotExistsException();
		} else {
			vertex = graphInstance.addVertex(object.getType());
		}

		for (Entry<String, Object> property : object.getProperties().entrySet()) {

			if (property.getValue() instanceof List) {
				for (Object subPropertyValue : (List<?>) property.getValue()) {
					vertex.property(VertexProperty.Cardinality.list, property.getKey(), subPropertyValue);
				}
			} else if (property.getValue() instanceof Set) {
				for (Object subPropertyValue : (Set<?>) property.getValue()) {
					vertex.property(VertexProperty.Cardinality.set, property.getKey(), subPropertyValue);
				}
			} else {
				vertex.property(property.getKey(), property.getValue());
			}
		}

		return vertex;
	}
	
	private Vertex replaceVertex(ChampObject object, ChampTransaction transaction) throws ChampObjectNotExistsException, ChampMarshallingException {
		Vertex vertex;
		
		Graph graphInstance = ((TinkerpopTransaction)transaction).getGraphInstance();
		
		if (object.getKey().isPresent()) {
			final Iterator<Vertex> vertexIter = graphInstance.vertices(object.getKey().get());

			if (vertexIter.hasNext()) {
				vertex = vertexIter.next();
			} else throw new ChampObjectNotExistsException();
		} else {
			throw new ChampObjectNotExistsException();
		}

		//clear all the existing properties
		Iterator<VertexProperty<Object>> it = vertex.properties();
		while (it.hasNext()) {
			it.next().remove();
		}
		
		for (Entry<String, Object> property : object.getProperties().entrySet()) {

			if (property.getValue() instanceof List) {
				for (Object subPropertyValue : (List<?>) property.getValue()) {
					vertex.property(VertexProperty.Cardinality.list, property.getKey(), subPropertyValue);
				}
			} else if (property.getValue() instanceof Set) {
				for (Object subPropertyValue : (Set<?>) property.getValue()) {
					vertex.property(VertexProperty.Cardinality.set, property.getKey(), subPropertyValue);
				}
			} else {
				vertex.property(property.getKey(), property.getValue());				
			}
		}

		return vertex;
	}

	private Edge writeEdge(ChampRelationship relationship, ChampTransaction transaction) throws ChampObjectNotExistsException, ChampRelationshipNotExistsException, ChampMarshallingException {

		final Vertex source = writeVertex(relationship.getSource(), transaction);
		final Vertex target = writeVertex(relationship.getTarget(), transaction);
		final Edge edge;

		Graph graphInstance = ((TinkerpopTransaction)transaction).getGraphInstance();

		if (relationship.getKey().isPresent()) {
			final Iterator<Edge> edgeIter = graphInstance.edges(relationship.getKey().get());

			if (edgeIter.hasNext()) {
				edge = edgeIter.next();
			} else throw new ChampRelationshipNotExistsException();
		} else {
			edge = source.addEdge(relationship.getType(), target);
		}

		for (Entry<String, Object> property : relationship.getProperties().entrySet()) {
			edge.property(property.getKey(), property.getValue());
		}

		return edge;
	}
	
	private Edge replaceEdge(ChampRelationship relationship, ChampTransaction tx) throws  ChampRelationshipNotExistsException, ChampMarshallingException {
		final Edge edge;
		Graph graphInstance = ((TinkerpopTransaction)tx).getGraphInstance();
		
		if(!relationship.getSource().getKey().isPresent() || !relationship.getTarget().getKey().isPresent()){
			throw new IllegalArgumentException("Invalid source/target");
		}
		
		if (relationship.getKey().isPresent()) {
			final Iterator<Edge> edgeIter = graphInstance.edges(relationship.getKey().get());


			if (edgeIter.hasNext()) {
				edge = edgeIter.next();
				//validate if the source/target are the same as before. Throw error if not the same
				if (!edge.outVertex().id().equals(relationship.getSource().getKey().get())
						|| !edge.inVertex().id().equals(relationship.getTarget().getKey().get())) {
					throw new IllegalArgumentException("source/target can't be updated");
				}

			} else throw new ChampRelationshipNotExistsException();
		} else {
			throw new ChampRelationshipNotExistsException();
		}
		
		// clear all the existing properties
		Iterator<Property<Object>> it = edge.properties();
		while (it.hasNext()) {
			it.next().remove();
		}
				
		for (Entry<String, Object> property : relationship.getProperties().entrySet()) {
			edge.property(property.getKey(), property.getValue());
		}

		return edge;
	}


	protected abstract Graph getGraph();

	

	private Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			try {
				shutdown();
			} catch (IllegalStateException e) {
				//Suppress, because shutdown() has already been called
			}
		}
	};

	protected boolean isShutdown() {
		return isShutdown.get();
	}

    @Override
	public Stream<ChampObject> queryObjects(Map<String, Object> queryParams) throws ChampTransactionException {
      return queryObjects(queryParams, Optional.empty());
    }

	
	@Override
	public Stream<ChampObject> queryObjects(Map<String, Object> queryParams, Optional<ChampTransaction> transaction) throws ChampTransactionException {
	  
	  if (isShutdown()) {
	    throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
	  }
	
	  // If we were not provided a transaction object then automatically open a transaction
      // now.
      final ChampTransaction tx = getOrCreateTransactionInstance(transaction);
      
      // Use the graph instance associated with our transaction.
      Graph graphInstance = ((TinkerpopTransaction)tx).getGraphInstance();
      
      //If they provided the object key, do this the quick way rather than creating a traversal
      if (queryParams.containsKey(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_KEY.toString())) {
        
        try {
          final Optional<ChampObject> object = 
              retrieveObject(queryParams.get(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_KEY.toString()),
                             transaction);
			
          if (object.isPresent()) {
            return Stream.of(object.get());
          } else {
            return Stream.empty();
          }
        } catch (ChampUnmarshallingException e) {
          LOGGER.warn(ChampCoreMsgs.CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_WARN, 
              "Failed to unmarshall object. " + e.getMessage());
          return Stream.empty();
        }
      }

      final GraphTraversal<Vertex, Vertex> query = graphInstance.traversal().V();

      for (Entry<String, Object> filter : queryParams.entrySet()) {      
        if (filter.getKey().equals(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString())) {
          continue; //For performance reasons, the label is the last thing to be added
        } else {
          query.has(filter.getKey(), filter.getValue());
        }
      }

      if (queryParams.containsKey(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString())) {
    	  hasLabel(query, queryParams.get(ChampObject.ReservedPropertyKeys.CHAMP_OBJECT_TYPE.toString()));
      }

      final Iterator<ChampObject> objIter = new Iterator<ChampObject> () {
	
        private ChampObject next;


          @Override
          public boolean hasNext() {
            while (query.hasNext()) {
              try {
                next = getChampformer().unmarshallObject(query.next());
                return true;
              } catch (ChampUnmarshallingException e) {
                LOGGER.warn(ChampCoreMsgs.CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_WARN, 
                    "Failed to unmarshall tinkerpop vertex during query, returning partial results" + e.getMessage());
              }					
            }

            // If we auto-created the transaction, then commit it now, otherwise it is up to the
            // caller to decide when and if to do the commit.
            if(!transaction.isPresent()) {
              try {
                tx.commit(); //Danger ahead if this iterator is not completely consumed
                             //then the transaction cache will hold stale values
              } catch (ChampTransactionException e) {
                LOGGER.warn(ChampCoreMsgs.CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_WARN, 
                    "Failed transaction commit due to: " + e.getMessage());
              } 
                           
            }

            next = null;
            return false;
          }

          @Override
          public ChampObject next() {
            if (next == null) {
              throw new NoSuchElementException();
            }
				
			return next;
		  }
      };

      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(objIter, 
                                                                      Spliterator.ORDERED | Spliterator.NONNULL), 
                                                                      false);
    }

    @Override
    public Optional<ChampObject> retrieveObject(Object key) throws ChampUnmarshallingException, ChampTransactionException {
      return retrieveObject(key, Optional.empty());
    }
    
	@Override
	public Optional<ChampObject> retrieveObject(Object key, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampTransactionException {
	  
	  if (isShutdown()) {
	    throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
	  }
	  
	  // If we were not provided a transaction object then automatically open a transaction
      // now.
      ChampTransaction tx = getOrCreateTransactionInstance(transaction);
	  
	  // Use the graph instance associated with our transaction.
	  Graph graphInstance = ((TinkerpopTransaction)tx).getGraphInstance();
	  
	  final Iterator<Vertex> vertices = graphInstance.vertices(key);
	  final Optional<ChampObject> optionalObject;

	  if (!vertices.hasNext()) {
	    optionalObject = Optional.empty();
	    
	  } else {
	    optionalObject = Optional.of(getChampformer().unmarshallObject(vertices.next()));
	  }
	  
	  // If we auto-created the transaction, then commit it now, otherwise it is up to the
	  // caller to decide when and if to do the commit.
	  if(!transaction.isPresent()) {
	    tx.commit();
	  }
	  
	  return optionalObject;
	}

    @Override
    public Stream<ChampRelationship> retrieveRelationships(ChampObject source) throws ChampUnmarshallingException, ChampObjectNotExistsException, ChampTransactionException {
      return retrieveRelationships(source, Optional.empty());
    }
    
	@Override
	public Stream<ChampRelationship> retrieveRelationships(ChampObject source, Optional<ChampTransaction> transaction) throws ChampUnmarshallingException, ChampObjectNotExistsException, ChampTransactionException {
	    
	  if (isShutdown()) {
	    throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
	  }
		
	  // If we were not provided a transaction object then automatically open a transaction
      // now.
      final ChampTransaction tx = getOrCreateTransactionInstance(transaction);
      
      // Use the graph instance associated with our transaction.
      Graph graphInstance = ((TinkerpopTransaction)tx).getGraphInstance();
      
	  final Vertex sourceVertex;

	  try {
	    sourceVertex = graphInstance.vertices(source.getKey().get()).next();
	  
	  } catch (NoSuchElementException e) {	
	    
	    // If we auto-created the transaction, then try to roll it back now, otherwise it is 
	    // up to the caller to decide when and if to do so.
	    if(!transaction.isPresent()) {
			tx.rollback();
	    }
	    
		throw new ChampObjectNotExistsException();
      }

	  final Iterator<Edge> edges = sourceVertex.edges(Direction.BOTH);
	  final Iterator<ChampRelationship> relIter = new Iterator<ChampRelationship> () {

	    private ChampRelationship next;

	    @Override
	    public boolean hasNext() {
	      while (edges.hasNext()) {
	        try {
	          next = getChampformer().unmarshallRelationship(edges.next());
	          return true;
	        } catch (ChampUnmarshallingException e) {
	          LOGGER.warn(ChampCoreMsgs.CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_WARN, 
	              "Failed to unmarshall tinkerpop edge during query, returning partial results" + e.getMessage());
	        }					
	      }

	      // If we auto-created the transaction, then commit it now, otherwise it is up to the
	      // caller to decide when and if to do the commit.
	      if(!transaction.isPresent()) {
	        try {
            tx.commit();   //Danger ahead if this iterator is not completely
                           //consumed, then the transaction cache will be stale
            
          } catch (ChampTransactionException e) {
            LOGGER.warn(ChampCoreMsgs.CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_WARN, 
                "Failed transaction commit due to: " + e.getMessage());
          } 
	                     
	      }        
	      next = null;
	      return false;
	    }

	    @Override
	    public ChampRelationship next() {
	      if (next == null) {
	        throw new NoSuchElementException();
	      }
				
	      return next;
	    }
	  };

	  return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                relIter, Spliterator.ORDERED | Spliterator.NONNULL), false);
	}

	
	@Override
    public ChampObject doStoreObject(ChampObject object, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampObjectNotExistsException, ChampTransactionException {
	      	
	  ChampTransaction tx = null;
	  
	  try {
	    
	    // If we were not provided a transaction object then automatically open a transaction
	    // now.
	    tx = getOrCreateTransactionInstance(transaction);
	    
	    // Now, store the object that we were supplied.
	    final Vertex vertex = writeVertex(object, tx);
	    
	    // Only auto-commit this operation if we were NOT provided a transaction context, 
	    // otherwise it is the caller's responsibility to commit the transaction when it
	    // is appropriate to do so.
	    if(!transaction.isPresent()) {
	      tx.commit();
	    }
	       
	    // Marshal the resulting vertex into a ChampObject and return it to the caller.
	    return ChampObject.create()
	                           .from(object)
	                           .withKey(vertex.id())
	                           .build();

	  } catch (ChampObjectNotExistsException e) {
	
        // Something went wrong.  If we auto-created the transaction, then try to roll it back
	    // now. If we were supplied a transaction context then it is the caller's responsibility 
	    // to decide whether or not to roll it back.
	    if(!transaction.isPresent()) {
	      tx.rollback();
	    }
	        
	    // Rethrow the exception.
	    throw e;
	  }
	}
	
	@Override
	public ChampObject doReplaceObject(ChampObject object, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampObjectNotExistsException, ChampTransactionException {

	  ChampTransaction tx = null;
	  
	  try {
		  
	    // If we were not provided a transaction object then automatically open a transaction
	    // now.
	    tx = getOrCreateTransactionInstance(transaction);
	        
	    final Vertex vertex = replaceVertex(object, tx);

        // Only auto-commit this operation if we were NOT provided a transaction context, 
        // otherwise it is the caller's responsibility to commit the transaction when it
        // is appropriate to do so.
        if(!transaction.isPresent()) {
          tx.commit();
        }
			
        // Marshal the resulting vertex into a ChampObject and return it to the caller.
        return ChampObject.create()
                               .from(object)
                               .withKey(vertex.id())
                               .build();
			
		} catch (ChampObjectNotExistsException e) {
		  
		  // Something went wrong.  If we auto-created the transaction, then try to roll it back
	      // now. If we were supplied a transaction context then it is the caller's responsibility 
	      // to decide whether or not to roll it back.
	      if(!transaction.isPresent()) {
	        tx.rollback();
	      }

	      // Rethrow the exception.
	      throw e;
		}
	}

	@Override
	public void executeDeleteObject(Object key, Optional<ChampTransaction> transaction) throws ChampObjectNotExistsException, ChampTransactionException {
	  
		if (isShutdown()) {
		  throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
		}

	    // If we were not provided a transaction object then automatically open a transaction
        // now.
        ChampTransaction tx = getOrCreateTransactionInstance(transaction);
		
	    // Use the graph instance associated with our transaction.
	    Graph graphInstance = ((TinkerpopTransaction)tx).getGraphInstance();
	      
		final Iterator<Vertex> vertex = graphInstance.vertices(key);

		if (!vertex.hasNext()) {
		  
		  // If we auto-created the transaction, then roll it back now, otherwise it
		  // is up to the caller to make that determination.
		  if(!transaction.isPresent()) {
			tx.rollback();
		  }
		  
		  throw new ChampObjectNotExistsException();
		}

		// Remove the vertex.
		vertex.next().remove();
		
		// If we auto-created the transaction, then commit it now, otherwise it
		// is up to the caller to decide if and when to commit.
		if(!transaction.isPresent()) {
		  tx.commit();
		}
	}

    @Override
    public ChampRelationship doStoreRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) 
        throws ChampUnmarshallingException, 
               ChampObjectNotExistsException, 
               ChampRelationshipNotExistsException, 
               ChampMarshallingException, 
               ChampTransactionException  {
      
      // If we were not provided a transaction object then automatically open a transaction
      // now.
      ChampTransaction tx = getOrCreateTransactionInstance(transaction);
      
      try {
        
        // Store the edge in the graph.
        final Edge edge = writeEdge(relationship, tx);
        
        // Unmarshal the stored edge into a ChampRelationship object
        ChampRelationship storedRelationship = getChampformer().unmarshallRelationship(edge);
        
        // If we auto-created the transaction, then commit it now, otherwise it
        // is up to the caller to decide if and when to commit.
        if(!transaction.isPresent()) {
          tx.commit();
        }
        
        // Finally, return the result to the caller.
        return storedRelationship;
        
      } catch (ChampObjectNotExistsException | 
               ChampRelationshipNotExistsException | 
               ChampUnmarshallingException | 
               ChampMarshallingException e) {
        
        // If we auto-create the transaction, then try to roll it back, otherwise
        // it is up to the caller to decide when and if to do so.
        if(!transaction.isPresent()) {
          tx.rollback();
        }
        throw e;
      }   
    }
    
	
	@Override
	public ChampRelationship doReplaceRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction)
			throws ChampUnmarshallingException, 
			       ChampRelationshipNotExistsException, 
			       ChampMarshallingException, 
			       ChampTransactionException  {

      // If we were not provided a transaction object then automatically open a transaction
      // now.
      ChampTransaction tx = getOrCreateTransactionInstance(transaction);
      
      try {
        final Edge edge = replaceEdge(relationship, tx);

		  ChampRelationship unmarshalledRelationship = getChampformer().unmarshallRelationship(edge);

		  // If we auto-created the transaction, then commit it now, otherwise it
		  // is up to the caller to decide if and when to commit.
		  if(!transaction.isPresent()) {
			  tx.commit();
		  }

		  return unmarshalledRelationship;
			
      } catch ( ChampRelationshipNotExistsException | ChampUnmarshallingException | ChampMarshallingException e) {
        
        // it is up to the caller to decide when and if to do so.
        if(!transaction.isPresent()) {
          tx.rollback();
        }

        throw e;
      }
	}  

	@Override
    public Stream<ChampRelationship> queryRelationships(Map<String, Object> queryParams) throws ChampTransactionException {
	  return queryRelationships(queryParams, Optional.empty());
	}
	   
	@Override
	public Stream<ChampRelationship> queryRelationships(Map<String, Object> queryParams, Optional<ChampTransaction> transaction) throws ChampTransactionException {
	  
	  if (isShutdown()) {
	    throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
	  }

      // If we were not provided a transaction object then automatically open a transaction
      // now.
      final ChampTransaction tx = getOrCreateTransactionInstance(transaction);
      
      // Use the graph instance associated with our transaction.
      Graph graphInstance = ((TinkerpopTransaction)tx).getGraphInstance();
      
	  // If they provided the relationship key, do this the quick way rather than creating a traversal
	  if (queryParams.containsKey(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_KEY.toString())) {
	    try {
	      final Optional<ChampRelationship> relationship = retrieveRelationship(queryParams.get(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_KEY.toString()),
	                                                                            Optional.of(tx));
			
	      if (relationship.isPresent()) {
	        return Stream.of(relationship.get());
	      
	      } else {
	        return Stream.empty();
	      }
	    } catch (ChampUnmarshallingException e) {
	      
	      LOGGER.warn(ChampCoreMsgs.CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_WARN, 
	          "Failed to unmarshall relationship" + e.getMessage());
	      return Stream.empty();
	    }
	  }
	 
	  final GraphTraversal<Edge, Edge> query = graphInstance.traversal().E();

	  for (Entry<String, Object> filter : queryParams.entrySet()) {
	    if (filter.getKey().equals(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString())) {
	      continue; //Add the label last for performance reasons
	    } else {
	      query.has(filter.getKey(), filter.getValue());
	    }
	  }

	  if (queryParams.containsKey(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString())) {
		  hasLabel(query, queryParams.get(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString()));
	  }

	  final Iterator<ChampRelationship> objIter = new Iterator<ChampRelationship> () {
	
	    private ChampRelationship next;

	    @Override
	    public boolean hasNext() {
	      while (query.hasNext()) {
	        try {
	          next = getChampformer().unmarshallRelationship(query.next());
	          return true;
	        } catch (ChampUnmarshallingException e) {
	          LOGGER.warn(ChampCoreMsgs.CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_WARN,
	              "Failed to unmarshall tinkerpop vertex during query, returning partial results" + e.getMessage());
	        }					
	      }

	      // If we auto-created the transaction, then commit it now, otherwise it
	      // is up to the caller to decide if and when to commit.
	      if(!transaction.isPresent()) {
	        try { 
            tx.commit();  //Danger ahead if this iterator is not completely
                          //consumed, then the transaction cache will be stale
            
          } catch (ChampTransactionException e) {
            LOGGER.warn(ChampCoreMsgs.CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_WARN,
                "Failed transaction commit due to " + e.getMessage());
          } 
	                     
	      }
					
	      next = null;
	      return false;
	    }

	    @Override
	    public ChampRelationship next() {
	      if (next == null) {
	        throw new NoSuchElementException();
	      }
				
	      return next;
	    }
	  };

	  return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
	            objIter, Spliterator.ORDERED | Spliterator.NONNULL), false);
	}

    @Override
    public Optional<ChampRelationship> retrieveRelationship(Object key)
            throws ChampUnmarshallingException, ChampTransactionException {
      return retrieveRelationship(key, Optional.empty());
    }
    
    @Override
    public Optional<ChampRelationship> retrieveRelationship(Object key, Optional<ChampTransaction> transaction)
			throws ChampUnmarshallingException, ChampTransactionException {
	  
      if (isShutdown()) {
        throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
      }

      // If we were not provided a transaction object then automatically open a transaction
      // now.
      ChampTransaction tx = getOrCreateTransactionInstance(transaction);
      
      // Use the graph instance associated with our transaction.
      Graph graphInstance = ((TinkerpopTransaction)tx).getGraphInstance();
      
      final Iterator<Edge> edge = graphInstance.edges(key);
      final Optional<ChampRelationship> optionalRelationship;

      if (!edge.hasNext()) {
        optionalRelationship = Optional.empty();
      } else {
        optionalRelationship = Optional.of(getChampformer().unmarshallRelationship(edge.next()));
      }

      // If we auto-created the transaction, then commit it now, otherwise it
      // is up to the caller to decide if and when to commit.
      if(!transaction.isPresent()) {
        tx.commit();
      }

      return optionalRelationship;
	}

    @Override
	public void executeDeleteRelationship(ChampRelationship relationship, Optional<ChampTransaction> transaction) throws ChampRelationshipNotExistsException, ChampTransactionException {

      if (isShutdown()) {
        throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
      }
      
      if (!relationship.getKey().isPresent()) {
        throw new IllegalArgumentException("Key must be provided when deleting a relationship");
      }

      // If we were not provided a transaction object then automatically open a transaction
      // now.
      ChampTransaction tx = getOrCreateTransactionInstance(transaction);
      
      // Use the graph instance associated with our transaction.
      Graph graphInstance = ((TinkerpopTransaction)tx).getGraphInstance();
      
      final Iterator<Edge> edge = graphInstance.edges(relationship.getKey().get());
		
      if (!edge.hasNext()) {
        
        // If we auto-created the transaction, then try to roll it back now, otherwise it
        // is up to the caller to decide if and when to do so.
        if(!transaction.isPresent()) {
          tx.rollback();
        }
        
        throw new ChampRelationshipNotExistsException();
      }
      
	  edge.next().remove();

	  // If we auto-created the transaction, then commit it now, otherwise it
	  // is up to the caller to decide if and when to commit.
	  if(!transaction.isPresent()) {
	    tx.commit();
	  }
    }

	
    @Override
    public ChampPartition doStorePartition(ChampPartition submittedPartition, Optional<ChampTransaction> transaction) throws ChampMarshallingException, ChampObjectNotExistsException, ChampRelationshipNotExistsException, ChampTransactionException {
		
	  if (isShutdown()) {
	    throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
	  }

      // If we were not provided a transaction object then automatically open a transaction
      // now.
      ChampTransaction tx = getOrCreateTransactionInstance(transaction);
            
	  try {
	    final HashMap<ChampObject, ChampObject> objectsWithKeys = new HashMap<ChampObject, ChampObject> ();
	    final CreateChampPartitionable storedPartition = ChampPartition.create();

	    for (ChampObject champObject : submittedPartition.getChampObjects()) {
	      final Vertex vertex = writeVertex(champObject, tx);
	      objectsWithKeys.put(champObject, ChampObject.create()
														.from(champObject)
														.withKey(vertex.id())
														.build());
	    }

	    for (ChampRelationship champRelationship : submittedPartition.getChampRelationships()) {
	      
	      if (!objectsWithKeys.containsKey(champRelationship.getSource())) {
	        final Vertex vertex = writeVertex(champRelationship.getSource(), tx);

	        objectsWithKeys.put(champRelationship.getSource(), ChampObject.create()
													             .from(champRelationship.getSource())
													             .withKey(vertex.id())
													             .build());
	      }

	      if (!objectsWithKeys.containsKey(champRelationship.getTarget())) {
	        final Vertex vertex = writeVertex(champRelationship.getTarget(), tx);

	        objectsWithKeys.put(champRelationship.getTarget(), ChampObject.create()
														        .from(champRelationship.getTarget())
														        .withKey(vertex.id())
														        .build());
	      }

	      final ChampRelationship.Builder relWithKeysBuilder = new ChampRelationship.Builder(objectsWithKeys.get(champRelationship.getSource()),
																							 objectsWithKeys.get(champRelationship.getTarget()),
																							 champRelationship.getType());

	      if (champRelationship.getKey().isPresent()) {
	        relWithKeysBuilder.key(champRelationship.getKey().get());
	      }
				
	      relWithKeysBuilder.properties(champRelationship.getProperties());

	      final Edge edge = writeEdge(relWithKeysBuilder.build(), tx);

	      storedPartition.withRelationship(ChampRelationship.create()
																.from(champRelationship)
																.withKey(edge.id())
																.build());
	    }

	    for (ChampObject object : objectsWithKeys.values()) {
	      storedPartition.withObject(object);
	    }

	    // If we auto-created the transaction, then commit it now, otherwise it
        // is up to the caller to decide if and when to commit.
        if(!transaction.isPresent()) {
          tx.commit();
        }
            
        return storedPartition.build();
			
      } catch (ChampObjectNotExistsException | ChampMarshallingException e) {
        
        // If we auto-created the transaction, then try to roll it back now, otherwise it
        // is up to the caller to decide if and when to do so.
        if(!transaction.isPresent()) {
          tx.rollback();
        }  

		throw e;
      }
	}

    @Override
	public void executeDeletePartition(ChampPartition graph, Optional<ChampTransaction> transaction) throws ChampTransactionException {
      
      if (isShutdown()) {
        throw new IllegalStateException("Cannot use ChampAPI after calling shutdown()");
      }

      // If we were not provided a transaction object then automatically open a transaction
      // now.
      ChampTransaction tx = getOrCreateTransactionInstance(transaction);
      
      // Use the graph instance associated with our transaction.
      Graph graphInstance = ((TinkerpopTransaction)tx).getGraphInstance();
      
      for (ChampObject champObject : graph.getChampObjects()) {
        try {
          final Object vertexId = champObject.getKey().get();
          final Iterator<Vertex> vertex = graphInstance.vertices(vertexId);
	
          if (vertex.hasNext()) {
            vertex.next().remove();
          }
		} catch (NoSuchElementException e) {
		  
		  // If we auto-created the transaction, then try to roll it back now, otherwise it
	      // is up to the caller to decide if and when to do so.
	      if(!transaction.isPresent()) {
	        tx.rollback();
	      } 

	      throw new IllegalArgumentException("Must pass a key to delete an object");
	    }
      }

      for (ChampRelationship champRelationship : graph.getChampRelationships()) {
        try {
          final Iterator<Edge> edge = graphInstance.edges(champRelationship.getKey().get());
		
          if (edge.hasNext()) {
            edge.next().remove();
          }
        } catch (NoSuchElementException e) {
          
          // If we auto-created the transaction, then try to roll it back now, otherwise it
          // is up to the caller to decide if and when to do so.
          if(!transaction.isPresent()) {
            tx.rollback();
          }

          throw new IllegalArgumentException("Must pass a key to delete a relationship");
        }
      }

      // If we auto-created the transaction, then commit it now, otherwise it
      // is up to the caller to decide if and when to commit.
      if(!transaction.isPresent()) {
        tx.commit();
      }
	}

	@Override
	public void shutdown() {

		if (isShutdown.compareAndSet(false, true)) {
		  super.shutdown();
			try {
				getGraph().close();
			} catch (Throwable t) {
				LOGGER.error(ChampCoreMsgs.CHAMPCORE_ABSTRACT_TINKERPOP_CHAMP_GRAPH_ERROR,
				    "Exception while shutting down graph" + t.getMessage());
			}
		} else {
			throw new IllegalStateException("Cannot call shutdown() after shutdown() was already initiated");
		}
	}

	@Override
	public void storeSchema(ChampSchema schema) throws ChampSchemaViolationException {
		if (isShutdown()) throw new IllegalStateException("Cannot call storeSchema() after shutdown has been initiated");

		if (getGraph().features().graph().variables().supportsVariables()) {
			try {
				getGraph().variables().set("schema", getObjectMapper().writeValueAsBytes(schema));
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		} else {
			super.storeSchema(schema);
		}
	}

	@Override
	public ChampSchema retrieveSchema() {
		if (isShutdown()) throw new IllegalStateException("Cannot call retrieveSchema() after shutdown has been initiated");

		if (getGraph().features().graph().variables().supportsVariables()) {
			final Optional<byte[]> schema = getGraph().variables().get("schema");

			if (schema.isPresent()) {
				try {
					return getObjectMapper().readValue(schema.get(), ChampSchema.class);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		return super.retrieveSchema();
	}

	@Override
	public void deleteSchema() {
		if (isShutdown()) throw new IllegalStateException("Cannot call deleteSchema() after shutdown has been initiated");

		if (getGraph().features().graph().variables().supportsVariables()) {
			getGraph().variables().remove("schema");
		} else {
			super.deleteSchema();
		}
	}
	
	public ChampTransaction getOrCreateTransactionInstance(Optional<ChampTransaction> transaction) {
	  
	  ChampTransaction tx = null;
	  
      // If we were not provided a transaction object then automatically open a transaction
      // now.
      if(!transaction.isPresent()) {
        
          tx = new TinkerpopTransaction(getGraph());
          
      } else {
        tx = transaction.get();
      }
      
      return tx;
	}
}
