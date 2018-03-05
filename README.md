Champ
=====

What is Champ?
--------------

Champ is an abstraction from underlying graph storage systems that A&AI would otherwise interface with.

Building Champ
--------------

To build Champ run the following maven command from the project's root level pom directory:

    mvn clean install

Deploying The Microservice
--------------------------

Push the Docker image that you have built to your Docker repository and pull it down to the location from which you will be running the service.

**Create the following directories on the host machine:**

    ../logs
    ../appconfig
	../appconfig/auth
    ../dynamic/conf
    
You will be mounting these as data volumes when you start the Docker container.

#### Configuring the Microservice

Create beans file **../dynamic/conf/champ-beans.xml**

Example:

    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:util="http://www.springframework.org/schema/util"
        xsi:schemaLocation="
            http://www.springframework.org/schema/beans
            http://www.springframework.org/schema/beans/spring-beans.xsd
            http://www.springframework.org/schema/util
            http://www.springframework.org/schema/util/spring-util.xsd
            ">

        <util:constant id="DEFAULT_BATCH_SIZE" static-field="com.att.ecomp.event.client.DMaaPEventPublisher.DEFAULT_BATCH_SIZE" />
        <util:constant id="DEFAULT_BATCH_AGE" static-field="com.att.ecomp.event.client.DMaaPEventPublisher.DEFAULT_BATCH_AGE" />
        <util:constant id="DEFAULT_BATCH_DELAY" static-field="com.att.ecomp.event.client.DMaaPEventPublisher.DEFAULT_BATCH_DELAY" />

	
        <bean id="champEventPublisher" class="com.att.ecomp.event.client.DMaaPEventPublisher" >
            <constructor-arg name="host" value="<%= @CHAMP_EVBUS_HOSTS %>" />
            <constructor-arg name="topic" value="<%= @CHAMP_EVENT_TOPIC %>" />
            <constructor-arg name="username" value="<%= @CHAMP_DMAAP_API_KEY %>" />
            <constructor-arg name="password" value="<%= @CHAMP_DMAAP_API_SECRET %>" />
            <constructor-arg name="maxBatchSize" ref="DEFAULT_BATCH_SIZE" />
            <constructor-arg name="maxAgeMs" ref="DEFAULT_BATCH_AGE" />
            <constructor-arg name="delayBetweenBatchesMs" ref="DEFAULT_BATCH_DELAY" />
            <constructor-arg name="transportType" value="HTTPAUTH" />		
        </bean>
    
        <!-- Graph Implementation Configuration-->
        <util:map id="props" map-class="java.util.HashMap" key-type="java.lang.String" value-type="java.lang.Object">
            <entry key="champcore.event.stream.buffer.capacity" value="50" value-type="java.lang.Integer"/>
            <entry key="champcore.event.stream.publisher-pool-size" value="10" value-type="java.lang.Integer"/>
            <entry key="champcore.event.stream.publisher" value-ref="champEventPublisher"/>
    
            <entry key="graph.name" value="<%= @CHAMP_GRAPH_NAME %>"/>
            <entry key="storage.backend" value="<%= @CHAMP_STORAGE_BACKEND_DB %>"/>
            <entry key="storage.hostname" value="<%= @CHAMP_GRAPH_HOST %>"/>
        
            <!-- Hbase Config -->
            <entry key="storage.hbase.ext.hbase.zookeeper.property.clientPort" value="<%= @CHAMP_GRAPH_PORT %>"/>
            <entry key="storage.hbase.ext.zookeeper.znode.parent" value="/hbase-unsecure"/>
        
            <!-- Cassandra Config -->
            <entry key="storage.port" value="<%= @CHAMP_GRAPH_PORT %>"/>
        </util:map>
    
        <!-- Janus Implementation -->
        <bean id="graphBuilder" class="org.onap.aai.champjanus.graph.impl.JanusChampGraphImpl$Builder">
            <constructor-arg value="<%= @CHAMP_GRAPH_NAME %>"/>
            <constructor-arg ref="props" />
        </bean>
        <bean id="graphImpl" class="org.onap.aai.champjanus.graph.impl.JanusChampGraphImpl">
            <constructor-arg ref="graphBuilder" />
        </bean>
        <bean id="champRestService" class="org.onap.champ.ChampRESTAPI" >
            <constructor-arg ref="graphImpl" />
		    <constructor-arg name="txTimeOutInSec" value="300" />
        </bean>
    </beans>

Create configuration file **../appconfig/auth/champ-policy.json**

