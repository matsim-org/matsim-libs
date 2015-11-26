package playground.balac.induceddemand.controler;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.pt.PtConstants;

import com.google.inject.name.Names;

import playground.balac.induceddemand.config.ActivityStrategiesConfigGroup;
import playground.balac.induceddemand.strategies.RandomActivitiesSwaperStrategy;
import playground.balac.induceddemand.strategies.RemoveRandomActivityStrategy;
import playground.balac.induceddemand.strategies.insertactivity.InsertRandomActivityWithLocationChoiceStrategy;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;

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

		final Config config = ConfigUtils.loadConfig(
				configFile,
				// this adds a new config group, used by the specific scoring function
				// we use
				new KtiLikeScoringConfigGroup(), new DestinationChoiceConfigGroup(),
				new ActivityStrategiesConfigGroup());
		
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

		connectFacilitiesWithNetwork( controler );

		initializeLocationChoice( controler );
		initializeActivityStrategies(scenario, controler);
		// We use a specific scoring function, that uses individual preferences
		// for activity durations.
		controler.setScoringFunctionFactory(
			new MATSim2010ScoringFunctionFactory(
					controler.getScenario(),
					new StageActivityTypesImpl(
						PtConstants.TRANSIT_ACTIVITY_TYPE ) ) ); 	

		controler.run();
	}
	
	private static void initializeActivityStrategies(Scenario sc, Controler controler){
		
		final QuadTreeRebuilder<ActivityFacility> shopFacilitiesQuadTree = new QuadTreeRebuilder<ActivityFacility>();
		
		for(ActivityFacility af : sc.getActivityFacilities().getFacilitiesForActivityType("shopping").values()) {
			
			shopFacilitiesQuadTree.put(af.getCoord(), af);
		}
		
		final QuadTreeRebuilder<ActivityFacility> leisureFacilitiesQuadTree = new QuadTreeRebuilder<ActivityFacility>();
		
		for(ActivityFacility af : sc.getActivityFacilities().getFacilitiesForActivityType("leisure").values()) {
			
			leisureFacilitiesQuadTree.put(af.getCoord(), af);
		}
		
		final QuadTree<ActivityFacility> shoping = shopFacilitiesQuadTree.getQuadTree();		
		
		final QuadTree<ActivityFacility> leisure = leisureFacilitiesQuadTree.getQuadTree();		

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(QuadTree.class)
				.annotatedWith(Names.named("shopQuadTree"))
				.toInstance(shoping);
				
				bind(QuadTree.class)
				.annotatedWith(Names.named("leisureQuadTree"))
				.toInstance(leisure);
			}
			
		});		
		
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("InsertRandomActivityWithLocationChoiceStrategy").to( InsertRandomActivityWithLocationChoiceStrategy.class ) ;

				this.addPlanStrategyBinding("RandomActivitiesSwaperStrategy").to( RandomActivitiesSwaperStrategy.class ) ;
				
				this.addPlanStrategyBinding("RemoveRandomActivityStrategy").to( RemoveRandomActivityStrategy.class ) ;

			}
		});	
		
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
