package solutionElementTests;

import static org.junit.Assert.assertTrue;

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

import lsp.usecase.DistributionCarrierAdapter;
import lsp.usecase.DistributionCarrierScheduler;
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionElementImpl;
import lsp.resources.Resource;

public class DistributionElementTest {

	private Network network;
	private DistributionCarrierScheduler scheduler;
	private CarrierVehicleType distributionType;
	private CarrierVehicle carrierVehicle;
	private CarrierCapabilities capabilities;
	private Carrier carrier;
	private DistributionCarrierAdapter adapter;
	private LogisticsSolutionElement distributionElement;
	
	
	@Before
	public void initialize() {
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("input\\lsp\\network\\2regions.xml");
        this.network = scenario.getNetwork();
		
        scheduler = new DistributionCarrierScheduler();
		Id<Carrier> carrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		distributionType = vehicleTypeBuilder.build();
		
		Id<Link> distributionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("CollectionVehicle");
		carrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId);
		carrierVehicle.setVehicleType(distributionType);
		
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(distributionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		capabilities = capabilitiesBuilder.build();
		carrier = CarrierImpl.newInstance(carrierId);
		carrier.setCarrierCapabilities(capabilities);
		
		
		Id<Resource> adapterId = Id.create("DistributionCarrierAdapter", Resource.class);
		DistributionCarrierAdapter.Builder builder = DistributionCarrierAdapter.Builder.newInstance(adapterId, network);
		builder.setDistributionScheduler(scheduler);
		builder.setCarrier(carrier);
		builder.setLocationLinkId(distributionLinkId);
		adapter = builder.build();
		
		Id<LogisticsSolutionElement> elementId = Id.create("DistributionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder distributionBuilder = LogisticsSolutionElementImpl.Builder.newInstance(elementId);
		distributionBuilder.setResource(adapter);
		distributionElement = distributionBuilder.build();
	
	}
	
	@Test
	public void testDistributionElement() {
		assertTrue(distributionElement.getIncomingShipments()!= null);
		assertTrue(distributionElement.getIncomingShipments().getShipments() != null);
		assertTrue(distributionElement.getIncomingShipments().getSortedShipments().isEmpty());
		assertTrue(distributionElement.getInfos() != null);
		assertTrue(distributionElement.getInfos().isEmpty());
		assertTrue(distributionElement.getLogisticsSolution() == null);
		assertTrue(distributionElement.getNextElement() == null);
		assertTrue(distributionElement.getOutgoingShipments()!= null);
		assertTrue(distributionElement.getOutgoingShipments().getShipments() != null);
		assertTrue(distributionElement.getOutgoingShipments().getSortedShipments().isEmpty());
		assertTrue(distributionElement.getPreviousElement() == null);
		assertTrue(distributionElement.getResource() == adapter);
		assertTrue(distributionElement.getResource().getClientElements().iterator().next() == distributionElement);
	}
}
