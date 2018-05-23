package lspScoringTests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

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
import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionImpl;
import lsp.functions.InfoFunctionValue;
import lsp.functions.InfoFunctionValueImpl;
import lsp.LSP;
import lsp.LSPImpl;
import lsp.LSPPlanImpl;
import lsp.LSPs;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.LogisticsSolutionElementImpl;
import lsp.LogisticsSolutionImpl;
import lsp.ShipmentAssigner;
import lsp.SolutionScheduler;
import lsp.resources.Resource;
import lsp.replanning.LSPReplanningModuleImpl;
import lsp.scoring.LSPScorer;
import lsp.scoring.LSPScoringModuleImpl;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;

public class CollectionLSPScoringTest {

	private Network network;
	private LSP collectionLSP;
	private Carrier carrier;
	private Resource collectionAdapter;
	private LogisticsSolutionElement collectionElement;
	private LSPScorer tipScorer;
	private TipSimulationTracker tipTracker;
	private TipInfo info;
	private InfoFunction function;
	private InfoFunctionValueImpl<Double> value;
	private int numberOfShipments = 25;

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
		CollectionCarrierAdapter.Builder adapterBuilder = CollectionCarrierAdapter.Builder.newInstance(adapterId,
				network);
		adapterBuilder.setCollectionScheduler(scheduler);
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		collectionAdapter = adapterBuilder.build();

		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder collectionElementBuilder = LogisticsSolutionElementImpl.Builder
				.newInstance(elementId);
		collectionElementBuilder.setResource(collectionAdapter);
		collectionElement = collectionElementBuilder.build();

		Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder collectionSolutionBuilder = LogisticsSolutionImpl.Builder
				.newInstance(collectionSolutionId);
		collectionSolutionBuilder.addSolutionElement(collectionElement);
		LogisticsSolution collectionSolution = collectionSolutionBuilder.build();

		ShipmentAssigner assigner = new DeterministicShipmentAssigner();
		LSPPlanImpl collectionPlan = new LSPPlanImpl();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addSolution(collectionSolution);

		LSPImpl.Builder collectionLSPBuilder = LSPImpl.Builder.getInstance();
		collectionLSPBuilder.setInitialPlan(collectionPlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		collectionLSPBuilder.setId(collectionLSPId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(collectionAdapter);

		SolutionScheduler simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		collectionLSPBuilder.setSolutionScheduler(simpleScheduler);
		collectionLSP = collectionLSPBuilder.build();

		TipEventHandler handler = new TipEventHandler();
		value = new InfoFunctionValueImpl<Double>("TIP IN EUR");
		function = new InfoFunctionImpl();
		function.getValues().add(value);
		info = new TipInfo(function);
		tipTracker = new TipSimulationTracker(handler, info);
		collectionAdapter.addSimulationTracker(tipTracker);
		tipScorer = new TipScorer(collectionLSP, tipTracker);
		collectionLSP.setScorer(tipScorer);

		ArrayList<Link> linkList = new ArrayList<Link>(network.getLinks().values());
		Id<Link> toLinkId = collectionLinkId;

		for (int i = 1; i < (numberOfShipments + 1); i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			LSPShipmentImpl.Builder builder = LSPShipmentImpl.Builder.newInstance(id);
			Random random = new Random(1);
			int capacityDemand = random.nextInt(10);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, random);
				Link pendingFromLink = linkList.get(0);
				if (pendingFromLink.getFromNode().getCoord().getX() <= 4000
						&& pendingFromLink.getFromNode().getCoord().getY() <= 4000
						&& pendingFromLink.getToNode().getCoord().getX() <= 4000
						&& pendingFromLink.getToNode().getCoord().getY() <= 4000) {
					builder.setFromLinkId(pendingFromLink.getId());
					break;
				}
			}

			builder.setToLinkId(toLinkId);
			TimeWindow endTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setEndTimeWindow(endTimeWindow);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
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

		LSPModule module = new LSPModule(lsps, new LSPReplanningModuleImpl(lsps), new LSPScoringModuleImpl(lsps),
				EventUtils.getStandardEventCreators());

		controler.addOverridingModule(module);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile("input/lsp/network/2regions.xml");
		controler.run();
	}

	@Test
	public void testCollectionLSPScoring() {
		System.out.println(collectionLSP.getSelectedPlan().getScore());
		assertTrue(collectionLSP.getShipments().size() == numberOfShipments);
		assertTrue(collectionLSP.getSelectedPlan().getSolutions().iterator().next().getShipments()
				.size() == numberOfShipments);
		assertTrue(collectionLSP.getSelectedPlan().getScore() > 0);
		assertTrue(collectionLSP.getSelectedPlan().getScore() <= (numberOfShipments * 5));
		/*noch zu testen
		 * tipTracker
		 * InfoFunction
		 * Info
		 * Scorer
		 */
	}

}
