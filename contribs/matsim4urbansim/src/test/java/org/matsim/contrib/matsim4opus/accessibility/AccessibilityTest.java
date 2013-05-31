package org.matsim.contrib.matsim4opus.accessibility;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matsim4opus.config.M4UConfigurationConverterV4;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.gis.GridUtils;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.contrib.matsim4opus.gis.ZoneLayer;
import org.matsim.contrib.matsim4opus.improvedpseudopt.PtMatrix;
import org.matsim.contrib.matsim4opus.interfaces.MATSim4UrbanSimInterface;
import org.matsim.contrib.matsim4opus.matsim4urbansim.ParcelBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.matsim4opus.matsim4urbansim.ZoneBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.matsim4opus.utils.CreateTestMATSimConfig;
import org.matsim.contrib.matsim4opus.utils.CreateTestNetwork;
import org.matsim.contrib.matsim4opus.utils.CreateTestUrbansimPopulation;
import org.matsim.contrib.matsim4opus.utils.helperObjects.Benchmark;
import org.matsim.contrib.matsim4opus.utils.io.ReadFromUrbanSimModel;
import org.matsim.contrib.matsim4opus.utils.io.TempDirectoryUtil;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

public class AccessibilityTest implements MATSim4UrbanSimInterface, LinkEnterEventHandler, LinkLeaveEventHandler {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private double resolution = 100.;
	private int nPersons = 3;

	/*
	 *	used network looks sth. like:
	 *
	 * (2)      (5)------(8)
	 *	|        |
	 *	|        |
	 * (1)------(4)------(7)
	 *	|        |
	 *	|	 	 |
	 * (3)      (6)------(9)
	 * 
	 * the test population contains n persons with home and work places
	 * home activities are at (0,100; node 1), work activities are at (250,100; node 7)
	 * 
	 * there are 9 measuring points (one per node). by that, the accessibility of mp 7 should be higher than the other's
	 * 
	 * (2)      (5)------(8)
	 * 	|        |
	 * 	|        |
	 * (1)------(4)------(7)
	 *  |        |
	 *  |        |
	 * (0)      (3)------(6)
	 * 
	 * no MATSim run
	 */	
	
	public static void main(String[] args) {
		
		new AccessibilityTest().testAccessibilityMeasure();

	}
	
	@Test
	public void testAccessibilityMeasure(){
		
//		String path = TempDirectoryUtil.createCustomTempDirectory("tmp");
		String path = utils.getOutputDirectory() ;
		
		Network net = CreateTestNetwork.createTestNetwork();
		new NetworkWriter(net).write(path+"network.xml");
		
		CreateTestMATSimConfig ctmc = new CreateTestMATSimConfig(path, path+"network.xml");
		String configLocation = ctmc.generate();
		
		InternalConstants.setOpusHomeDirectory(path);

		CreateTestUrbansimPopulation.createUrbanSimTestPopulation(path, nPersons);

		M4UConfigurationConverterV4 connector = new M4UConfigurationConverterV4(configLocation);
		connector.init();
		Config config = connector.getConfig();
		Assert.assertTrue(config!=null) ;

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler ctrl = new Controler(scenario);
		ctrl.setOverwriteFiles(true);
		
		NetworkBoundaryBox bbox = new NetworkBoundaryBox();
		double[] boundary = NetworkUtils.getBoundingBox(net.getNodes().values());
		
		double minX = boundary[0]-resolution/2;
		double minY = boundary[1]-resolution/2;
		double maxX = boundary[2]+resolution/2;
		double maxY = boundary[3]+resolution/2;

		if(resolution>100){
			
			minX = boundary[0] - 150*(2*resolution/200-1);
			minY = boundary[1] - 150*(2*resolution/200-1);
			maxX = boundary[2] + 150*(2*resolution/200+1);
			maxY = boundary[3] + 150*(2*resolution/200+1);
			
		}
		
		bbox.setCustomBoundaryBox(minX,minY,maxX,maxY);
		
		ZoneLayer<Id> startZones = GridUtils.createGridLayerByGridSizeByNetwork(resolution, bbox.getBoundingBox());

		SpatialGrid freeSpeedGrid = new SpatialGrid(bbox.getBoundingBox(), resolution);
		SpatialGrid carGrid = new SpatialGrid(freeSpeedGrid);
		SpatialGrid bikeGrid = new SpatialGrid(freeSpeedGrid);
		SpatialGrid walkGrid = new SpatialGrid(freeSpeedGrid);
		SpatialGrid ptGrid = new SpatialGrid(freeSpeedGrid);
		
		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl("parcels");
		ActivityFacilitiesImpl zones = new ActivityFacilitiesImpl("zones");
		
		this.getReadFromUrbanSimModel().readFacilitiesParcel(parcels, zones);
		
		Benchmark benchmark = new Benchmark();

		PtMatrix ptMatrix = null;

		ControlerListener listener = null;
		
		if(this.isParcelMode()){
			listener = new ParcelBasedAccessibilityControlerListenerV3(this, startZones, parcels, freeSpeedGrid, carGrid, bikeGrid, walkGrid,
					ptGrid, ptMatrix, benchmark, scenario);
			ctrl.addControlerListener(listener);
			testParcelBasedAccessibility(ctrl,listener, freeSpeedGrid);
		}
		
//		if(this.isParcelMode())
//			listener = new ParcelBasedAccessibilityControlerListenerV3(this, startZones, parcels, freeSpeedGrid, carGrid, bikeGrid, walkGrid,
//					ptGrid, ptMatrix, benchmark, scenario);
//		else
//			listener = new ZoneBasedAccessibilityControlerListenerV3(this, startZones, zones, ptMatrix, benchmark, scenario);
//
//		
//		ctrl.addControlerListener(listener);
//		
//		ctrl.run();
		
		//Assert.assertTrue(sth.);
		
		//new AccessibilityTest().postProcessTest();
		
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
		
	}
	
