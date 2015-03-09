package playground.balac.allcsmodestest.controler;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.pt.PtConstants;

import playground.balac.allcsmodestest.config.AllCSModesConfigGroup;
import playground.balac.allcsmodestest.controler.listener.AllCSModesTestListener;
import playground.balac.allcsmodestest.qsim.AllCSModesQsimFactory;
import playground.balac.allcsmodestest.scoring.CarsharingMATSimLectureScoringFunctionFactory;
import playground.balac.analysis.TripsAnalyzer;
import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.router.FreeFloatingRoutingModule;
import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;
import playground.balac.onewaycarsharingredisgned.router.OneWayCarsharingRDRoutingModule;
import playground.balac.twowaycarsharingredisigned.config.TwoWayCSConfigGroup;
import playground.balac.twowaycarsharingredisigned.router.TwoWayCSRoutingModule;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;

public class CarsharingMATSimLControler {

			
		public static void main(final String[] args) {
			final String configFile = args[ 0 ];

			// This allows to get a log file containing the log messages happening
			// before controler init.
			OutputDirectoryLogging.catchLogEntries();

			// This is the location choice MultiNodeDijkstra.
			// Suppress all log messages of level below error --- to avoid spaming the config
			// file with zillions of "not route found" messages.
			Logger.getLogger( org.matsim.core.router.MultiNodeDijkstra.class ).setLevel( Level.ERROR ); // this is location choice
			Logger.getLogger( org.matsim.pt.router.MultiNodeDijkstra.class ).setLevel( Level.ERROR );

			final Config config = ConfigUtils.loadConfig(
					configFile,
					// this adds a new config group, used by the specific scoring function
					// we use
						
					new KtiLikeScoringConfigGroup() );

			// This is currently needed for location choice: initializing
			// the location choice writes K-values files to the output directory, which:
			// - fails if the directory does not exist
			// - makes the controler crash latter if the unsafe setOverwriteFiles( true )
			// is not called.
			// This ensures that we get safety with location choice working as expected,
			// before we sort this out and definitely kick out setOverwriteFiles.
			createEmptyDirectoryOrFailIfExists( config.controler().getOutputDirectory() );
			
	    	OneWayCarsharingRDConfigGroup configGroup = new OneWayCarsharingRDConfigGroup();
	    	config.addModule(configGroup);
	    	
	    	FreeFloatingConfigGroup configGroupff = new FreeFloatingConfigGroup();
	    	config.addModule(configGroupff);
	    	
	    	TwoWayCSConfigGroup configGrouptw = new TwoWayCSConfigGroup();
	    	config.addModule(configGrouptw);
	    	
	    	AllCSModesConfigGroup configGroupAll = new AllCSModesConfigGroup();
	    	config.addModule(configGroupAll);		
			
			final Scenario scenario = ScenarioUtils.loadScenario( config );

			final Controler controler = new Controler( scenario );
			controler.setOverwriteFiles( true );
			
			 Set<String> modes = new TreeSet<String>();
			    modes.add("freefloating");
			    modes.add("twowaycarsharing");
			    modes.add("onewaycarsharing");
			    modes.add("car");
			    modes.add("walk");
			    modes.add("pt");
			    modes.add("bike");
			    TripsAnalyzer tripsAnalyzer = new TripsAnalyzer(controler.getConfig().getParam("controler", "outputDirectory")+ "/tripsFile", 
			    		controler.getConfig().getParam("controler", "outputDirectory") + "/durationsFile",
			    		controler.getConfig().getParam("controler", "outputDirectory") + "/distancesFile",
			    		modes, true, controler.getScenario().getNetwork());
			    
			    controler.addControlerListener(tripsAnalyzer);
			    
			    controler.addControlerListener(new AllCSModesTestListener(controler,
			    		Integer.parseInt(controler.getConfig().getModule("AllCSModes").getValue("statsWriterFrequency"))));

			try {
				controler.setMobsimFactory( new AllCSModesQsimFactory(scenario, controler) );
			
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

			connectFacilitiesWithNetwork( controler );

			initializeLocationChoice( controler );

			// We use a specific scoring function, that uses individual preferences
			// for activity durations.
			controler.setScoringFunctionFactory(
				new CarsharingMATSimLectureScoringFunctionFactory(
						controler.getScenario(),
						new StageActivityTypesImpl(
							PtConstants.TRANSIT_ACTIVITY_TYPE ) ) ); 	

			controler.run();
			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private static void connectFacilitiesWithNetwork(Controler controler) {
	        ActivityFacilities facilities = controler.getScenario().getActivityFacilities();
			//log.warn("number of facilities: " +facilities.getFacilities().size());
	        NetworkImpl network = (NetworkImpl) controler.getScenario().getNetwork();
			//log.warn("number of links: " +network.getLinks().size());

			WorldConnectLocations wcl = new WorldConnectLocations(controler.getConfig());
			wcl.connectFacilitiesWithLinks(facilities, network);
		}

		private static void initializeLocationChoice( final Controler controler ) {
			final Scenario scenario = controler.getScenario();
			final DestinationChoiceBestResponseContext lcContext =
				new DestinationChoiceBestResponseContext( scenario );
			lcContext.init();

			controler.addControlerListener(
					new DestinationChoiceInitializer(
						lcContext));
		}

		private static void createEmptyDirectoryOrFailIfExists(final String directory) {
			final File file = new File( directory +"/" );
			if ( file.exists() && file.list().length > 0 ) {
				throw new UncheckedIOException( "Directory "+directory+" exists and is not empty!" );
			}
			file.mkdirs();
		}
		
		

	

}
