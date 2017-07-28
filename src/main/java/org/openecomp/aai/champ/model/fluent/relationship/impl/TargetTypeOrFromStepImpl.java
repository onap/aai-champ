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
package org.openecomp.aai.champ.model.fluent.relationship.impl;

import org.openecomp.aai.champ.model.ChampObject;
import org.openecomp.aai.champ.model.ChampRelationship;
import org.openecomp.aai.champ.model.fluent.relationship.TargetBuildOrPropertiesStep;
import org.openecomp.aai.champ.model.fluent.relationship.TargetKeyStep;
import org.openecomp.aai.champ.model.fluent.relationship.TargetTypeOrFromStep;

public final class TargetTypeOrFromStepImpl implements TargetTypeOrFromStep {

	private final String relationshipType;
	private final Object relationshipKey;
	private final ChampRelationship relationship;
	private final ChampObject source;

	public TargetTypeOrFromStepImpl(String relationshipType, ChampRelationship relationship, Object key, ChampObject source) {
		this.relationshipType = relationshipType;
		this.relationship = relationship;
		this.relationshipKey = key;
		this.source = source;
	}

	@Override
	public TargetKeyStep ofType(String type) {
		final ChampObject.Builder targetBuilder = new ChampObject.Builder(type);
		return new TargetKeyStepImpl(relationshipType, relationship, relationshipKey, source, targetBuilder);
	}
	
	@Override
	public TargetBuildOrPropertiesStep from(ChampObject object) {
		final ChampObject.Builder targetBuilder = new ChampObject.Builder(object);
		return new TargetBuildOrPropertiesStepImpl(relationshipType, relationship, relationshipKey, source, targetBuilder);
	}
}
