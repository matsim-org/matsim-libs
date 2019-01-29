package org.matsim.contrib.accessibility.run;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.accessibility.AccessibilityCalculator;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityContributionCalculator;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.ConstantSpeedAccessibilityExpContributionCalculator;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.NetworkModeAccessibilityExpContributionCalculator;
import org.matsim.contrib.accessibility.ZoneBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestNetwork;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestPopulation;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

/**
 * I can't say how similar or different to {@link AccessibilityIntegrationTest} this one here is.  kai, feb'17
 *
 * @author nagel
 */
//public class AccessibilityIT implements SpatialGridDataExchangeInterface, FacilityDataExchangeInterface {
public class AccessibilityIT implements FacilityDataExchangeInterface {
	private static Logger log = Logger.getLogger( AccessibilityIT.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private int resolution = 100;
	private int nPersons = 3;
	private double[][] accessibilities;

	private Map<String,Double> accessibilitiesHomeZone;
	private Map<String,Double> accessibilitiesWorkZone;
	
	private Map<Tuple<ActivityFacility, Double>, Map<String,Double>> accessibilitiesMap = new HashMap<>();


	@SuppressWarnings("static-method")
	@Before
	public void setUp() throws Exception {
		OutputDirectoryLogging.catchLogEntries();		
		// (collect log messages internally before they can be written to file.  Can be called multiple times without harm.)
	}


	/**
	 * This method tests the grid based accessibility computation.
	 * The test scenario contains a small network with 9 nodes at (0,0),(100,0),(200,0),(0,100),(100,100),(200,100),(0,200),(100,200),(200,200)
	 *  and a population of n (to be specified in variable <code>nPersons</code>. There is also a measuring point for the accessibility computation
	 *  on each node.
	 * All home activities are placed at one node (0,100) and so are all work activities (here: opportunities, (200,100)).
	 * The test result should be that the accessibility of the measuring point at (200,100) is higher than the accessibility at
	 * any other measuring point.
	 */
	@Ignore
	@Test
	public void testGridBasedAccessibilityMeasure(){
		
		//create test network with 9 nodes and 8 links:
		final Network net = CreateTestNetwork.createTestNetwork();

		//create a test population of n persons
		Population population = CreateTestPopulation.createTestPopulation(nPersons);
		
		// ---

		Config config = AccessibilityIT.generateConfigV3() ;
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists) ;
		
		// ---

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		((MutableScenario)scenario).setPopulation(population);
		((MutableScenario)scenario).setNetwork(net);
		
		// ---

		//create a new controler for the simulation
		Controler ctrl = new Controler(scenario);

		{

			//initialize opportunities for accessibility computation
			final ActivityFacilitiesImpl opportunities = new ActivityFacilitiesImpl("opportunities");
			opportunities.createAndAddFacility(Id.create("opp", ActivityFacility.class), new Coord(200, 100));
			
			AccessibilityModule module = new AccessibilityModule() ;
//			module.addSpatialGridDataExchangeListener(this);
			module.addFacilityDataExchangeListener(this);
			ctrl.addOverridingModule(module);

		}
		ctrl.run();

		//test case: verify that accessibility of measuring point 7 (200,100) is higher than all other's
		for(int i=0;i<4;i++){
			for (double[] accessibility : accessibilities) {
				Assert.assertTrue(accessibility[i] <= accessibilities[7][i]);
			}
		}
	}

	/**
	 * This method tests the zone based accessibility computation.
	 * The test scenario contains a small network with 9 nodes at (0,0),(100,0),(200,0),(0,100),(100,100),(200,100),(0,200),(100,200),(200,200)
	 *  and a population of n (to be specified in variable <code>nPersons</code>.
	 * All home activities are placed at one node (0,100) and so are all work activities (here: opportunities, (200,100)). There are also
	 * two zones (home and work zone) with their centroids on the respective node.
	 * The test result should be that the accessibility of the work zone is higher than the accessibility of the home zone.
	 */
	@Test
	public void testZoneBasedAccessibilityMeasure(){

		final String path = utils.getOutputDirectory();

		//create test network with 9 nodes and 8 links and write it into the temp directory
		Network net = CreateTestNetwork.createTestNetwork();

		//create a test population of n persons
		Population population = CreateTestPopulation.createTestPopulation(nPersons);

		// ---
		
		final Config config = AccessibilityIT.generateConfigV3() ;
		
		// ---

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		//add the generated test data to the scenario
		((MutableScenario)scenario).setNetwork(net);
		((MutableScenario)scenario).setPopulation(population);
		
		// ---

		//create a new controler for the simulation
		final Controler ctrl = new Controler(scenario);
		ctrl.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		//create a box with 9 measuring points (one for each node)
		double[] boundary = NetworkUtils.getBoundingBox(net.getNodes().values());

		final double minX = boundary[0]-resolution/2;
		final double minY = boundary[1]-resolution/2;
		final double maxX = boundary[2]+resolution/2;
		final double maxY = boundary[3]+resolution/2;

		//initialize opportunities for accessibility computation
		final ActivityFacilitiesImpl opportunities = new ActivityFacilitiesImpl("opportunities");
		opportunities.createAndAddFacility(Id.create("opp", ActivityFacility.class), new Coord(200, 100));

		final ActivityFacilitiesImpl measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(minX, minY, maxX, maxY, resolution);
		final AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(scenario, measuringPoints, scenario.getNetwork()) ;

		ctrl.addOverridingModule(new AbstractModule() {
			@Override public void install() {

				addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {		
					@Inject private Map<String,TravelTime> travelTimes ;
					@Inject private Map<String,TravelDisutilityFactory> travelDisutilityFactories ;
					@Inject private Network network ;
					
					@Override public ControlerListener get() {

						AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.class);
						for ( Modes4Accessibility mode : acg.getIsComputingMode() ) {
							AccessibilityContributionCalculator calc = null ;
							switch( mode ) {
							case bike:
								calc = new ConstantSpeedAccessibilityExpContributionCalculator( mode.name(), config, network);
								break;
							case car: {
								final TravelTime travelTime = travelTimes.get(mode.name());
								Gbl.assertNotNull(travelTime);
								final TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get(mode.name());
								calc = new NetworkModeAccessibilityExpContributionCalculator(travelTime, travelDisutilityFactory, scenario, scenario.getNetwork()) ;
								break; }
							case freespeed: {
								final TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get(TransportMode.car);
								Gbl.assertNotNull(travelDisutilityFactory);
								calc = new NetworkModeAccessibilityExpContributionCalculator( new FreeSpeedTravelTime(), travelDisutilityFactory, scenario, scenario.getNetwork()) ;
								break; }
							case walk:
								calc = new ConstantSpeedAccessibilityExpContributionCalculator( mode.name(), config, network);
								break;
							default:
								throw new RuntimeException("not implemented") ;
							}
							accessibilityCalculator.putAccessibilityContributionCalculator(mode.name(), calc ) ;
						}
						ZoneBasedAccessibilityControlerListenerV3 listener = new ZoneBasedAccessibilityControlerListenerV3(
								accessibilityCalculator, opportunities, path, scenario);

						listener.addFacilityDataExchangeListener(AccessibilityIT.this);

						return listener ;
					}
				} ) ;
			}
		});
		ctrl.run();

		//test case: verify that accessibility of work zone (200,100) is higher than the home zone's (0,100)

		for ( String mode : accessibilityCalculator.getModes() ) {

			final Double accHZ = accessibilitiesHomeZone.get(mode);
			Gbl.assertNotNull(accHZ);
			Gbl.assertNotNull(accessibilitiesWorkZone);
			final Double accWZ = accessibilitiesWorkZone.get(mode);
			Gbl.assertNotNull(accWZ);
			if ( accHZ != null && accWZ!=null ) {
				Assert.assertTrue (accHZ < accWZ ) ;
			}
		}


	}
	
	

