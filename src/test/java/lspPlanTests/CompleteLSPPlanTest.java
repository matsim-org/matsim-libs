package lspPlanTests;

import static org.junit.Assert.*;

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
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.DeterministicShipmentAssigner;
import lsp.usecase.DistributionCarrierAdapter;
import lsp.usecase.DistributionCarrierScheduler;
import lsp.usecase.MainRunCarrierAdapter;
import lsp.usecase.MainRunCarrierScheduler;
import lsp.usecase.ReloadingPoint;
import lsp.usecase.ReloadingPointScheduler;
import lsp.resources.Resource;

public class CompleteLSPPlanTest {
	
	private Network network;
	private ShipmentAssigner assigner;
	private LSPPlan completePlan;
	private LogisticsSolution completeSolution;
 
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
		Carrier collectionCarrier = CarrierImpl.newInstance(collectionCarrierId);
		collectionCarrier.setCarrierCapabilities(collectionCapabilities);
		
		
		Id<Resource> collectionAdapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder collectionAdapterBuilder = CollectionCarrierAdapter.Builder.newInstance(collectionAdapterId, network);
		collectionAdapterBuilder.setCollectionScheduler(collectionScheduler);
		collectionAdapterBuilder.setCarrier(collectionCarrier);
		collectionAdapterBuilder.setLocationLinkId(collectionLinkId);
		
		
		Id<LogisticsSolutionElement> collectionElementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder collectionBuilder = LogisticsSolutionElementImpl.Builder.newInstance(collectionElementId);
		collectionBuilder.setResource(collectionAdapterBuilder.build());
		LogisticsSolutionElement collectionElement = collectionBuilder.build();
		
