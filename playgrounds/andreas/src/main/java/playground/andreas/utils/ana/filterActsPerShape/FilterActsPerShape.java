package playground.andreas.utils.ana.filterActsPerShape;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class FilterActsPerShape {
	
	private static final Logger log = Logger.getLogger(FilterActsPerShape.class);
	private static final Level logLevel = Level.INFO;

	public static void run(String networkFile, String plansFile, String shapeFile, Coord minXY, Coord maxXY, String actTypeOne, String actTypeTwo, String filename) {
		
		log.setLevel(logLevel);
		
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		log.info("Reading network from " + networkFile);
		new MatsimNetworkReader(sc).readFile(networkFile);
		
		final PopulationImpl plans = (PopulationImpl) sc.getPopulation();
		plans.setIsStreaming(true);

		WorkHomeShapeCounter wHSC = new WorkHomeShapeCounter(minXY, maxXY, actTypeOne, actTypeTwo, shapeFile);
		plans.addAlgorithm(wHSC);
		
		PopulationReader plansReader = new MatsimPopulationReader(sc);		
		log.info("Reading plans file from " + plansFile);
		plansReader.readFile(plansFile);
		plans.printPlansCount();
		log.info(wHSC.toString());
		wHSC.toFile(filename);		
		log.info("Finished");
		
	}

}