This policy file defines which client certificates are authorized to use the service's APIs.  An example policy file follows:

    {
        "roles": [
            {
                "name": "admin",
                "functions": [
                    {
                        "name": "search", "methods": [ { "name": "GET" },{ "name": "DELETE" }, { "name": "PUT" }, { "name": "POST" } ]
                    }
                ],
                "users": [
                    {
                        "username": "CN=admin, OU=My Organization Unit, O=, L=Sometown, ST=SomeProvince, C=CA"
                    }    
                ]
            }
        ]
    }

Create keystore file **../appconfig/auth/tomcat\_keystore**
_tomcat\_keystore_

Create a keystore with this name containing whatever CA certificates that you want your instance of the CHAMP service to accept for HTTPS traffic.

#### Start the service

You can now start the Docker container in the following manner:

	docker run -d \
	    -p 9520:9520 \
		-e CONFIG_HOME=/opt/app/champ-service/config/ \
		-e KEY_STORE_PASSWORD={{obfuscated password}} \
		-e KEY_MANAGER_PASSWORD=OBF:{{obfuscated password}} \
	    -v /<path>/logs:/opt/aai/logroot/AAI-CHAMP \
	    -v /<path>/appconfig:/opt/app/champ-service/config \
	    --name champ-service \
	    {{your docker repo}}/champ-service

Where,

    {{your docker repo}} = The Docker repository you have published your CHAMP Service image to.
    {{obfuscated password}} = The password for your key store/key manager after running it through the Jetty obfuscation tool.

API Specification
-----------------

Champ has methods for:

1) Objects
2) Relationships
3) Transactions

In the future we plan on adding in partitions, indicies, schemas, and traversals, but at the moment that is outside the scope of Champ.  If you have suggestions on how this should be implemented, we look forward to your pull request.

API Implementations
-------------------

Champ ships with both Titan and Janus implementations. These are switchable after deployment, but the champ-beans.xml file needs to be changed and the champ-service must be restarted.

Event Generation
----------------

Champ can be configured to generate a notification whenever it is used to modify data in the graph.  The notification comes in the form of an event which is posted to the **champRawEvents** Event Bus topic.  This event stream allows downstream clients to be notified about objects and relationships which are added/modified/deleted in the graph.

The following configuration parameters define the behaviour of the event generation feature:

- **champ.event.stream.publisher**: _EventClientPublisher_ instance to use for forwarding events to the event stream (see below).
- **champ.event.stream.publisher-pool-size**: Optional: number of worker threads to use for event publishing.
- **champ.event.stream.buffer.capacity**: Optional: maximum number of events which may be enqueued waiting to be published at any given time.
 

The following examples illustrate snippets of typical spring-beans configuration file which instantiate a producer (if your client is not spring enabled then you may just directly instantiate an _EventBusPublisher_ - refer to the _ECOMP Event Bus Client_ library for details):

_Instantiating an event producer backed by a native Kafka implementation:_

    <!-- Event publisher to pass to the Champ library for logging raw graph events. -->
    <bean id="champEventPublisher" class="com.att.ecomp.event.client.KafkaEventPublisher" >
        <constructor-arg name="hosts" value="1.2.3.4:9092, 5.6.7.8:9092, 9.10.11.12:9092" />
        <constructor-arg name="topic" value="champRawEvents" />
    </bean>

_Instantiating an event producer backed by a DMaaP Client implementation:_

    <bean id="champEventPublisher" class="com.att.ecomp.event.client.DMaaPEventPublisher" >
        <constructor-arg name="urlList" value="1.2.3.4, 5.6.7.8, 9.10.11.12" />
        <constructor-arg name="topic" value="champRawEvents" />
        <constructor-arg name="apiKey" value="OBF:1r2v1qc51i0r1l6n1m4q1jew1bpt1lkp1ll11bot1jee1m7o1l6n1i0z1qax1r53" />
        <constructor-arg name="apiSecret" value="OBF:1ro81caa1myq1mzs1nx31kfl1d2q1zsp1yt81nz31f8h1hmd1hmx1fa51nxb1yte1zt11d3g1kct1nzb1mxi1myk1cb01rqe" />
    </bean>

	

Usage
-----

### Echo

    URL: https://<host>:9522/services/echo-service/echo/<input>
    Method: GET
    Success Response: 200 OK


### Objects

