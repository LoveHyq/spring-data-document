package org.springframework.persistence.document.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.persistence.support.ChangeSet;
import org.springframework.persistence.support.ChangeSetBacked;
import org.springframework.persistence.support.ChangeSetConfiguration;
import org.springframework.persistence.support.ChangeSetPersister;
import org.springframework.persistence.support.ChangeSetSynchronizer;
import org.springframework.persistence.support.HashMapChangeSet;
import org.springframework.persistence.support.ChangeSetPersister.NotFoundException;

/**
 * EXAMPLE OF CODE THAT SHOULD BE GENERATED BY ROO BESIDES EACH MONGOENTITY CLASS 
 * 
 * Note: Combines X_Roo_Entity with X_Roo_Finder, as
 * we need only a single aspect for entities.
 * 
 * @author Thomas Risberg
 *
 */
privileged aspect MongoPerson_Roo_Mongo_Entity {
	
	private static ChangeSetPersister<Object> changeSetPersister() {
		return new MongoConfigurationHolder().changeSetConfig.getChangeSetPersister();
	}
	
	private static ChangeSetSynchronizer<ChangeSetBacked> changeSetManager() {
		return new MongoConfigurationHolder().changeSetConfig.getChangeSetManager();
	}

	@Configurable
	public static class MongoConfigurationHolder {
		@Autowired
		@Qualifier("mongoChangeSetConfiguration")
		public ChangeSetConfiguration<Object> changeSetConfig;
	}

	/**
	 * Add constructor that takes ChangeSet.
	 * @param ChangeSet
	 */
	public MongoPerson.new(ChangeSet cs) {
		super();
		setChangeSet(cs);
	}

	public static MongoPerson MongoPerson.findPerson(Object id) {
		ChangeSet rv = new HashMapChangeSet();
		try {
			changeSetPersister().getPersistentState(MongoPerson.class, id, rv);
			return new MongoPerson(rv);
		}
		catch (NotFoundException ex) {
			return null;
		}
	}

}
