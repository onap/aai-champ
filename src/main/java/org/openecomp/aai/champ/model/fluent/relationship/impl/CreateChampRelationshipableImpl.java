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

import org.openecomp.aai.champ.model.ChampRelationship;
import org.openecomp.aai.champ.model.fluent.KeyStep;
import org.openecomp.aai.champ.model.fluent.relationship.CreateChampRelationshipable;
import org.openecomp.aai.champ.model.fluent.relationship.RelationshipBuildOrPropertiesStep;
import org.openecomp.aai.champ.model.fluent.relationship.SourceStep;

public final class CreateChampRelationshipableImpl implements CreateChampRelationshipable {

	@Override
	public KeyStep<SourceStep> ofType(String type) {
		return new ChampRelationshipKeyStepImpl(type);
	}

	@Override
	public KeyStep<RelationshipBuildOrPropertiesStep> from(ChampRelationship relationship) {
		return new KeyStep<RelationshipBuildOrPropertiesStep> () {

			@Override
			public RelationshipBuildOrPropertiesStep withKey(Object key) {
				return new RelationshipBuildOrPropertiesStepImpl(new ChampRelationship.Builder(relationship).key(key));
			}

			@Override
			public RelationshipBuildOrPropertiesStep withoutKey() {
				return new RelationshipBuildOrPropertiesStepImpl(new ChampRelationship.Builder(relationship));
			}
		};
	}

}
