package playground.vsp.andreas.utils.ana.filterActsPerShape;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class FilterActsPerShape {
	
	private static final Logger log = Logger.getLogger(FilterActsPerShape.class);
	private static final Level logLevel = Level.INFO;

	public static void run(String networkFile, String plansFile, String shapeFile, Coord minXY, Coord maxXY, String actTypeOne, String actTypeTwo, String filename) {
		
		log.setLevel(logLevel);
		
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		log.info("Reading network from " + networkFile);
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
		
		final Population plans = (Population) sc.getPopulation();
		StreamingDeprecated.setIsStreaming(plans, true);

		WorkHomeShapeCounter wHSC = new WorkHomeShapeCounter(minXY, maxXY, actTypeOne, actTypeTwo, shapeFile);
		final PersonAlgorithm algo = wHSC;
		StreamingDeprecated.addAlgorithm(plans, algo);
		
		MatsimReader plansReader = new PopulationReader(sc);		
		log.info("Reading plans file from " + plansFile);
		plansReader.readFile(plansFile);
		PopulationUtils.printPlansCount(plans) ;
		log.info(wHSC.toString());
		wHSC.toFile(filename);		
		log.info("Finished");
		
	}

}
