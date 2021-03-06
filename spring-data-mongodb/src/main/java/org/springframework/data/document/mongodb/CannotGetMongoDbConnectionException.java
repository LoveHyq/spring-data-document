/*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.document.mongodb;

import org.springframework.dao.DataAccessResourceFailureException;

public class CannotGetMongoDbConnectionException extends DataAccessResourceFailureException {

	private static final long serialVersionUID = 1172099106475265589L;

	public CannotGetMongoDbConnectionException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public CannotGetMongoDbConnectionException(String msg) {
		super(msg);
	}
}
