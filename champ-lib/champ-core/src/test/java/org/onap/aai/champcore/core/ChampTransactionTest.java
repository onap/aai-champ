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
package org.onap.aai.champcore.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.FeatureDescriptor;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.champcore.ChampAPI;
import org.onap.aai.champcore.ChampCapabilities;
import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.NoOpTinkerPopTransaction;
import org.onap.aai.champcore.exceptions.ChampIndexNotExistsException;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.graph.impl.AbstractTinkerpopChampGraph;
import org.onap.aai.champcore.graph.impl.TinkerpopTransaction;
import org.onap.aai.champcore.model.ChampObjectIndex;
import org.onap.aai.champcore.model.ChampRelationshipIndex;
import org.onap.aai.champcore.schema.ChampSchemaEnforcer;


public class ChampTransactionTest {
  
  TestGraph g = null;
  
  @Before
  public void setup() {
    g = new TestGraph();
  }
    
  /**
   * This test validates the behaviour when the underlying graph implementation
   * does not support transactions.
   */
  @Test
  public void testNoTransactionSupport() {
    
    // By default our test graph should be configured to support transactions, but
    // set it explicitly anyway just to be sure.
    g.setTransactionSupport(true);
    
    // Now, try to start a new transaction against our graph - it should succeed.
    TinkerpopTransaction t = new TinkerpopTransaction(g);
    Graph gi = t.getGraphInstance(); // GDF: Making jacoco happy...
    
    // Now, configure our graph to specify that transactions are NOT supported.
    g.setTransactionSupport(false);
    
    try {
      
      // Now, try to start a new transaction against our graph.
      t = new TinkerpopTransaction(g);
      
    } catch (UnsupportedOperationException e) {
      
      // If we're here, it's all good since we expected an exception to be thrown (since our
      // graph does NOT support transactions).
      return;
    }

    // If we're here then we were able to open a transaction even though our graph does 
    // not support that functionality.
    fail("Attempt to open a transaction against a graph with no transaction support should not succeed.");
  }
  
  
  /**
   * This test validates the behaviour when committing a transaction under various
   * circumstances.
   * @throws ChampTransactionException 
   */
  @Test
  public void testCommit() throws ChampTransactionException {
    
    // By default our test graph should simulate successful commits, but set
    // the configuration anyway, just to be sure.
    g.setFailCommits(false);
    
    // Now, start a transaction.
    TinkerpopTransaction t = new TinkerpopTransaction(g);
    
    // Call commit against the transaction - it should complete successfully.
    t.commit();
    
    // Now, configure our test graph to simulate failing to commit.
    g.setFailCommits(true);
    
    // Open another transaction...
    t = new TinkerpopTransaction(g);
    boolean exceptionThrown = false;
    try {
      
      //...and try to commit it.
      t.commit();
      
    } catch (Throwable e) {
      
      // Our commit should have failed and ultimately thrown an exception - so if we
      // are here then it's all good.
      exceptionThrown = true;
    }
    
    assertTrue("Failed commit should have produced an exception.", exceptionThrown);
  }

  
  @Test
  public void testRollback() throws ChampTransactionException {
    
    // By default our test graph should simulate successful commits, but set
    // the configuration anyway, just to be sure.
    g.setFailCommits(false);
    
    // Now, start a transaction.
    TinkerpopTransaction t = new TinkerpopTransaction(g);
    
    // Call rollback against the transaction - it should complete successfully.
    t.rollback();
    
    // Now, configure our test graph to simulate failing to commit.
    g.setFailCommits(true);
    
    // Open another transaction...
    t = new TinkerpopTransaction(g);
    boolean exceptionThrown = false;
    try {
      
      //...and try to commit it.
      t.rollback();
      
    } catch (Throwable e) {
      
      // Our commit should have failed and ultimately thrown an exception - so if we
      // are here then it's all good.
      exceptionThrown = true;
    }
    
    assertTrue("Failed rollback should have produced an exception.", exceptionThrown);
  }

