package solutionTests;

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

import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionElementImpl;
import lsp.LogisticsSolutionImpl;
import lsp.resources.Resource;

public class CollectionSolutionTest {

	private Network network;
	private Carrier carrier;
	private CollectionCarrierAdapter adapter;
	private LogisticsSolutionElement collectionElement; 
	private LogisticsSolution collectionSolution;
	
	@Before
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("input/lsp/network/2regions.xml");
		this.network = scenario.getNetwork();

		CollectionCarrierScheduler scheduler = new CollectionCarrierScheduler();
		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50 / 3.6);
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
		carrier = CarrierImpl.newInstance(carrierId);
		carrier.setCarrierCapabilities(capabilities);

		Id<Resource> adapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder adapterBuilder = CollectionCarrierAdapter.Builder.newInstance(adapterId,
				network);
		adapterBuilder.setCollectionScheduler(scheduler);
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		adapter = adapterBuilder.build();
		
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder collectionElementBuilder = LogisticsSolutionElementImpl.Builder
				.newInstance(elementId);
		collectionElementBuilder.setResource(adapter);
				collectionElement = collectionElementBuilder.build();

		Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder collectionSolutionBuilder = LogisticsSolutionImpl.Builder
				.newInstance(collectionSolutionId);
		collectionSolutionBuilder.addSolutionElement(collectionElement);
		collectionSolution = collectionSolutionBuilder.build();

	}

	@Test
	public void testCollectionSolution() {
		assertTrue(collectionSolution.getEventHandlers() != null);
		assertTrue(collectionSolution.getEventHandlers().isEmpty());
		assertTrue(collectionSolution.getInfos() != null);
		assertTrue(collectionSolution.getInfos().isEmpty());
		assertTrue(collectionSolution.getLSP() == null);
		assertTrue(collectionSolution.getShipments() != null);
		assertTrue(collectionSolution.getShipments().isEmpty());
		assertTrue(collectionSolution.getSolutionElements().size() == 1);
		ArrayList<LogisticsSolutionElement> elements = new ArrayList<LogisticsSolutionElement>(collectionSolution.getSolutionElements());
		for(LogisticsSolutionElement element : elements) {
			if(elements.indexOf(element) == 0) {
				assertTrue(element.getPreviousElement() == null);
			}
			if(elements.indexOf(element) == (elements.size() -1)) {
				assertTrue(element.getNextElement() == null);
			}
			assertTrue(element.getLogisticsSolution() == collectionSolution);
		}	
	}
	
}