//	@Override
	public void setAndProcessSpatialGrids( Map<String,SpatialGrid> spatialGrids ) {

		if(accessibilities==null)
			//initialize accessibilities-array with place for 9 measuring points and 4 modes
			accessibilities = new double[9][4];

		for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
			if ( mode != Modes4Accessibility.pt && mode != Modes4Accessibility.matrixBasedPt ) {
				final SpatialGrid spatialGrid = spatialGrids.get(mode);
				if ( spatialGrid != null ) {
					getAccessibilities( spatialGrid, mode.ordinal() ) ;
				}
			}
		}

	}

	private void getAccessibilities(SpatialGrid grid,int index) {

		int i=0;

		//store the accessibility of measuring point n in the array at position (n,mode)
		for(double x=grid.getXmin()+resolution/2; x<=grid.getXmax()-resolution/2; x+=grid.getResolution()){
			for(double y=grid.getYmin()+resolution/2;y<=grid.getYmax()-resolution/2; y+=grid.getResolution()){
				accessibilities[i][index] = grid.getValue(x, y);
				i++;
			}
		}

	}

	@Override
	public void setFacilityAccessibilities(ActivityFacility measurePoint, Double timeOfDay, Map<String, Double> accessibilities1) {
		log.warn("");
		log.warn("RECEIVING accessibilities; start zone=" + measurePoint.getId() + " with coord=" + measurePoint.getCoord() );
		for ( Entry<String, Double> entry : accessibilities1.entrySet() ) {
			log.warn( "mode=" + entry.getKey() + "; accessibility=" + entry.getValue() ) ;
		}

		//store the accessibilities of the zone in the list for home or work accessibilities
		if(measurePoint.getCoord().equals(new Coord(100, 0))){
			log.warn("accepting as HZ");
			accessibilitiesHomeZone = accessibilities1 ;
		}
		if(measurePoint.getCoord().equals(new Coord(200, 100))){
			log.warn("accepting as WZ");
			accessibilitiesWorkZone = accessibilities1 ;
		}

	}

	@Override
	public void finish() {

	}


	/**
		 * Starting from a copy of {@link CreateTestM4UConfig#generateM4UConfigV3()} and then modifying it so that it generates a matsim config.
		 */
		public final static Config generateConfigV3(){
			
			Config config = ConfigUtils.createConfig() ;
			
			AccessibilityConfigGroup accConfig = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class ) ;
			accConfig.setTileSize_m(100);
			accConfig.setAreaOfAccessibilityComputation( AreaOfAccesssibilityComputation.fromNetwork );
	
	//		config.network().setInputFile(this.networkInputFileName);
	
			{
				ActivityParams params = new ActivityParams("home") ;
				params.setTypicalDuration(43200);
				config.planCalcScore().addActivityParams(params);
			}
			{
				ActivityParams params = new ActivityParams("work") ;
				params.setTypicalDuration(28800);
				params.setOpeningTime(25200);
				params.setLatestStartTime(32400);
				config.planCalcScore().addActivityParams(params);
			}
	
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(1);
			
			// there are default strategies set by M4UConfigUtils; now I need to set them here explicitly.
			// I don't think that they are ever used, but the injector fails if not at least one of them is there.
			{
				StrategySettings changeExpBeta = new StrategySettings();
				changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
				changeExpBeta.setWeight( 0.8 ) ;
				config.strategy().addStrategySettings(changeExpBeta);
			}
	
			return config ;
		}
}