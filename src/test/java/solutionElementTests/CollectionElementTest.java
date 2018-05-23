package solutionElementTests;

import static org.junit.Assert.*;

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
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionElementImpl;
import lsp.resources.Resource;

public class CollectionElementTest {

	private Network network;
	private CollectionCarrierScheduler scheduler;
	private CarrierVehicleType collectionType;
	private CarrierCapabilities capabilities;
	private Carrier carrier; 
	private LogisticsSolutionElement collectionElement;
	private CollectionCarrierAdapter adapter;
	
	@Before
	public void initialize() {
		
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("input/lsp/network/2regions.xml");
        this.network = scenario.getNetwork();
		
		scheduler = new CollectionCarrierScheduler();
		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		collectionType = vehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId);
		carrierVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		capabilities = capabilitiesBuilder.build();
		carrier = CarrierImpl.newInstance(carrierId);
		carrier.setCarrierCapabilities(capabilities);
		
		
		Id<Resource> adapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder builder = CollectionCarrierAdapter.Builder.newInstance(adapterId, network);
		builder.setCollectionScheduler(scheduler);
		builder.setCarrier(carrier);
		builder.setLocationLinkId(collectionLinkId);
		adapter = builder.build();
		
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder collectionBuilder = LogisticsSolutionElementImpl.Builder.newInstance(elementId);
		collectionBuilder.setResource(adapter);
		collectionElement = collectionBuilder.build();
	}
	
	@Test
	public void testCollectionElement() {
		assertTrue(collectionElement.getIncomingShipments()!= null);
		assertTrue(collectionElement.getIncomingShipments().getShipments() != null);
		assertTrue(collectionElement.getIncomingShipments().getSortedShipments().isEmpty());
		assertTrue(collectionElement.getInfos() != null);
		assertTrue(collectionElement.getInfos().isEmpty());
		assertTrue(collectionElement.getLogisticsSolution() == null);
		assertTrue(collectionElement.getNextElement() == null);
		assertTrue(collectionElement.getOutgoingShipments()!= null);
		assertTrue(collectionElement.getOutgoingShipments().getShipments() != null);
		assertTrue(collectionElement.getOutgoingShipments().getSortedShipments().isEmpty());
		assertTrue(collectionElement.getPreviousElement() == null);
		assertTrue(collectionElement.getResource() == adapter);
		assertTrue(collectionElement.getResource().getClientElements().iterator().next() == collectionElement);
	}
	
}
