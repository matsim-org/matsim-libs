package org.matsim.contrib.matsim4urbansim.accessibility;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.ZoneBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestNetwork;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestPopulation;
import org.matsim.contrib.matsim4urbansim.config.CreateTestM4UConfig;
import org.matsim.contrib.matsim4urbansim.config.M4UConfigurationConverterV4;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class AccessibilityTest implements SpatialGridDataExchangeInterface, FacilityDataExchangeInterface {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private double resolution = 100.;
	private int nPersons = 3;
	private double[][] accessibilities;

	private Map<Modes4Accessibility,Double> accessibilitiesHomeZone;
	private Map<Modes4Accessibility,Double> accessibilitiesWorkZone;

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

	@Test
	public void testGridBasedAccessibilityMeasure(){

		//create local temp directory
		String path = utils.getOutputDirectory();

		//create test network with 9 nodes and 8 links and write it into the temp directory
		final Network net = CreateTestNetwork.createTestNetwork();
		new NetworkWriter(net).write(path+"network.xml");

		//create matsim config file and write it into the temp director<
		String configLocation = new CreateTestM4UConfig(path, path+"network.xml").generateConfigV3();

		//create a test population of n persons
		Population population = CreateTestPopulation.createTestPopulation(nPersons);

		//get the config file and initialize it
		M4UConfigurationConverterV4 connector = new M4UConfigurationConverterV4(configLocation);
		connector.init();
		final Config config = connector.getConfig();
		Assert.assertTrue(config!=null) ;

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		//add the generated test population to the scenario
		((MutableScenario)scenario).setPopulation(population);

		//create a new controler for the simulation
		Controler ctrl = new Controler(scenario);
		ctrl.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		//pt not used in this test
		final PtMatrix ptMatrix = null;


		{
			//create a bounding box with 9 measuring points (one for each node)
			double[] boundary = NetworkUtils.getBoundingBox(net.getNodes().values());
			final double minX = boundary[0]-resolution/2;
			final double minY = boundary[1]-resolution/2;
			final double maxX = boundary[2]+resolution/2;
			final double maxY = boundary[3]+resolution/2;

			//initialize opportunities for accessibility computation
			final ActivityFacilitiesImpl opportunities = new ActivityFacilitiesImpl("opportunities");
			opportunities.createAndAddFacility(Id.create("opp", ActivityFacility.class), new Coord((double) 200, (double) 100));

			ctrl.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
						@Inject Map<String, TravelTime> travelTimes;
						@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;

						@Override
						public ControlerListener get() {
							//initialize new grid based accessibility controler listener and grids for the modes we want to analyze here
							GridBasedAccessibilityControlerListenerV3 listener = new GridBasedAccessibilityControlerListenerV3(opportunities, ptMatrix, config, scenario, travelTimes, travelDisutilityFactories);
							for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
								listener.setComputingAccessibilityForMode(mode, true);
							}
							listener.setComputingAccessibilityForMode( Modes4Accessibility.pt, false );
							listener.generateGridsAndMeasuringPointsByCustomBoundary(minX, minY, maxX, maxY, resolution);

							//add grid data exchange listener to get accessibilities
							listener.addSpatialGridDataExchangeListener(AccessibilityTest.this);
							return listener;
						}
					});

				}
			});
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

		//create local temp directory
		final String path = utils.getOutputDirectory();

		//create test network with 9 nodes and 8 links and write it into the temp directory
		Network net = CreateTestNetwork.createTestNetwork();
		new NetworkWriter(net).write(path+"network.xml");

		//create matsim config file and write it into the temp director<
		CreateTestM4UConfig ctmc = new CreateTestM4UConfig(path, path+"network.xml");
		String configLocation = ctmc.generateConfigV3();

		//create a test population of n persons
		Population population = CreateTestPopulation.createTestPopulation(nPersons);

		//get the config file and initialize it
		M4UConfigurationConverterV4 connector = new M4UConfigurationConverterV4(configLocation);
		connector.init();
		final Config config = connector.getConfig();
		Assert.assertTrue(config!=null) ;

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		//add the generated test population to the scenario
		((MutableScenario)scenario).setPopulation(population);

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
		opportunities.createAndAddFacility(Id.create("opp", ActivityFacility.class), new Coord((double) 200, (double) 100));

		final ActivityFacilitiesImpl measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(minX, minY, maxX, maxY, resolution);
		ctrl.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
					@Inject Map<String, TravelTime> travelTimes;
					@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;

					@Override
					public ControlerListener get() {
						//initialize new zone based accessibility controler listener and grids for the modes we want to analyze here
						ZoneBasedAccessibilityControlerListenerV3 listener = new ZoneBasedAccessibilityControlerListenerV3(measuringPoints, opportunities, null, path, scenario, travelTimes, travelDisutilityFactories);
						for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
							listener.setComputingAccessibilityForMode(mode, true);
						}
						listener.setComputingAccessibilityForMode( Modes4Accessibility.pt, false );
						// don't know why this is not activated as a test. kai, feb'14

						listener.addZoneDataExchangeListener(AccessibilityTest.this);

						//add grid based accessibility services listener to the services and run the simulation
						return listener;
					}
				});

			}
		});
		ctrl.run();

		//test case: verify that accessibility of work zone (200,100) is higher than the home zone's (0,100)

		for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
			final Double accHZ = accessibilitiesHomeZone.get(mode);
			final Double accWZ = accessibilitiesWorkZone.get(mode);
			if ( accHZ != null && accWZ!=null ) {
				Assert.assertTrue (accHZ < accWZ ) ;
			}
		}


	}

	@Override
	public void setAndProcessSpatialGrids( Map<Modes4Accessibility,SpatialGrid> spatialGrids ) {

		if(accessibilities==null)
			//initialize accessibilities-array with place for 9 measuring points and 4 modes
			accessibilities = new double[9][4];

		for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
			if ( mode != Modes4Accessibility.pt ) {
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
		for(double x=grid.getXmin()+resolution/2;x<=grid.getXmax()-resolution/2;x+=grid.getResolution()){
			for(double y=grid.getYmin()+resolution/2;y<=grid.getYmax()-resolution/2;y+=grid.getResolution()){
				accessibilities[i][index] = grid.getValue(x, y);
				i++;
			}
		}

	}

	@Override
	public void setFacilityAccessibilities(ActivityFacility measurePoint, Double timeOfDay, Map<Modes4Accessibility, Double> accessibilities1) {

		//store the accessibilities of the zone in the list for home or work accessibilities
		if(measurePoint.getCoord().equals(new Coord((double) 100, (double) 0))){
			accessibilitiesHomeZone = accessibilities1 ;
		}
		if(measurePoint.getCoord().equals(new Coord((double) 200, (double) 100))){
			accessibilitiesWorkZone = accessibilities1 ;
		}

	}

	@Override
	public void finish() {

	}

}
