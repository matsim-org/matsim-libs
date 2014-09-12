package playground.staheale.matsim2030;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class PopulationGenerator {

	private static Logger log = Logger.getLogger(PopulationGenerator.class);
	int idCounter = 1;	//new ids will start from this number
	int countUnknownId = 0;
	int countWeight = 0;
	int countWeight10 = 0;
	double totalWeight = 8738439;
	double totalWeight10 = 873928;
	Id idTemp = null;
	

	public PopulationGenerator() {
		super();		
	}

	public static void main(String[] args) throws Exception {
		PopulationGenerator populationGenerator = new PopulationGenerator();
		populationGenerator.run(args);
	}

	public void run(String[] args) throws Exception {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = sc.getPopulation();
		
		
		// ------------------- preparing output file for unknown ids ---------
		final String header="Plan_id";
		final BufferedWriter out =
				IOUtils.getBufferedWriter("./output/Unknown_ids.txt");
		out.write(header);
		out.newLine();
		

		// ------------------- read in population ----------------------------
		log.info("Reading plans...");	
		MatsimPopulationReader PlansReader = new MatsimPopulationReader(sc); 
		String filename = "./input/population.13.xml";
		if (args.length > 0)
			filename = args[0];
		PlansReader.readFile(filename);
		log.info("Reading plans...done.");
		log.info("population size is " +population.getPersons().size());

		// ------------------- read in weight csv file -----------------------
		log.info("Reading weight csv file...");		
		String pathname = "./input/mzp-cal.csv";
		if (args.length > 1)
			pathname = args[1];
		File file = new File(pathname);
		BufferedReader bufRdr = new BufferedReader(new FileReader(file));
		String curr_line = bufRdr.readLine();
		log.info("Start line iteration through weight csv file");

		PopulationWriter pw = new PopulationWriter(population, null);
		pw.writeStartPlans("./output/population.xml.gz");

//		for (Person p : population.getPersons().values()) {
//			pw.writePerson(p);
//		}
		while ((curr_line = bufRdr.readLine()) != null) {
			String[] entries = curr_line.split(",");
			String idCvs = entries[1].trim();
			String idShort = idCvs.substring(1, 8);
			Id<Person> id = Id.create(idShort, Person.class);
			String weight = entries[3].trim();
			String weight10 = entries[4].trim();
			int w = Integer.parseInt(weight);
			int w10 = Integer.parseInt(weight10);
			//log.info("loop will be repeated " +w10+ " times");
			//log.info("person id from cvs file is: " +id);
			//log.info("the corresponding person in the plans file is: " +population.getPersons().get(id));
			if (population.getPersons().get(id) == null) {	
				countWeight += w;
				countWeight10 += w10;
				if (idTemp != id) {
					//log.error("person with id = " +id+ " cannot be found in plans file");
					//System.exit(-1);
					countUnknownId += 1;
					out.write(id.toString());
					out.newLine();
				}
				idTemp = id;
			}
			else {
				for (int i = 0; i<w10; i++) {
					Person p = population.getPersons().get(id);
					//log.info("person p with old id is: " +p);
					String nId = String.valueOf(idCounter+i);
					Id<Person> newId = Id.create(nId, Person.class);
                    ((PersonImpl) p).setId(newId);
                    //log.info("person p with new id is: " +p);
					pw.writePerson(p);
                    ((PersonImpl) p).setId(id);
                    if ((i+1) == w10) {
						idCounter = (Integer.parseInt(nId)+1);
					}
				}
			}
		}
		bufRdr.close();
		out.flush();
		out.close();
		log.info("End line iteration");
		log.info(countUnknownId+ " IDs cannot be found in MZ2010 plans file");
		log.info("Weight not factored in = " +(double)Math.round((countWeight/totalWeight*100)*1000)/1000+ "%");
		log.info("Weight 10% scenario not factored in = " +(double)Math.round((countWeight10/totalWeight10*100)*1000)/1000+ "%");
		pw.writeEndPlans();
		log.info("Writing plans...done");	
	}
}
