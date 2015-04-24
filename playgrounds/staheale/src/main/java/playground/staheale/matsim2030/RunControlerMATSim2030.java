package playground.staheale.matsim2030;


import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.DefaultDelegateFactory;
import org.matsim.contrib.multimodal.router.TransitTripRouterFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouterConfig;

import playground.ivt.utils.TripModeShares;


public class RunControlerMATSim2030 extends Controler {
	
	protected static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "multimodalLegDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
	private DestinationChoiceBestResponseContext lcContext;

	
	public RunControlerMATSim2030(final String[] args) {
		super(args);	
	}

	public RunControlerMATSim2030(Scenario scenario) {
		super( scenario );
	}

	public static void main(String[] args) {
		RunControlerMATSim2030 controler =
				new RunControlerMATSim2030(
						ScenarioUtils.loadScenario(
								ConfigUtils.loadConfig(
										args[ 0 ] ) ) );
		controler.setOverwriteFiles(true);
        controler.getConfig().controler().setCreateGraphs(true);
        controler.init();
		controler.run();
	}
	
	private void init() {

        ActivityFacilities facilities = getScenario().getActivityFacilities();
		//log.warn("number of facilities: " +facilities.getFacilities().size());
        NetworkImpl network = (NetworkImpl) getScenario().getNetwork();
		//log.warn("number of links: " +network.getLinks().size());

		WorldConnectLocations wcl = new WorldConnectLocations(this.getConfig());
		wcl.connectFacilitiesWithLinks(facilities, network);
		
		
		/*
		 * would be muuuuch nicer to have this in DestinationChoiceInitializer, but startupListeners are called after corelisteners are called
		 * -> scoringFunctionFactory cannot be replaced
		 */
		this.lcContext = new DestinationChoiceBestResponseContext(super.getScenario());	
		/* 
		 * add ScoringFunctionFactory to controler
		 *  in this way scoringFunction does not need to create new, identical k-vals by itself    
		 */
  		DCScoringFunctionFactory dcScoringFunctionFactory = new DCScoringFunctionFactory(this.getScenario(), this.lcContext);
		super.setScoringFunctionFactory(dcScoringFunctionFactory);
		dcScoringFunctionFactory.setUsingConfigParamsForScoring(false);		
		
		TransitRouterConfig conf = new TransitRouterConfig(super.getConfig());
		NewTransitRouterImplFactory factory = new NewTransitRouterImplFactory(super.getScenario().getTransitSchedule(), conf, this.getConfig());
		
		
		
		
		/*
		 * Cannot get the factory from the controler, therefore create a new one as the controler does. 
		 */
		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = builder.createDefaultLeastCostPathCalculatorFactory(this.getScenario());
		
		/*
		 * Use a ...
		 * - defaultDelegateFactory for the QNetsim modes
		 * - transitTripRouterFactory for transit trips
		 * 
		 * Note that a FastDijkstraFactory is used for the multiModalTripRouterFactory
		 * since ...
		 * - only "fast" router implementations handle sub-networks correct
		 * - AStarLandmarks uses link speed information instead of agent speeds
		 */
		MultiModalConfigGroup mmcg = new MultiModalConfigGroup();
		mmcg.setSimulatedModes("");
		getConfig().addModule(mmcg);
		TripRouterFactory defaultDelegateFactory = new DefaultDelegateFactory(this.getScenario(), leastCostPathCalculatorFactory);
		TripRouterFactory transitTripRouterFactory = new TransitTripRouterFactory(this.getScenario(), defaultDelegateFactory, 
				factory);
		this.setTripRouterFactory(transitTripRouterFactory);
		
	}
	
	protected void loadControlerListeners() {
		this.lcContext.init(); // this is an ugly hack, but I somehow need to get the scoring function + context into the controler
		
		this.addControlerListener(new DestinationChoiceInitializer(this.lcContext));
		
		this.addControlerListener(new CalcLegTimesHerbieListener(CALC_LEG_TIMES_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME, this.getScenario().getNetwork()));
		this.addControlerListener(new TripModeShares(1, this.getControlerIO(), this.getScenario(),
				new MainModeIdentifierImpl(), new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE )));
		super.loadControlerListeners();
	}
}

