package lspMobsimTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import lsp.*;
import lsp.shipment.*;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.usecase.DeterministicShipmentAssigner;
import lsp.usecase.MainRunCarrierAdapter;
import lsp.usecase.MainRunCarrierScheduler;
import lsp.usecase.SimpleForwardSolutionScheduler;
import lsp.controler.LSPModule;
import lsp.events.EventUtils;
import lsp.resources.Resource;
import lsp.replanning.LSPReplanningModuleImpl;
import lsp.scoring.LSPScoringModuleImpl;

public class MainRunOnlyLSPMobsimTest {
	private Network network;
	private LogisticsSolution completeSolution;
	private ShipmentAssigner assigner;
	private LSPPlan completePlan;
	private SolutionScheduler simpleScheduler;
	private LSP lsp;	

	@Before
	public void initialize() {
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
        this.network = scenario.getNetwork();	
        
        Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> mainRunVehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder mainRunVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(mainRunVehicleTypeId);
		mainRunVehicleTypeBuilder.setCapacity(30);
		mainRunVehicleTypeBuilder.setCostPerDistanceUnit(0.0002);
		mainRunVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		mainRunVehicleTypeBuilder.setFixCost(120);
		mainRunVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType mainRunType = mainRunVehicleTypeBuilder.build();
				
		
		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId);
		mainRunCarrierVehicle.setVehicleType(mainRunType);
				
		
		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = mainRunCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarrierImpl.newInstance(mainRunCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);
        
        
        
        MainRunCarrierScheduler mainRunScheduler = new MainRunCarrierScheduler();
        Id<Resource> mainRunId = Id.create("MainRunAdapter", Resource.class);
        MainRunCarrierAdapter.Builder mainRunAdapterBuilder = MainRunCarrierAdapter.Builder.newInstance(mainRunId, network);
        mainRunAdapterBuilder.setMainRunCarrierScheduler(mainRunScheduler);
        mainRunAdapterBuilder.setFromLinkId(Id.createLinkId("(4 2) (4 3)"));
        mainRunAdapterBuilder.setToLinkId(Id.createLinkId("(14 2) (14 3)"));
        mainRunAdapterBuilder.setCarrier(mainRunCarrier);
        Resource mainRunAdapter = mainRunAdapterBuilder.build();
	
        Id<LogisticsSolutionElement> mainRunElementId = Id.create("MainRunElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder mainRunBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(mainRunElementId );
		mainRunBuilder.setResource(mainRunAdapter);
		LogisticsSolutionElement mainRunElement = mainRunBuilder.build();
		
		
		
		Id<LogisticsSolution> solutionId = Id.create("SolutionId", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder completeSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(solutionId );
	
		completeSolutionBuilder.addSolutionElement(mainRunElement);
		completeSolution = completeSolutionBuilder.build();
		
		assigner = new DeterministicShipmentAssigner();
		completePlan = LSPUtils.createLSPPlan();
		completePlan.setAssigner(assigner);;
		completePlan.addSolution(completeSolution);
		
		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance();
		completeLSPBuilder.setInitialPlan(completePlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		completeLSPBuilder.setId(collectionLSPId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		
		resourcesList.add(mainRunAdapter);

		simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setSolutionScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();
	
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
		
		 for(int i = 1; i < 2; i++) {
	        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
	        	ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id );
	        	int capacityDemand = 1 + new Random().nextInt(4);
	        	builder.setCapacityDemand(capacityDemand);
	        	
	        	while(true) {
	        		Collections.shuffle(linkList);
	        		Link pendingToLink = linkList.get(0);
	        		if((pendingToLink.getFromNode().getCoord().getX() <= 18000 &&
	        			pendingToLink.getFromNode().getCoord().getY() <= 4000 &&
	        			pendingToLink.getFromNode().getCoord().getX() >= 14000 &&       			
	        			pendingToLink.getToNode().getCoord().getX() <= 18000 &&
	        			pendingToLink.getToNode().getCoord().getY() <= 4000  &&
	        			pendingToLink.getToNode().getCoord().getX() >= 14000	)) {
	        		   builder.setToLinkId(pendingToLink.getId());
	        		   break;	
	        		}
	        	
	        	}
	        	
	        	while(true) {
	        		Collections.shuffle(linkList);
	        		Link pendingFromLink = linkList.get(0);
	        		if(pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
	        		   pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
	        		   pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
	        		   pendingFromLink.getToNode().getCoord().getY() <= 4000    ) {
	        		   builder.setFromLinkId(pendingFromLink.getId());
	        		   break;	
	        		}
	        	
	        	}
	        	
	        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setEndTimeWindow(endTimeWindow);
	        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setStartTimeWindow(startTimeWindow);
	        	builder.setServiceTime(capacityDemand * 60);
	        	LSPShipment shipment = builder.build();
	        	lsp.assignShipmentToLSP(shipment);
	        }
		 lsp.scheduleSoultions();
	
			ArrayList<LSP> lspList = new ArrayList<LSP>();
			lspList.add(lsp);
			LSPs lsps = new LSPs(lspList);
			
			Controler controler = new Controler(config);
			
			LSPModule module = new LSPModule(lsps, new LSPReplanningModuleImpl(lsps), new LSPScoringModuleImpl(lsps), EventUtils.getStandardEventCreators());

			controler.addOverridingModule(module);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(0);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			config.network().setInputFile("scenarios/2regions/2regions-network.xml");
			controler.run();
	}
	@Test
	public void testFirstReloadLSPMobsim() {
		for(LSPShipment shipment : lsp.getShipments()) {
			assertFalse(shipment.getLog().getPlanElements().isEmpty());
			assertTrue(shipment.getSchedule().getPlanElements().size() == shipment.getLog().getPlanElements().size());
			ArrayList<AbstractShipmentPlanElement> scheduleElements = new ArrayList<AbstractShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(scheduleElements, new AbstractShipmentPlanElementComparator());
			ArrayList<AbstractShipmentPlanElement> logElements = new ArrayList<AbstractShipmentPlanElement>(shipment.getLog().getPlanElements().values());
			Collections.sort(logElements, new AbstractShipmentPlanElementComparator());

			for(AbstractShipmentPlanElement scheduleElement : scheduleElements){
				AbstractShipmentPlanElement logElement = logElements.get(scheduleElements.indexOf(scheduleElement));
				assertTrue(scheduleElement.getElementType() == logElement.getElementType());
				assertTrue(scheduleElement.getResourceId() == logElement.getResourceId());
				assertTrue(scheduleElement.getSolutionElement() == logElement.getSolutionElement());
				assertEquals(scheduleElement.getStartTime(), logElement.getStartTime(), 300);
			}	
		}
	}
}
