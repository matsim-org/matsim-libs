package playground.acmarmol.microcensus2010;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class MZ2010ToXmlFiles {

	
	public static void createXmls() throws Exception {
		
	System.out.println("MATSim-DB: creating xml files from MicroCensus 2010 database");
	
	System.out.println("  creating scenario object... ");
	Scenario scenario = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
	System.out.println("  done.");
	

	//household and vehicles xmls...
	new HouseholdsFromMZ("input/Microcensus2010/haushalte.dat", "input/Microcensus2010/haushaltspersonen.dat", "input/Microcensus2010/fahrzeuge.dat").run();
	
	//population xmls...
	new PopulationFromMZ("input/Microcensus2010/zielpersonen.dat","input/Microcensus2010/wege.dat").run(scenario.getPopulation());
	
	
	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();

		createXmls();

		Gbl.printElapsedTime();
	}
	
	
}
