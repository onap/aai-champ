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
package org.onap.aai.champcore.exceptions;

public final class ChampMarshallingException extends Exception {

	private static final long serialVersionUID = 7962962670920382670L;

	public ChampMarshallingException() {}
	
	public ChampMarshallingException(String message) {
		super(message);
	}
	
	public ChampMarshallingException(Throwable cause) {
		super(cause);
	}
	
	public ChampMarshallingException(String message, Throwable cause) {
		super(message, cause);
	}
}
