package example.lsp.initialPlans;

import lsp.*;
import lsp.resources.LSPResource;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentPlanElementComparator;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.UsecaseUtils;
import org.apache.log4j.Logger;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

/**	The LSP have to possibilities to send the goods from the first depot to the recipients:
 * A) via another hub and then distributed or
 * B) directly from the depot
 *
 * This examples bases on the Example ExampleSchedulingOfTransportChain.class
 * -- the collection Part is removed, Chain is now starting at the CollectionHub
 */

/*package-private*/ class ExampleSchedulingOfTransportChainHubsVsDirect {

	static Logger log = Logger.getLogger(ExampleSchedulingOfTransportChainHubsVsDirect.class);

	private static LSP createInitialLSP(Network network) {

		log.info("");
		log.info("The first reloading adapter i.e. the Resource is created");
		//The first reloading adapter i.e. the Resource is created
        Id<LSPResource> depotId = Id.create("Depot", LSPResource.class);
        Id<Link> depotLinkId = Id.createLinkId("(4 2) (4 3)");
        UsecaseUtils.ReloadingPointBuilder firstReloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(depotId, depotLinkId);

        //The scheduler for the first reloading point is created
    	UsecaseUtils.ReloadingPointSchedulerBuilder firstReloadingSchedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
        firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);

        //The scheduler is added to the Resource and the Resource is created
        firstReloadingPointBuilder.setReloadingScheduler(firstReloadingSchedulerBuilder.build());
        LSPResource firstReloadingPointAdapter = firstReloadingPointBuilder.build();

        //The SolutionElement for the first reloading point is created
        Id<LogisticsSolutionElement> DepotElementId = Id.create("DepotElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder DepotElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(DepotElementId );
		DepotElementBuilder.setResource(firstReloadingPointAdapter);
		LogisticsSolutionElement firstReloadElement = DepotElementBuilder.build();


		log.info("");
		log.info("The Carrier for the main run Resource is created");
		//The Carrier for the main run Resource is created
		Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> mainRunVehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder mainRunVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(mainRunVehicleTypeId);
		mainRunVehicleTypeBuilder.setCapacity(30);
		mainRunVehicleTypeBuilder.setCostPerDistanceUnit(0.0002);
		mainRunVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		mainRunVehicleTypeBuilder.setFixCost(120);
		mainRunVehicleTypeBuilder.setMaxVelocity(50/3.6);
		VehicleType mainRunType = mainRunVehicleTypeBuilder.build();


		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId);
		mainRunCarrierVehicle.setType( mainRunType );

		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = mainRunCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarrierUtils.createCarrier( mainRunCarrierId );
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);

		//The adapter i.e. the main run resource is created
		Id<LSPResource> mainRunId = Id.create("MainRunAdapter", LSPResource.class);
        UsecaseUtils.MainRunCarrierAdapterBuilder mainRunAdapterBuilder = UsecaseUtils.MainRunCarrierAdapterBuilder.newInstance(mainRunId, network);
        mainRunAdapterBuilder.setFromLinkId(Id.createLinkId("(4 2) (4 3)"));
        mainRunAdapterBuilder.setToLinkId(Id.createLinkId("(14 2) (14 3)"));
        mainRunAdapterBuilder.setCarrier(mainRunCarrier);

        //The scheduler for the main run Resource is created and added to the Resource
		mainRunAdapterBuilder.setMainRunCarrierScheduler(UsecaseUtils.createDefaultMainRunCarrierScheduler());
        LSPResource mainRunAdapter = mainRunAdapterBuilder.build();

        //The LogisticsSolutionElement for the main run Resource is created
        Id<LogisticsSolutionElement> mainRunElementId = Id.create("MainRunElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder mainRunBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(mainRunElementId );
		mainRunBuilder.setResource(mainRunAdapter);
		LogisticsSolutionElement mainRunElement = mainRunBuilder.build();


		log.info("");
		log.info("The second reloading adapter i.e. the Resource is created");
		//The second reloading adapter i.e. the Resource is created
        Id<LSPResource> secondReloadingId = Id.create("ReloadingPoint2", LSPResource.class);
        Id<Link> secondReloadingLinkId = Id.createLinkId("(14 2) (14 3)");
        UsecaseUtils.ReloadingPointBuilder secondReloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(secondReloadingId, secondReloadingLinkId);

        //The scheduler for the second reloading point is created
        UsecaseUtils.ReloadingPointSchedulerBuilder secondSchedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        secondSchedulerBuilder.setCapacityNeedFixed(10);
        secondSchedulerBuilder.setCapacityNeedLinear(1);

        //The scheduler is added to the Resource and the Resource is created
        secondReloadingPointBuilder.setReloadingScheduler(secondSchedulerBuilder.build());
        LSPResource secondReloadingPointAdapter = secondReloadingPointBuilder.build();

        //The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
        Id<LogisticsSolutionElement> secondReloadingElementId = Id.create("SecondReloadElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder secondReloadingElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(secondReloadingElementId );
		secondReloadingElementBuilder.setResource(secondReloadingPointAdapter);
		LogisticsSolutionElement secondReloadElement = secondReloadingElementBuilder.build();


		//The Carrier for distribution from reloading Point is created
		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder distributionCarrierVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(distributionVehicleTypeId);
		distributionCarrierVehicleTypeBuilder.setCapacity(10);
		distributionCarrierVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		distributionCarrierVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		distributionCarrierVehicleTypeBuilder.setFixCost(49);
		distributionCarrierVehicleTypeBuilder.setMaxVelocity(50/3.6);
		VehicleType distributionVehicleType = distributionCarrierVehicleTypeBuilder.build();
		
		Id<Link> distributionCarrierVehicleLinkId = Id.createLinkId("(14 2) (14 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionCarrierVehicleLinkId);
		distributionCarrierVehicle.setType( distributionVehicleType );

		CarrierCapabilities.Builder distributionCarrierCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		distributionCarrierCapabilitiesBuilder.addType(distributionVehicleType);
		distributionCarrierCapabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		distributionCarrierCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCarrierCapabilities = distributionCarrierCapabilitiesBuilder.build();
		Carrier distributionCarrier = CarrierUtils.createCarrier( distributionCarrierId );
		distributionCarrier.setCarrierCapabilities(distributionCarrierCapabilities);
		
		//The distribution adapter i.e. the Resource is created
		Id<LSPResource> distributionAdapterId = Id.create("DistributionCarrierAdapter", LSPResource.class);
		UsecaseUtils.DistributionCarrierAdapterBuilder distributionAdapterBuilder = UsecaseUtils.DistributionCarrierAdapterBuilder.newInstance(distributionAdapterId, network);
		distributionAdapterBuilder.setCarrier(distributionCarrier);
		distributionAdapterBuilder.setLocationLinkId(distributionCarrierVehicleLinkId);
		
		//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		distributionAdapterBuilder.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler());
		LSPResource distributionAdapter = distributionAdapterBuilder.build();
		
		//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
		Id<LogisticsSolutionElement> distributionElementId = Id.create("DistributionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder distributionBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(distributionElementId );
		distributionBuilder.setResource(distributionAdapter);
		LogisticsSolutionElement distributionElement =    distributionBuilder.build();


		//### New (KMT): Carrier for direct distribution from Depot (without 2nd reloading Point)
		//The Carrier for distribution from reloading Point is created
		Id<Carrier> directDistributionCarrierId = Id.create("DirectDistributionCarrier", Carrier.class);
		Id<VehicleType> directDistributionVehicleTypeId = Id.create("DirectDistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder directDistributionCarrierVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(directDistributionVehicleTypeId);
		directDistributionCarrierVehicleTypeBuilder.setCapacity(10);
		directDistributionCarrierVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		directDistributionCarrierVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		directDistributionCarrierVehicleTypeBuilder.setFixCost(49);
		directDistributionCarrierVehicleTypeBuilder.setMaxVelocity(50/3.6);
		VehicleType directDistributionVehicleType = directDistributionCarrierVehicleTypeBuilder.build();

		Id<Link> directDistributionCarrierVehicleLinkId = Id.createLinkId("(4 2) (4 3)");			//= location of the depot.
		Id<Vehicle> directDistributionVehicleId = Id.createVehicleId("DirectDistributionVehicle");
		CarrierVehicle directDistributionCarrierVehicle = CarrierVehicle.newInstance(directDistributionVehicleId, directDistributionCarrierVehicleLinkId);
		directDistributionCarrierVehicle.setType( directDistributionVehicleType );

		CarrierCapabilities.Builder directDistributionCarrierCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		directDistributionCarrierCapabilitiesBuilder.addType(directDistributionVehicleType);
		directDistributionCarrierCapabilitiesBuilder.addVehicle(directDistributionCarrierVehicle);
		directDistributionCarrierCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities directDistributionCarrierCapabilities = directDistributionCarrierCapabilitiesBuilder.build();
		Carrier directDistributionCarrier = CarrierUtils.createCarrier( directDistributionCarrierId );
		directDistributionCarrier.setCarrierCapabilities(directDistributionCarrierCapabilities);

		//The distribution adapter i.e. the Resource is created
		Id<LSPResource> directDistributionAdapterId = Id.create("DirectDistributionCarrierAdapter", LSPResource.class);
		UsecaseUtils.DistributionCarrierAdapterBuilder directDistributionAdapterBuilder = UsecaseUtils.DistributionCarrierAdapterBuilder.newInstance(directDistributionAdapterId, network);
		directDistributionAdapterBuilder.setCarrier(directDistributionCarrier);
		directDistributionAdapterBuilder.setLocationLinkId(directDistributionCarrierVehicleLinkId);

		//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		directDistributionAdapterBuilder.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler());
		LSPResource directDistributionAdapter = directDistributionAdapterBuilder.build();

		//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
		Id<LogisticsSolutionElement> directDistributionElementId = Id.create("DirectDistributionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder directDistributionBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(directDistributionElementId );
		directDistributionBuilder.setResource(directDistributionAdapter);
		LogisticsSolutionElement directDistributionElement =    directDistributionBuilder.build();

		//### end new

		log.info("");
		log.info("(existing) locisitc Solution - via relaoding Point2 is created");

		log.info("");
		log.info("The Order of the logisticsSolutionElements is now specified");
		//The Order of the logisticsSolutionElements is now specified
		firstReloadElement.setNextElement(mainRunElement);
		mainRunElement.setPreviousElement(firstReloadElement);
		mainRunElement.setNextElement(secondReloadElement);
		secondReloadElement.setPreviousElement(mainRunElement);
		secondReloadElement.setNextElement(directDistributionElement);
		directDistributionElement.setPreviousElement(secondReloadElement);
		
		
		//The SolutionElements are now inserted into the only LogisticsSolution of the LSP
		Id<LogisticsSolution> solutionId = Id.create("SolutionId", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder completeSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(solutionId );
		completeSolutionBuilder.addSolutionElement(firstReloadElement);
		completeSolutionBuilder.addSolutionElement(mainRunElement);
		completeSolutionBuilder.addSolutionElement(secondReloadElement);
		completeSolutionBuilder.addSolutionElement(directDistributionElement);
		LogisticsSolution completeSolution = completeSolutionBuilder.build();

		//TODO: Create Logistic solution with direct delivery

		//TODO: Beide Lösungen anbieten und "bessere" oder zunächst "eine" auswählen"

		//TODO: Für die Auswahl "CostInfo an die Solutions dran heften.

		log.info("");
		log.info("The initial plan of the lsp is generated and the assigner and the solution from above are added");
		//The initial plan of the lsp is generated and the assigner and the solution from above are added
		LSPPlan completePlan = LSPUtils.createLSPPlan();
		ShipmentAssigner assigner = UsecaseUtils.createDeterministicShipmentAssigner();
		completePlan.setAssigner(assigner);
		completePlan.addSolution(completeSolution);
		
		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance();
		completeLSPBuilder.setInitialPlan(completePlan);
		Id<LSP> completeLSPId = Id.create("CollectionLSP", LSP.class);
		completeLSPBuilder.setId(completeLSPId);

		log.info("");
		log.info("The exogenous list of Resoruces for the SolutionScheduler is compiled and the Scheduler is added to the LSPBuilder");
		//The exogenous list of Resoruces for the SolutionScheduler is compiled and the Scheduler is added to the LSPBuilder 
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(firstReloadingPointAdapter);
		resourcesList.add(mainRunAdapter);
		resourcesList.add(secondReloadingPointAdapter);
		resourcesList.add(directDistributionAdapter);
		SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		completeLSPBuilder.setSolutionScheduler(simpleScheduler);
		
		return completeLSPBuilder.build();
		
	}
	
	private static Collection<LSPShipment> createInitialLSPShipments(Network network){
		ArrayList<LSPShipment> shipmentList = new ArrayList<>();
		ArrayList <Link> linkList = new ArrayList<>(network.getLinks().values());
		Random rand = new Random(1);
		 for(int i = 1; i < 6; i++) {
	        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
	        	ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id );
	        	int capacityDemand = rand.nextInt(10);
	        	builder.setCapacityDemand(capacityDemand);
	        	
	        	while(true) {
	        		Collections.shuffle(linkList, rand);
	        		Link pendingToLink = linkList.get(0);
	        		if((pendingToLink.getFromNode().getCoord().getX() <= 18000 &&
	        			pendingToLink.getFromNode().getCoord().getY() <= 4000 &&
	        			pendingToLink.getFromNode().getCoord().getX() >= 14000 &&       			
	        			pendingToLink.getToNode().getCoord().getX() <= 18000 &&
	        			pendingToLink.getToNode().getCoord().getY() <= 4000  &&
	        			pendingToLink.getToNode().getCoord().getX() >= 14000	)) {
	        		   builder.setToLinkId(pendingToLink.getId());
	        		   break;	
	        		}
	        	
	        	}

			 	builder.setFromLinkId(Id.createLinkId("(4 2) (4 3)")); //Here was the "first" reloading Point, now called depot.
	        	
	        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setEndTimeWindow(endTimeWindow);
	        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setStartTimeWindow(startTimeWindow);
	        	builder.setServiceTime(capacityDemand * 60);
	        	LSPShipment shipment = builder.build();
        	shipmentList.add(shipment);
		} 	
		return shipmentList;
	}


	public static void main (String [] args) {

		log.info("Starting ...");
		log.info("Set up required MATSim classes");

		//Set up required MATSim classes
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();


		 //Create LSP and shipments
		log.info("create LSP");
        LSP lsp = createInitialLSP(network);
		log.info("create initial LSPShipments");
        Collection<LSPShipment> shipments =  createInitialLSPShipments(network);

        //assign the shipments to the LSP
		log.info("assign the shipments to the LSP");
        for(LSPShipment shipment : shipments) {
        	lsp.assignShipmentToLSP(shipment);
        }
        
        //schedule the LSP with the shipments and according to the scheduler of the Resource
		log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
        lsp.scheduleSoultions();
        
     	 //print the schedules for the assigned LSPShipments
		log.info("print the schedules for the assigned LSPShipments");
        for(LSPShipment shipment : lsp.getShipments()) {
			ArrayList<ShipmentPlanElement> elementList = new ArrayList<>(shipment.getSchedule().getPlanElements().values());
			elementList.sort(new ShipmentPlanElementComparator());
			System.out.println("Shipment: " + shipment.getId());
			for(ShipmentPlanElement element : elementList) {
				System.out.println(element.getSolutionElement().getId() + "\t\t" + element.getResourceId() + "\t\t" + element.getElementType() + "\t\t" + element.getStartTime() + "\t\t" + element.getEndTime());	
			}			
			System.out.println();
		}
		
		log.info("Done.");
	
	}
	
}