#### Create a new object
Inserts a new object into the graph with the type and properties from the body of the request. Returns the object that was created, along with its assigned key and timestamps.

    URL: https://<host>:9522/services/champ-service/v1/objects
    Method: POST
    Body: 
         {
           "type" : "test",
           "properties" : {
             "key1" : "val1",
             "key2" : "val2"
           }
         }
         
    Success Response:
        Code: 201
        Content:
            {
                "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                "type": "test",
                "properties": {
                    "key1": "val1",
                    "key2": "val2",
                    "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                    "aai-created-ts": 1516731449014,
                    "aai-last-mod-ts": 1516731449014
                }
            }

#### Retrieve an object
A GET request that returns the object with the given key

    URL: https://<host>:9522/services/champ-service/v1/objects/<key>
    Method: GET
    Success Response:
        Code: 200 OK
        Content:
            {
                "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                "type": "test",
                "properties": {
                    "key1": "val1",
                    "key2": "val2",
                    "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                    "aai-created-ts": 1516731449014,
                    "aai-last-mod-ts": 1516731449014
                }
            }

#### Updating an object
Replace any of the properties with a PUT request, get the updated object back. Inclusion of timestamps is optional, but the request will be rejected if they do not match the DB.

    URL: https://<host>:9522/services/champ-service/v1/objects/<key>
    Method: PUT
    Content:
        {
            "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
            "type": "test",
            "properties": {
                "key1": "val3",
                "key2": "val2",
                "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                "key4": "val4",
                "aai-created-ts": 1516731449014
            }
        }
        
    Response:
        Code: 200 OK
        Content:
            {
                "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                "type": "test",
                "properties": {
                    "key1": "val3",
                    "key2": "val2",
                    "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                    "key4": "val4",
                    "aai-created-ts": 1516731449014,
                    "aai-last-mod-ts": 1516730919213
                }
            }

#### Delete objects
Deletes the object from the graph if there are no connected relationships

    URL: https://<host>:9522/services/champ-service/v1/objects/<key>
    Method: DELETE
    Success Response:
        Code: 200 OK
        

  
#### Filtered object search
Get a list of objects filtered by key/value pairs

    URL: https://<host>:9522/services/champ-service/v1/objects/filter?<key>=<val>
    Method: GET
    Success Response:
        [
            {
                "key": "e0d3a253-4a1b-4ca4-a862-8a52b1e3fdfb",
                "type": "test2",
                "properties": {}
            }
        ]

Get a list of objects filtered by key/value pairs with specific properties

    URL: https://<host>:9522/services/champ-service/v1/objects/filter?<key>=<val>&properties=<prop1>&properties=<prop2>
    Method: GET
    Success Response:
        [
            {
                "key": "e0d3a253-4a1b-4ca4-a862-8a52b1e3fdfb",
                "type": "test2",
                "properties": {
                    "key1": "val1",
                    "filter-sample": "yes"
                }
            }
        ] 

Get a list of objects filtered by key/value pairs with all properties

    URL: https://<host>:9522/services/champ-service/v1/objects/filter?<key>=<val>&properties=all
    Method: GET
    Success Response:
        [
            {
                "key": "e0d3a253-4a1b-4ca4-a862-8a52b1e3fdfb",
                "type": "test2",
                "properties": {
                    "key1": "val1",
                    "aai-uuid": "e0d3a253-4a1b-4ca4-a862-8a52b1e3fdfb",
                    "filter-sample": "yes"
                }
            }
        ]

### Relationships
Relationships are used to create a connection between two pre-existing objects.

#### Create a new relationship
Creates a new relationship with the specified properties. "source" and "target" must be objects that have already been created, specified by their keys. Returns the created relationship with its key and timestamps.

    URL: https://<host>:9522/services/champ-service/v1/relationships
    Method: POST
    Content:
        {
            "type": "testOnTest2",
            "properties": {
                "beep": "boop",
                "a": "b"
            },
            "source": {
                "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                "type": "test",
                "properties": {
                    "key1": "val3",
                    "key2": "val2",
                    "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                    "key4": "val4"
                }
            },
            "target": {
                "key": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                "type": "test2",
                "properties": {
                    "key1": "val1",
                    "key2": "val2",
                    "aai-uuid": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                    "key4": "val4"
                }
            }
        }
        
    Response:
        Code: 201 Created
        Content:
        {
            "key": "7a3282d0-6904-40f2-ae1e-8246bb1f49c1",
            "type": "testOnTest2",
            "properties": {
                "beep": "boop",
                "a": "b",
                "aai-uuid": "7a3282d0-6904-40f2-ae1e-8246bb1f49c1"
                "aai-last-mod-ts": 1516730919213,
                "aai-created-ts": 1516730919213
            },
            "source": {
                "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                "type": "test",
                "properties": {
                    "key1": "val3",
                    "key2": "val2",
                    "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                    "key4": "val4"
                }
            },
            "target": {
                "key": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                "type": "test2",
                "properties": {
                    "key1": "val1",
                    "key2": "val2",
                    "aai-uuid": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                    "key4": "val4"
                }
            }
        }
        
