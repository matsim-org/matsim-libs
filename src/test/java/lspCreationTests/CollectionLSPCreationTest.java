package lspCreationTests;

import static org.junit.Assert.*;

import java.util.ArrayList;

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

import lsp.LSP;
import lsp.LSPImpl;
import lsp.LSPPlan;
import lsp.LSPPlanImpl;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionElementImpl;
import lsp.LogisticsSolutionImpl;
import lsp.ShipmentAssigner;
import lsp.SolutionScheduler;
import lsp.resources.Resource;
import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.DeterministicShipmentAssigner;
import lsp.usecase.SimpleForwardSolutionScheduler;



public class CollectionLSPCreationTest {

	private Network network;
	private LogisticsSolution collectionSolution;
	private ShipmentAssigner assigner;
	private LSPPlan collectionPlan;
	private SolutionScheduler simpleScheduler;
	private LSP collectionLSP;	
	
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
		CarrierVehicleType collectionType = vehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId);
		carrierVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		Carrier carrier = CarrierImpl.newInstance(carrierId);
		carrier.setCarrierCapabilities(capabilities);
		
		
		Id<Resource> adapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder adapterBuilder = CollectionCarrierAdapter.Builder.newInstance(adapterId, network);
		adapterBuilder.setCollectionScheduler(scheduler);
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		Resource collectionAdapter = adapterBuilder.build();
		
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder collectionElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(elementId);
		collectionElementBuilder.setResource(collectionAdapter);
		LogisticsSolutionElement collectionElement = collectionElementBuilder.build();
		
		Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder collectionSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(collectionSolutionId);
		collectionSolutionBuilder.addSolutionElement(collectionElement);
		collectionSolution = collectionSolutionBuilder.build();
		
		assigner = new DeterministicShipmentAssigner();
		LSPPlan collectionPlan = new LSPPlanImpl();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addSolution(collectionSolution);
	
		LSPImpl.Builder collectionLSPBuilder = LSPImpl.Builder.getInstance();
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		collectionLSPBuilder.setId(collectionLSPId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(collectionAdapter);
		
		simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		collectionLSPBuilder.setSolutionScheduler(simpleScheduler);
		collectionLSP = collectionLSPBuilder.build();
	}

	@Test
	public void testCollectionLSPCreation() {
		assertTrue(collectionLSP.getPlans() != null);
		assertFalse(collectionLSP.getPlans().isEmpty());
		assertTrue(collectionLSP.getResources() != null);
		LSPPlan selectedPlan = collectionLSP.getSelectedPlan();
		assertTrue(selectedPlan.getScore() == 0);
		assertTrue(selectedPlan.getLsp() == collectionLSP);
		assertTrue(selectedPlan.getAssigner() == assigner);
		assertTrue(selectedPlan.getSolutions().iterator().next() == collectionSolution);
		assertTrue(selectedPlan.getSolutions().iterator().next().getLSP() == collectionLSP);
		assertTrue(selectedPlan.getAssigner().getLSP()== collectionLSP);
	}

}
