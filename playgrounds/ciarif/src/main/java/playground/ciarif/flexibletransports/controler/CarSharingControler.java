package playground.ciarif.flexibletransports.controler;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.ciarif.flexibletransports.controler.listeners.CarSharingListener;
import playground.ciarif.flexibletransports.controler.listeners.FtPopulationPreparation;
import playground.ciarif.flexibletransports.data.MyTransportMode;
import playground.ciarif.flexibletransports.router.FtCarSharingRouteFactory;
import playground.ciarif.flexibletransports.router.FtTravelCostCalculatorFactory;
import playground.ciarif.flexibletransports.router.PlansCalcRouteFT;
import playground.ciarif.flexibletransports.router.PlansCalcRouteFtInfo;
import playground.ciarif.flexibletransports.scenario.FtScenarioLoaderImpl;
import playground.ciarif.flexibletransports.scoring.FtScoringFunctionFactory;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.ScoreElements;
import playground.meisterk.kti.router.KtiLinkNetworkRouteFactory;
import playground.meisterk.kti.scenario.KtiScenarioLoaderImpl;

public class CarSharingControler extends Controler
{
  protected static final String SVN_INFO_FILE_NAME = "svninfo.txt";
  protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
  protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
  protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
  protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
  //private KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
  private FtConfigGroup ftConfigGroup = new FtConfigGroup();
  private final PlansCalcRouteFtInfo plansCalcRouteFtInfo = new PlansCalcRouteFtInfo(this.ftConfigGroup);

  private static final Logger log = Logger.getLogger(FlexibleTransportControler.class);

  public CarSharingControler(String[] args) {
   super(args);

  // super.config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);
   super.config.addModule(FtConfigGroup.GROUP_NAME, this.ftConfigGroup);

    this.getNetwork().getFactory().setRouteFactory(MyTransportMode.car, new KtiLinkNetworkRouteFactory(getNetwork(), super.getConfig().planomat()));
    this.getNetwork().getFactory().setRouteFactory(MyTransportMode.pt, new FtCarSharingRouteFactory(this.plansCalcRouteFtInfo));
    //this.getNetwork().getFactory().setRouteFactory(MyTransportMode.ride, new FtCarSharingRouteFactory(this.plansCalcRouteFtInfo));
    this.getNetwork().getFactory().setRouteFactory(MyTransportMode.carsharing, new FtCarSharingRouteFactory(this.plansCalcRouteFtInfo));
  }

  @Override
  protected void loadData() {
	if (!this.scenarioLoaded) {
			FtScenarioLoaderImpl loader = new FtScenarioLoaderImpl(this.scenarioData, this.plansCalcRouteFtInfo, this.ftConfigGroup);
			loader.loadScenario();
			this.network = this.scenarioData.getNetwork();
			this.population = this.scenarioData.getPopulation();
			this.scenarioLoaded = true;
	}
  }
  @Override
  protected void setUp(){
  
  {
    if (this.ftConfigGroup.isUsePlansCalcRouteFt()) {
      log.info("Using ftRouter");
      this.plansCalcRouteFtInfo.prepare(getNetwork());
    }

    FtScoringFunctionFactory ftScoringFunctionFactory = new FtScoringFunctionFactory(
      this.config, 
      this.ftConfigGroup, 
      this.getFacilityPenalties(), 
      this.getFacilities());
    this.setScoringFunctionFactory(ftScoringFunctionFactory);
//
    FtTravelCostCalculatorFactory costCalculatorFactory = new FtTravelCostCalculatorFactory(this.ftConfigGroup);
    setTravelCostCalculatorFactory(costCalculatorFactory);
    super.setUp();
  	}
  }
  
  @Override
  protected void loadControlerListeners()
  {
    super.loadControlerListeners();

    this.addControlerListener(new FacilitiesLoadCalculator(getFacilityPenalties()));
    this.addControlerListener(new ScoreElements("scoreElementsAverages.txt"));
    this.addControlerListener(new CalcLegTimesKTIListener("calcLegTimesKTI.txt", "legTravelTimeDistribution.txt"));
    this.addControlerListener(new LegDistanceDistributionWriter("legDistanceDistribution.txt"));
    this.addControlerListener(new FtPopulationPreparation(this.ftConfigGroup));
    this.addControlerListener(new CarSharingListener(this.ftConfigGroup));
  }

  @Override
  public PlanAlgorithm createRoutingAlgorithm(PersonalizableTravelCost travelCosts, PersonalizableTravelTime travelTimes)
  {
    PlanAlgorithm router = null;
    log.info("travelcosts = " + travelCosts );

    if (!(this.ftConfigGroup.isUsePlansCalcRouteFt())) {
      router = super.createRoutingAlgorithm(travelCosts, travelTimes);
    }
    else {
      router = new PlansCalcRouteFT(
        super.getConfig().plansCalcRoute(), 
        super.network, 
        travelCosts, 
        travelTimes, 
        super.getLeastCostPathCalculatorFactory(), 
        this.plansCalcRouteFtInfo);
    }

    return router;
  }

  public static void main(String[] args)
  {
    if ((args == null) || (args.length == 0)) {
      System.out.println("No argument given!");
      System.out.println("Usage: CarSharingControler config-file [dtd-file]");
      System.out.println();
    } else {
      final Controler controler = new CarSharingControler(args);
      controler.run();
    }
    System.exit(0);
  }
}
