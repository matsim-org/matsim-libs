package playground.christoph.population;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;

public class AddFacilitities2Population {

	private static final Logger log = Logger.getLogger(AddFacilitities2Population.class);
	
	private String populationFile = "../../matsim/mysimulations/crossboarder/plansCB_updated.xml.gz";
	private String networkFile = "../../matsim/mysimulations/crossboarder/network.xml.gz";
	private String facilitiesFile = "../../matsim/mysimulations/crossboarder/facilities.xml.gz";
	private String outFile = "../../matsim/mysimulations/crossboarder/plansCB_updated_with_facilities.xml.gz";
		
	public static void main(String[] args) {
		try {
			new AddFacilitities2Population(new ScenarioImpl());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public AddFacilitities2Population(Scenario scenario) throws IOException {
		log.info("Read Network File...");
		new MatsimNetworkReader(scenario).readFile(networkFile);
		log.info("done.");
		
		log.info("Reading facilities file...");
		new MatsimFacilitiesReader((ScenarioImpl)scenario).readFile(facilitiesFile);
		log.info("done.");
		
		log.info("Setting up plans objects...");
		PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		PopulationWriter plansWriter = new PopulationWriter(plans, scenario.getNetwork(), ((ScenarioImpl)scenario).getKnowledges());
		plansWriter.startStreaming(outFile);
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		log.info("done.");
		
		log.info("Adding Facilities Selector...");
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new PersonSetFacilities(((ScenarioImpl)scenario).getActivityFacilities()));
		log.info("done.");
		
		log.info("Adding Knowledges setter...");
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new PersonSetKnowledges(scenario));
		log.info("done.");
		
		log.info("Reading, processing, writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(populationFile);
		plans.printPlansCount();
		plansWriter.closeStreaming();
		log.info("done.");
	}
}
