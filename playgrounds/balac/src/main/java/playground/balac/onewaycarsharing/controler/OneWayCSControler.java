package playground.balac.onewaycarsharing.controler;

/**
 * @author balacm
 */

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.onewaycarsharing.config.OneWayCSConfigGroup;
import playground.balac.onewaycarsharing.controler.listener.CarSharingListener;
import playground.balac.onewaycarsharing.router.OneWayCarsharingRoutingModule;
import playground.balac.onewaycarsharing.router.PlansCalcRouteFtInfo;
import playground.balac.onewaycarsharing.scenario.FtScenarioLoaderImpl;
import playground.balac.onewaycarsharing.scoring.OneWayCSScoringFunctionFactory;


public final class OneWayCSControler extends Controler
{
  protected static final String SVN_INFO_FILE_NAME = "svninfo.txt";
  protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
  protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
  protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
  protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
  private OneWayCSConfigGroup ftConfigGroup;
  private final PlansCalcRouteFtInfo plansCalcRouteFtInfo ;
  private static final Logger log = Logger.getLogger(OneWayCSControler.class);
  public OneWayCSControler(Scenario scenario, PlansCalcRouteFtInfo plansCalcRouteFtInfo) {
	  
   super(scenario);
   ftConfigGroup = (OneWayCSConfigGroup) this.config.getModule("OneWayCS");
   this.plansCalcRouteFtInfo = plansCalcRouteFtInfo;
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
   

    //FtTravelCostCalculatorFactory costCalculatorFactory = new FtTravelCostCalculatorFactory(this.ftConfigGroup);
    //setTravelDisutilityFactory(costCalculatorFactory);
    super.setUp();
  	}
  }
  
  @Override
  protected void loadControlerListeners() {  
	  
    super.loadControlerListeners();   
    //this.addControlerListener(new FtPopulationPreparation(this.ftConfigGroup));
    this.addControlerListener(new CarSharingListener(this.ftConfigGroup, this.plansCalcRouteFtInfo));
  }

  public void init() {
	  OneWayCSScoringFunctionFactory ftScoringFunctionFactory = new OneWayCSScoringFunctionFactory(
			      this.config, this, 
			      this.ftConfigGroup, 
			      this.getFacilities(), network);
			    this.setScoringFunctionFactory(ftScoringFunctionFactory); 	
			
	}
  
  public static void tuneConfig(final Config config) {
		
		final ActivityParams scoreTelepInteract = new ActivityParams( "onewaycarsharingInteraction" );
		scoreTelepInteract.setTypicalDuration( 60 );
		scoreTelepInteract.setOpeningTime( 0 );
		scoreTelepInteract.setClosingTime( 0 );
		config.planCalcScore().addActivityParams( scoreTelepInteract );
	}
  public static void main(String[] args)
  {
    if ((args == null) || (args.length == 0)) {
      System.out.println("No argument given!");
      System.out.println("Usage: CarSharingControler config-file [dtd-file]");
      System.out.println();
    } else { 
    	
    	//this can all be moved to some function inside of the CarSharingControler    	
    	
    	OneWayCSConfigGroup csConfigGroup = new OneWayCSConfigGroup();
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	
    	config.addModule(csConfigGroup);
    	
    	Scenario sc = ScenarioUtils.createScenario(config);	
    	final PlansCalcRouteFtInfo plansCalcRouteFtInfo = new PlansCalcRouteFtInfo(csConfigGroup);
    	
    	tuneConfig( config );

    	FtScenarioLoaderImpl loader = new FtScenarioLoaderImpl((ScenarioImpl)sc, plansCalcRouteFtInfo, csConfigGroup);
		loader.loadScenario();   	
    	
    	
      final OneWayCSControler controler = new OneWayCSControler(sc, plansCalcRouteFtInfo);
      
      //we need to set a tripRouterFactory for carsharing trips
      controler.setTripRouterFactory(
				new TripRouterFactory() {
					@Override
					public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
						// this factory initializes a TripRouter with default modules,
						// taking into account what is asked for in the config
					
						// This allows us to just add our module and go.
						final TripRouterFactory delegate = DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(controler.getScenario());

						final TripRouter router = delegate.instantiateAndConfigureTripRouter(routingContext);
						
						// add our module to the instance
						router.setRoutingModule(
							"onewaycarsharing",
							new OneWayCarsharingRoutingModule(controler.getConfig().plansCalcRoute(),
								// use the default routing module for the
								// carsharing sub-part.
								
								router.getRoutingModule( TransportMode.car ),
								controler.getScenario().getPopulation().getFactory(), plansCalcRouteFtInfo, controler));

						// we still need to provide a way to identify our trips
						// as being carsharing trips.
						// This is for instance used at re-routing.
						final MainModeIdentifier defaultModeIdentifier =
							router.getMainModeIdentifier();
						router.setMainModeIdentifier(
								new MainModeIdentifier() {
									@Override
									public String identifyMainMode(
											final List<PlanElement> tripElements) {
										for ( PlanElement pe : tripElements ) {
											if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "onewaycarsharing" ) ) {
												return "onewaycarsharing";
											}
										}
										// if the trip doesn't contain a carsharing leg,
										// fall back to the default identification method.
										return defaultModeIdentifier.identifyMainMode( tripElements );
									}
								});
						
						return router;
					}

					
				});
      controler.getConfig().setParam("controler", "runId", "1");
      controler.setOverwriteFiles(true);
      controler.init();
      controler.run();
    }
    System.exit(0);
  }
}
