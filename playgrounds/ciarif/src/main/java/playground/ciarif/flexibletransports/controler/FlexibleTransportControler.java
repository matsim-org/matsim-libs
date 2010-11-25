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
import playground.ciarif.flexibletransports.scoring.FtScoringFunctionFactory;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.ScoreElements;
import playground.meisterk.kti.router.KtiLinkNetworkRouteFactory;

public class FlexibleTransportControler extends Controler
{
  protected static final String SVN_INFO_FILE_NAME = "svninfo.txt";
  protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
  protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
  protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
  protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
  private FtConfigGroup ftConfigGroup = new FtConfigGroup();
  private final PlansCalcRouteFtInfo plansCalcRouteFtInfo = new PlansCalcRouteFtInfo(this.ftConfigGroup);

  private static final Logger log = Logger.getLogger(FlexibleTransportControler.class);

  public FlexibleTransportControler(String[] args) {
    super(args);

    this.config.addModule("ft", this.ftConfigGroup);

    getNetwork().getFactory().setRouteFactory(MyTransportMode.car, new KtiLinkNetworkRouteFactory(getNetwork(), super.getConfig().planomat()));
    getNetwork().getFactory().setRouteFactory(MyTransportMode.pt, new FtCarSharingRouteFactory(this.plansCalcRouteFtInfo));
    getNetwork().getFactory().setRouteFactory(MyTransportMode.ride, new FtCarSharingRouteFactory(this.plansCalcRouteFtInfo));
    getNetwork().getFactory().setRouteFactory(MyTransportMode.carsharing, new FtCarSharingRouteFactory(this.plansCalcRouteFtInfo));
  }

  protected void setUp()
  {
    if (this.ftConfigGroup.isUsePlansCalcRouteFT()) {
      log.info("Using ftRouter");
      this.plansCalcRouteFtInfo.prepare(getNetwork());
    }

    FtScoringFunctionFactory ftScoringFunctionFactory = new FtScoringFunctionFactory(
      this.config, 
      this.ftConfigGroup, 
      getFacilityPenalties(), 
      getFacilities());
    setScoringFunctionFactory(ftScoringFunctionFactory);

    FtTravelCostCalculatorFactory costCalculatorFactory = new FtTravelCostCalculatorFactory(this.ftConfigGroup);
    setTravelCostCalculatorFactory(costCalculatorFactory);
    super.setUp();
  }

  protected void loadControlerListeners()
  {
    super.loadControlerListeners();

    addControlerListener(new FacilitiesLoadCalculator(getFacilityPenalties()));
    addControlerListener(new ScoreElements("scoreElementsAverages.txt"));
    addControlerListener(new CalcLegTimesKTIListener("calcLegTimesKTI.txt", "legTravelTimeDistribution.txt"));
    addControlerListener(new LegDistanceDistributionWriter("legDistanceDistribution.txt"));
    addControlerListener(new FtPopulationPreparation(this.ftConfigGroup));
    addControlerListener(new CarSharingListener(this.ftConfigGroup));
  }

  public PlanAlgorithm createRoutingAlgorithm(PersonalizableTravelCost travelCosts, PersonalizableTravelTime travelTimes)
  {
    PlanAlgorithm router = null;

    if (!(this.ftConfigGroup.isUsePlansCalcRouteFT())) {
      router = super.createRoutingAlgorithm(travelCosts, travelTimes);
    }
    else {
      router = new PlansCalcRouteFT(
        super.getConfig().plansCalcRoute(), 
        this.network, 
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
      System.out.println("Usage: FlexibleTransportControler config-file [dtd-file]");
      System.out.println();
    } else {
      Controler controler = new FlexibleTransportControler(args);
      controler.run();
    }
    System.exit(0);
  }
}
