package playground.staheale.matsim2030;


import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;

public class RunControlerMATSim2030 extends Controler {
	
	protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
	private DestinationChoiceBestResponseContext lcContext;

	
	public RunControlerMATSim2030(final String[] args) {
		super(args);	
	}

	public static void main(String[] args) {
		//String configFile = args[0];
		//Config config = ConfigUtils.loadConfig(configFile);
		//Controler controler = new Controler(config);
		RunControlerMATSim2030 controler = new RunControlerMATSim2030(args);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(true);
		controler.init();
		controler.run();
	}
	
	private void init() {
		/*
		 * would be muuuuch nicer to have this in DestinationChoiceInitializer, but startupListeners are called after corelisteners are called
		 * -> scoringFunctionFactory cannot be replaced
		 */
		this.lcContext = new DestinationChoiceBestResponseContext(super.getScenario());	
		/* 
		 * add ScoringFunctionFactory to controler
		 *  in this way scoringFunction does not need to create new, identical k-vals by itself    
		 */
  		DCScoringFunctionFactory dcScoringFunctionFactory = new DCScoringFunctionFactory(this.getConfig(), this, this.lcContext); 	
		super.setScoringFunctionFactory(dcScoringFunctionFactory);
		// dcScoringFunctionFactory.setUsingFacilityOpeningTimes(false); // TODO: make this configurable
	}
	
	protected void loadControlerListeners() {
		this.lcContext.init(); // this is an ugly hack, but I somehow need to get the scoring function + context into the controler
		
		this.addControlerListener(new DestinationChoiceInitializer(this.lcContext));
		
		this.addControlerListener(new CalcLegTimesKTIListener("calcLegTimesKTI.txt", "legTravelTimeDistribution.txt"));
		this.addControlerListener(new LegDistanceDistributionWriter("legDistanceDistribution.txt"));
		super.loadControlerListeners();
	}
}

