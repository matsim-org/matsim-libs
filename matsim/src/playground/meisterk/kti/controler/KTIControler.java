package playground.meisterk.kti.controler;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.xml.sax.SAXException;

import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.kti.controler.listeners.KtiPopulationPreparation;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.ScoreElements;
import playground.meisterk.kti.router.KtiNodeNetworkRouteFactory;
import playground.meisterk.kti.router.KtiPtRoute;
import playground.meisterk.kti.router.KtiPtRouteFactory;
import playground.meisterk.kti.router.PlansCalcRouteKti;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.scoring.KTIYear3ScoringFunctionFactory;

/**
 * A special controler for the KTI-Project.
 * 
 * @author meisterk
 * @author mrieser
 * @author wrashid
 *
 */
public class KTIControler extends Controler {

	protected static final String SVN_INFO_FILE_NAME = "svninfo.txt";
	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
	
	private PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo();

	private final KtiConfigGroup ktiConfigGroup;

	private static final Logger logger = Logger.getLogger(KTIControler.class);

	public KTIControler(String[] args) {
		super(args);

		this.ktiConfigGroup = new KtiConfigGroup();
		super.config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);

		String tempConfigFilename = args[0];
		this.initPlansCalcRouteKtiInfo(tempConfigFilename);
		
		this.getNetworkFactory().setRouteFactory(TransportMode.car, new KtiNodeNetworkRouteFactory());
		this.getNetworkFactory().setRouteFactory(TransportMode.pt, new KtiPtRouteFactory(this.plansCalcRouteKtiInfo));

	}
	
	/**
	 * workaround in order to have {@link PlansCalcRouteKtiInfo} data available when loading a population which contains {@link KtiPtRoute}s
	 */
	private void initPlansCalcRouteKtiInfo(String configFilename) {
		// workaround in order to have PlansCalcRouteKtiInfo data available
		// when loading a population which contains kti pt routes
		logger.info("Loading temporary config in order to get network filename...");
		Config tempConfig = new Config();
		tempConfig.addCoreModules();
		tempConfig.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
		if (configFilename != null) {
			new MatsimConfigReader(this.config).readFile(configFilename);
		}
		logger.info("Loading temporary config in order to get network filename...done.");
		logger.info("Checking consistency of temporary config...");
		this.config.checkConsistency();
		logger.info("Checking consistency of temporary config...done.");
		
		logger.info("Loading temporary network...");
		String networkFileName = this.config.network().getInputFile();
		NetworkLayer tempNetwork = new NetworkLayer();
		try {
			new MatsimNetworkReader(tempNetwork).parse(networkFileName);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		logger.info("Loading temporary network...done.");
		
		logger.info("Loading PlansCalcRouteKtiInfo...");
		this.plansCalcRouteKtiInfo.prepare(this.ktiConfigGroup, tempNetwork);
		logger.info("Loading PlansCalcRouteKtiInfo...done.");
		// TODO integrate PlansCalcRouteKti(Info) into scenarioImpl, and scenario config
	}
	
	@Override
	protected void setUp() {

		KTIYear3ScoringFunctionFactory kTIYear3ScoringFunctionFactory = new KTIYear3ScoringFunctionFactory(
				super.config, 
				this.ktiConfigGroup,
				this.getFacilityPenalties());
		this.setScoringFunctionFactory(kTIYear3ScoringFunctionFactory);

		super.setUp();
	}

	
	@Override
	protected void loadControlerListeners() {
		
		super.loadControlerListeners();
		
		// the scoring function processes facility loads
		this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesKTIListener(CALC_LEG_TIMES_KTI_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
		// TODO balmermi: there is a problem with that listener. It uses a system call,
		// and this call needs at least as much memory as the main process (i do not know why, but it is like that).
		// Therefore, for a big run, we cannot use that.
//		this.addControlerListener(new SaveRevisionInfo(SVN_INFO_FILE_NAME));
	}

	@Override
	public PlanAlgorithm getRoutingAlgorithm(final TravelCost travelCosts, final TravelTime travelTimes) {

		PlanAlgorithm router = null;

		if (!this.ktiConfigGroup.isUsePlansCalcRouteKti()) {
			router = super.getRoutingAlgorithm(travelCosts, travelTimes);
		} else {

			router = new PlansCalcRouteKti(
					super.getConfig().plansCalcRoute(), 
					super.network, 
					travelCosts, 
					travelTimes, 
					super.getLeastCostPathCalculatorFactory(), 
					this.plansCalcRouteKtiInfo);

		}

		return router;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: KtiControler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new KTIControler(args);
			controler.run();
		}
		System.exit(0);
	}


}
