Champ
=====

What is Champ?
--------------

Champ is an abstraction from underlying graph storage systems that A&AI would otherwise interface with.

Building Champ
--------------

Good ol' 'mvn clean install' does the trick.

API Specification
-----------------

Champ has CRUD methods for:

1) Objects
2) Relationships
3) Partitions (subgraphs)
3) Indices (on object and relationship properties)
4) Schemas

For each of these types, we offer builders and a more user-friendly fluent interface.

In the future we plan on adding in traversals, but at the moment that is outside the scope of Champ.  If you have suggestions on how this should be implemented, we look forward to your pull request.

API Implementations
-------------------

Champ ships with a simple in-memory implementation as well as a Titan implementation.  We recommend the in-memory implementation for unit testing and prototyping.  If you would like to have your implementation referenced in this readme, please create a pull-request.  Please note that all implementations will reside in their own repository - not in the Champ repository.

Usage
-----

### ChampAPI

The ChampAPI interface is basically just for tracking and properly shutting down multiple graph instances.  If you need this functionality, use the ChampAPI.  However, if you only ever access 1 graph, you may choose to use a single ChampGraph.

For getting started quickly, use the ChampAPI.Factory or ChampGraph.Factory to create either an In-memory implementation (for dev/test) or if you're running Titan locally, you can start a Titan instance.  For complex environments, each ChampGraph implementation will vary - the Titan implementation is described below.

### ChampGraph

This is the meat and potatoes of Champ.  It contains all of the CRUD methods mentioned above for Objects, Relationships, Partitions, Indices, and Schemas. Each implementation varies in how you instantiate it. The ones that ship with Champ are described below.

#### In-memory ChampGraph

Simply:

```
final ChampGraph graph = ChampGraph.Factory.newInstance(ChampGraph.Type.IN_MEMORY, "someGraphName");

//Do stuff with graph

graph.shutdown();

```

or:

```
final ChampAPI api = ChampAPI.Factory.newInstance(ChampGraph.Type.IN_MEMORY);
final ChampGraph dogsGraph = api.getGraph("dogsGraph");
final ChampGraph catsGraph = api.getGraph("catsGraph");

api.shutdown(); //This will shutdown all graphs started by api.getGraph(String)

```

#### Titan ChampGraph

For a Titan instance running on top of Cassandra locally, simply:

```
final ChampGraph graph = ChampGraph.Factory.newInstance(ChampGraph.Type.TITAN, "dogsGraph");

//Do stuff with graph

graph.shutdown();

```
or:

```
final ChampAPI api = ChampAPI.Factory.newInstance(ChampGraph.Type.TITAN);
final ChampGraph dogsGraph = api.getGraph("dogsGraph");
final ChampGraph cats Graph = api.getGraph("catsGraph");

api.shutdown(); //This will shutdown all graph started by api.getGraph(String);

```

For more complex/customized configurations:

```
	final ChampGraph graph = new TitanChampGraphImpl.Builder(graphName)
							.property("storage.backend", "cassandrathrift")
							.property("storage.hostname", "localhost")
							.build();
```

