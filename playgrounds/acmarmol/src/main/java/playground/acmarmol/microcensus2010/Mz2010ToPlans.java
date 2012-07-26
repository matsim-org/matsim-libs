package playground.acmarmol.microcensus2010;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class Mz2010ToPlans {

	
	public static void createMZ2Plans(Config config) throws Exception {
		
	System.out.println("MATSim-DB: create Population based on micro census 2010 data.");

	
	System.out.println("  creating plans object... ");
	Population plans = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
	System.out.println("  done.");
		
	new HouseholdsFromMZ("input/Microcensus2010/haushalte.dat").run();
	
	
	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();

		Config config = ConfigUtils.loadConfig(args[0]);
		createMZ2Plans(config);

		Gbl.printElapsedTime();
	}
	
	
}
