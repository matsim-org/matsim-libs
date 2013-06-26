package org.matsim.contrib.matsim4urbansim.accessibility;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.ZoneBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.accessibility.interfaces.ZoneDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.BoundingBox;
import org.matsim.contrib.improvedPseudoPt.PtMatrix;
import org.matsim.contrib.matsim4urbansim.config.M4UConfigurationConverterV4;
import org.matsim.contrib.matsim4urbansim.utils.CreateTestMATSimConfig;
import org.matsim.contrib.matsim4urbansim.utils.CreateTestNetwork;
import org.matsim.contrib.matsim4urbansim.utils.CreateTestPopulation;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

public class AccessibilityTest implements SpatialGridDataExchangeInterface, ZoneDataExchangeInterface {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private double resolution = 100.;
	private int nPersons = 3;
	private double[][] accessibilities;
	
	private List<Double> accessibilitiesHomeZone;
	private List<Double> accessibilitiesWorkZone;

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
		Network net = CreateTestNetwork.createTestNetwork();
		new NetworkWriter(net).write(path+"network.xml");
		
		//create matsim config file and write it into the temp director<
		CreateTestMATSimConfig ctmc = new CreateTestMATSimConfig(path, path+"network.xml");
		String configLocation = ctmc.generateConfigV3();

		//create a test population of n persons
		Population population = CreateTestPopulation.createTestPopulation(nPersons);

		//get the config file and initialize it
		M4UConfigurationConverterV4 connector = new M4UConfigurationConverterV4(configLocation);
		connector.init();
		Config config = connector.getConfig();
		Assert.assertTrue(config!=null) ;

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//add the generated test population to the scenario
		((ScenarioImpl)scenario).setPopulation(population);

		//create a new controler for the simulation
		Controler ctrl = new Controler(scenario);
		ctrl.setOverwriteFiles(true);
		
		//create a bounding box with 9 measuring points (one for each node)
		BoundingBox bbox = new BoundingBox();
		double[] boundary = NetworkUtils.getBoundingBox(net.getNodes().values());
		
		double minX = boundary[0]-resolution/2;
		double minY = boundary[1]-resolution/2;
		double maxX = boundary[2]+resolution/2;
		double maxY = boundary[3]+resolution/2;
		
		bbox.setCustomBoundaryBox(minX,minY,maxX,maxY);
		
		//initialize opportunities for accessibility computation
		ActivityFacilitiesImpl opportunities = new ActivityFacilitiesImpl("opportunities");
		opportunities.createAndAddFacility(new IdImpl("opp"), new CoordImpl(200, 100));
		
		//pt not used in this test
		PtMatrix ptMatrix = null;

		//initialize new grid based accessibility controler listener and grids for the modes we want to analyze here
		GridBasedAccessibilityControlerListenerV3 listener = new GridBasedAccessibilityControlerListenerV3(opportunities, ptMatrix, config, net);
		listener.setComputingAccessibilityForFreeSpeedCar(true);
		listener.setComputingAccessibilityForCongestedCar(true);
		listener.setComputingAccessibilityForBike(true);
		listener.setComputingAccessibilityForWalk(true);
		listener.generateGridsAndMeasuringPointsByCustomBoundary(minX, minY, maxX, maxY, resolution);
		
		//add grid data exchange listener to get accessibilities
		listener.addSpatialGridDataExchangeListener(this);
		
		//add grid based accessibility controler listener to the controler and run the simulation
		ctrl.addControlerListener(listener);
		ctrl.run();
	
