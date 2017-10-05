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
import org.onap.aai.champ.model.fluent.relationship.SourceBuildOrPropertiesStep;
import org.onap.aai.champ.model.fluent.relationship.SourceKeyStep;
import org.onap.aai.champ.model.fluent.relationship.SourceTypeOrFromStep;

public final class SourceTypeOrFromStepImpl implements SourceTypeOrFromStep {

	private final String relationshipType;
	private final Object relationshipKey;
	private final ChampRelationship relationship;

	public SourceTypeOrFromStepImpl(String relationshipType, ChampRelationship relationship, Object key) {
		this.relationshipType = relationshipType;
		this.relationship = relationship;
		this.relationshipKey = key;
	}

	@Override
	public SourceKeyStep ofType(String type) {
		final ChampObject.Builder sourceBuilder = new ChampObject.Builder(type);
		return new SourceKeyStepImpl(relationshipType, relationship, relationshipKey, sourceBuilder);
	}
	
	@Override
	public SourceBuildOrPropertiesStep from(ChampObject object) {
		final ChampObject.Builder sourceBuilder = new ChampObject.Builder(object);
		return new SourceBuildOrPropertiesStepImpl(relationshipType, relationship, relationshipKey, sourceBuilder);
	}
}
