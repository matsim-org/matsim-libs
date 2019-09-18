package lspMobsimTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import lsp.*;
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

import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.DeterministicShipmentAssigner;
import lsp.usecase.ReloadingPoint;
import lsp.usecase.ReloadingPointScheduler;
import lsp.usecase.SimpleForwardSolutionScheduler;
import lsp.controler.LSPModule;
import lsp.events.EventUtils;
import lsp.resources.Resource;
import lsp.replanning.LSPReplanningModuleImpl;
import lsp.scoring.LSPScoringModuleImpl;
import lsp.shipment.AbstractShipmentPlanElement;
import lsp.shipment.AbstractShipmentPlanElementComparator;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;

public class MultipleIterationsFirstReloadLSPMobsimTest {
	private Network network;
	private LogisticsSolution completeSolution;
	private ShipmentAssigner assigner;
	private LSPPlan completePlan;
	private SolutionScheduler simpleScheduler;
	private Carrier collectionCarrier;
	private LogisticsSolutionElement collectionElement;
	private LogisticsSolutionElement firstReloadElement;
	private LSP lsp;	
	private Resource collectionAdapter;
	private Resource firstReloadingPointAdapter;
	private int numberOfShipments;
	
	@Before
	public void initialize() {
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
        this.network = scenario.getNetwork();	
	
   
		CollectionCarrierScheduler collectionScheduler = new CollectionCarrierScheduler();
		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> collectionVehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder collectionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(collectionVehicleTypeId);
		collectionVehicleTypeBuilder.setCapacity(10);
		collectionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		collectionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		collectionVehicleTypeBuilder.setFixCost(49);
		collectionVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = collectionVehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle collectionCarrierVehicle = CarrierVehicle.newInstance(collectionVehicleId, collectionLinkId);
		collectionCarrierVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder collectionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		collectionCapabilitiesBuilder.addType(collectionType);
		collectionCapabilitiesBuilder.addVehicle(collectionCarrierVehicle);
		collectionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities collectionCapabilities = collectionCapabilitiesBuilder.build();
		collectionCarrier = CarrierImpl.newInstance(collectionCarrierId);
		collectionCarrier.setCarrierCapabilities(collectionCapabilities);
		
		
		Id<Resource> collectionAdapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder collectionAdapterBuilder = CollectionCarrierAdapter.Builder.newInstance(collectionAdapterId, network);
		collectionAdapterBuilder.setCollectionScheduler(collectionScheduler);
		collectionAdapterBuilder.setCarrier(collectionCarrier);
		collectionAdapterBuilder.setLocationLinkId(collectionLinkId);
		collectionAdapter = collectionAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> collectionElementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder collectionBuilder = LogisticsSolutionElementImpl.Builder.newInstance(collectionElementId);
		collectionBuilder.setResource(collectionAdapter);
		collectionElement = collectionBuilder.build();
		
		ReloadingPointScheduler.Builder firstReloadingSchedulerBuilder =  ReloadingPointScheduler.Builder.newInstance();
        firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
        firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);
       
        
        Id<Resource> firstReloadingId = Id.create("ReloadingPoint1", Resource.class);
        Id<Link> firstReloadingLinkId = Id.createLinkId("(4 2) (4 3)");
        
        ReloadingPoint.Builder firstReloadingPointBuilder = ReloadingPoint.Builder.newInstance(firstReloadingId, firstReloadingLinkId);
        firstReloadingPointBuilder.setReloadingScheduler(firstReloadingSchedulerBuilder.build());
        firstReloadingPointAdapter = firstReloadingPointBuilder.build();
        
        Id<LogisticsSolutionElement> firstReloadingElementId = Id.create("FirstReloadElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder firstReloadingElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(firstReloadingElementId);
		firstReloadingElementBuilder.setResource(firstReloadingPointAdapter);
		firstReloadElement = firstReloadingElementBuilder.build();
		
		collectionElement.setNextElement(firstReloadElement);
		firstReloadElement.setPreviousElement(collectionElement);
		
		
		Id<LogisticsSolution> solutionId = Id.create("SolutionId", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder completeSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(solutionId);
		completeSolutionBuilder.addSolutionElement(collectionElement);
		completeSolutionBuilder.addSolutionElement(firstReloadElement);
		completeSolution = completeSolutionBuilder.build();
		
		assigner = new DeterministicShipmentAssigner();
		completePlan = LSPUtils.createLSPPlan();
		completePlan.setAssigner(assigner);
		completePlan.addSolution(completeSolution);
		
		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance();
		completeLSPBuilder.setInitialPlan(completePlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		completeLSPBuilder.setId(collectionLSPId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(collectionAdapter);
		resourcesList.add(firstReloadingPointAdapter);
		

		simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setSolutionScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();
	
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
		numberOfShipments = 1 + new Random().nextInt(50);
		
		for(int i = 1; i <  1+ numberOfShipments; i++) {
	        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
	        	LSPShipmentImpl.Builder builder = LSPShipmentImpl.Builder.newInstance(id);
	        	//Random random = new Random(1);
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
		config.controler().setLastIteration(1 + new Random().nextInt(10));
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
