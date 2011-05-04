package tutorial;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;

public class PopulationAndDemandCreation {
	
	private Scenario scenario;
	
	private String censusFilePath = "";
	private String pusFilePath = "";
	private String facilitiesFilePath = "";
	private String networkFilePath = "";

	public static void main(String[] args) {
		PopulationAndDemandCreation creator = new PopulationAndDemandCreation();
		creator.run();		
	}
	
	private void run() {
		this.init();
		this.populationCreation();
		this.createPlansFromPUS();
		this.assignPUSPlansToMATSimPopulation();
		this.write();
	}
	
	private void init() {
		/*
		 * Read the network store it in the scenario
		 */
		new MatsimNetworkReader(scenario).readFile(networkFilePath);
		/*
		 * Read the facilities and store them in the scenario
		 */
		new FacilitiesReaderMatsimV1((ScenarioImpl)scenario).readFile(facilitiesFilePath);		
	}
	
	private void populationCreation() {
		/*
		 * Read the census file
		 */
		
		/*
		 * Create the persons and add the socio-demographics
		 */
		
	}
	
	private void createPlansFromPUS() {
		/*
		 * Read the PUS file
		 */
		
		/*
		 * Create the PUS population
		 */
	}
	
	private void assignPUSPlansToMATSimPopulation() {
		
	}
	
	
	private void write() {
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write("./output/population.xml");
	}
}
