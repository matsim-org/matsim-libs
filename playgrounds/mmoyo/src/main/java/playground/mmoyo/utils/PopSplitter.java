package playground.mmoyo.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**chops up a population file in a given number of smaller files*/
public class PopSplitter {
	private static final Logger log = Logger.getLogger(PopSplitter.class);
	
	public void run(Scenario sc, int fileNum){
		final String procesing = "processing ";
		final String writting = "writting until agent: ";
		final String xmlgz = ".xml.gz";
		Population inputPop = sc.getPopulation();
		
		int i=1;
		ScenarioImpl tempScenario =(ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationImpl newPopulation = new PopulationImpl(tempScenario);
		int size = inputPop.getPersons().size();
		for (Person person : inputPop.getPersons().values()) {
			log.info(procesing + i);
			newPopulation.addPerson(person);
			if (i%(size/fileNum)==0 || i==size ){
				PopulationWriter writer = new PopulationWriter(newPopulation, sc.getNetwork());
				String outFileName = sc.getConfig().controler().getOutputDirectory()  +  i + xmlgz; 
				writer.write(outFileName);
				new FileCompressor().run(outFileName);
				
				log.info(writting + i);
				
				tempScenario =(ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
				newPopulation = new PopulationImpl(tempScenario);
			}
			i++;
		}
	}

	public static void main(String[] args) {
		String configFile = null;
		String strFileNum = null;
		if (args.length==2){
			configFile = args[0];
			strFileNum = args[1];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			strFileNum = "7";
		}
		Scenario scenario = new DataLoader().loadScenario(configFile);
		int numFiles = Integer.valueOf (strFileNum);
		
		new PopSplitter().run(scenario, numFiles);
		System.out.println("done");
	}

}
