package solutionElementTests;

import static org.junit.Assert.*;

import lsp.LSPUtils;
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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.LogisticsSolutionElement;
import lsp.resources.LSPResource;

public class MainRunElementTest {

	private LSPResource mainRunAdapter;
	private LogisticsSolutionElement mainRunElement;

	@Before
	public void initialize() {
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();
	
       
        Id<Carrier> carrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(30);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0002);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(120);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType mainRunType = vehicleTypeBuilder.build();
				
		
		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, fromLinkId, mainRunType);


		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(mainRunType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		Carrier carrier = CarrierUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);


		Id<LSPResource> mainRunId = Id.create("MainRunAdapter", LSPResource.class);
        UsecaseUtils.MainRunCarrierAdapterBuilder mainRunAdapterBuilder = UsecaseUtils.MainRunCarrierAdapterBuilder.newInstance(mainRunId, network);
        mainRunAdapterBuilder.setMainRunCarrierScheduler(UsecaseUtils.createDefaultMainRunCarrierScheduler());
        mainRunAdapterBuilder.setFromLinkId(Id.createLinkId("(4 2) (4 3)"));
        mainRunAdapterBuilder.setToLinkId(Id.createLinkId("(14 2) (14 3)"));
        mainRunAdapterBuilder.setCarrier(carrier);
        mainRunAdapter = mainRunAdapterBuilder.build();
	
        Id<LogisticsSolutionElement> elementId = Id.create("MainRunElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder mainRunBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId );
		mainRunBuilder.setResource( mainRunAdapter);
		mainRunElement = mainRunBuilder.build();
	
	}

	@Test
	public void testDistributionElement() {
		assertNotNull(mainRunElement.getIncomingShipments());
		assertNotNull(mainRunElement.getIncomingShipments().getShipments());
		assertTrue(mainRunElement.getIncomingShipments().getSortedShipments().isEmpty());
		assertNotNull(mainRunElement.getInfos());
		assertTrue(mainRunElement.getInfos().isEmpty());
		assertNull(mainRunElement.getLogisticsSolution());
		assertNull(mainRunElement.getNextElement());
		assertNotNull(mainRunElement.getOutgoingShipments());
		assertNotNull(mainRunElement.getOutgoingShipments().getShipments());
		assertTrue(mainRunElement.getOutgoingShipments().getSortedShipments().isEmpty());
		assertNull(mainRunElement.getPreviousElement());
		assertSame(mainRunElement.getResource(), mainRunAdapter);
		assertSame(mainRunElement.getResource().getClientElements().iterator().next(), mainRunElement);
	}


}
