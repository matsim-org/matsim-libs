package playground.mzilske.vis;

import java.io.File;

import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

class MovieDatabase {
	
	private Environment env;
	
	private static final String CLASS_CATALOG = "java_class_catalog";
	
	private static final String TIMESTEP_STORE = "timestep_store";
	
    private StoredClassCatalog javaCatalog;

    private Database timestepDb;

	public MovieDatabase(File tempFile) throws DatabaseException {
    	System.out.println("Opening environment in: " + tempFile);

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);

        env = new Environment(tempFile, envConfig);
        
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);

        Database catalogDb = env.openDatabase(null, CLASS_CATALOG, dbConfig);
        
        javaCatalog = new StoredClassCatalog(catalogDb);
        
        timestepDb = env.openDatabase(null, TIMESTEP_STORE, dbConfig);
        
    }

    public void close() throws DatabaseException {
    	timestepDb.close();
    	javaCatalog.close();
    	env.close();
    }

	public Environment getEnv() {
		return env;
	}

	public StoredClassCatalog getJavaCatalog() {
		return javaCatalog;
	}

	public Database getTimestepDb() {
		return timestepDb;
	}

}
