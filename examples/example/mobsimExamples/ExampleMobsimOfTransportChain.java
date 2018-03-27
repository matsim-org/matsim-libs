package example.mobsimExamples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

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
import lsp.controler.LSPModule;
import lsp.events.EventUtils;
import lsp.replanning.LSPReplanningModuleImpl;
import lsp.resources.Resource;
import lsp.scoring.LSPScoringModuleImpl;
import lsp.shipment.AbstractShipmentPlanElement;
import lsp.shipment.AbstractShipmentPlanElementComparator;
import lsp.shipment.LSPShipment;
import lsp.shipment.LSPShipmentImpl;
import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.DeterministicShipmentAssigner;
import lsp.usecase.DistributionCarrierAdapter;
import lsp.usecase.DistributionCarrierScheduler;
import lsp.usecase.MainRunCarrierAdapter;
import lsp.usecase.MainRunCarrierScheduler;
import lsp.usecase.ReloadingPoint;
import lsp.usecase.ReloadingPointScheduler;
import lsp.usecase.SimpleForwardSolutionScheduler;

public class ExampleMobsimOfTransportChain {

public static LSP createInitialLSP(Network network) {
		
		//The Carrier for collection is created
		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
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
		
		Carrier collectionCarrier = CarrierImpl.newInstance(collectionCarrierId);
		collectionCarrier.setCarrierCapabilities(capabilities);
		
		//The collection adapter i.e. the Resource is created
		Id<Resource> adapterId = Id.create("CollectionCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder adapterBuilder = CollectionCarrierAdapter.Builder.newInstance(adapterId, network);
		
		//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		CollectionCarrierScheduler scheduler = new CollectionCarrierScheduler();
		adapterBuilder.setCollectionScheduler(scheduler);
		adapterBuilder.setCarrier(collectionCarrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		Resource collectionAdapter = adapterBuilder.build();
		
		//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder collectionElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(elementId);
		collectionElementBuilder.setResource(collectionAdapter);
		LogisticsSolutionElement collectionElement = collectionElementBuilder.build();

		
		//The first reloading adapter i.e. the Resource is created
        Id<Resource> firstReloadingId = Id.create("ReloadingPoint1", Resource.class);
        Id<Link> firstReloadingLinkId = Id.createLinkId("(4 2) (4 3)");
        ReloadingPoint.Builder firstReloadingPointBuilder = ReloadingPoint.Builder.newInstance(firstReloadingId, firstReloadingLinkId);      
        
        //The scheduler for the first reloading point is created
    	ReloadingPointScheduler.Builder firstReloadingSchedulerBuilder =  ReloadingPointScheduler.Builder.newInstance();
        firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
        firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);
        
        //The scheduler is added to the Resource and the Resource is created
        firstReloadingPointBuilder.setReloadingScheduler(firstReloadingSchedulerBuilder.build());
        Resource firstReloadingPointAdapter = firstReloadingPointBuilder.build();
        
        //The SolutionElement for the first reloading point is created
        Id<LogisticsSolutionElement> firstReloadingElementId = Id.create("FirstReloadElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder firstReloadingElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(firstReloadingElementId);
		firstReloadingElementBuilder.setResource(firstReloadingPointAdapter);
		LogisticsSolutionElement firstReloadElement = firstReloadingElementBuilder.build();
		
		
		//The Carrier for the main run Resource is created
		Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> mainRunVehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder mainRunVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(mainRunVehicleTypeId);
		mainRunVehicleTypeBuilder.setCapacity(30);
		mainRunVehicleTypeBuilder.setCostPerDistanceUnit(0.0002);
		mainRunVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		mainRunVehicleTypeBuilder.setFixCost(120);
		mainRunVehicleTypeBuilder.setMaxVelocity(50/3.6);
		CarrierVehicleType mainRunType = mainRunVehicleTypeBuilder.build();
				
		
		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId);
		mainRunCarrierVehicle.setVehicleType(mainRunType);
					
		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = mainRunCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarrierImpl.newInstance(mainRunCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);
        
		//The adapter i.e. the main run resource is created
		Id<Resource> mainRunId = Id.create("MainRunAdapter", Resource.class);
        MainRunCarrierAdapter.Builder mainRunAdapterBuilder = MainRunCarrierAdapter.Builder.newInstance(mainRunId, network);
        mainRunAdapterBuilder.setFromLinkId(Id.createLinkId("(4 2) (4 3)"));
        mainRunAdapterBuilder.setToLinkId(Id.createLinkId("(14 2) (14 3)"));
        mainRunAdapterBuilder.setCarrier(mainRunCarrier);
        
        //The scheduler for the main run Rescource is created and added to the Resource
        MainRunCarrierScheduler mainRunScheduler = new MainRunCarrierScheduler();
        mainRunAdapterBuilder.setMainRunCarrierScheduler(mainRunScheduler);
        Resource mainRunAdapter = mainRunAdapterBuilder.build();

        //The LogisticsSolutionElement for the main run Resource is created
        Id<LogisticsSolutionElement> mainRunElementId = Id.create("MainRunElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder mainRunBuilder = LogisticsSolutionElementImpl.Builder.newInstance(mainRunElementId);
		mainRunBuilder.setResource(mainRunAdapter);
		LogisticsSolutionElement mainRunElement = mainRunBuilder.build();
		
		
		//The second reloading adapter i.e. the Resource is created       
        Id<Resource> secondReloadingId = Id.create("ReloadingPoint2", Resource.class);
        Id<Link> secondReloadingLinkId = Id.createLinkId("(14 2) (14 3)");
        ReloadingPoint.Builder secondReloadingPointBuilder = ReloadingPoint.Builder.newInstance(secondReloadingId, secondReloadingLinkId);
        
        //The scheduler for the second reloading point is created
        ReloadingPointScheduler.Builder secondSchedulerBuilder =  ReloadingPointScheduler.Builder.newInstance();
        secondSchedulerBuilder.setCapacityNeedFixed(10);
        secondSchedulerBuilder.setCapacityNeedLinear(1); 
       
        //The scheduler is added to the Resource and the Resource is created
        secondReloadingPointBuilder.setReloadingScheduler(secondSchedulerBuilder.build());
        Resource secondReloadingPointAdapter = secondReloadingPointBuilder.build();
        
        //The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
        Id<LogisticsSolutionElement> secondReloadingElementId = Id.create("SecondReloadElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder secondReloadingElementBuilder = LogisticsSolutionElementImpl.Builder.newInstance(secondReloadingElementId);
		secondReloadingElementBuilder.setResource(secondReloadingPointAdapter);
		LogisticsSolutionElement secondReloadElement = secondReloadingElementBuilder.build();
		
		
		//The Carrier for distribution is created
		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder dsitributionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(distributionVehicleTypeId);
		dsitributionVehicleTypeBuilder.setCapacity(10);
		dsitributionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		dsitributionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		dsitributionVehicleTypeBuilder.setFixCost(49);
		dsitributionVehicleTypeBuilder.setMaxVelocity(50/3.6);
		CarrierVehicleType distributionType = dsitributionVehicleTypeBuilder.build();
		
		Id<Link> distributionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId);
		distributionCarrierVehicle.setVehicleType(distributionType);
			
		CarrierCapabilities.Builder distributionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		distributionCapabilitiesBuilder.addType(distributionType);
		distributionCapabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		distributionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCapabilities = distributionCapabilitiesBuilder.build();
		Carrier distributionCarrier = CarrierImpl.newInstance(distributionCarrierId);
		distributionCarrier.setCarrierCapabilities(distributionCapabilities);
		
		//The distribution adapter i.e. the Resource is created
		Id<Resource> distributionAdapterId = Id.create("DistributionCarrierAdapter", Resource.class);
		DistributionCarrierAdapter.Builder distributionAdapterBuilder = DistributionCarrierAdapter.Builder.newInstance(distributionAdapterId, network);
		distributionAdapterBuilder.setCarrier(distributionCarrier);
		distributionAdapterBuilder.setLocationLinkId(distributionLinkId);
		
		//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		DistributionCarrierScheduler distriutionScheduler = new DistributionCarrierScheduler();
		distributionAdapterBuilder.setDistributionScheduler(distriutionScheduler);
		Resource distributionAdapter = distributionAdapterBuilder.build();
		
		//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
		Id<LogisticsSolutionElement> distributionElementId = Id.create("DistributionElement", LogisticsSolutionElement.class);
		LogisticsSolutionElementImpl.Builder distributionBuilder = LogisticsSolutionElementImpl.Builder.newInstance(distributionElementId);
		distributionBuilder.setResource(distributionAdapter);
		LogisticsSolutionElement distributionElement =    distributionBuilder.build();
		
		//The Order of the logisticsSolutionElements is now specified
		collectionElement.setNextElement(firstReloadElement);
		firstReloadElement.setPreviousElement(collectionElement);
		firstReloadElement.setNextElement(mainRunElement);
		mainRunElement.setPreviousElement(firstReloadElement);
		mainRunElement.setNextElement(secondReloadElement);
		secondReloadElement.setPreviousElement(mainRunElement);
		secondReloadElement.setNextElement(distributionElement);
		distributionElement.setPreviousElement(secondReloadElement);	
		
		
		//The SolutionElements are now inserted into the only LogisticsSolution of the LSP
		Id<LogisticsSolution> solutionId = Id.create("SolutionId", LogisticsSolution.class);
		LogisticsSolutionImpl.Builder completeSolutionBuilder = LogisticsSolutionImpl.Builder.newInstance(solutionId);
		completeSolutionBuilder.addSolutionElement(collectionElement);
		completeSolutionBuilder.addSolutionElement(firstReloadElement);
		completeSolutionBuilder.addSolutionElement(mainRunElement);
		completeSolutionBuilder.addSolutionElement(secondReloadElement);
		completeSolutionBuilder.addSolutionElement(distributionElement);
		LogisticsSolution completeSolution = completeSolutionBuilder.build();
		
		
		//The initial plan of the lsp is generated and the assigner and the solution from above are added
		LSPPlanImpl completePlan = new LSPPlanImpl();
		ShipmentAssigner assigner = new DeterministicShipmentAssigner();
		completePlan.setAssigner(assigner);
		completePlan.addSolution(completeSolution);
		
		LSPImpl.Builder completeLSPBuilder = LSPImpl.Builder.getInstance();
		completeLSPBuilder.setInitialPlan(completePlan);
		Id<LSP> completeLSPId = Id.create("CollectionLSP", LSP.class);
		completeLSPBuilder.setId(completeLSPId);
		
		//The exogenous list of Resoruces for the SolutionScheduler is compiled and the Scheduler is added to the LSPBuilder 
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(collectionAdapter);
		resourcesList.add(firstReloadingPointAdapter);
		resourcesList.add(mainRunAdapter);
		resourcesList.add(secondReloadingPointAdapter);
		resourcesList.add(distributionAdapter);
		SolutionScheduler simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		completeLSPBuilder.setSolutionScheduler(simpleScheduler);
		
		return completeLSPBuilder.build();
		
	}
	
	public static Collection<LSPShipment> createInitialLSPShipments(Network network){
		ArrayList<LSPShipment> shipmentList = new ArrayList<LSPShipment>();
		ArrayList <Link> linkList = new ArrayList<Link>(network.getLinks().values());
		Random rand = new Random(1);
		 for(int i = 1; i < 6; i++) {
	        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
	        	LSPShipmentImpl.Builder builder = LSPShipmentImpl.Builder.newInstance(id);
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
	        	
	        	while(true) {
	        		Collections.shuffle(linkList, rand);
	        		Link pendingFromLink = linkList.get(0);
	        		if(pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
	        		   pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
	        		   pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
	        		   pendingFromLink.getToNode().getCoord().getY() <= 4000    ) {
	        		   builder.setFromLinkId(pendingFromLink.getId());
	        		   break;	
	        		}
	        	
	        	}
	        	
	        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setEndTimeWindow(endTimeWindow);
	        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setStartTimeWindow(startTimeWindow);
	        	builder.setServiceTime(capacityDemand * 60);
	        	LSPShipment shipment = builder.build();
        	shipmentList.add(builder.build());
		} 	
		return shipmentList;
	}

	
	public static void main (String[]args) {
		//Set up required MATSim classes
				Config config = new Config();
				config.addCoreModules();
				Scenario scenario = ScenarioUtils.createScenario(config);
				new MatsimNetworkReader(scenario.getNetwork()).readFile("input\\lsp\\network\\2regions.xml");
				Network network = scenario.getNetwork();
				        
				//Create LSP and shipments
				LSP lsp = createInitialLSP(network);
				Collection<LSPShipment> shipments =  createInitialLSPShipments(network);
				        
				//assign the shipments to the LSP
				for(LSPShipment shipment : shipments) {
				    	lsp.assignShipmentToLSP(shipment);
				}
				        
				//schedule the LSP with the shipments and according to the scheduler of the Resource
				lsp.scheduleSoultions();
			
				//set up simulation controler and LSPModule
				ArrayList<LSP> lspList = new ArrayList<LSP>();
				lspList.add(lsp);
				LSPs lsps = new LSPs(lspList);
				LSPModule module = new LSPModule(lsps, new LSPReplanningModuleImpl(lsps), new LSPScoringModuleImpl(lsps), EventUtils.getStandardEventCreators());
				
				Controler controler = new Controler(config);
				controler.addOverridingModule(module);
				config.controler().setFirstIteration(0);
				config.controler().setLastIteration(0);
				config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
				config.network().setInputFile("input\\lsp\\network\\2regions.xml");
				controler.run();
			
				for(LSPShipment shipment : lsp.getShipments()) {
					System.out.println("Shipment: " + shipment.getId());
					ArrayList<AbstractShipmentPlanElement> scheduleElements = new ArrayList<AbstractShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
					Collections.sort(scheduleElements, new AbstractShipmentPlanElementComparator());
					ArrayList<AbstractShipmentPlanElement> logElements = new ArrayList<AbstractShipmentPlanElement>(shipment.getLog().getPlanElements().values());
					Collections.sort(logElements, new AbstractShipmentPlanElementComparator());
					
					for(int i = 0; i < shipment.getSchedule().getPlanElements().size(); i++) {
						System.out.println("Scheduled: " + scheduleElements.get(i).getSolutionElement().getId() + "  " + scheduleElements.get(i).getResourceId()+ "  " + scheduleElements.get(i).getElementType() + " Start: " + scheduleElements.get(i).getStartTime() + " End: " + scheduleElements.get(i).getEndTime());
					}
					System.out.println();
					for(int i = 0; i < shipment.getLog().getPlanElements().size(); i++) {
						System.out.println("Logged: " + logElements.get(i).getSolutionElement().getId() + "  " + logElements.get(i).getResourceId() +"  " + logElements.get(i).getElementType() + " Start: " + logElements.get(i).getStartTime() + " End: " + logElements.get(i).getEndTime());
					}
					System.out.println();
				}
	}
	
	
}