#### Retrieving relationships
Returns the relationship, looked up by key.

    URL: https://<host>:9522/services/champ-service/v1/relationships/<key>
    Method: GET
    Response:
        Code: 200 OK
        Content:
            {
                "key": "7a3282d0-6904-40f2-ae1e-8246bb1f49c1",
                "type": "testOnTest2",
                "properties": {
                    "beep": "boop",
                    "a": "b",
                    "aai-uuid": "7a3282d0-6904-40f2-ae1e-8246bb1f49c1"
                    "aai-last-mod-ts": 1516730919213,
                    "aai-created-ts": 1516730919213
                },
                "source": {
                    "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                    "type": "test",
                    "properties": {
                        "key1": "val3",
                        "key2": "val2",
                        "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                        "key4": "val4"
                    }
                },
                "target": {
                    "key": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                    "type": "test2",
                    "properties": {
                        "key1": "val1",
                        "key2": "val2",
                        "aai-uuid": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                        "key4": "val4"
                    }
                }
            }
            
            #### Get relationships for an object
            Given an object, returns all connected relationships.
            
                URL: https://<host>:9522/services/champ-service/v1/objects/relationships/<object-id>
                Method: GET
                Success Response:
                    Code: 200 OK
                    Content: 
                        [
                            {
                                "key": "4ba8dcc2-806d-4312-aecb-503435f355e5",
                                "type": "testOnTest2",
                                "properties": {
                                    "beep": "fdsa",
                                    "a": "c",
                                    "aai-uuid": "4ba8dcc2-806d-4312-aecb-503435f355e5"
                                },
                                "source": {
                                    "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                                    "type": "test",
                                    "properties": {
                                        "key1": "val3",
                                        "key2": "val2",
                                        "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                                        "key4": "val4"
                                    }
                                },
                                "target": {
                                    "key": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                                    "type": "test2",
                                    "properties": {
                                        "key1": "val1",
                                        "key2": "val2",
                                        "aai-uuid": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                                        "key4": "val4"
                                    }
                                }
                            },
                            {
                                "key": "a3096bb8-dc66-4a9c-ab33-a1183f784fbb",
                                "type": "testOnTest2",
                                "properties": {
                                    "beep": "fdsa",
                                    "a": "c",
                                    "aai-uuid": "a3096bb8-dc66-4a9c-ab33-a1183f784fbb"
                                },
                                "source": {
                                    "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                                    "type": "test",
                                    "properties": {
                                        "key1": "val3",
                                        "key2": "val2",
                                        "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                                        "key4": "val4"
                                    }
                                },
                                "target": {
                                    "key": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                                    "type": "test2",
                                    "properties": {
                                        "key1": "val1",
                                        "key2": "val2",
                                        "aai-uuid": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                                        "key4": "val4"
                                    }
                                }
                            }
                        ]

#### Updating relationships
Update the relationship properties. Passing timestamps is optional, but the request will be rejected if they are incorrect.

    URL: https://<host>:9522/services/champ-service/v1/relationships/<key>
    Method: PUT
    Content: 
        {
            "key": "7a3282d0-6904-40f2-ae1e-8246bb1f49c1",
            "type": "testOnTest2",
            "properties": {
                "beep": "borp",
                "a": "c",
                "aai-uuid": "7a3282d0-6904-40f2-ae1e-8246bb1f49c1"
            },
            "source": {
                "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                "type": "test",
                "properties": {
                    "key1": "val3",
                    "key2": "val2",
                    "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                    "key4": "val4"
                }
            },
            "target": {
                "key": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                "type": "test2",
                "properties": {
                    "key1": "val1",
                    "key2": "val2",
                    "aai-uuid": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                    "key4": "val4"
                }
            }
        }
        
    Response:
        Code: 200 OK
        Content:
            {
                "key": "7a3282d0-6904-40f2-ae1e-8246bb1f49c1",
                "type": "testOnTest2",
                "properties": {
                    "beep": "borp",
                    "a": "c",
                    "aai-uuid": "7a3282d0-6904-40f2-ae1e-8246bb1f49c1",
                    "aai-last-mod-ts": 1516734987294,
                    "aai-created-ts": 1516730919213
                },
                "source": {
                    "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                    "type": "test",
                    "properties": {
                        "key1": "val3",
                        "key2": "val2",
                        "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                        "key4": "val4"
                    }
                },
                "target": {
                    "key": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                    "type": "test2",
                    "properties": {
                        "key1": "val1",
                        "key2": "val2",
                        "aai-uuid": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                        "key4": "val4"
                    }
                }
            }
            
