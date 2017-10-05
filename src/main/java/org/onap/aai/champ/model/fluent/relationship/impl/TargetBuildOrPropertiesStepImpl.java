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
package org.onap.aai.champ.model.fluent.relationship.impl;

import org.onap.aai.champ.model.ChampObject;
import org.onap.aai.champ.model.ChampRelationship;
import org.onap.aai.champ.model.fluent.relationship.RelationshipBuildOrPropertiesStep;
import org.onap.aai.champ.model.fluent.relationship.TargetBuildOrPropertiesStep;

public final class TargetBuildOrPropertiesStepImpl implements TargetBuildOrPropertiesStep {

	private final String relationshipType;
	private final ChampRelationship relationship;
	private final Object relationshipKey;
	private final ChampObject source;
	private final ChampObject.Builder targetBuilder;

	public TargetBuildOrPropertiesStepImpl(String relationshipType, ChampRelationship relationship, Object relationshipKey,
			ChampObject source, ChampObject.Builder targetBuilder) {
		this.relationshipType = relationshipType;
		this.relationship = relationship;
		this.relationshipKey = relationshipKey;
		this.source = source;
		this.targetBuilder = targetBuilder;
	}

	@Override
	public TargetBuildOrPropertiesStep withProperty(String key, Object value) {
		targetBuilder.property(key, value);
		return this;
	}
	
	@Override
	public RelationshipBuildOrPropertiesStep build() {

		final ChampRelationship.Builder relationshipBuilder;

		if (relationship != null) relationshipBuilder = new ChampRelationship.Builder(relationship);
		else relationshipBuilder = new ChampRelationship.Builder(source, targetBuilder.build(), relationshipType);

		if (relationshipKey != null) relationshipBuilder.key(relationshipKey);
		
		return new RelationshipBuildOrPropertiesStepImpl(relationshipBuilder);
	}

}
