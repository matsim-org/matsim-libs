package playground.mzilske.vis;

import java.io.File;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.scenario.ScenarioImpl;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class PopulationOnDisk implements Population {

	private Environment env;
	
	private static final String CLASS_CATALOG = "java_class_catalog";
	
	private static final String TIMESTEP_STORE = "timestep_store";
	
    private StoredClassCatalog javaCatalog;

    private Database timestepDb;

	private StoredSortedMap<Id, Person> personMap;

	private PopulationFactoryImpl factory;

	public PopulationOnDisk(ScenarioImpl sc, File tempFile) {

		factory = new PopulationFactoryImpl( sc );
		System.out.println("Opening environment in: " + tempFile);

		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setTransactional(false);
		envConfig.setLocking(false);
		envConfig.setAllowCreate(true);

		env = new Environment(tempFile, envConfig);

		DatabaseConfig dbConfig = new DatabaseConfig();
		dbConfig.setAllowCreate(true);
		Database catalogDb = env.openDatabase(null, CLASS_CATALOG, dbConfig);
		javaCatalog = new StoredClassCatalog(catalogDb);

		dbConfig.setSortedDuplicates(true);
		timestepDb = env.openDatabase(null, TIMESTEP_STORE, dbConfig); 
		
		EntryBinding<Id> timestepKeyBinding = new SerialBinding<Id>(javaCatalog, Id.class);
		EntryBinding<Person> timestepValueBindung = new SerialBinding<Person>(javaCatalog, Person.class);
		personMap = new StoredSortedMap<Id, Person>(timestepDb, timestepKeyBinding, timestepValueBindung, true);

	}

	@Override
	public void addPerson(Person p) {
		getPersons().put(p.getId(), p);
	}

	@Override
	public PopulationFactory getFactory() {
		return factory;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Map<Id, Person> getPersons() {
		return personMap;
	}

	@Override
	public void setName(String name) {
		
	}

}
