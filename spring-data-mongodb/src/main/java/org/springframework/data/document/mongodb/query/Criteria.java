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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.data.document.InvalidDocumentStoreApiUsageException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class Criteria implements CriteriaDefinition {
	
	private String key;
	
	private LinkedHashMap<String, Object> criteria = new LinkedHashMap<String, Object>();

	private Object isValue = null;
	
	
	public Criteria(String key) {
		this.key = key;
	}


	/**
	 * Static factory method to create a Criteria using the provided key
	 * 
	 * @param key
	 * @return
	 */
	public static Criteria where(String key) {
		return new Criteria(key);
	}

	/**
	 * Creates a criterion using the $is operator
	 * 
	 * @param o
	 * @return
	 */
	public Criteria is(Object o) {
		if (isValue != null) {
			throw new InvalidDocumentStoreApiUsageException("Multiple 'is' values declared.");
		}
		this.isValue = o;
		return this;
	}

	/**
	 * Creates a criterion using the $lt operator
	 * 
	 * @param o
	 * @return
	 */
	public Criteria lt(Object o) {
		criteria.put("$lt", o);
		return this;
	}
	
	/**
	 * Creates a criterion using the $lte operator
	 * 
	 * @param o
	 * @return
	 */
	public Criteria lte(Object o) {
		criteria.put("$lte", o);
		return this;
	}
	
	/**
	 * Creates a criterion using the $gt operator
	 * 
	 * @param o
	 * @return
	 */
	public Criteria gt(Object o) {
		criteria.put("$gt", o);
		return this;
	}
	
	/**
	 * Creates a criterion using the $gte operator
	 * 
	 * @param o
	 * @return
	 */
	public Criteria gte(Object o) {
		criteria.put("$gte", o);
		return this;
	}
	
	/**
	 * Creates a criterion using the $in operator
	 * @param o
	 * @return
	 */
	public Criteria in(Object... o) {
		criteria.put("$in", o);
		return this;
	}

	/**
	 * Creates a criterion using the $nin operator
	 * 
	 * @param o
	 * @return
	 */
	public Criteria nin(Object... o) {
		criteria.put("$nin", o);
		return this;
	}

	/**
	 * Creates a criterion using the $mod operator
	 * 
	 * @param value
	 * @param remainder
	 * @return
	 */
	public Criteria mod(Number value, Number remainder) {
		List<Object> l = new ArrayList<Object>();
		l.add(value);
		l.add(remainder);
		criteria.put("$mod", l);
		return this;
	}

	/**
	 * Creates a criterion using the $all operator
	 * 
	 * @param o
	 * @return
	 */
	public Criteria all(Object o) {
		criteria.put("$is", o);
		return this;
	}

	/**
	 * Creates a criterion using the $size operator
	 * 
	 * @param s
	 * @return
	 */
	public Criteria size(int s) {
		criteria.put("$size", s);
		return this;
	}

	/**
	 * Creates a criterion using the $exists operator
	 * 
	 * @param b
	 * @return
	 */
	public Criteria exists(boolean b) {
		criteria.put("$exists", b);
		return this;
	}

	/**
	 * Creates a criterion using the $type operator
	 * 
	 * @param t
	 * @return
	 */
	public Criteria type(int t) {
		criteria.put("$type", t);
		return this;
	}

	/**
	 * Creates a criterion using the $not meta operator which affects the clause directly following
	 * 
	 * @return
	 */
	public Criteria not() {
		criteria.put("$not", null);
		return this;
	}
	
	/**
	 * Creates a criterion using a $regex
	 * 
	 * @param re
	 * @return
	 */
	public Criteria regex(String re) {
		criteria.put("$regex", re);
		return this;
	}

	/**
	 * Creates an or query using the $or operator for all of the provided queries
	 * 
	 * @param queries
	 */
	public void or(List<Query> queries) {
		criteria.put("$or", queries);		
	}
	
	public String getKey() {
		return this.key; 
	}

	/* (non-Javadoc)
	 * @see org.springframework.datastore.document.mongodb.query.Criteria#getCriteriaObject(java.lang.String)
	 */
	public DBObject getCriteriaObject() {
		DBObject dbo = new BasicDBObject();
		boolean not = false;
		for (String k : this.criteria.keySet()) {
			if (not) {
				DBObject notDbo = new BasicDBObject();
				notDbo.put(k, convertValueIfNecessary(this.criteria.get(k)));
				dbo.put("$not", notDbo);
				not = false;
			}
			else {
				if ("$not".equals(k)) {
					not = true;
				}
				else {
					dbo.put(k, convertValueIfNecessary(this.criteria.get(k)));
				}
			}
		}
		DBObject queryCriteria = new BasicDBObject();
		if (isValue != null) {
			queryCriteria.put(this.key, convertValueIfNecessary(this.isValue));
			queryCriteria.putAll(dbo);
		}
		else {
			queryCriteria.put(this.key, dbo);
		}
		return queryCriteria;
	}
	
	private Object convertValueIfNecessary(Object value) {
		if (value instanceof Enum) {
			return ((Enum<?>)value).name();
		}
		return value;
	}

}
