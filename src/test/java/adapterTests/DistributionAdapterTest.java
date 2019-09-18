package adapterTests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

import lsp.resources.CarrierResource;
import lsp.resources.Resource;
import lsp.usecase.DistributionCarrierAdapter;
import lsp.usecase.DistributionCarrierScheduler;



public class DistributionAdapterTest {
		
		//die Trackers sind ja erst ein Bestandteil des Scheduling bzw. Replanning und kommen hier noch nicht rein.
		//Man kann sie deshalb ja extra au�erhalb des Builders einsetzen.
		
		private Network network;
		private DistributionCarrierScheduler scheduler;
		private org.matsim.vehicles.VehicleType distributionType;
		private CarrierVehicle distributionCarrierVehicle;
		private CarrierCapabilities capabilities;
		private Carrier distributionCarrier;
		private DistributionCarrierAdapter distributionAdapter;
		private Id<Link> distributionLinkId;
		
		@Before
		public void initialize() {
			Config config = new Config();
	        config.addCoreModules();
	        Scenario scenario = ScenarioUtils.createScenario(config);
	        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
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
			
			distributionLinkId = Id.createLinkId("(4 2) (4 3)");
			Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
			distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId);
			distributionCarrierVehicle.setVehicleType(distributionType);
			
			CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
			capabilitiesBuilder.addType(distributionType);
			capabilitiesBuilder.addVehicle(distributionCarrierVehicle);
			capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
			capabilities = capabilitiesBuilder.build();
			distributionCarrier = CarrierImpl.newInstance(carrierId);
			distributionCarrier.setCarrierCapabilities(capabilities);
			
			
			Id<Resource> adapterId = Id.create("DistributionCarrierAdapter", Resource.class);
			DistributionCarrierAdapter.Builder builder = DistributionCarrierAdapter.Builder.newInstance(adapterId, network);
			builder.setDistributionScheduler(scheduler);
			builder.setCarrier(distributionCarrier);
			builder.setLocationLinkId(distributionLinkId);
			distributionAdapter = builder.build();
		}


		@Test
		public void testCollectionAdapter() {
			assertTrue(distributionAdapter.getClientElements() != null);
			assertTrue(distributionAdapter.getClientElements().isEmpty());
			assertTrue(CarrierResource.class.isAssignableFrom(distributionAdapter.getClass()));
			if(CarrierResource.class.isAssignableFrom(distributionAdapter.getClass())) {
				assertTrue(Carrier.class.isAssignableFrom(distributionAdapter.getClassOfResource()));
				assertTrue(distributionAdapter.getCarrier() == distributionCarrier);
			}
			assertTrue(distributionAdapter.getEndLinkId() == distributionLinkId);
			assertTrue(distributionAdapter.getStartLinkId() == distributionLinkId);
			assertTrue(distributionAdapter.getEventHandlers() != null);
			assertTrue(distributionAdapter.getEventHandlers().isEmpty());
			assertTrue(distributionAdapter.getInfos() != null);
			assertTrue(distributionAdapter.getInfos().isEmpty());
			assertTrue(distributionAdapter.getStartLinkId() == distributionLinkId);
			if(distributionAdapter.getCarrier() == distributionCarrier) {
				assertTrue(distributionCarrier.getCarrierCapabilities() == capabilities);
				assertTrue(Carrier.class.isAssignableFrom(distributionCarrier.getClass()));
				assertTrue(distributionCarrier.getPlans().isEmpty());
				assertTrue(distributionCarrier.getSelectedPlan() == null);
				assertTrue(distributionCarrier.getServices().isEmpty());
				assertTrue(distributionCarrier.getShipments().isEmpty());
				if(distributionCarrier.getCarrierCapabilities() == capabilities) {
					assertTrue(capabilities.getFleetSize() == FleetSize.INFINITE);
					assertFalse(capabilities.getVehicleTypes().isEmpty());
					ArrayList<VehicleType> types = new ArrayList<>( capabilities.getVehicleTypes() );
					if(types.size() ==1) {
						assertTrue(types.get(0) == distributionType);
						assertTrue(distributionType.getCarrierVehicleCapacity() == 10);
						assertTrue(distributionType.getVehicleCostInformation().getPerDistanceUnit() == 0.0004);
						assertTrue(distributionType.getVehicleCostInformation().getPerTimeUnit() == 0.38);
						assertTrue(distributionType.getVehicleCostInformation().getFix() == 49);
						assertTrue(distributionType.getMaximumVelocity() == (50/3.6));
						
					}
					ArrayList<CarrierVehicle> vehicles = new ArrayList<CarrierVehicle>(capabilities.getCarrierVehicles());
					if(vehicles.size() == 1) {
						assertTrue(vehicles.get(0) == distributionCarrierVehicle);
						assertTrue(distributionCarrierVehicle.getVehicleType() == distributionType);
						assertTrue(distributionCarrierVehicle.getLocation() == distributionLinkId);
					}
				}
			}
		}


}
