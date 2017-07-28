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
import org.openecomp.aai.champ.model.fluent.relationship.TargetStep;
import org.openecomp.aai.champ.model.fluent.relationship.TargetTypeOrFromStep;

public final class TargetStepImpl implements TargetStep {

	private String relationshipType;
	private ChampRelationship relationship;
	private Object relationshipKey;
	private ChampObject source;

	public TargetStepImpl(String relationshipType, ChampRelationship relationship, Object relationshipKey,
			ChampObject source) {
		this.relationshipType = relationshipType;
		this.relationship = relationship;
		this.relationshipKey = relationshipKey;
		this.source = source;
	}

	@Override
	public TargetTypeOrFromStep withTarget() {
		return new TargetTypeOrFromStepImpl(relationshipType, relationship, relationshipKey, source);
	}

}
