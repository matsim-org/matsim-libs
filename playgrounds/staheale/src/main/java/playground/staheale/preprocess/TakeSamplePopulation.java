package playground.staheale.preprocess;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class TakeSamplePopulation {

	private static Logger log = Logger.getLogger(TakeSamplePopulation.class);
	private Random random = new Random(37835409);
	int sampleSize = 1000;
	int index = 0;
	List<Id> idList = new ArrayList<Id>();



	public static void main(String[] args) {
		TakeSamplePopulation takeSamplePopulation = new TakeSamplePopulation();
		takeSamplePopulation.run();
	}
	
	public void run() {
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = sc.getPopulation();

		//////////////////////////////////////////////////////////////////////
		// read in population

		log.info("Reading plans...");	
		MatsimPopulationReader PlansReader = new MatsimPopulationReader(sc); 
		PlansReader.readFile("./input/population2010.xml.gz");
		log.info("Reading plans...done.");
		log.info("Population size is " +population.getPersons().size());
		
		PopulationWriter pw = new PopulationWriter(population, null);
		pw.writeStartPlans("./output/population2010sample.xml.gz");
		
		for (Person p : population.getPersons().values()) {
			idList.add(p.getId());
		}
		log.info("idList size is " +idList.size());

		
		for (int i = 0; i < sampleSize; i++) {
			index = random.nextInt(idList.size());
			Person pers = population.getPersons().get(idList.get(index));
			pw.writePerson(pers);
		}
		pw.writeEndPlans();
		
	}

}
