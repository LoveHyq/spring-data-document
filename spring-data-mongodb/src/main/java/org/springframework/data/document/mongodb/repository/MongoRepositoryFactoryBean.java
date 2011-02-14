/*
 * Copyright 2002-2010 the original author or authors.
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
import java.lang.reflect.Method;

import org.springframework.data.document.mongodb.MongoPropertyDescriptors.MongoPropertyDescriptor;
import org.springframework.data.document.mongodb.MongoTemplate;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.support.RepositoryFactorySupport;
import org.springframework.data.repository.support.RepositorySupport;
import org.springframework.data.repository.util.ClassUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * {@link org.springframework.beans.factory.FactoryBean} to create {@link MongoRepository} instances.
 * 
 * @author Oliver Gierke
 */
public class MongoRepositoryFactoryBean extends
        RepositoryFactoryBeanSupport<MongoRepository<?, ?>> {

    private MongoTemplate template;


    /**
     * Configures the {@link MongoTemplate} to be used.
     * 
     * @param template the template to set
     */
    public void setTemplate(MongoTemplate template) {

        this.template = template;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.RepositoryFactoryBeanSupport
     * #createRepositoryFactory()
     */
    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {

        return new MongoRepositoryFactory(template);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.RepositoryFactoryBeanSupport
     * #afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() {

        super.afterPropertiesSet();
        Assert.notNull(template, "MongoTemplate must not be null!");
    }

    /**
     * Repository to create {@link MongoRepository} instances.
     * 
     * @author Oliver Gierke
     */
    public static class MongoRepositoryFactory extends RepositoryFactorySupport {
    	
    	private static final boolean QUERY_DSL_PRESENT = org.springframework.util.ClassUtils.isPresent(
                "com.mysema.query.types.Predicate",
                MongoRepositoryFactory.class.getClassLoader());
    	
    	private final MongoTemplate template;
    	
    	/**
    	 * Creates a new {@link MongoRepositoryFactory} fwith the given {@link MongoTemplate}.
    	 * 
    	 * @param template
    	 */
		public MongoRepositoryFactory(MongoTemplate template) {
		
			this.template = template;
		}


		@Override
        protected <T, ID extends Serializable> RepositorySupport<T, ID> getTargetRepository(
                Class<T> domainClass, Class<?> repositoryInterface) {

			if (isQueryDslRepository(repositoryInterface)) {
				return new QueryDslMongoRepository<T, ID>(domainClass, template);
			} else {
				return new SimpleMongoRepository<T, ID>(domainClass, template);
			}
        }


        @Override
        @SuppressWarnings("rawtypes")
        protected Class<? extends RepositorySupport> getRepositoryClass(Class<?> repositoryInterface) {

            return isQueryDslRepository(repositoryInterface) ? QueryDslMongoRepository.class : SimpleMongoRepository.class;
        }
        
        
        private static boolean isQueryDslRepository(Class<?> repositoryInterface) {
        	return QUERY_DSL_PRESENT && QueryDslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
        }


        @Override
        protected QueryLookupStrategy getQueryLookupStrategy(Key key) {

            return new MongoQueryLookupStrategy();
        }

        /**
         * {@link QueryLookupStrategy} to create {@link MongoQuery} instances.
         * 
         * @author Oliver Gierke
         */
        private class MongoQueryLookupStrategy implements QueryLookupStrategy {

            public RepositoryQuery resolveQuery(Method method) {

                return new MongoQuery(new QueryMethod(method), template);
            }
        }
        
        /* (non-Javadoc)
         * @see org.springframework.data.repository.support.RepositoryFactorySupport#validate(java.lang.Class, java.lang.Object)
         */
        @Override
        protected void validate(Class<? extends Repository<?, ?>> repositoryInterface, Object customImplementation) {
        	
        	Class<?> idClass = ClassUtils.getIdClass(repositoryInterface);
        	if (!MongoPropertyDescriptor.SUPPORTED_ID_CLASSES.contains(idClass)) {
				throw new IllegalArgumentException(String.format("Unsupported id class! Only %s are supported!",
						StringUtils.collectionToCommaDelimitedString(MongoPropertyDescriptor.SUPPORTED_ID_CLASSES)));
        	}
        	
        	super.validate(repositoryInterface, customImplementation);
        }
    }
}
