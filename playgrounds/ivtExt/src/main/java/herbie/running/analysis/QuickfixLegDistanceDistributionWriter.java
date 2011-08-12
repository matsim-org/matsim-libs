package herbie.running.analysis;

import herbie.running.population.algorithms.AbstractClassifiedFrequencyAnalysis;
import herbie.running.population.algorithms.PopulationLegDistanceDistribution;
import herbie.running.population.algorithms.AbstractClassifiedFrequencyAnalysis.CrosstabFormat;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;

public class QuickfixLegDistanceDistributionWriter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final double[] distanceClasses = new double[]{
				0.0, 
				100, 200, 500,  
				1000, 2000, 50000, 
				10000, 20000, 50000,
				100000, 200000, 500000,
				1000000, Double.MAX_VALUE};

		
//		final double[] distanceClasses = new double[]{
//				0.0, 
//				1000, 2000, 3000, 4000, 5000, 
//				10000, 20000, 30000, 40000, 50000, 
//				100000, 200000, 300000, 400000, 500000,
//				1000000, Double.MAX_VALUE};

		final Logger log = Logger.getLogger(QuickfixLegDistanceDistributionWriter.class);

		String configFile = args[0] ;
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		
		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		pop.setIsStreaming(true);
		
		Network network = scenario.getNetwork();
		((NetworkImpl) network).getFactory().setRouteFactory("pt", new ExperimentalTransitRouteFactory());

		PrintStream out = null;
		try {
			out = new PrintStream("D:/Arbeit/Projekte/herbie/output/newDistanceDistribution.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		AbstractClassifiedFrequencyAnalysis algo = new PopulationLegDistanceDistribution(out, network);
		pop.addAlgorithm(algo);
		
		new MatsimPopulationReader(scenario).readFile(config.plans().getInputFile());

		log.info("Writing results file...");
		algo.printClasses(CrosstabFormat.ABSOLUTE, false, distanceClasses, out);
		algo.printDeciles(true, out);
		out.close();
		log.info("Writing results file...done.");




	}

}