  @Test
  public void test() throws ChampTransactionException {
    
    AbstractTinkerpopChampGraph graph = new AbstractTinkerpopChampGraph(new HashMap<String, Object>()) { 
      
      @Override
      protected Graph getGraph() {
        return TinkerGraph.open();
      }

      @Override
      protected ChampSchemaEnforcer getSchemaEnforcer() {
        return null;
      }

      @Override
      public void executeStoreObjectIndex(ChampObjectIndex index) { }

      @Override
      public Optional<ChampObjectIndex> retrieveObjectIndex(String indexName) {
        return null;
      }

      @Override
      public Stream<ChampObjectIndex> retrieveObjectIndices() {
        return null;
      }

      @Override
      public void executeDeleteObjectIndex(String indexName) throws ChampIndexNotExistsException {}

      @Override
      public void executeStoreRelationshipIndex(ChampRelationshipIndex index) {}

      @Override
      public Optional<ChampRelationshipIndex> retrieveRelationshipIndex(String indexName) {
        return null;
      }

      @Override
      public Stream<ChampRelationshipIndex> retrieveRelationshipIndices() {
        return null;
      }

      @Override
      public void executeDeleteRelationshipIndex(String indexName) throws ChampIndexNotExistsException {}

      @Override
      public ChampCapabilities capabilities() {
        return null;
      }

	@Override
	public GraphTraversal<?, ?> hasLabel(GraphTraversal<?, ?> query, Object type) {
		// TODO Auto-generated method stub
		return null;
	}
    };
    
    TinkerpopTransaction t = new TinkerpopTransaction(g);
    t.id();
    graph.commitTransaction(t);
    graph.rollbackTransaction(t);
    
  }
  
  private class TestGraph implements Graph {

    private boolean supportsTransactions = true;
    private boolean failCommits = false;
    
    
    public void setTransactionSupport(boolean supportsTransactions) {
      this.supportsTransactions = supportsTransactions;
    }
    
    public void setFailCommits(boolean failCommits) {
      this.failCommits = failCommits;
    }
    @Override
    public Vertex addVertex(Object... keyValues) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void close() throws Exception {
      // TODO Auto-generated method stub
      
    }

    @Override
    public GraphComputer compute() throws IllegalArgumentException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public <C extends GraphComputer> C compute(Class<C> graphComputerClass)
        throws IllegalArgumentException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Configuration configuration() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Iterator<Edge> edges(Object... edgeIds) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Transaction tx() {
      return new TestTransaction();
    }

    @Override
    public Variables variables() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Iterator<Vertex> vertices(Object... vertexIds) {
      // TODO Auto-generated method stub
      return null;
    }
    
    /**
     * Gets the {@link Features} exposed by the underlying {@code Graph} implementation.
     */
    public Features features() {
      return new TestFeatures() {
      };
    }
    
    public class TestFeatures implements Graph.Features {
      
      /**
       * Gets the features related to "graph" operation.
       */
      public GraphFeatures graph() {
          return new TestGraphFeatures() {
          };
      }
      
      public class TestGraphFeatures implements Graph.Features.GraphFeatures {
        
        /**
         * Determines if the {@code Graph} implementations supports transactions.
         */
        @FeatureDescriptor(name = FEATURE_TRANSACTIONS)
        public boolean supportsTransactions() {
            return supportsTransactions;
        }
      }
    }
  }
  
  private class TestTransaction implements Transaction {

    @Override
    public void addTransactionListener(Consumer<Status> listener) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void clearTransactionListeners() {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void close() {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void commit() {
      
      if(g.failCommits) {
        throw new UnsupportedOperationException();
      } 
    }

    @Override
    public <G extends Graph> G createThreadedTx() {
     return (G) g;
    }

    @Override
    public boolean isOpen() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Transaction onClose(Consumer<Transaction> consumer) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Transaction onReadWrite(Consumer<Transaction> consumer) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void open() {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void readWrite() {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void removeTransactionListener(Consumer<Status> listener) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void rollback() {
      if(g.failCommits) {
        throw new UnsupportedOperationException();
      } 
    }

    @Override
    public <R> Workload<R> submit(Function<Graph, R> work) {
      // TODO Auto-generated method stub
      return null;
    }
    
  }

  private class TestTinkerpopGraph extends AbstractTinkerpopChampGraph {

    protected TestTinkerpopGraph(Map<String, Object> properties) {
      super(properties);
    }

    @Override
    protected Graph getGraph() {
      return TinkerGraph.open();
    }

    
    @Override
    protected ChampSchemaEnforcer getSchemaEnforcer() {
      return null;
    }

    @Override
    public void executeStoreObjectIndex(ChampObjectIndex index) {      
    }

    @Override
    public Optional<ChampObjectIndex> retrieveObjectIndex(String indexName) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Stream<ChampObjectIndex> retrieveObjectIndices() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void executeDeleteObjectIndex(String indexName) throws ChampIndexNotExistsException {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void executeStoreRelationshipIndex(ChampRelationshipIndex index) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public Optional<ChampRelationshipIndex> retrieveRelationshipIndex(String indexName) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Stream<ChampRelationshipIndex> retrieveRelationshipIndices() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void executeDeleteRelationshipIndex(String indexName)
        throws ChampIndexNotExistsException {
      // TODO Auto-generated method stub
      
    }

    @Override
    public ChampCapabilities capabilities() {
      // TODO Auto-generated method stub
      return null;
    }

	@Override
	public GraphTraversal<?, ?> hasLabel(GraphTraversal<?, ?> query, Object type) {
		// TODO Auto-generated method stub
		return null;
	}
    
  }
}
