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

import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.aai.champcore.model.fluent.KeyStep;
import org.onap.aai.champcore.model.fluent.relationship.SourceStep;

public final class ChampRelationshipKeyStepImpl implements KeyStep<SourceStep> {

	private final String type;
	private final ChampRelationship relationship;

	public ChampRelationshipKeyStepImpl(String type) {
		this.type = type;
		this.relationship = null;
	}

	public ChampRelationshipKeyStepImpl(ChampRelationship relationship) {
		this.type = null;
		this.relationship = relationship;
	}

	@Override
	public SourceStep withKey(Object key) {
		return new SourceStepImpl(type, relationship, key);
	}

	@Override
	public SourceStep withoutKey() {
		return new SourceStepImpl(type, relationship, null);
	}

}
