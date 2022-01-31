package example.lsp.simulationTrackers;

import java.util.*;

import lsp.*;
import lsp.replanning.LSPReplanningUtils;
import lsp.scoring.LSPScoringUtils;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.controler.LSPModule;
import org.matsim.contrib.freight.events.eventsCreator.LSPEventCreatorUtils;
import lsp.functions.LSPInfo;
import lsp.functions.LSPInfoFunctionValue;
import lsp.resources.LSPResource;
import lsp.shipment.LSPShipment;

/*package-private*/ class ExampleSimulationTrackers {

	/*package-private*/ static LSP createLSPWithTracker(Network network) {

		//The Carrier for the resource of the sole LogisticsSolutionElement of the LSP is created
		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId);
		carrierVehicle.setType( collectionType );

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();

		Carrier carrier = CarrierUtils.createCarrier( carrierId );
		carrier.setCarrierCapabilities(capabilities);

		//The Adapter i.e. the Resource is created
		Id<LSPResource> adapterId = Id.create("CollectionCarrierAdapter", LSPResource.class);
		UsecaseUtils.CollectionCarrierAdapterBuilder adapterBuilder = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(adapterId, network);

		//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		adapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		LSPResource collectionAdapter = adapterBuilder.build();

		//The adapter is now inserted into the only LogisticsSolutionElement of the only LogisticsSolution of the LSP
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder collectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId );
		collectionElementBuilder.setResource(collectionAdapter);
		LogisticsSolutionElement collectionElement = collectionElementBuilder.build();

		//The LogisticsSolutionElement is now inserted into the only LogisticsSolution of the LSP
		Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder collectionSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(collectionSolutionId );
		collectionSolutionBuilder.addSolutionElement(collectionElement);
		LogisticsSolution collectionSolution = collectionSolutionBuilder.build();

		//Create cost tracker and add it to solution
		example.lsp.simulationTrackers.LinearCostTracker tracker = new example.lsp.simulationTrackers.LinearCostTracker(0.2);
		tracker.getEventHandlers().add(new example.lsp.simulationTrackers.TourStartHandler());
		tracker.getEventHandlers().add(new example.lsp.simulationTrackers.CollectionServiceHandler());
		tracker.getEventHandlers().add(new example.lsp.simulationTrackers.DistanceAndTimeHandler(network));
		collectionSolution.addSimulationTracker(tracker);


		//The initial plan of the lsp is generated and the assigner and the solution from above are added
		LSPPlan collectionPlan = LSPUtils.createLSPPlan();
		ShipmentAssigner assigner = UsecaseUtils.createDeterministicShipmentAssigner();
		collectionPlan.setAssigner(assigner);
		collectionPlan.addSolution(collectionSolution);

		LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		collectionLSPBuilder.setInitialPlan(collectionPlan);

		//The exogenous list of Resoruces for the SolutionScheduler is compiled and the Scheduler is added to the LSPBuilder 
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionAdapter);
		SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		collectionLSPBuilder.setSolutionScheduler(simpleScheduler);

		return collectionLSPBuilder.build();

	}

	public static Collection<LSPShipment> createInitialLSPShipments(Network network){
		ArrayList<LSPShipment> shipmentList = new ArrayList<>();
		ArrayList <Link> linkList = new ArrayList<>(network.getLinks().values());

		//Create five LSPShipments that are located in the left half of the network.
		for(int i = 1; i < 6; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id );
			Random random = new Random(1);
			int capacityDemand = random.nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while(true) {
				Collections.shuffle(linkList, random);
				Link pendingFromLink = linkList.get(0);
				if(pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
						pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
						pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
						pendingFromLink.getToNode().getCoord().getY() <= 4000) {
					builder.setFromLinkId(pendingFromLink.getId());
					break;
				}
			}

			builder.setToLinkId(Id.createLinkId("(4 2) (4 3)"));
			TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
			builder.setEndTimeWindow(endTimeWindow);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
			builder.setStartTimeWindow(startTimeWindow);
			builder.setDeliveryServiceTime(capacityDemand * 60 );
			shipmentList.add(builder.build());
		}
		return shipmentList;
	}


	public static void main (String [] args) {

		//Set up required MATSim classes
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();

		//Create LSP and shipments
		LSP lsp = createLSPWithTracker(network);
		Collection<LSPShipment> shipments =  createInitialLSPShipments(network);

		//assign the shipments to the LSP
		for(LSPShipment shipment : shipments) {
			lsp.assignShipmentToLSP(shipment);
		}

		//schedule the LSP with the shipments and according to the scheduler of the Resource
		lsp.scheduleSolutions();

		//Prepare LSPModule and add the LSP
		ArrayList<LSP> lspList = new ArrayList<>();
		lspList.add(lsp);
		LSPs lsps = new LSPs(lspList);
		LSPModule module = new LSPModule(lsps, LSPReplanningUtils.createDefaultLSPReplanningModule(lsps), LSPScoringUtils.createDefaultLSPScoringModule(lsps ), LSPEventCreatorUtils.getStandardEventCreators());

		//Start the Mobsim one iteration is sufficient for tracking
		Controler controler = new Controler(config);
		controler.addOverridingModule(module);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile("scenarios/2regions/2regions-network.xml");
		controler.run();

		//Retrieve cost info from lsp
		LSPInfo costInfo;
		for(LSPInfo info : lsp.getSelectedPlan().getSolutions().iterator().next().getInfos()) {
			if( Objects.equals( info.getName(), "cost_function" ) ) {
				costInfo = info;
				for(LSPInfoFunctionValue value : costInfo.getFunction().getValues()){
					System.out.println(value.getName() + " " + value.getValue());
				}
			}
		}
	}
}
