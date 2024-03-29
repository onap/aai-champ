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
package org.onap.champ.exception;

import javax.ws.rs.core.Response.Status;

public class ChampServiceException extends Exception {

  private static final long serialVersionUID = 8162385108397238865L;

  private Status httpStatus;

  public ChampServiceException() {
  }

  public ChampServiceException(String message, Status httpStatus) {
    super(message);
    this.setHttpStatus(httpStatus);
  }

  public ChampServiceException(Throwable cause) {
    super(cause);
  }

  public ChampServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public ChampServiceException(String message, Throwable cause, boolean enableSuppression,
                       boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public Status getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus(Status httpStatus) {
    this.httpStatus = httpStatus;
  }
}
