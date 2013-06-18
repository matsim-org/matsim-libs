package playground.wrashid.kti;

import java.util.TreeMap;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricing;

import playground.ivt.kticompatibility.KtiPtConfigGroup;
import playground.ivt.kticompatibility.KtiTripRouterFactory;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.controler.KTIControler;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.kti.controler.listeners.KtiPopulationPreparation;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.ScoreElements;
import playground.meisterk.kti.router.KtiLinkNetworkRouteFactory;
import playground.meisterk.kti.router.KtiPtRouteFactory;
import playground.meisterk.kti.router.KtiTravelCostCalculatorFactory;
import playground.meisterk.kti.router.PlansCalcRouteKti;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.scenario.KtiScenarioLoaderImpl;
import playground.meisterk.kti.scoring.KTIYear3ScoringFunctionFactory;
import playground.meisterk.org.matsim.config.PlanomatConfigGroup;

public class KTIScenarioWithoutRoadPricing {
	
	public static void main(final String[] args) {
		OutputDirectoryLogging.catchLogEntries();
		final String configFile = args[ 0 ];

		// read the config with our special parameters
		// Note that you need 
		final Config config = ConfigUtils.createConfig();
		config.addModule( new KtiPtConfigGroup() );
		ConfigUtils.loadConfig( config , configFile );

		// just make sure the scenario is loaded
		// Controler accepts a config, but if the Scenario is not
		// fully loaded when creating the routing module, we may get into
		// troubles later...
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		final Controler controler = new Controler( scenario );
		controler.setTripRouterFactory(
				new KtiTripRouterFactory(
					controler ) );

		// we're done!
		controler.run();
	}

}