		ReloadingPointScheduler.Builder firstReloadingSchedulerBuilder =  ReloadingPointScheduler.Builder.newInstance();
        firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
        firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);
       
        
        Id<Resource> firstReloadingId = Id.create("ReloadingPoint1", Resource.class);
        Id<Link> firstReloadingLinkId = Id.createLinkId("(4 2) (4 3)");
        
        ReloadingPoint.Builder firstReloadingPointBuilder = ReloadingPoint.Builder.newInstance(firstReloadingId, firstReloadingLinkId);
        firstReloadingPointBuilder.setReloadingScheduler(firstReloadingSchedulerBuilder.build());
        
        
        Id<LogisticsSolutionElement> firstReloadingElementId = Id.create("FiretReloadElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder firstReloadingElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(firstReloadingElementId);
		firstReloadingElementBuilder.setResource(firstReloadingPointBuilder.build());
		LogisticsSolutionElement firstReloadElement = firstReloadingElementBuilder.build();
		
		Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> mainRunVehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder mainRunVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(collectionVehicleTypeId);
		mainRunVehicleTypeBuilder.setCapacity(30);
		mainRunVehicleTypeBuilder.setCostPerDistanceUnit(0.0002);
		mainRunVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		mainRunVehicleTypeBuilder.setFixCost(120);
		mainRunVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType mainRunType = collectionVehicleTypeBuilder.build();
				
		
		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId);
		mainRunCarrierVehicle.setVehicleType(mainRunType);
				
		
		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = collectionCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarrierImpl.newInstance(collectionCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);
        
        
        
        MainRunCarrierScheduler mainRunScheduler = new MainRunCarrierScheduler();
        Id<Resource> mainRunId = Id.create("MainRunAdapter", Resource.class);
        MainRunCarrierAdapter.Builder mainRunAdapterBuilder = MainRunCarrierAdapter.Builder.newInstance(mainRunId, network);
        mainRunAdapterBuilder.setMainRunCarrierScheduler(mainRunScheduler);
        mainRunAdapterBuilder.setFromLinkId(Id.createLinkId("(4 2) (4 3)"));
        mainRunAdapterBuilder.setToLinkId(Id.createLinkId("(14 2) (14 3)"));
        mainRunAdapterBuilder.setCarrier(collectionCarrier);
       
	
        Id<LogisticsSolutionElement> mainRunElementId = Id.create("MainRunElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder mainRunBuilder = LogisticsSolutionElementImpl.Builder.newInstance(mainRunElementId);
		mainRunBuilder.setResource( mainRunAdapterBuilder.build());
		LogisticsSolutionElement mainRunElement = mainRunBuilder.build();
		
		ReloadingPointScheduler.Builder secondSchedulerBuilder =  ReloadingPointScheduler.Builder.newInstance();
        secondSchedulerBuilder.setCapacityNeedFixed(10);
        secondSchedulerBuilder.setCapacityNeedLinear(1);
       
        
        Id<Resource> secondReloadingId = Id.create("ReloadingPoint2", Resource.class);
        Id<Link> secondReloadingLinkId = Id.createLinkId("(14 2) (14 3)");
        
        ReloadingPoint.Builder secondReloadingPointBuilder = ReloadingPoint.Builder.newInstance(secondReloadingId, secondReloadingLinkId);
        secondReloadingPointBuilder.setReloadingScheduler(secondSchedulerBuilder.build());
        
        
        Id<LogisticsSolutionElement> secondReloadingElementId = Id.create("SecondReloadElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder secondReloadingElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(secondReloadingElementId);
		secondReloadingElementBuilder.setResource(secondReloadingPointBuilder.build());
		LogisticsSolutionElement secondReloadElement = secondReloadingElementBuilder.build();
		
		DistributionCarrierScheduler distriutionScheduler = new DistributionCarrierScheduler();
		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder dsitributionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(distributionVehicleTypeId);
		dsitributionVehicleTypeBuilder.setCapacity(10);
		dsitributionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		dsitributionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		dsitributionVehicleTypeBuilder.setFixCost(49);
		dsitributionVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType distributionType = dsitributionVehicleTypeBuilder.build();
		
		Id<Link> distributionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId);
		distributionCarrierVehicle.setVehicleType(distributionType);
		
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(distributionType);
		capabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCapabilities = capabilitiesBuilder.build();
		Carrier carrier = CarrierImpl.newInstance(distributionCarrierId);
		carrier.setCarrierCapabilities(distributionCapabilities);
		
		
		Id<Resource> distributionAdapterId = Id.create("DistributionCarrierAdapter", Resource.class);
		DistributionCarrierAdapter.Builder distributionAdapterBuilder = DistributionCarrierAdapter.Builder.newInstance(distributionAdapterId, network);
		distributionAdapterBuilder.setDistributionScheduler(distriutionScheduler);
		distributionAdapterBuilder.setCarrier(carrier);
		distributionAdapterBuilder.setLocationLinkId(distributionLinkId);
		
		
		Id<LogisticsSolutionElement> distributionElementId = Id.create("DistributionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder distributionBuilder = LogisticsSolutionElementImpl.Builder.newInstance(distributionElementId);
		distributionBuilder.setResource(distributionAdapterBuilder.build());
		LogisticsSolutionElement distributionElement =    distributionBuilder.build();
		
		collectionElement.setNextElement(firstReloadElement);
		firstReloadElement.setPreviousElement(collectionElement);
		firstReloadElement.setNextElement(mainRunElement);
		mainRunElement.setPreviousElement(firstReloadElement);
		mainRunElement.setNextElement(secondReloadElement);
		secondReloadElement.setPreviousElement(mainRunElement);
		secondReloadElement.setNextElement(distributionElement);
		distributionElement.setPreviousElement(secondReloadElement);
		
		Id<LogisticsSolution> solutionId = Id.create("SolutionId", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder completeSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(solutionId);
		completeSolutionBuilder.addSolutionElement(collectionElement);
		completeSolutionBuilder.addSolutionElement(firstReloadElement);
		completeSolutionBuilder.addSolutionElement(mainRunElement);
		completeSolutionBuilder.addSolutionElement(secondReloadElement);
		completeSolutionBuilder.addSolutionElement(distributionElement);
		completeSolution = completeSolutionBuilder.build();
		
		assigner = new DeterministicShipmentAssigner();
		completePlan = LSPUtils.createLSPPlan();
		completePlan.setAssigner(assigner);
		completePlan.addSolution(completeSolution);
	}

	@Test
	public void testCompleteLSPPlan() {
		assertTrue(completePlan.getAssigner() == assigner);
		assertTrue(completePlan.getLsp() == null);
		assertTrue(completePlan.getScore() == 0);
		assertTrue(completePlan.getSolutions().size() ==1);
		assertTrue(completePlan.getSolutions().iterator().next() == completeSolution);
	}
	
}
