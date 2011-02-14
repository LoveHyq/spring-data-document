/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.document.mongodb.repository;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.apache.commons.collections15.Transformer;
import org.springframework.data.document.mongodb.MongoConverter;
import org.springframework.data.document.mongodb.MongoOperations;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.mongodb.DBObject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.MongodbSerializer;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.PathBuilder;

/**
 * Special QueryDsl based repository implementation that allows execution {@link Predicate}s in various forms.
 * 
 * TODO: Extract {@link EntityPathResolver} into Spring Data Commons TODO: Refactor Spring Data JPA to use this common
 * infrastructure
 * 
 * @author Oliver Gierke
 */
public class QueryDslMongoRepository<T, ID extends Serializable> extends SimpleMongoRepository<T, ID> implements
		QueryDslPredicateExecutor<T> {

	private final MongoConverterTransformer transformer;
	private final MongodbSerializer serializer;
	private final PathBuilder<T> builder;

	/**
	 * Creates a new {@link QueryDslMongoRepository} for the given domain class and {@link MongoTemplate}. Uses the
	 * {@link SimpleEntityPathResolver} to create an {@link EntityPath} for the given domain class.
	 * 
	 * @param domainClass
	 * @param template
	 */
	public QueryDslMongoRepository(Class<T> domainClass, MongoTemplate template) {

		this(domainClass, template, SimpleEntityPathResolver.INSTANCE);
	}

	/**
	 * Creates a new {@link QueryDslMongoRepository} for the given domain class, {@link MongoTemplate} and
	 * {@link EntityPathResolver}.
	 * 
	 * @param domainClass
	 * @param template
	 * @param resolver
	 */
	public QueryDslMongoRepository(Class<T> domainClass, MongoTemplate template, EntityPathResolver resolver) {

		this(resolver.createPath(domainClass), template);
	}

	/**
	 * Creates a new {@link QueryDslMongoRepository} for the given {@link EntityPath} and {@link MongoTemplate}.
	 * 
	 * @param path
	 * @param template
	 */
	@SuppressWarnings("unchecked")
	public QueryDslMongoRepository(EntityPath<T> path, MongoTemplate template) {

		super((Class<T>) path.getType(), template);
		this.transformer = new MongoConverterTransformer(template.getConverter());
		this.serializer = new MongodbSerializer();
		this.builder = new PathBuilder<T>(path.getType(), path.getMetadata());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.document.mongodb.repository.QueryDslExecutor
	 * #findOne(com.mysema.query.types.Predicate)
	 */
	public T findOne(Predicate predicate) {

		return createQueryFor(predicate).uniqueResult();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.document.mongodb.repository.QueryDslExecutor
	 * #findAll(com.mysema.query.types.Predicate)
	 */
	public List<T> findAll(Predicate predicate) {

		return createQueryFor(predicate).list();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.document.mongodb.repository.QueryDslExecutor
	 * #findAll(com.mysema.query.types.Predicate, com.mysema.query.types.OrderSpecifier<?>[])
	 */
	public List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {

		return createQueryFor(predicate).orderBy(orders).list();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.document.mongodb.repository.QueryDslExecutor
	 * #findAll(com.mysema.query.types.Predicate, org.springframework.data.domain.Pageable)
	 */
	public Page<T> findAll(Predicate predicate, Pageable pageable) {

		MongodbQuery<T> countQuery = createQueryFor(predicate);
		MongodbQuery<T> query = createQueryFor(predicate);

		return new PageImpl<T>(applyPagination(query, pageable).list(), pageable, countQuery.count());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.document.mongodb.repository.QueryDslExecutor
	 * #count(com.mysema.query.types.Predicate)
	 */
	public Long count(Predicate predicate) {

		return createQueryFor(predicate).count();
	}

	/**
	 * Creates a {@link MongodbQuery} for the given {@link Predicate}.
	 * 
	 * @param predicate
	 * @return
	 */
	private MongodbQuery<T> createQueryFor(Predicate predicate) {

		MongodbQuery<T> query = new MongoTemplateQuery(getMongoOperations());
		return query.where(predicate);
	}

	/**
	 * Applies the given {@link Pageable} to the given {@link MongodbQuery}.
	 * 
	 * @param query
	 * @param pageable
	 * @return
	 */
	private MongodbQuery<T> applyPagination(MongodbQuery<T> query, Pageable pageable) {

		if (pageable == null) {
			return query;
		}

		query = query.offset(pageable.getOffset()).limit(pageable.getPageSize());
		return applySorting(query, pageable.getSort());
	}

	/**
	 * Applies the given {@link Sort} to the given {@link MongodbQuery}.
	 * 
	 * @param query
	 * @param sort
	 * @return
	 */
	private MongodbQuery<T> applySorting(MongodbQuery<T> query, Sort sort) {

		if (sort == null) {
			return query;
		}

		for (Order order : sort) {
			query.orderBy(toOrder(order));
		}

		return query;
	}

	/**
	 * Transforms a plain {@link Order} into a QueryDsl specific {@link OrderSpecifier}.
	 * 
	 * @param order
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private OrderSpecifier<?> toOrder(Order order) {

		Expression<Object> property = builder.get(order.getProperty());

		return new OrderSpecifier(order.isAscending() ? com.mysema.query.types.Order.ASC
				: com.mysema.query.types.Order.DESC, property);
	}

	/**
	 * Special {@link MongodbQuery} implementation to use our {@link MongoOperations} for actually accessing Mongo.
	 * 
	 * @author Oliver Gierke
	 */
	private class MongoTemplateQuery extends MongodbQuery<T> {

		public MongoTemplateQuery(MongoOperations operations) {

			super(operations.getCollection(QueryUtils.getCollectionName(getDomainClass())), transformer, serializer);
		}
	}

	/**
	 * {@link Transformer} implementation to delegate to a {@link MongoConverter}.
	 *
	 * @author Oliver Gierke
	 */
	private class MongoConverterTransformer implements Transformer<DBObject, T> {

		private final MongoConverter converter;

		/**
		 * Creates a new {@link MongoConverterTransformer} with the given {@link MongoConverter}.
		 * 
		 * @param converter
		 */
		public MongoConverterTransformer(MongoConverter converter) {

			this.converter = converter;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.commons.collections15.Transformer#transform(java.lang. Object)
		 */
		public T transform(DBObject input) {

			return converter.read(getDomainClass(), input);
		}
	}

	/**
	 * Strategy interface to abstract the ways to translate an plain domain class into a {@link EntityPath}.
	 * 
	 * @author Oliver Gierke
	 */
	public static interface EntityPathResolver {

		<T> EntityPath<T> createPath(Class<T> domainClass);
	}

	/**
	 * Simple implementation of {@link EntityPathResolver} to lookup a query class by reflection and using the static
	 * field of the same type.
	 * 
	 * @author Oliver Gierke
	 */
	static enum SimpleEntityPathResolver implements EntityPathResolver {

		INSTANCE;

		private static final String NO_CLASS_FOUND_TEMPLATE = "Did not find a query class %s for domain class %s!";
		private static final String NO_FIELD_FOUND_TEMPLATE = "Did not find a static field of the same type in %s!";

		/**
		 * Creates an {@link EntityPath} instance for the given domain class. Tries to lookup a class matching the
		 * naming convention (prepend Q to the simple name of the class, same package) and find a static field of the
		 * same type in it.
		 * 
		 * @param domainClass
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public <T> EntityPath<T> createPath(Class<T> domainClass) {

			String pathClassName = getQueryClassName(domainClass);

			try {
				Class<?> pathClass = ClassUtils.forName(pathClassName, QueryDslMongoRepository.class.getClassLoader());
				Field field = getStaticFieldOfType(pathClass);

				if (field == null) {
					throw new IllegalStateException(String.format(NO_FIELD_FOUND_TEMPLATE, pathClass));
				} else {
					return (EntityPath<T>) ReflectionUtils.getField(field, null);
				}

			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(String.format(NO_CLASS_FOUND_TEMPLATE, pathClassName,
						domainClass.getName()), e);
			}
		}

		/**
		 * Returns the first static field of the given type inside the given type.
		 * 
		 * @param type
		 * @return
		 */
		private Field getStaticFieldOfType(Class<?> type) {

			for (Field field : type.getDeclaredFields()) {

				boolean isStatic = Modifier.isStatic(field.getModifiers());
				boolean hasSameType = type.equals(field.getType());

				if (isStatic && hasSameType) {
					return field;
				}
			}

			Class<?> superclass = type.getSuperclass();
			return Object.class.equals(superclass) ? null : getStaticFieldOfType(superclass);
		}

		/**
		 * Returns the name of the query class for the given domain class.
		 * 
		 * @param domainClass
		 * @return
		 */
		private String getQueryClassName(Class<?> domainClass) {

			String simpleClassName = ClassUtils.getShortName(domainClass);
			return String.format("%s.Q%s%s", domainClass.getPackage().getName(), getClassBase(simpleClassName),
					domainClass.getSimpleName());
		}

		/**
		 * Analyzes the short class name and potentially returns the outer class.
		 * 
		 * @param shortName
		 * @return
		 */
		private String getClassBase(String shortName) {

			String[] parts = shortName.split("\\.");

			if (parts.length < 2) {
				return "";
			}

			return parts[0] + "_";
		}
	}
}
