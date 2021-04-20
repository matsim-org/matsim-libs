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
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.*;

/**	The LSP have to possibilities to send the goods from the first depot to the recipients:
 * A) via another hub and then distributed or
 * B) directly from the depot
 *
 * This examples bases on the Example ExampleSchedulingOfTransportChain.class
 * -- the collection Part is removed, Chain is now starting at the CollectionHub
 *
 * Scheduler = Macht die Pläne für die Fahrzeuge für die nächste MATSim-Iteration. Er plant es für jede Ressource. --> jede Ressource hatfür jede Resoource einen eigenen scehduler:
 * 1.) Simple: Nimm die mitgegebene Reihenfolge.
 * 2.)
 */
/*package-private*/ class ExampleSchedulingOfTransportChainHubsVsDirect {

	private static final Logger log = Logger.getLogger(ExampleSchedulingOfTransportChainHubsVsDirect.class );

	enum SolutionType { original, direct }
	private static SolutionType solutionType = SolutionType.direct;

	private static LSP createInitialLSP(Network network) {
		LSPResource depotResource;
		{
			log.info( "" );
			log.info( "Create depot" );

			Id<LSPResource> depotId = Id.create( "Depot", LSPResource.class );
			Id<Link> depotLinkId = Id.createLinkId( "(4 2) (4 3)" );
			UsecaseUtils.ReloadingPointBuilder firstReloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance( depotId, depotLinkId );

			//The scheduler for the first reloading point is created --> this will be the depot in this usecase
			UsecaseUtils.ReloadingPointSchedulerBuilder depotSchedulerBuilder = UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
			depotSchedulerBuilder.setCapacityNeedFixed( 10 );
			depotSchedulerBuilder.setCapacityNeedLinear( 1 );

			//The scheduler is added to the Resource and the Resource is created
			firstReloadingPointBuilder.setReloadingScheduler( depotSchedulerBuilder.build() );
			depotResource = firstReloadingPointBuilder.build();
		}
		LogisticsSolutionElement depotElement;
		{

			//The SolutionElement for the first reloading point is created
			Id<LogisticsSolutionElement> depotElementId = Id.create( "DepotElement", LogisticsSolutionElement.class );
			LSPUtils.LogisticsSolutionElementBuilder depotElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance( depotElementId );
			depotElementBuilder.setResource( depotResource );
			depotElement = depotElementBuilder.build(); //Niocht unbedingt nötig, aber nehme das alte Hub nun als Depot. Waren werden dann dort "Zusammengestellt".
			//Maybe TODO: Depot als LogisticSolutionElement raus nehmen.(?)
		}

		//The adapter i.e. the main run resource is created
		LSPResource mainRunResource;
		{
			UsecaseUtils.MainRunCarrierAdapterBuilder mainRunAdapterBuilder = UsecaseUtils.MainRunCarrierAdapterBuilder.newInstance(
					Id.create( "MainRunAdapter", LSPResource.class ), network );
			mainRunAdapterBuilder.setFromLinkId( Id.createLinkId( "(4 2) (4 3)" ) );
			mainRunAdapterBuilder.setToLinkId( Id.createLinkId( "(14 2) (14 3)" ) );

			{
				log.info( "" );
				log.info( "The Carrier for the main run is created" );
				Carrier mainRunCarrier = CarrierUtils.createCarrier( Id.create( "MainRunCarrier", Carrier.class ) );

				Id<VehicleType> mainRunVehicleTypeId = Id.create( "MainRunCarrierVehicleType", VehicleType.class );

				VehicleType mainRunVehicleType = CarrierVehicleType.Builder.newInstance( mainRunVehicleTypeId ).setCapacity( 30 ).setCostPerDistanceUnit( 0.0002 )
						.setCostPerTimeUnit( 0.38 ).setFixCost( 120 ).setMaxVelocity( 50 / 3.6 ).build();

				Id<Link> fromLinkId = Id.createLinkId( "(4 2) (4 3)" );
				Id<Vehicle> mainRunVehicleId = Id.createVehicleId( "MainRunVehicle" );
				CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance( mainRunVehicleId, fromLinkId );
				mainRunCarrierVehicle.setType( mainRunVehicleType );

				mainRunCarrier.setCarrierCapabilities(
						CarrierCapabilities.Builder.newInstance().addType( mainRunVehicleType ).addVehicle( mainRunCarrierVehicle ).setFleetSize( FleetSize.INFINITE ).build() );

				mainRunAdapterBuilder.setCarrier( mainRunCarrier );
			}

			//The scheduler for the main run Resource is created and added to the Resource
			mainRunAdapterBuilder.setMainRunCarrierScheduler( UsecaseUtils.createDefaultMainRunCarrierScheduler() );
			mainRunResource = mainRunAdapterBuilder.build();
		}

		//The LogisticsSolutionElement for the main run Resource is created
		LogisticsSolutionElement mainRunElement;
		{
			Id<LogisticsSolutionElement> mainRunElementId = Id.create( "MainRunElement", LogisticsSolutionElement.class );
			LSPUtils.LogisticsSolutionElementBuilder mainRunBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance( mainRunElementId );
			mainRunBuilder.setResource( mainRunResource );
			mainRunElement = mainRunBuilder.build();
		}

		LogisticsSolutionElement secondReloadElement;
		LSPResource secondReloadingPointResource;
		{
			log.info( "" );
			log.info( "The second reloading adapter i.e. the Resource is created" );
			//The second reloading adapter i.e. the Resource is created
			Id<LSPResource> secondReloadingId = Id.create( "ReloadingPoint2", LSPResource.class );
			Id<Link> secondReloadingLinkId = Id.createLinkId( "(14 2) (14 3)" );
			UsecaseUtils.ReloadingPointBuilder secondReloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance( secondReloadingId, secondReloadingLinkId );

			//The scheduler for the second reloading point is created
			UsecaseUtils.ReloadingPointSchedulerBuilder secondSchedulerBuilder = UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
			secondSchedulerBuilder.setCapacityNeedFixed( 10 );
			secondSchedulerBuilder.setCapacityNeedLinear( 1 );

			//The scheduler is added to the Resource and the Resource is created
			secondReloadingPointBuilder.setReloadingScheduler( secondSchedulerBuilder.build() );
			secondReloadingPointResource = secondReloadingPointBuilder.build();

			//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
			Id<LogisticsSolutionElement> secondReloadingElementId = Id.create( "SecondReloadElement", LogisticsSolutionElement.class );
			LSPUtils.LogisticsSolutionElementBuilder secondReloadingElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance( secondReloadingElementId );
			secondReloadingElementBuilder.setResource( secondReloadingPointResource );
			secondReloadElement = secondReloadingElementBuilder.build();
		}


		LogisticsSolutionElement distributionElement;
		LSPResource distributionResource;
		{
			//The Carrier for distribution from reloading Point is created
			Id<Carrier> distributionCarrierId = Id.create( "DistributionCarrier", Carrier.class );
			VehicleType distributionVehicleType = CarrierVehicleType.Builder.newInstance( Id.create( "DistributionCarrierVehicleType", VehicleType.class ) ).setCapacity( 10 )
					.setCostPerDistanceUnit( 0.0004 ).setCostPerTimeUnit( 0.38 ).setFixCost( 49 ).setMaxVelocity( 50 / 3.6 ).build();

			CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance( Id.createVehicleId( "DistributionVehicle" ), Id.createLinkId( "(14 2) (14 3)" ) );
			distributionCarrierVehicle.setType( distributionVehicleType );

			Carrier distributionCarrier = CarrierUtils.createCarrier( distributionCarrierId );
			distributionCarrier.setCarrierCapabilities(
					CarrierCapabilities.Builder.newInstance().addType( distributionVehicleType ).addVehicle( distributionCarrierVehicle ).setFleetSize( FleetSize.INFINITE ).build() );

			//The distribution adapter i.e. the Resource is created
			distributionResource = UsecaseUtils.DistributionCarrierAdapterBuilder.newInstance(
					Id.create("DistributionCarrierAdapter", LSPResource.class ), network ).setCarrier( distributionCarrier ).setLocationLinkId( Id.createLinkId( "(14 2) (14 3)" ) )
					.setDistributionScheduler( UsecaseUtils.createDefaultDistributionCarrierScheduler() ).build();
			// (The scheduler is where jsprit comes into play.)

			//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP

			distributionElement = LSPUtils.LogisticsSolutionElementBuilder.newInstance(
					Id.create("DistributionElement", LogisticsSolutionElement.class ) ).setResource(distributionResource ).build();
		}


		//### New (KMT): Carrier for direct distribution from Depot (without 2nd reloading Point)
		//The Carrier for distribution from reloading Point is created
		Id<Carrier> directDistributionCarrierId = Id.create("DirectDistributionCarrier", Carrier.class);
		VehicleType directDistributionVehicleType = CarrierVehicleType.Builder.newInstance(
				Id.create("DirectDistributionCarrierVehicleType", VehicleType.class ) ).setCapacity(10 ).setCostPerDistanceUnit(0.0004 )
				.setCostPerTimeUnit(0.38 ).setFixCost(49 ).setMaxVelocity(50/3.6 ).build();

		CarrierVehicle directDistributionCarrierVehicle = CarrierVehicle.newInstance( Id.createVehicleId("DirectDistributionVehicle" ), Id.createLinkId("(4 2) (4 3)" ) );
		directDistributionCarrierVehicle.setType( directDistributionVehicleType );

		CarrierCapabilities directDistributionCarrierCapabilities = CarrierCapabilities.Builder.newInstance().addType(directDistributionVehicleType )
				.addVehicle(directDistributionCarrierVehicle ).setFleetSize(FleetSize.INFINITE ).build();
		Carrier directDistributionCarrier = CarrierUtils.createCarrier( directDistributionCarrierId );
		directDistributionCarrier.setCarrierCapabilities(directDistributionCarrierCapabilities);

		//The distribution adapter i.e. the Resource is created
		LSPResource directDistributionAdapter = UsecaseUtils.DistributionCarrierAdapterBuilder.newInstance(
				Id.create("DirectDistributionCarrierAdapter", LSPResource.class ), network ).setCarrier(directDistributionCarrier ).setLocationLinkId( Id.createLinkId("(4 2) (4 3)" ) )
				.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler() ).build();
		// (The scheduler is where jsprit comes into play.)

		//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
		LogisticsSolutionElement directDistributionElement =    LSPUtils.LogisticsSolutionElementBuilder.newInstance(
				Id.create("DirectDistributionElement", LogisticsSolutionElement.class ) ).setResource(directDistributionAdapter ).build();

		//### end new

		//TODO: Beide Lösungen anbieten und "bessere" oder zunächst "eine" auswählen"

		//TODO: Für die Auswahl "CostInfo an die Solutions dran heften.

//		log.info("");
//		log.info("(existing) logistic Solution - via reloadingPoint2 is created");
//
//		log.info("");
//		log.info("The Order of the logisticsSolutionElements is now specified");
//		//The Order of the logisticsSolutionElements is now specified
//		firstReloadElement.setNextElement(mainRunElement);
//		mainRunElement.setPreviousElement(firstReloadElement);
//		mainRunElement.setNextElement(secondReloadElement);
//		secondReloadElement.setPreviousElement(mainRunElement);
//		secondReloadElement.setNextElement(directDistributionElement);
//		directDistributionElement.setPreviousElement(secondReloadElement);


		//The SolutionElements are now inserted into the only LogisticsSolution of the LSP
		//Die Reihenfolge des Hinzufügens ist egal, da weiter oben die jeweils direkten Vorgänger/Nachfolger bestimmt wurden.

		switch (solutionType) {
			case original: {
				// ### This is the original solution with mainRun - ReloadingPoint - distributionRun

				log.info("");
				log.info("set up logistic Solution - original solution is created");

				LogisticsSolution completeSolutionWithReloading = LSPUtils.LogisticsSolutionBuilder.newInstance(
						Id.create("SolutionWithReloadingId", LogisticsSolution.class)).addSolutionElement(depotElement).addSolutionElement(mainRunElement).addSolutionElement(secondReloadElement).addSolutionElement(distributionElement).build();


				log.info("");
				log.info("The initial plan of the lsp is generated and the assigner and the solution from above are added");

				LSPPlan completePlan = LSPUtils.createLSPPlan().setAssigner(UsecaseUtils.createDeterministicShipmentAssigner()).addSolution(completeSolutionWithReloading);

				log.info("");
				log.info("The exogenous list of Resources for the SolutionScheduler is compiled and the Scheduler is added to the LSPBuilder");

				List<LSPResource> resourcesList = new ArrayList<>(Arrays.asList(depotResource, mainRunResource, secondReloadingPointResource, distributionResource));

				SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);

				return LSPUtils.LSPBuilder.getInstance().setInitialPlan(completePlan).setId(Id.create("CollectionLSP", LSP.class)).setSolutionScheduler(simpleScheduler).build();

			}
			case direct: {
				// ### This is the new solution with with directDistribution from the Depot.

				log.info("");
				log.info("The order of the logisticsSolutionElements is now specified");
				depotElement.setNextElement(directDistributionElement);
				directDistributionElement.setPreviousElement(depotElement);

				//TODO WIP: KostenInfo an das Element dran hängen.
				//		LSPInfo costInfo = SimulationTrackersUtils.createDefaultCostInfo();
				//		SimulationTrackersUtils.getFixedCostFunctionValue(costInfo.getFunction());
				//		directDistributionElement.getInfos().add(costInfo);

				log.info("");
				log.info("set up logistic Solution - direct distribution from the depot is created");

				LogisticsSolution completeSolutionDirect = LSPUtils.LogisticsSolutionBuilder.newInstance(
						Id.create("SolutionDirectId", LogisticsSolution.class)).addSolutionElement(depotElement).addSolutionElement(directDistributionElement).build();

				log.info("");
				log.info("The initial plan of the lsp is generated and the assigner and the solution from above are added");

				LSPPlan completePlan = LSPUtils.createLSPPlan().setAssigner(UsecaseUtils.createDeterministicShipmentAssigner()).addSolution(completeSolutionDirect);

				log.info("");
				log.info("The exogenous list of Resources for the SolutionScheduler is compiled and the Scheduler is added to the LSPBuilder");

				ArrayList<LSPResource> resourcesList = new ArrayList<>(Arrays.asList(depotResource, directDistributionAdapter));
				// TODO: Wenn hier der "falsche" distributionAdapter, dann läuft es dennoch durch, auch wenn es keine Lösung geben kann. Zumindest wird es im Output nicht angezeigt.

				//			SolutionScheduler simpleScheduler = LSPUtils.createForwardSolutionScheduler();
				SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);

				return LSPUtils.LSPBuilder.getInstance().setInitialPlan(completePlan).setId(Id.create("MyLSP", LSP.class)).setSolutionScheduler(simpleScheduler).build();
			}
			default:
				throw new IllegalStateException("Unexpected value: " + solutionType);
		}

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


	public static void main (String [] args) throws CommandLine.ConfigurationException{

		for( String arg : args ){
			log.warn(arg);
		}

		CommandLine cmd = new CommandLine.Builder( args )
				.allowAnyOption( true )
				.build();

		ExampleSchedulingOfTransportChainHubsVsDirect.solutionType = SolutionType.valueOf( cmd.getOption( "solutionType" ).get() ) ;
		log.warn( "solutionType=" + ExampleSchedulingOfTransportChainHubsVsDirect.solutionType );

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
			ArrayList<ShipmentPlanElement> elementList = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
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