		//test case: verify that accessibility of measuring point 7 (200,100) is higher than all other's
		for(int i=0;i<4;i++){
			for(int j=0;j<accessibilities.length;j++){
				Assert.assertTrue(accessibilities[j][i]<=accessibilities[7][i]);
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
		String path = utils.getOutputDirectory();

		//create test network with 9 nodes and 8 links and write it into the temp directory
		Network net = CreateTestNetwork.createTestNetwork();
		new NetworkWriter(net).write(path+"network.xml");
		
		//create matsim config file and write it into the temp director<
		CreateTestMATSimConfig ctmc = new CreateTestMATSimConfig(path, path+"network.xml");
		String configLocation = ctmc.generateConfigV3();

		//create a test population of n persons
		Population population = CreateTestPopulation.createTestPopulation(nPersons);

		//get the config file and initialize it
		M4UConfigurationConverterV4 connector = new M4UConfigurationConverterV4(configLocation);
		connector.init();
		Config config = connector.getConfig();
		Assert.assertTrue(config!=null) ;

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//add the generated test population to the scenario
		((ScenarioImpl)scenario).setPopulation(population);

		//create a new controler for the simulation
		Controler ctrl = new Controler(scenario);
		ctrl.setOverwriteFiles(true);
			
		//create a bounding box with 9 measuring points (one for each node)
		BoundingBox bbox = new BoundingBox();
		double[] boundary = NetworkUtils.getBoundingBox(net.getNodes().values());
				
		double minX = boundary[0]-resolution/2;
		double minY = boundary[1]-resolution/2;
		double maxX = boundary[2]+resolution/2;
		double maxY = boundary[3]+resolution/2;
				
		bbox.setCustomBoundaryBox(minX,minY,maxX,maxY);
				
		//initialize opportunities for accessibility computation
		ActivityFacilitiesImpl opportunities = new ActivityFacilitiesImpl("opportunities");
		opportunities.createAndAddFacility(new IdImpl("opp"), new CoordImpl(200, 100));
		
		ActivityFacilitiesImpl measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(minX, minY, maxX, maxY, resolution);
			
		//pt not used in this test
		PtMatrix ptMatrix = null;

		//initialize new zone based accessibility controler listener and grids for the modes we want to analyze here
		ZoneBasedAccessibilityControlerListenerV3 listener = new ZoneBasedAccessibilityControlerListenerV3(measuringPoints, opportunities,
				ptMatrix, path, scenario);
		listener.setComputingAccessibilityForFreeSpeedCar(true);
		listener.setComputingAccessibilityForCongestedCar(true);
		listener.setComputingAccessibilityForBike(true);
		listener.setComputingAccessibilityForWalk(true);
		
		//
		listener.addZoneDataExchangeListener(this);
		
		//add grid based accessibility controler listener to the controler and run the simulation
		ctrl.addControlerListener(listener);
		ctrl.run();
		
		//test case: verify that accessibility of work zone (200,100) is higher than the home zone's (0,100)
		if(this.endReached()){
			for(int i=0;i<accessibilitiesHomeZone.size();i++){
				Assert.assertTrue(accessibilitiesHomeZone.get(i)<accessibilitiesWorkZone.get(i));
			}
		}
				
	}

	@Override
	public void getAndProcessSpatialGrids(SpatialGrid freeSpeedGrid,
			SpatialGrid carGrid, SpatialGrid bikeGrid, SpatialGrid walkGrid,
			SpatialGrid ptGrid) {
		
		if(accessibilities==null)
			//initialize accessibilities-array with place for 9 measuring points and 4 modes
			accessibilities = new double[9][4];
		
		//get accessibilities from the grids
		if(freeSpeedGrid != null)
			getAccessibilities(freeSpeedGrid,0);
		if(carGrid != null)
			getAccessibilities(carGrid,1);
		if(bikeGrid != null)
			getAccessibilities(bikeGrid,2);
		if(walkGrid != null)
			getAccessibilities(walkGrid,3);
		
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
	public void getZoneAccessibilities(ActivityFacility measurePoint,
			double freeSpeedAccessibility, double carAccessibility,
			double bikeAccessibility, double walkAccessibility, double ptAccessibility) {

		//store the accessibilities of the zone in the list for home or work accessibilities
		if(measurePoint.getCoord().equals(new CoordImpl(100,0))){
			if(accessibilitiesHomeZone==null)
				accessibilitiesHomeZone = new ArrayList<Double>();
			accessibilitiesHomeZone.add(freeSpeedAccessibility);
			accessibilitiesHomeZone.add(carAccessibility);
			accessibilitiesHomeZone.add(bikeAccessibility);
			accessibilitiesHomeZone.add(walkAccessibility);
		}
		if(measurePoint.getCoord().equals(new CoordImpl(200,100))){
			if(accessibilitiesWorkZone==null)
				accessibilitiesWorkZone = new ArrayList<Double>();
			accessibilitiesWorkZone.add(freeSpeedAccessibility);
			accessibilitiesWorkZone.add(carAccessibility);
			accessibilitiesWorkZone.add(bikeAccessibility);
			accessibilitiesWorkZone.add(walkAccessibility);
		}
		
	}
	
	@Override
	public boolean endReached(){
		if(this.accessibilitiesHomeZone!=null&&this.accessibilitiesWorkZone!=null)
			return true;
		return false;
	}

}
