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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 */
package org.onap.champ.util.etag;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.onap.aai.champcore.model.ChampObject;
import org.onap.aai.champcore.model.ChampRelationship;
import org.onap.champ.util.HashGenerator;
/**
 * Computes etag for ChampObjects and ChampRelationships
 *
 */
public class EtagGenerator {

    private static final String AAI_LAST_MOD_TS = "aai-last-mod-ts";
    private final HashGenerator hashGenerator;

    public EtagGenerator() throws NoSuchAlgorithmException {
        this.hashGenerator = new HashGenerator();
    }

     /**
     * Takes in the ChampObject for which the hash is to be computed.
     * @param champObject
     * @return hash for the ChampObject
     * @throws IOException
     */
    public String computeHashForChampObject(ChampObject champObject) throws IOException {
        return hashGenerator.generateSHA256AsHex(champObject.getKey().orElse(""), champObject.getType(), filterAndSortProperties(champObject.getProperties()));
    }

    /**
     * Takes in the ChampRelationship for which the hash is to be computed.
     * @param ChampRelationship
     * @return hash for the ChampRelationship
     * @throws IOException
     */
    public String computeHashForChampRelationship(ChampRelationship champRelationship) throws IOException {
        return hashGenerator.generateSHA256AsHex(champRelationship.getKey().orElse(""), champRelationship.getType(), filterAndSortProperties(champRelationship.getProperties()), computeHashForChampObject(champRelationship.getSource()), computeHashForChampObject(champRelationship.getTarget()));
    }

    /**
     * Takes in the list of ChampObjects for which the hash is to be computed.<br>
     * Computes the individual hash, adds them to a List. <br>
     * Note that the order of items in the list affects the hash.
     * @param champObjects
     * @return hash for the list of ChampObjects
     * @throws IOException
     */
    public String computeHashForChampObjects(List<ChampObject> champObjects) throws IOException {
        List<String> champObjectHashList = new ArrayList<>();
        for(ChampObject champObject : champObjects) {
            champObjectHashList.add(computeHashForChampObject(champObject));
        }
        return hashGenerator.generateSHA256AsHex(champObjectHashList);
    }

    /**
     * Takes in the list of ChampRelationships for which the hash is to be computed.<br>
     * Computes the individual hash, adds them to a List. <br>
     * Note that the order of items in the list affects the hash.
     * @param champRelationships
     * @return hash for the list of ChampRelationships
     * @throws IOException
     */
    public String computeHashForChampRelationships(List<ChampRelationship> champRelationships) throws IOException {
        List<String> champRelationshipHashList = new ArrayList<>();
        for(ChampRelationship champRelationship : champRelationships) {
            champRelationshipHashList.add(computeHashForChampRelationship(champRelationship));
        }
        return hashGenerator.generateSHA256AsHex(champRelationshipHashList);
    }

    private Map<String, Object> filterAndSortProperties(Map<String, Object> properties) {
        return properties
                .entrySet()
                .stream()
                .filter(x -> !x.getKey().equals(AAI_LAST_MOD_TS))
                .sorted((x, y) -> x.getKey().compareTo(y.getKey()))
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        Map::putAll);
    }
}
