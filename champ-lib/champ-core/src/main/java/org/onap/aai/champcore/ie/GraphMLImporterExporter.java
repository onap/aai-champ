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
package org.onap.aai.champcore.ie;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.onap.aai.champcore.ChampAPI;
import org.onap.aai.champcore.ChampGraph;
import org.onap.aai.champcore.ChampTransaction;
import org.onap.aai.champcore.exceptions.ChampMarshallingException;
import org.onap.aai.champcore.exceptions.ChampObjectNotExistsException;
import org.onap.aai.champcore.exceptions.ChampRelationshipNotExistsException;
import org.onap.aai.champcore.exceptions.ChampSchemaViolationException;
import org.onap.aai.champcore.exceptions.ChampTransactionException;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampObjectIndex;
import org.onap.aai.champcore.model.ChampRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GraphMLImporterExporter implements Importer, Exporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphMLImporterExporter.class);

	private static class GraphMLKey {
		private final String id;
		private final String attrName;
		private final String attrType;

		public GraphMLKey(String id, String attrName, Class<?> attrType) {
			this.id = id;
			this.attrName = attrName;

			if (attrType.equals(Boolean.class)) {
				this.attrType = "boolean";
			} else if (attrType.equals(Integer.class)) {
				this.attrType = "int";
			} else if (attrType.equals(Long.class)) {
				this.attrType = "long";
			} else if (attrType.equals(Float.class)) {
				this.attrType = "float";
			} else if (attrType.equals(Double.class)) {
				this.attrType = "double";
			} else if (attrType.equals(String.class)) {
				this.attrType = "string";
			} else {
				throw new RuntimeException("Cannot handle type " + attrType + " in GraphML");
			}
		}
	}

	public void importData(ChampAPI api, InputStream is) {

		try {
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); 
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final InputSource inputSource = new InputSource(is);
		    final Document doc = builder.parse(inputSource);

			final Map<String, Map<String, String>> nodePropertyDefinitions = new HashMap<String, Map<String, String>> ();
			final Map<String, Map<String, String>> edgePropertyDefinitions = new HashMap<String, Map<String, String>> ();
			final Set<Map<String, String>> nodeDefaults = new HashSet<Map<String, String>> ();
			final Set<Map<String, String>> edgeDefaults = new HashSet<Map<String, String>> ();

			final NodeList keys = doc.getElementsByTagName("key");

			for (int i = 0; i < keys.getLength(); i++) {
				final Node key = keys.item(i);
				final String id = key.getAttributes().getNamedItem("id").getNodeValue();
				final String attrName = key.getAttributes().getNamedItem("attr.name").getNodeValue();
				final String attrType = key.getAttributes().getNamedItem("attr.type").getNodeValue();
				final String elementType = key.getAttributes().getNamedItem("for").getNodeValue();
				final Map<String, String> propertyDefinitions = new HashMap<String, String> ();

				propertyDefinitions.put("attr.name", attrName);
				propertyDefinitions.put("attr.type",  attrType);

				final NodeList keyChildren = key.getChildNodes();

				for (int j = 0; j < keyChildren.getLength(); j++) {
					final Node keyChild = keyChildren.item(j);

					if (keyChild.getNodeType() != Node.ELEMENT_NODE) continue;

					if (keyChild.getNodeName().equals("default")) {
						propertyDefinitions.put("default", keyChild.getFirstChild().getNodeValue());

						if (elementType.equals("node")) nodeDefaults.add(propertyDefinitions);
						else if (elementType.equals("edge")) edgeDefaults.add(propertyDefinitions);
					}
				}

				if (elementType.equals("node")) {
					nodePropertyDefinitions.put(id, propertyDefinitions);
				} else if (elementType.equals("edge")) {
					edgePropertyDefinitions.put(id, propertyDefinitions);
				} else {
					LOGGER.warn("Unknown element type {}, skipping", elementType);
				}
			}

			final NodeList graphs = doc.getElementsByTagName("graph");

			for (int i = 0; i < graphs.getLength(); i++) {
				final Node graph = graphs.item(i);
				final String graphName = graph.getAttributes().getNamedItem("id").getNodeValue();
				final NodeList nodesAndEdges = graph.getChildNodes();

				api.getGraph(graphName).storeObjectIndex(ChampObjectIndex.create()
																			.ofName("importAssignedId")
																			.onAnyType()
																			.forField("importAssignedId")
																			.build());

				for (int j = 0; j < nodesAndEdges.getLength(); j++) {
					final Node nodeOrEdge = nodesAndEdges.item(j);

					if (nodeOrEdge.getNodeType() != Node.ELEMENT_NODE) continue;

					if (nodeOrEdge.getNodeName().equals("node")) {
						writeNode(api.getGraph(graphName), nodeOrEdge, nodePropertyDefinitions, nodeDefaults);
					} else if (nodeOrEdge.getNodeName().equals("edge")) {
						writeEdge(api.getGraph(graphName), nodeOrEdge, edgePropertyDefinitions, edgeDefaults);
					} else {
						LOGGER.warn("Unknown object {} found in graphML, skipping", nodeOrEdge.getNodeName());
					}
				}
			}
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Failed to setup DocumentBuilder", e);
		} catch (SAXException e) {
			throw new RuntimeException("Failed to parse input stream", e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse input stream", e);
		}
	}

	private void writeEdge(ChampGraph graph, Node edge, Map<String, Map<String, String>> edgePropertyDefinitions, Set<Map<String, String>> edgeDefaults) {
		final NamedNodeMap edgeAttributes = edge.getAttributes();
		final NodeList data = edge.getChildNodes();
		final Object sourceKey = edgeAttributes.getNamedItem("source").getNodeValue();
		final Object targetKey = edgeAttributes.getNamedItem("target").getNodeValue();
		ChampObject sourceObject=null;
		ChampObject targetObject=null;
		
		try {
			final Optional<ChampObject> source = graph.queryObjects(Collections.singletonMap("importAssignedId", sourceKey), Optional.empty()).findFirst();
			final Optional<ChampObject> target = graph.queryObjects(Collections.singletonMap("importAssignedId", targetKey), Optional.empty()).findFirst();

			if (!source.isPresent()) {
				sourceObject = graph.storeObject(ChampObject.create()
														.ofType("undefined")
														.withoutKey()
														.build(),
												 Optional.empty());
			} else sourceObject = source.get();
	
			if (!target.isPresent()) {
				targetObject = graph.storeObject(ChampObject.create()
														.ofType("undefined")
														.withoutKey()
														.build(),
												 Optional.empty());
			} else targetObject = target.get();

		} catch (ChampMarshallingException e) {
			LOGGER.error("Failed to marshall object to backend type, skipping this edge", e);
			return;
		} catch (ChampSchemaViolationException e) {
			LOGGER.error("Source/target object violates schema constraint(s)", e);
			return;
		} catch (ChampObjectNotExistsException e) {
			LOGGER.error("Failed to update existing source/target ChampObject", e);
			return;
		} catch (ChampTransactionException e) {
            LOGGER.error("Failed to commit or rollback transaction", e);
		}

		final ChampRelationship.Builder champRelBuilder = new ChampRelationship.Builder(sourceObject, targetObject, "undefined");

		for (Map<String, String> defaultProperty : edgeDefaults) {
			champRelBuilder.property(defaultProperty.get("attr.name"), defaultProperty.get("default"));
		}

		for (int k = 0; k < data.getLength(); k++) {
			final Node datum = data.item(k);

			if (datum.getNodeType() != Node.ELEMENT_NODE) continue;

			final String nodeProperty = datum.getAttributes().getNamedItem("key").getNodeValue();
			final Map<String, String> nodePropertyDefinition = edgePropertyDefinitions.get(nodeProperty);

			switch (nodePropertyDefinition.get("attr.type")) {
			case "boolean":
				champRelBuilder.property(nodePropertyDefinition.get("attr.name"), Boolean.valueOf(datum.getFirstChild().getNodeValue()));
				break;
			case "int":
				champRelBuilder.property(nodePropertyDefinition.get("attr.name"), Integer.valueOf(datum.getFirstChild().getNodeValue()));
				break;
			case "long":
				champRelBuilder.property(nodePropertyDefinition.get("attr.name"), Long.valueOf(datum.getFirstChild().getNodeValue()));
				break;
			case "float":
				champRelBuilder.property(nodePropertyDefinition.get("attr.name"), Float.valueOf(datum.getFirstChild().getNodeValue()));
				break;
			case "double":
				champRelBuilder.property(nodePropertyDefinition.get("attr.name"), Double.valueOf(datum.getFirstChild().getNodeValue()));
				break;
			case "string":
				champRelBuilder.property(nodePropertyDefinition.get("attr.name"), datum.getFirstChild().getNodeValue());
				break;
			default:
				throw new RuntimeException("Unknown node property attr.type " + nodePropertyDefinition.get("attr.type"));
			}
		}

		final ChampRelationship relToStore = champRelBuilder.build();

		try {
			graph.storeRelationship(relToStore, Optional.empty());
		} catch (ChampMarshallingException e) {
			LOGGER.warn("Failed to marshall ChampObject to backend type", e);
		} catch (ChampSchemaViolationException e) {
			LOGGER.error("Failed to store object (schema violated): " + relToStore, e);
		} catch (ChampRelationshipNotExistsException e) {
			LOGGER.error("Failed to update existing ChampRelationship", e);
		} catch (ChampObjectNotExistsException e) {
			LOGGER.error("Objects bound to relationship do not exist (should never happen)");
		} catch (ChampUnmarshallingException e) {
			LOGGER.error("Failed to unmarshall ChampObject to backend type");
		} catch (ChampTransactionException e) {
		    LOGGER.error("Failed to commit or rollback transaction");
		}
		
	}

	private void writeNode(ChampGraph graph, Node node, Map<String, Map<String, String>> nodePropertyDefinitions, Set<Map<String, String>> nodeDefaults) {
		final NamedNodeMap nodeAttributes = node.getAttributes();
		final Object importAssignedId = nodeAttributes.getNamedItem("id").getNodeValue();
		final NodeList data = node.getChildNodes();
		final Map<String, Object> properties = new HashMap<String, Object> ();

		for (int k = 0; k < data.getLength(); k++) {
			final Node datum = data.item(k);

			if (datum.getNodeType() != Node.ELEMENT_NODE) continue;

			final String nodeProperty = datum.getAttributes().getNamedItem("key").getNodeValue();
			final Map<String, String> nodePropertyDefinition = nodePropertyDefinitions.get(nodeProperty);

			switch (nodePropertyDefinition.get("attr.type")) {
			case "boolean":
				properties.put(nodePropertyDefinition.get("attr.name"), Boolean.valueOf(datum.getFirstChild().getNodeValue()));
				break;
			case "int":
				properties.put(nodePropertyDefinition.get("attr.name"), Integer.valueOf(datum.getFirstChild().getNodeValue()));
				break;
			case "long":
				properties.put(nodePropertyDefinition.get("attr.name"), Long.valueOf(datum.getFirstChild().getNodeValue()));
				break;
			case "float":
				properties.put(nodePropertyDefinition.get("attr.name"), Float.valueOf(datum.getFirstChild().getNodeValue()));
				break;
			case "double":
				properties.put(nodePropertyDefinition.get("attr.name"), Double.valueOf(datum.getFirstChild().getNodeValue()));
				break;
			case "string":
				properties.put(nodePropertyDefinition.get("attr.name"), datum.getFirstChild().getNodeValue());
				break;
			default:
				throw new RuntimeException("Unknown node property attr.type " + nodePropertyDefinition.get("attr.type"));
			}
		}

		if (!properties.containsKey("type")) throw new RuntimeException("No type provided for object (was this GraphML exported by Champ?)");

		final ChampObject.Builder champObjBuilder = new ChampObject.Builder((String) properties.get("type"));

		for (Map<String, String> defaultProperty : nodeDefaults) {
			champObjBuilder.property(defaultProperty.get("attr.name"), defaultProperty.get("default"));
		}

		properties.remove("type");

		champObjBuilder.properties(properties)
						.property("importAssignedId", importAssignedId);

		final ChampObject objectToStore = champObjBuilder.build();

		try {  
		  graph.storeObject(objectToStore, Optional.empty());
		} catch (ChampMarshallingException e) {
			LOGGER.warn("Failed to marshall ChampObject to backend type", e);
		} catch (ChampSchemaViolationException e) {
			LOGGER.error("Failed to store object (schema violated): " + objectToStore, e);
		} catch (ChampObjectNotExistsException e) {
			LOGGER.error("Failed to update existing ChampObject", e);
		} catch (ChampTransactionException e) {
          LOGGER.error("Failed to commit or rollback transaction");
        }
	}

	@Override
	public void exportData(ChampGraph graph, OutputStream os) {

		final XMLOutputFactory output = XMLOutputFactory.newInstance();

		try {
			final XMLStreamWriter writer = output.createXMLStreamWriter(os);

			writer.writeStartDocument();
			writer.writeStartElement("graphml");
			writer.writeDefaultNamespace("http://graphml.graphdrawing.org/xmlns");
			writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
			writer.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", "http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd");

			final List<ChampObject> nodes = new LinkedList<ChampObject> ();
			final List<ChampRelationship> edges = new LinkedList<ChampRelationship> ();
			final Map<String, GraphMLKey> nodeKeys = new HashMap<String, GraphMLKey> ();
			final Map<String, GraphMLKey> edgeKeys = new HashMap<String, GraphMLKey> ();
			final AtomicInteger elementCount = new AtomicInteger();

			graph.queryObjects(Collections.emptyMap(), Optional.empty()).forEach(object -> {
				nodes.add(object);

				for (Map.Entry<String, Object> property : object.getProperties().entrySet()) {
					if (nodeKeys.containsKey(property.getKey())) continue;

					nodeKeys.put(property.getKey(), new GraphMLKey("d" + elementCount.incrementAndGet(), property.getKey(), property.getValue().getClass()));
				}

				nodeKeys.put("type", new GraphMLKey("d" + elementCount.incrementAndGet(), "type", String.class));
			});

			graph.queryRelationships(Collections.emptyMap(), Optional.empty()).forEach(relationship -> {
				edges.add(relationship);

				for (Map.Entry<String, Object> property : relationship.getProperties().entrySet()) {
					if (nodeKeys.containsKey(property.getKey())) continue;

					edgeKeys.put(property.getKey(), new GraphMLKey("d" + elementCount.incrementAndGet(), property.getKey(), property.getValue().getClass()));
				}

				edgeKeys.put("type", new GraphMLKey("d" + elementCount.incrementAndGet(), "type", String.class));
			});

			for (Entry<String, GraphMLKey> nodeKey : nodeKeys.entrySet()) {
				final GraphMLKey graphMlKey = nodeKey.getValue();

				writer.writeStartElement("key");
				writer.writeAttribute("id", graphMlKey.id);
				writer.writeAttribute("for", "node");
				writer.writeAttribute("attr.name", graphMlKey.attrName);
				writer.writeAttribute("attr.type", graphMlKey.attrType);
				writer.writeEndElement();
			}

			for (Entry<String, GraphMLKey> edgeKey : edgeKeys.entrySet()) {
				final GraphMLKey graphMlKey = edgeKey.getValue();

				writer.writeStartElement("key");
				writer.writeAttribute("id", graphMlKey.id);
				writer.writeAttribute("for", "edge");
				writer.writeAttribute("attr.name", graphMlKey.attrName);
				writer.writeAttribute("attr.type", graphMlKey.attrType);
				writer.writeEndElement();
			}

			for (ChampObject object : nodes) {
				try {
					writer.writeStartElement("node");
					writer.writeAttribute("id", String.valueOf(object.getKey().get()));

					writer.writeStartElement("data");
					writer.writeAttribute("key", nodeKeys.get("type").id);
					writer.writeCharacters(object.getType());
					writer.writeEndElement();

					for (Entry<String, Object> property : object.getProperties().entrySet()) {
						final GraphMLKey key = nodeKeys.get(property.getKey());

						writer.writeStartElement("data");
						writer.writeAttribute("key", key.id);
						writer.writeCharacters(String.valueOf(property.getValue()));
						writer.writeEndElement();
					}

					writer.writeEndElement();
				} catch (XMLStreamException e) {
					throw new RuntimeException("Failed to write edge to output stream", e);
				}
			}

			for (ChampRelationship relationship : edges) {
				try {
					writer.writeStartElement("edge");
					writer.writeAttribute("id", String.valueOf(relationship.getKey().get()));

					writer.writeStartElement("data");
					writer.writeAttribute("key", edgeKeys.get("type").id);
					writer.writeCharacters(relationship.getType());
					writer.writeEndElement();

					for (Entry<String, Object> property : relationship.getProperties().entrySet()) {
						final GraphMLKey key = edgeKeys.get(property.getKey());

						writer.writeStartElement("data");
						writer.writeAttribute("key", key.id);
						writer.writeCharacters(String.valueOf(property.getValue()));
						writer.writeEndElement();
					}

					writer.writeEndElement();
				} catch (XMLStreamException e) {
					throw new RuntimeException("Failed to write edge to output stream", e);
				}
			}

			writer.writeEndElement();
			writer.writeEndDocument();
			writer.flush();
		} catch (XMLStreamException | ChampTransactionException e) {
			throw new RuntimeException(e);
		}
	}
}