	private void testParcelBasedAccessibility(Controler ctrl,
			ControlerListener listener, SpatialGrid freeSpeedGrid) {
		ctrl.run();
		
		double maxVal = freeSpeedGrid.getValue(200, 100);
		for(double x=freeSpeedGrid.getXmin();x<freeSpeedGrid.getXmax();x+=resolution){
			for(double y=freeSpeedGrid.getYmin();y<freeSpeedGrid.getYmax();y++){
				if(x!=200||y!=100)
					Assert.assertTrue(freeSpeedGrid.getValue(x, y)<=maxVal);
			}
		}
		
	}

	@Override
	public double getOpportunitySampleRate() {
		return 1.;
	}

	@Override
	public ReadFromUrbanSimModel getReadFromUrbanSimModel() {
		return new ReadFromUrbanSimModel(2010);
	}

	@Override
	public boolean isParcelMode() {
		return true;
	}

	@Override
	public double getTimeOfDay() {
		return 0.;
	}
	
	
//	plan b
//	public void postProcessTest(){
//		
//		Config config = ConfigUtils.loadConfig("C:/Users/Daniel/Desktop/AccessibilityTest/out/run0.output_config.xml");
//		
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		
//		AccessibilityParameterConfigModule APCM = new AccessibilityParameterConfigModule(AccessibilityParameterConfigModule.GROUP_NAME);
//		APCM.setUsingRawSumsWithoutLn(false);
//		APCM.setLogitScaleParameter(1.);
//		APCM.setUsingCarParameterFromMATSim(true);
//		APCM.setUsingBikeParameterFromMATSim(true);
//		APCM.setUsingWalkParameterFromMATSim(true);
//		APCM.setUsingPtParameterFromMATSim(true);
//		
//		APCM.setBetaCarTravelTime(-12.);
//		APCM.setBetaBikeTravelTime(-12.);
//		APCM.setBetaWalkTravelTime(-12.);
//		APCM.setBetaPtTravelTime(-12.);
//		
//		config.addModule(AccessibilityParameterConfigModule.GROUP_NAME, APCM);
//		
//		Controler ctrl = new Controler(scenario);
//		ctrl.setOverwriteFiles(true);
//		
//		TravelTimeCalculator travelTimeCalculator = ctrl.getTravelTimeCalculatorFactory().createTravelTimeCalculator(scenario.getNetwork(), config.travelTimeCalculator());
//		
//		EventsManager em = EventsUtils.createEventsManager();
//		MatsimEventsReader er = new MatsimEventsReader(em);
//		em.addHandler(travelTimeCalculator);
//		
//		er.readFile("C:/Users/Daniel/Desktop/AccessibilityTest/out/ITERS/it.0/run0.0.events.xml.gz");
//		
//		NetworkBoundaryBox bbox = new NetworkBoundaryBox();
//		bbox.setCustomBoundaryBox(100-resolution,100-(resolution/2),100+resolution,100+(resolution/2));
//		ZoneLayer<Id> startZones = GridUtils.createGridLayerByGridSizeByNetwork(resolution, bbox.getBoundingBox());
//		SpatialGrid freeSpeedGrid = new SpatialGrid(bbox.getBoundingBox(), resolution);
//		SpatialGrid carGrid = new SpatialGrid(freeSpeedGrid);
//		SpatialGrid bikeGrid = new SpatialGrid(freeSpeedGrid);
//		SpatialGrid walkGrid = new SpatialGrid(freeSpeedGrid);
//		SpatialGrid ptGrid = new SpatialGrid(freeSpeedGrid);
//		
//		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl();
//		int i = 1;
//		for(Person p : scenario.getPopulation().getPersons().values()){
//			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
//				if(pe instanceof Activity){
//					if(((Activity)pe).getType().equalsIgnoreCase("work")){
//						parcels.createAndAddFacility(new IdImpl(i), ((Activity)pe).getCoord());
//						i++;
//					}
//				}
//			}
//		}
//		
//		InternalConstants.setOpusHomeDirectory("C:/Users/Daniel/Desktop/AccessibilityTest");
//		Benchmark benchmark = new Benchmark();
//
//		PtMatrix ptMatrix = null;
//		
//		ParcelBasedAccessibilityControlerListenerV3 listener = new ParcelBasedAccessibilityControlerListenerV3(this, startZones, parcels,
//				freeSpeedGrid, carGrid, bikeGrid, walkGrid, ptGrid, ptMatrix, benchmark, (ScenarioImpl)scenario);
//		
//		ctrl.addControlerListener(listener);
//		
//		new ShutdownEvent(ctrl, false);
//		
//	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		
	}

}
