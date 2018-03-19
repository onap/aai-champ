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
package org.onap.aai.champcore.transform;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.onap.aai.champcore.exceptions.ChampUnmarshallingException;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.fluent.object.ObjectBuildOrPropertiesStep;
import org.onap.aai.champcore.model.fluent.relationship.RelationshipBuildOrPropertiesStep;

public final class TinkerpopChampformer implements Champformer<Vertex, Edge> {

	@Override
	public Vertex marshallObject(ChampObject object) throws ChampUnmarshallingException {
		throw new UnsupportedOperationException("Cannot marshall object to Tinkerpop Vertex without adding it to a graph");
	}

	@Override
	public Edge marshallRelationship(ChampRelationship relationship) throws ChampUnmarshallingException {
		throw new UnsupportedOperationException("Cannot marshall relationships to Tinkerpop Edge without adding it to a graph");
	}

	@Override
	public ChampObject unmarshallObject(Vertex vertex) throws ChampUnmarshallingException {
		final String type = vertex.label();
		final ObjectBuildOrPropertiesStep aaiObjBuilder = ChampObject.create()
															.ofType(type)
															.withKey(vertex.id());
		final Iterator<VertexProperty<Object>> properties = vertex.properties();

		while (properties.hasNext()) {
			final VertexProperty<Object> property = properties.next();

			if (ChampObject.ReservedPropertyKeys.contains(property.key()) ||
				ChampObject.IgnoreOnReadPropertyKeys.contains(property.key())) continue;
			
			aaiObjBuilder.withProperty(property.key(), property.value());
		}

		return aaiObjBuilder.build();
	}

	@Override
	public ChampRelationship unmarshallRelationship(Edge edge) throws ChampUnmarshallingException {
		final ChampObject source = unmarshallObject(edge.outVertex());
		final ChampObject target = unmarshallObject(edge.inVertex());
		final String type = edge.label();
		final RelationshipBuildOrPropertiesStep aaiRelBuilder = ChampRelationship.create()
																			.ofType(type)
																			.withKey(edge.id())
																			.withSource()
																				.from(source)
																				.build()
																			.withTarget()
																				.from(target)
																				.build();
		final Iterator<Property<Object>> properties = edge.properties();
		
		while (properties.hasNext()) {
			final Property<Object> property = properties.next();
			
			if (ChampRelationship.ReservedPropertyKeys.contains(property.key())) continue;

			aaiRelBuilder.withProperty(property.key(), property.value());
		}
		
		return aaiRelBuilder.build();
	}
}
