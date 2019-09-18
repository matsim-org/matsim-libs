package lspMobsimTests;

import static org.junit.Assert.*;

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

import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.DeterministicShipmentAssigner;
import lsp.usecase.SimpleForwardSolutionScheduler;
import lsp.controler.LSPModule;
import lsp.events.EventUtils;
import lsp.resources.Resource;
import lsp.replanning.LSPReplanningModuleImpl;
import lsp.scoring.LSPScoringModuleImpl;

public class MultipleShipmentsCollectionLSPMobsimTest {
	private Network network;
	private LSP collectionLSP;	
	private Carrier carrier;
	private Resource collectionAdapter;
	private LogisticsSolutionElement collectionElement;
	private int numberOfShipments;
	
	@Before
	public void initialize() {
		
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
        this.network = scenario.getNetwork();
		
		CollectionCarrierScheduler scheduler = new CollectionCarrierScheduler();
		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Link collectionLink = network.getLinks().get(collectionLinkId);
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLink.getId());
		carrierVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		carrier = CarrierImpl.newInstance(carrierId);
		carrier.setCarrierCapabilities(capabilities);
		
		
		Id<Resource> adapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder adapterBuilder = CollectionCarrierAdapter.Builder.newInstance(adapterId, network);
		adapterBuilder.setCollectionScheduler(scheduler);
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		collectionAdapter = adapterBuilder.build();
		
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder collectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId );
		collectionElementBuilder.setResource(collectionAdapter);
		collectionElement = collectionElementBuilder.build();
		
		Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder collectionSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(collectionSolutionId );
		collectionSolutionBuilder.addSolutionElement(collectionElement);
		LogisticsSolution collectionSolution = collectionSolutionBuilder.build();
		
		ShipmentAssigner assigner = new DeterministicShipmentAssigner();
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addSolution(collectionSolution);
	
		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance();
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		collectionLSPBuilder.setId(collectionLSPId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(collectionAdapter);
		
		SolutionScheduler simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		collectionLSPBuilder.setSolutionScheduler(simpleScheduler);
		collectionLSP = collectionLSPBuilder.build();
	
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
	    Id<Link> toLinkId = collectionLinkId;
	
	    numberOfShipments = 1 + new Random().nextInt(50);   
	    for(int i = 1; i < 1+ numberOfShipments; i++) {
        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
        	ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id );
        	//Random random = new Random(1);
        	int capacityDemand = 1 + new Random().nextInt(4);
        	builder.setCapacityDemand(capacityDemand);
        	
        	while(true) {
        		Collections.shuffle(linkList);
        		Link pendingFromLink = linkList.get(0);
        		if(pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
        		   pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
        		   pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
        		   pendingFromLink.getToNode().getCoord().getY() <= 4000) {
        		   builder.setFromLinkId(pendingFromLink.getId());
        		   break;	
        		}	
        	}
        	
        	builder.setToLinkId(toLinkId);
        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setEndTimeWindow(endTimeWindow);
        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
        	builder.setStartTimeWindow(startTimeWindow);
        	builder.setServiceTime(capacityDemand * 60);
        	LSPShipment shipment = builder.build();
        	collectionLSP.assignShipmentToLSP(shipment);
        }
		collectionLSP.scheduleSoultions();
		
		ArrayList<LSP> lspList = new ArrayList<LSP>();
		lspList.add(collectionLSP);
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
	public void testCollectionLSPMobsim() {
		
		for(LSPShipment shipment : collectionLSP.getShipments()) {
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
