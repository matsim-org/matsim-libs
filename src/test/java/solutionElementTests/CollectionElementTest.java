package solutionElementTests;

import static org.junit.Assert.*;

import lsp.LSPUtils;
import lsp.LSPCarrierResource;
import lsp.usecase.UsecaseUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

import lsp.LogisticsSolutionElement;
import lsp.LSPResource;

public class CollectionElementTest {

	private LogisticsSolutionElement collectionElement;
	private LSPCarrierResource carrierAdapter;
	
	@Before
	public void initialize() {
		
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();

		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		VehicleType collectionType = vehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("CollectionVehicle"), collectionLinkId, collectionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		Carrier carrier = CarrierUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);
		
		
		Id<LSPResource> adapterId = Id.create("CollectionCarrierAdapter", LSPResource.class);
		UsecaseUtils.CollectionCarrierAdapterBuilder builder = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(adapterId, network);
		builder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		builder.setCarrier(carrier);
		builder.setLocationLinkId(collectionLinkId);
		carrierAdapter = builder.build();
		
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder collectionBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId );
		collectionBuilder.setResource(carrierAdapter);
		collectionElement = collectionBuilder.build();
	}
	
	@Test
	public void testCollectionElement() {
		assertNotNull(collectionElement.getIncomingShipments());
		assertNotNull(collectionElement.getIncomingShipments().getShipments());
		assertTrue(collectionElement.getIncomingShipments().getSortedShipments().isEmpty());
		assertNotNull(collectionElement.getInfos());
		assertTrue(collectionElement.getInfos().isEmpty());
		assertNull(collectionElement.getLogisticsSolution());
		assertNull(collectionElement.getNextElement());
		assertNotNull(collectionElement.getOutgoingShipments());
		assertNotNull(collectionElement.getOutgoingShipments().getShipments());
		assertTrue(collectionElement.getOutgoingShipments().getSortedShipments().isEmpty());
		assertNull(collectionElement.getPreviousElement());
		assertSame(collectionElement.getResource(), carrierAdapter);
		assertSame(collectionElement.getResource().getClientElements().iterator().next(), collectionElement);
	}
	
}