#### Deleting relationships
Deletes the relationship specified by key.

    URL: https://<host>:9522/services/champ-service/v1/relationships/<key>
    Method: Delete
    Response: 200 OK
    
#### Filtered Relationship
Returns a list of relationships that have key/value properties matching the filter

    URL: https://<host>:9522/services/champ-service/v1/objects/filter?<key>=<val>
    Method: GET
    Success Response:
        [
            {
                "key": "a4d51cd9-f271-4201-975d-168ec6bde501",
                "type": "testOnTest2",
                "properties": {
                    "beep": "yes",
                    "a": "c",
                    "aai-uuid": "a4d51cd9-f271-4201-975d-168ec6bde501"
                },
                "source": {
                    "key": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                    "type": "test",
                    "properties": {
                        "key1": "val3",
                        "key2": "val2",
                        "aai-uuid": "890c8b3f-892f-48e3-85cd-748ebf0426a5",
                        "key4": "val4"
                    }
                },
                "target": {
                    "key": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                    "type": "test2",
                    "properties": {
                        "key1": "val1",
                        "key2": "val2",
                        "aai-uuid": "559855df-62e2-4b06-a1ae-18e0d5ac9826",
                        "key4": "val4"
                    }
                }
            }
        ]

### Transactions
Transactions allow multiple graph operations to be grouped into a logical, sandboxed context, so that the option exists to either persist ALL of the grouped changes together, or NONE of them.

Explicit use of transactions is entirely optional for the client.  Calling the API methods described below without supplying a transaction
object results in a transaction being implicitly opened for the single operation, and then automatically committed.

However, all of the API calls described above related to persisting, retrieving, and deleting vertices, edges and graph partitions also 
expose a version of the call which optionally accepts a transaction id (acquired by explicitly calling the /transaction API endpoint).
In this case, the supplied transaction is used for the operation, and no automatic commit occurs.  It is the responsibility of
the client to explicitly commit or rollback the transaction at his or her discretion.

#### Open a new transaction
To use explicit transaction the client must request a transaction id from the Champ service by making a request to open a new transaction.

	URL: https://<host>:9522/services/champ-service/v1/transaction/
	Method: POST
	Response:
	    Code: 200 OK
	    Content: 5d90f5ae-1f1e-4c3e-b20b-2f7c45f822eb
	    
#### Working within a transaction
Operations can be done within a transaction by putting the transactionId in the query string.

    Query string: transactionId=<id>
    
Example object creation:

    URL: https://<host>:9522/services/champ-service/v1/objects?transactionId=5d90f5ae-1f1e-4c3e-b20b-2f7c45f822eb
    
Example relationship update:

    URL: https://<host>:9522/services/champ-service/v1/relationships/<key>?transactionId=5d90f5ae-1f1e-4c3e-b20b-2f7c45f822eb

#### Checking a transaction
If you wish to check the status of a transaction, you can do a get on it

    URL: https://<host>:9522/services/champ-service/v1/transaction/<transaction-id>
    Method: GET
If the transaction is currently open:

    Response:
        Code: 200 OK
        Content: "fa0890d9-6ac4-40aa-9838-745a25a61fa6 is OPEN"
If the transaction is not open:

    Response:
        Code: 404 Not Found
        Content: {}
    

#### Committing a transaction
Operations performed within the context of a transaction are not visible outside of that context 
until the client explicitly commits the transaction.  Up until that point, there is always the
option to just roll back the changes.

    URL: https://<host>:9522/services/champ-service/v1/transaction/<transaction-id>
    Method: PUT
    Content: {"method":"commit"}
    
    Response:
        Code: 200 OK
        Content: COMMITED
    
### Rolling back a transaction.
In the event that a sequence of graph operations which were performed within the same transactional context need to be aborted (for example one of the operations in the sequence encountered a failure of some kind) the entire transaction can be aborted by rolling back the transaction.  This effectively undoes all of the operations which were performed within the open transaction.

Note, once a transaction has been committed, it is no longer possible to rollback the contents of the transaction.
 
    URL: https://<host>:9522/services/champ-service/v1/transaction/<transaction-id>
        Method: PUT
        Content: {"method":"rollback"}
        
        Response:
            Code: 200 OK
            Content: ROLLED BACK
