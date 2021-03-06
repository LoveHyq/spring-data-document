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
package org.springframework.data.document.mongodb.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class Update {

	public enum Position {
		LAST, FIRST
	}

	private HashMap<String, Object> criteria = new LinkedHashMap<String, Object>();

	/**
	 * Update using the $set update modifier
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Update set(String key, Object value) {
		criteria.put("$set", Collections.singletonMap(key, convertValueIfNecessary(value)));
		return this;
	}

	/**
	 * Update using the $unset update modifier
	 * 
	 * @param key
	 * @return
	 */
	public Update unset(String key) {
		criteria.put("$unset", Collections.singletonMap(key, 1));
		return this;
	}

	/**
	 * Update using the $inc update modifier
	 * 
	 * @param key
	 * @param inc
	 * @return
	 */
	public Update inc(String key, Number inc) {
		criteria.put("$inc", Collections.singletonMap(key, inc));
		return this;
	}

	/**
	 * Update using the $push update modifier
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Update push(String key, Object value) {
		criteria.put("$push", Collections.singletonMap(key, convertValueIfNecessary(value)));
		return this;
	}

	/**
	 * Update using the $pushAll update modifier
	 * 
	 * @param key
	 * @param values
	 * @return
	 */
	public Update pushAll(String key, Object[] values) {
		Object[] convertedValues = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			convertedValues[i] = convertValueIfNecessary(values[i]);
		}
		DBObject keyValue = new BasicDBObject();
		keyValue.put(key, convertedValues);
		criteria.put("$pushAll", keyValue);
		return this;
	}

	/**
	 * Update using the $addToSet update modifier
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Update addToSet(String key, Object value) {
		criteria.put("$addToSet", Collections.singletonMap(key, convertValueIfNecessary(value)));
		return this;
	}

	/**
	 * Update using the $pop update modifier
	 * 
	 * @param key
	 * @param pos
	 * @return
	 */
	public Update pop(String key, Position pos) {
		criteria.put("$pop", Collections.singletonMap(key, (pos == Position.FIRST ? -1 : 1)));
		return this;
	}

	/**
	 * Update using the $pull update modifier
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Update pull(String key, Object value) {
		criteria.put("$pull", Collections.singletonMap(key, convertValueIfNecessary(value)));
		return this;
	}

	/**
	 * Update using the $pullAll update modifier
	 * 
	 * @param key
	 * @param values
	 * @return
	 */
	public Update pullAll(String key, Object[] values) {
		Object[] convertedValues = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			convertedValues[i] = convertValueIfNecessary(values[i]);
		}
		DBObject keyValue = new BasicDBObject();
		keyValue.put(key, convertedValues);
		criteria.put("$pullAll", keyValue);
		return this;
	}

	/**
	 * Update using the $rename update modifier
	 * 
	 * @param oldName
	 * @param newName
	 * @return
	 */
	public Update rename(String oldName, String newName) {
		criteria.put("$rename", Collections.singletonMap(oldName, newName));
		return this;
	}

	public DBObject getUpdateObject() {
		DBObject dbo = new BasicDBObject();
		for (String k : criteria.keySet()) {
			dbo.put(k, criteria.get(k));
		}
		return dbo;
	}

	protected Object convertValueIfNecessary(Object value) {
		if (value instanceof Enum) {
			return ((Enum<?>)value).name();
		}
		return value;
	}

}
