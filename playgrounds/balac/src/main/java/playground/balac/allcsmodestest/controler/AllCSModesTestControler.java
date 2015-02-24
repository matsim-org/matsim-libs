package playground.balac.allcsmodestest.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import playground.balac.allcsmodestest.config.AllCSModesConfigGroup;
import playground.balac.allcsmodestest.controler.listener.AllCSModesTestListener;
import playground.balac.allcsmodestest.qsim.AllCSModesQsimFactory;
import playground.balac.allcsmodestest.scoring.AllCSModesScoringFunctionFactory;
import playground.balac.analysis.TripsAnalyzer;
import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.router.FreeFloatingRoutingModule;
import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;
import playground.balac.onewaycarsharingredisgned.router.OneWayCarsharingRDRoutingModule;
import playground.balac.twowaycarsharingredisigned.config.TwoWayCSConfigGroup;
import playground.balac.twowaycarsharingredisigned.router.TwoWayCSRoutingModule;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class AllCSModesTestControler {
	
	
	public static void main(final String[] args) {
		//Logger.getLogger( "org.matsim.core.reader..." ).setLevel( Level.OFF );
		
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	OneWayCarsharingRDConfigGroup configGroup = new OneWayCarsharingRDConfigGroup();
    	config.addModule(configGroup);
    	
    	FreeFloatingConfigGroup configGroupff = new FreeFloatingConfigGroup();
    	config.addModule(configGroupff);
    	
    	TwoWayCSConfigGroup configGrouptw = new TwoWayCSConfigGroup();
    	config.addModule(configGrouptw);
    	
    	AllCSModesConfigGroup configGroupAll = new AllCSModesConfigGroup();
    	config.addModule(configGroupAll);
    	
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final Controler controler = new Controler( sc );
		
		try {
			controler.setMobsimFactory( new AllCSModesQsimFactory(sc, controler) );
		
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
								"twowaycarsharing",
								new TwoWayCSRoutingModule());
							
							router.setRoutingModule(
									"freefloating",
									new FreeFloatingRoutingModule());
							
							router.setRoutingModule(
									"onewaycarsharing",
									new OneWayCarsharingRDRoutingModule());
							
							// we still need to provide a way to identify our trips
							// as being twowaycarsharing trips.
							// This is for instance used at re-routing.
							final MainModeIdentifier defaultModeIdentifier =
								router.getMainModeIdentifier();
							router.setMainModeIdentifier(
									new MainModeIdentifier() {
										@Override
										public String identifyMainMode(
												final List<PlanElement> tripElements) {
											for ( PlanElement pe : tripElements ) {
												if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "twowaycarsharing" ) ) {
													return "twowaycarsharing";
												}
												else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "onewaycarsharing" ) ) {
													return "onewaycarsharing";
												}
												else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "freefloating" ) ) {
													return "freefloating";
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
		

			//setting up the scoring function factory, inside different scoring functions are set-up
			AllCSModesScoringFunctionFactory allCSModesScoringFunctionFactory = new AllCSModesScoringFunctionFactory(
				      config, 
				      sc.getNetwork(), sc);
	    controler.setScoringFunctionFactory(allCSModesScoringFunctionFactory); 
			
			
	    Set<String> modes = new TreeSet<String>();
	    modes.add("freefloating");
	    modes.add("twowaycarsharing");
	    modes.add("onewaycarsharing");
	    modes.add("car");
	    modes.add("walk");
	    modes.add("pt");
	    modes.add("bike");
        TripsAnalyzer tripsAnalyzer = new TripsAnalyzer(sc.getConfig().getParam("controler", "outputDirectory")+ "/tripsFile",
	    		sc.getConfig().getParam("controler", "outputDirectory") + "/durationsFile",
	    		sc.getConfig().getParam("controler", "outputDirectory") + "/distancesFile",
	    		modes, true, sc.getNetwork());
	    
	    controler.addControlerListener(tripsAnalyzer);
	    
	    controler.addControlerListener(new AllCSModesTestListener(controler,
	    		Integer.parseInt(controler.getConfig().getModule("AllCSModes").getValue("statsWriterFrequency"))));		
		controler.run();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