The calls to .property(String, String) accept all configuration options found [here](http://s3.thinkaurelius.com/docs/titan/1.0.0/titan-config-ref.html)

You could also implement the ChampAPI interface to manage multiple graphs connected to this Titan cluster.  See the ChampAPIImpl class for an example of how to do this.

### Creating Objects

#### Create a new object

```
ChampObject.create()
	   .ofType("foo")  //The "foo" type of object can be constrained by a ChampObjectConstraint
	   .withoutKey()   //No key for this object indicates that the underlying Champ implementation should create this object
	   .withProperty("bar", "string") //Zero or more properties can be set on a ChampObject
	   .withProperty("baz", 30)
	   .build()
```

#### Copy an existing object

```
ChampObject.create()
	   .from(foo) //'foo' is a reference to a ChampObject
	   .withoutKey()
	   .withProperty("pi", 3.14f)
	   .build()
```

#### Persisting an object

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
graph.storeObject(foo); //'foo' is a reference to a ChampObject
graph.shutdown(); //The ChampGraph is thread-safe, and only one needs to be created
		//Once your application is finished using it, call shutdown()
		//to cleanup any loose ends
```

#### Retrieve an object

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
final Optional<ChampObject> object = graph.retrieveObject("329847"); //Note that the key is usually only known by virtue of previously storing/retrieving/querying it

graph.shutdown();

```

#### Query objects

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
final Stream<ChampObject> objects = graph.queryObjects(Collection.singletonMap("favoriteDog", "Ace"));

objects.close(); //You must close the stream when you are finished with it
graph.shutdown();

```

### Creating Relationships

#### Create a new relationship

In this example we create the relationship:

dog eats dogPellets

```
ChampRelationship.create()
		 .ofType("eats")
		 .withoutKey()
		 .withSource()
		 	.ofType("dog")
			.withoutKey()
			.withProperty("name", "champ")
			.build()
		 .withTarget()
			.ofType("dogPellets")
			.withoutKey()
			.withProperty("brand", "costco")
			.build()
		 .withProperty("at", System.currentTimeMillis())
		 .build()
```

#### Persisting a relationship

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
graph.storeRelationship(champEatsCostcoFood); //'champEatsCostcoFood' is a reference to a ChampRelationship
graph.shutdown(); //The ChampGraph is thread-safe, and only one needs to be created
		//Once your application is finished using it, call shutdown()
		//to cleanup any loose ends
```

#### Retrieving incident relationships

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
final Stream<ChampRelationship> relationships = graph.retrieveRelationships(ChampObject.create().withKey("foo").build());

relationships.close(); //You must close the stream when you are done with it
graph.shutdown(); //The ChampGraph is thread-safe, and only one needs to be created

```

#### Querying relationship

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
final Stream<ChampRelationship> relationships = graph.queryRelationships(Collections.singletonMap("favoriteHydrant", 42);

relationships.close(); //You must close the stream when you are done with it
graph.shutdown(); //The ChampGraph is thread-safe, and only one needs to be created

```

### Creating partitions
#### Create a new partition

Champ partitions are subgraphs (i.e. a collection of objects and relationships)
** Note: We are still in the proces of creating a fluent API for partitions **


```
ChampPartition.create()
	      .withObject(
			ChampObject.create()
				   .ofType("dog")
				   .withoutKey()
				   .build()
	      )
	      .withObject(
			ChampObject.create()
				   .ofType("cat")
				   .withoutKey()
				   .build()
	      .withObject(
			ChampObject.create()
				   .ofType("bird")
				   .withoutKey()
				   .build()
	      )
	      .withRelationship(
			ChampRelationship.create()
						...
					 .build()
	      )
	      .build()
```

#### Persisting a partition

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
graph.storePartition(dogsOnMyBlock); //'dogsOnMyBlock' is a reference to a ChampPartition
graph.shutdown(); //The ChampGraph is thread-safe, and only one needs to be created
		//Once your application is finished using it, call shutdown()
		//to cleanup any loose ends
```

### Creating indices
#### Create an object index

```
ChampObjectIndex.create()
		.ofName("dogName")
		.onType("dog")
		.forField("name")
		.build()
```

#### Create a relationship index

```
ChampRelationshipIndex.create()
		      .ofName("eatsAtTime")
		      .onType("eats")
		      .forField("at")
		      .build()
```

#### Persisting an object index

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
graph.storeObjectIndex(dogName); //'dogName' is a reference to a ChampObjectIndex
graph.shutdown(); //The ChampGraph is thread-safe, and only one needs to be created
		//Once your application is finished using it, call shutdown()
		//to cleanup any loose ends
```

#### Persisting a relationship index

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
graph.storeRelationshipIndex(eatsAtTime); //'eatsAtTime' is a reference to a ChampObjectIndex
graph.shutdown(); //The ChampGraph is thread-safe, and only one needs to be created
		//Once your application is finished using it, call shutdown()
		//to cleanup any loose ends
```
#### Retrieving an object index

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
graph.retrieveObjectIndex("dogName");
graph.shutdown();

```

#### Retrieving a relationship index

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
graph.retrieveRelationshipIndex("eatsAtTime");
graph.shutdown();

```

### Creating schemas
#### Create a schema

The following schema restricts objects of type foo to have a required property "property1" as an Integer, and optional property "property2" as a String (Strings are the default type for object properties).  It also restricts relationships of type bar to only be allowed to originate from the object type foo.

```
ChampSchema.create()
	.withObjectConstraint()
		.onType("foo")
		.withPropertyConstraint()
			.onField("property1")
			.ofType(Integer.class)
			.required()
			.build()
		.withPropertyConstraint()
			.onField("property2")
			.optional()
			.build()
		.build()
	.withRelationshipConstraint()
		.onType("bar")
		.withPropertyConstraint()
			.onField("at")
			.ofType(String.class)
			.optional()
			.build()
		.withConnectionConstraint()
			.sourcedFrom("foo")
			.targetToAny()
			.build()
		.build()
	.build();
```

#### Persisting a schema

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");
graph.storeSchema(neighborhoodDogsSchema); //'neighborhoodDogsSchema' is a reference to a ChampObjectIndex
graph.updateSchema(neighborhoodDogConstraint); //'neighborhoosDogConstraint' is a reference to a ChampObjectConstraint
graph.updateSchema(eatsAtConstraint); //'eatsAtConstraint' is a reference to a ChampRelationshipIndex
graph.shutdown(); //The ChampGraph is thread-safe, and only one needs to be created
	//Once your application is finished using it, call shutdown()
	//to cleanup any loose ends
```

#### Retrieving a schema

```
final ChampGraph graph = ChampGraph.Factory.newInstance(graphType, "neighborhoodDogsGraph");

final ChampSchema schema = graph.retrieveSchema();

graph.shutdown();

```

### Champ Performance Testing

There is a jar-with-dependencies provided in maven that contains a performance test you can move around and get some idea of how well Champ is running on a cluster of your choice.  At the moment, the test only runs against a Titan implementation.

#### Example running an in-memory test

```

java -cp champ-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.openecomp.aai.champ.perf.ChampAPIPerformanceTest --champ.graph.type=IN_MEMORY


```

#### Example running a Titan test

Note that after the --champ.graph.type=TITAN parameter is provided, you may provide any configuration that is specified by Titan (see link above for the documentation)

```

java -cp champ-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.openecomp.aai.champ.perf.ChampAPIPerformanceTest --champ.graph.type=TITAN --storage.backend=cassandrathrift --storage.hostname=localhost


```
