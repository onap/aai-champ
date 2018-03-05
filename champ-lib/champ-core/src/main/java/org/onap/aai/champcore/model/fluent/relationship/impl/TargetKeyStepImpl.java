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
package org.onap.aai.champcore.model.fluent.relationship.impl;

import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.fluent.relationship.TargetBuildOrPropertiesStep;
import org.onap.aai.champcore.model.fluent.relationship.TargetKeyStep;

public final class TargetKeyStepImpl implements TargetKeyStep {

	private final String relationshipType;
	private final Object relationshipKey;
	private final ChampRelationship relationship;
	private final ChampObject source;
	private final ChampObject.Builder targetBuilder;

	public TargetKeyStepImpl(String relationshipType, ChampRelationship relationship, Object key, ChampObject source, ChampObject.Builder targetBuilder) {
		this.relationshipType = relationshipType;
		this.relationship = relationship;
		this.relationshipKey = key;
		this.source = source;
		this.targetBuilder = targetBuilder;
	}

	@Override
	public TargetBuildOrPropertiesStep withKey(Object key) {
		targetBuilder.key(key);
		return new TargetBuildOrPropertiesStepImpl(relationshipType, relationship, relationshipKey, source, targetBuilder);
	}
	
	@Override
	public TargetBuildOrPropertiesStep withoutKey() {
		return new TargetBuildOrPropertiesStepImpl(relationshipType, relationship, relationshipKey, source, targetBuilder);
	}

}
