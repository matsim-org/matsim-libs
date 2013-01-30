package matsim2030;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

public class PopulationGenerator {

	private static Logger log = Logger.getLogger(PopulationGenerator.class);

	public PopulationGenerator() {
		super();		
	}

	public static void main(String[] args) throws Exception {
		PopulationGenerator populationGenerator = new PopulationGenerator();
		populationGenerator.run();
	}

	public void run() throws Exception {
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = sc.getPopulation();

		// ------------------- read in population ----------------------------
		log.info("Reading plans...");	
		MatsimPopulationReader PlansReader = new MatsimPopulationReader(sc); 
		PlansReader.readFile("./input/population.xml");
		log.info("Reading plans...done.");
		log.info("population size is " +population.getPersons().size());

		// ------------------- read in weight csv file -----------------------
		log.info("Reading weight csv file...");		
		File file = new File("./input/mzp-cal.csv");
		BufferedReader bufRdr = new BufferedReader(new FileReader(file));
		String curr_line = bufRdr.readLine();
		int seed = 10000000;	
		log.info("Start line iteration");


		PopulationWriter pw = new PopulationWriter(population, null);
		pw.writeStartPlans("population.xml.gz");

		for (Person p : population.getPersons().values()) {
			pw.writePerson(p);
		}

		while ((curr_line = bufRdr.readLine()) != null) {
			String[] entries = curr_line.split(",");
			String idCvs = entries[1].trim();
			String idShort = idCvs.substring(1, 8);
			Id id = sc.createId(idShort);
			//log.info("person id from cvs file is: " +id);
			//log.info("the corresponding person in the plans file is: " +population.getPersons().get(id));
			if (population.getPersons().get(id) == null) {
				log.error("person with id = " +id+ " cannot be found in plans file");
				System.exit(-1);
			}
			String weight = entries[3].trim();
			String weight10 = entries[4].trim();
			int w = Integer.parseInt(weight);
			int w10 = Integer.parseInt(weight10);
			//log.info("loop will be repeated " +w10+ " times");

			for (int i = 0; i<w10; i++) {
				Person p = population.getPersons().get(id);
				//log.info("person p with old id is: " +p);
				String nId = String.valueOf(seed+i);
				Id newId = sc.createId(nId);
				p.setId(newId);
				//log.info("person p with new id is: " +p);
				pw.writePerson(p);
				p.setId(id);
			}
			seed = seed + 10000;
		}
		bufRdr.close();	
		log.info("End line iteration");

		pw.writeEndPlans();
		log.info("Writing plans...done");	
	}

}
