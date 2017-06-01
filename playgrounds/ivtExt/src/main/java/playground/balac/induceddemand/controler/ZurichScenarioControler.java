package playground.balac.induceddemand.controler;

import com.google.inject.name.Names;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.socnetsim.jointtrips.scoring.ElementalCharyparNagelLegScoringFunction;
import org.matsim.contrib.socnetsim.jointtrips.scoring.ElementalCharyparNagelLegScoringFunction.LegScoringParameters;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import playground.balac.induceddemand.config.ActivityStrategiesConfigGroup;
import playground.balac.induceddemand.controler.listener.ActivitiesAnalysisListener;
import playground.balac.induceddemand.strategies.activitychainmodifier.ActivityChainModifierStrategy;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.scoring.LineChangeScoringFunction;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Mostly a copy-paste from RunZurichScenario, with added strategies for schedule adaptation.
 * 
 */
public class ZurichScenarioControler {

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
		Logger.getLogger( "org.matsim.analysis.ModeStatsControlerListener" ).setLevel(Level.OFF);


		final Config config = ConfigUtils.loadConfig(
				configFile,
				// this adds a new config group, used by the specific scoring function
				// we use
				new KtiLikeScoringConfigGroup(), new DestinationChoiceConfigGroup(),
				new ActivityStrategiesConfigGroup(), 				new BJActivityScoringConfigGroup());
		
		// This is currently needed for location choice: initializing
		// the location choice writes K-values files to the output directory, which:
		// - fails if the directory does not exist
		// - makes the controler crash latter if the unsafe setOverwriteFiles( true )
		// is not called.
		// This ensures that we get safety with location choice working as expected,
		// before we sort this out and definitely kick out setOverwriteFiles.
		createEmptyDirectoryOrFailIfExists( config.controler().getOutputDirectory() );
		final Scenario scenario = ScenarioUtils.loadScenario( config );

		final Controler controler = new Controler( scenario );
		controler.getConfig().controler().setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles  );

		//connectFacilitiesWithNetwork( controler );

		//initializeLocationChoice( controler );
		initializeActivityStrategies(scenario, controler);
		// We use a specific scoring function, that uses individual preferences
		// for activity durations.
		//controler.addOverridingModule( new MATSim2010ScoringModule() );

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sumScoringFunction = new SumScoringFunction();

				
				double slopeHome1 = ((BJActivityScoringConfigGroup)controler.getConfig().getModule("BJactivityscoring")).getSlopeHome1();
				double slopeHome2 = ((BJActivityScoringConfigGroup)controler.getConfig().getModule("BJactivityscoring")).getSlopeHome2();
				
				Map<String, Double> slopes = new HashMap<>();
				slopes.put("home_1", slopeHome1);
				slopes.put("home_2", slopeHome2);
				slopes.put("work", 0.0);
				slopes.put("secondary", 0.0);
				slopes.put("shopping", 0.0);
				// Score activities, legs, payments and being stuck
				// with the default MATSim scoring based on utility parameters in the config file.
				final ScoringParameters params =
						new ScoringParameters.Builder(controler.getScenario(), person.getId()).build();
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));

				final Collection<String> travelCards = PersonUtils.getTravelcards(person);
				
						sumScoringFunction.addScoringFunction(
						new ElementalCharyparNagelLegScoringFunction(
							TransportMode.pt,
							new LegScoringParameters(
								params.modeParams.get(TransportMode.pt).constant,
								params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s,
								params.modeParams.get(TransportMode.pt).marginalUtilityOfDistance_m),
							scenario.getNetwork()));
						sumScoringFunction.addScoringFunction(
						new ElementalCharyparNagelLegScoringFunction(
							TransportMode.walk,
							LegScoringParameters.createForWalk(
								params ),
							scenario.getNetwork()));
						sumScoringFunction.addScoringFunction(
						new ElementalCharyparNagelLegScoringFunction(
							TransportMode.bike,
							LegScoringParameters.createForBike(
									params),
							scenario.getNetwork()));
						sumScoringFunction.addScoringFunction(
						new ElementalCharyparNagelLegScoringFunction(
							TransportMode.transit_walk,
							LegScoringParameters.createForWalk(
									params),
							scenario.getNetwork()));
						sumScoringFunction.addScoringFunction(
								new LineChangeScoringFunction(
										params ) );
				
				
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				return sumScoringFunction;
			}

		});
		
		
		controler.run();
	}
	
	private static void initializeActivityStrategies(Scenario sc, Controler controler){
		
		final QuadTreeRebuilder<ActivityFacility> shopFacilitiesQuadTree = new QuadTreeRebuilder<ActivityFacility>();
		
		for(ActivityFacility af : sc.getActivityFacilities().getFacilitiesForActivityType("shopping").values()) {
			
			shopFacilitiesQuadTree.put(af.getCoord(), af);
		}
		
		final QuadTreeRebuilder<ActivityFacility> leisureFacilitiesQuadTree = new QuadTreeRebuilder<ActivityFacility>();
		
		for(ActivityFacility af : sc.getActivityFacilities().getFacilitiesForActivityType("secondary").values()) {
			
			leisureFacilitiesQuadTree.put(af.getCoord(), af);
		}
		
		final QuadTree<ActivityFacility> shoping = shopFacilitiesQuadTree.getQuadTree();		
		
		final QuadTree<ActivityFacility> leisure = leisureFacilitiesQuadTree.getQuadTree();		
		HashMap<String, Double> scoreChange = new HashMap<String, Double>();
		
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(QuadTree.class)
				.annotatedWith(Names.named("shopQuadTree"))
				.toInstance(shoping);
				
				bind(QuadTree.class)
				.annotatedWith(Names.named("leisureQuadTree"))
				.toInstance(leisure);
				bind(HashMap.class)
				.annotatedWith(Names.named("scoreChangeMap"))
				.toInstance(scoreChange);
			}
			
		});		
		controler.addControlerListener(new ActivitiesAnalysisListener(sc, scoreChange));

		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
			//	this.addPlanStrategyBinding("InsertRandomActivityWithLocationChoiceStrategy").to( InsertRandomActivityWithLocationChoiceStrategy.class ) ;

			//	this.addPlanStrategyBinding("RandomActivitiesSwaperStrategy").to( RandomActivitiesSwaperStrategy.class ) ;
				
			//	this.addPlanStrategyBinding("RemoveRandomActivityStrategy").to( RemoveRandomActivityStrategy.class ) ;
				this.addPlanStrategyBinding("ActivityChainModifierStrategy").to(ActivityChainModifierStrategy.class);

			}
		});	
		
	}

	private static void connectFacilitiesWithNetwork(MatsimServices controler) {
        ActivityFacilities facilities = controler.getScenario().getActivityFacilities();
		//log.warn("number of facilities: " +facilities.getFacilities().size());
        Network network = (Network) controler.getScenario().getNetwork();
		//log.warn("number of links: " +network.getLinks().size());

		WorldConnectLocations wcl = new WorldConnectLocations(controler.getConfig());
		wcl.connectFacilitiesWithLinks(facilities, network);
	}

	private static void initializeLocationChoice( final MatsimServices controler ) {
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
