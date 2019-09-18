package adapterTests;

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
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.resources.CarrierResource;
import lsp.resources.Resource;
import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;



public class CollectionAdapterTest {

	//die Trackers sind ja erst ein Bestandteil des Scheduling bzw. Replanning und kommen hier noch nicht rein.
	//Man kann sie deshalb ja extra au�erhalb des Builders einsetzen.
	
	private Network network;
	private CollectionCarrierScheduler scheduler;
	private org.matsim.vehicles.VehicleType collectionType;
	private CarrierVehicle collectionCarrierVehicle;
	private Carrier collectionCarrier;
	private CollectionCarrierAdapter collectionAdapter;
	private Id<Link> collectionLinkId;
	private CarrierCapabilities capabilities;
	
	@Before
	public void initialize() {
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
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
		
		collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		collectionCarrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId);
		collectionCarrierVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(collectionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		capabilities = capabilitiesBuilder.build();
		collectionCarrier = CarrierImpl.newInstance(carrierId);
		collectionCarrier.setCarrierCapabilities(capabilities);
		
		
		Id<Resource> adapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder builder = CollectionCarrierAdapter.Builder.newInstance(adapterId, network);
		builder.setCollectionScheduler(scheduler);
		builder.setCarrier(collectionCarrier);
		builder.setLocationLinkId(collectionLinkId);
		collectionAdapter = builder.build();
	}
	
	
	@Test
	public void testCollectionAdapter() {
		assertTrue(collectionAdapter.getClientElements() != null);
		assertTrue(collectionAdapter.getClientElements().isEmpty());
		assertTrue(CarrierResource.class.isAssignableFrom(collectionAdapter.getClass()));
		if(CarrierResource.class.isAssignableFrom(collectionAdapter.getClass())) {
			assertTrue(Carrier.class.isAssignableFrom(collectionAdapter.getClassOfResource()));
			assertTrue(collectionAdapter.getCarrier() == collectionCarrier);
		}
		assertTrue(collectionAdapter.getEndLinkId() == collectionLinkId);
		assertTrue(collectionAdapter.getStartLinkId() == collectionLinkId);
		assertTrue(collectionAdapter.getEventHandlers() != null);
		assertTrue(collectionAdapter.getEventHandlers().isEmpty());
		assertTrue(collectionAdapter.getInfos() != null);
		assertTrue(collectionAdapter.getInfos().isEmpty());
		assertTrue(collectionAdapter.getStartLinkId() == collectionLinkId);
		if(collectionAdapter.getCarrier() == collectionCarrier) {
			assertTrue(collectionCarrier.getCarrierCapabilities() == capabilities);
			assertTrue(Carrier.class.isAssignableFrom(collectionCarrier.getClass()));
			assertTrue(collectionCarrier.getPlans().isEmpty());
			assertTrue(collectionCarrier.getSelectedPlan() == null);
			assertTrue(collectionCarrier.getServices().isEmpty());
			assertTrue(collectionCarrier.getShipments().isEmpty());
			if(collectionCarrier.getCarrierCapabilities() == capabilities) {
				assertTrue(capabilities.getFleetSize() == FleetSize.INFINITE);
				assertFalse(capabilities.getVehicleTypes().isEmpty());
				ArrayList<VehicleType> types = new ArrayList<VehicleType>(capabilities.getVehicleTypes());
				if(types.size() ==1) {
					assertTrue(types.get(0) == collectionType);
					assertTrue(collectionType.getCarrierVehicleCapacity() == 10);
					assertTrue(collectionType.getVehicleCostInformation().getPerDistanceUnit() == 0.0004);
					assertTrue(collectionType.getVehicleCostInformation().getPerTimeUnit() == 0.38);
					assertTrue(collectionType.getVehicleCostInformation().getFix() == 49);
					assertTrue(collectionType.getMaximumVelocity() == (50/3.6));
					
				}
				ArrayList<CarrierVehicle> vehicles = new ArrayList<CarrierVehicle>(capabilities.getCarrierVehicles());
				if(vehicles.size() == 1) {
					assertTrue(vehicles.get(0) == collectionCarrierVehicle);
					assertTrue(collectionCarrierVehicle.getVehicleType() == collectionType);
					assertTrue(collectionCarrierVehicle.getLocation() == collectionLinkId);
				}
			}
		}
	}
}
