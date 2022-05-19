/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package example.lsp.initialPlans;

import lsp.*;
import lsp.controler.LSPModule;
import lsp.replanning.LSPReplanningModule;
import lsp.replanning.LSPReplanningModuleImpl;
import lsp.replanning.LSPReplanningUtils;
import lsp.LSPResource;
import lsp.LSPResourceScheduler;
import lsp.scoring.LSPScoringModule;
import lsp.scoring.LSPScoringModuleImpl;
import lsp.scoring.LSPScoringUtils;
import lsp.shipment.*;
import lsp.usecase.UsecaseUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.events.eventsCreator.LSPEventCreatorUtils;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**	The LSP have to possibilities to send the goods from the first depot to the recipients:
 * A) via another hub and then distributed or
 * B) directly from the depot
 *
 * This examples bases on the Example ExampleSchedulingOfTransportChain.class
 * -- the collection Part is removed, Chain is now starting at the CollectionHub
 *
 * Scheduler = Macht die Pläne für die Fahrzeuge für die nächste MATSim-Iteration. Er plant es für jede Ressource. --> jede Ressource hat einen eigenen Scheduler:
 * 1.) Simple: Nimm die mitgegebene Reihenfolge.
 * 2.)
 */
/*package-private*/ class ExampleSchedulingOfTransportChainHubsVsDirect {

	private static final Logger log = Logger.getLogger(ExampleSchedulingOfTransportChainHubsVsDirect.class );

	enum SolutionType {onePlan_withHub, onePlan_direct, twoPlans_directAndHub }
	private static SolutionType solutionType = SolutionType.onePlan_direct;
	// yyyy please avoid static nonfinal variables ... they are dangerous.  kai, may'22

	public static void main (String [] args) throws CommandLine.ConfigurationException{

		for( String arg : args ){
			log.warn(arg);
		}

		//Set up required MATSim classes

		Config config = ConfigUtils.createConfig();
		if (args.length != 0) {
			for (String arg : args) {
				log.warn(arg);
			}
//			config = ConfigUtils.loadConfig(args);
			ConfigUtils.applyCommandline(config, args);

			CommandLine cmd = ConfigUtils.getCommandLine(args);
			ExampleSchedulingOfTransportChainHubsVsDirect.solutionType = SolutionType.valueOf(cmd.getOption("solutionType").orElseThrow());

		} else {
			config.controler().setOutputDirectory("output/ChainVsDirect/" + solutionType);
			config.controler().setLastIteration(2);

		}
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		log.warn( "solutionType=" + ExampleSchedulingOfTransportChainHubsVsDirect.solutionType );

		log.info("Starting ...");
		log.info("Set up required MATSim classes");

		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();

		//########

		log.info("create LSP");
		LSP lsp = createInitialLSP(network);

		log.info("create initial LSPShipments");
		Collection<LSPShipment> shipments =  createInitialLSPShipments(network);

		log.info("assign the shipments to the LSP");
		for(LSPShipment shipment : shipments) {
			lsp.assignShipmentToLSP(shipment);
		}

		log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
		lsp.scheduleSolutions();

		log.info("Set up simulation controler and LSPModule");
		LinkedHashSet<LSP> lspList = new LinkedHashSet<>();
		lspList.add(lsp);
		LSPs lsps = new LSPs(lspList);
		LSPUtils.addLSPs( scenario, lsps );

		// @KMT: LSPModule ist vom Design her nur im Zusammenhang mit dem Controler sinnvoll.  Damit kann man dann auch vollständig auf
		// Injection setzen.

		Controler controler = new Controler(scenario);
		controler.addOverridingModule( new LSPModule() );
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.bind( LSPReplanningModule.class ).to( LSPReplanningModuleImpl.class );
				this.bind( LSPScoringModule.class ).to( LSPScoringModuleImpl.class );
			}
		} );

		log.info("Run MATSim");
		controler.run();

		//print the schedules for the assigned LSPShipments
		log.info("print the schedules for the assigned LSPShipments");
		printResults(config.controler().getOutputDirectory() , lsp);

		log.info("Done.");

	}

	private static LSP createInitialLSP(Network network) {

		final Id<Link> depotLinkId = Id.createLinkId("(4 2) (4 3)"); //TODO: Hochziehen aber non-static.
		final Id<Link> hubLinkId = Id.createLinkId("(14 2) (14 3)");

		LSPResource depotResource;
		{
			log.info( "" );
			log.info( "Create depot" );

			//The scheduler for the first reloading point is created --> this will be the depot in this use case
			LSPResourceScheduler depotScheduler = UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance()
					.setCapacityNeedFixed(10) //Time needed, fixed (for Scheduler)
					.setCapacityNeedLinear(1) //additional time needed per shipmentSize (for Scheduler)
					.build();

			//The scheduler is added to the Resource and the Resource is created
			depotResource = UsecaseUtils.ReloadingPointBuilder.newInstance(Id.create( "Depot", LSPResource.class ), depotLinkId)
					.setReloadingScheduler( depotScheduler )
					.build();
		}

		LogisticsSolutionElement depotElement;
		{
			//The SolutionElement for the first reloading point is created
			depotElement = LSPUtils.LogisticsSolutionElementBuilder.newInstance(Id.create( "DepotElement", LogisticsSolutionElement.class ))
				.setResource( depotResource )
				.build(); //Nicht unbedingt nötig, aber nehme den alten Hub nun als Depot. Waren werden dann dort "Zusammengestellt".
			//Maybe TODO: Depot als LogisticSolutionElement raus nehmen.(?)
		}

		//The adapter i.e. the main run resource is created
		LSPResource mainRunResource;
		{

			log.info( "" );
			log.info( "The Carrier for the main run is created" );
			Carrier mainRunCarrier = CarrierUtils.createCarrier( Id.create( "MainRunCarrier", Carrier.class ) );

			VehicleType mainRunVehicleType = CarrierVehicleType.Builder.newInstance(Id.create( "MainRunCarrierVehicleType", VehicleType.class ))
					.setCapacity( 30 )
					.setCostPerDistanceUnit( 0.0002 )
					.setCostPerTimeUnit( 0.38 )
					.setFixCost( 120 )
					.setMaxVelocity( 50 / 3.6 )
					.build();

			CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.Builder.newInstance(Id.createVehicleId( "MainRunVehicle" ), depotLinkId, mainRunVehicleType).build();

			mainRunCarrier.setCarrierCapabilities(
					CarrierCapabilities.Builder.newInstance()
							.addType( mainRunVehicleType )
							.addVehicle( mainRunCarrierVehicle )
							.setFleetSize( FleetSize.INFINITE )
							.build() );

			//The scheduler for the main run Resource is created and added to the Resource
			mainRunResource = UsecaseUtils.MainRunCarrierAdapterBuilder.newInstance(
					Id.create( "MainRunAdapter", LSPResource.class ), network )
					.setFromLinkId(depotLinkId)
					.setToLinkId(hubLinkId)
					.setCarrier( mainRunCarrier )
					.setMainRunCarrierScheduler( UsecaseUtils.createDefaultMainRunCarrierScheduler() )
					.build();
		}

		//The LogisticsSolutionElement for the main run Resource is created
		LogisticsSolutionElement mainRunElement;
		{
			mainRunElement = LSPUtils.LogisticsSolutionElementBuilder.newInstance(Id.create( "MainRunElement", LogisticsSolutionElement.class ))
				.setResource( mainRunResource )
			 	.build();
		}


		LSPResource hubResource;
		LogisticsSolutionElement hubElement;
		{
			log.info( "" );
			log.info( "The second reloading adapter (hub) i.e. the Resource is created" );
			//The scheduler for the second reloading point is created
			LSPResourceScheduler hubScheduler = UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance()
					.setCapacityNeedFixed( 10 )
					.setCapacityNeedLinear( 1 )
					.build();

			//The scheduler is added to the Resource and the Resource is created
			//The second reloading adapter i.e. the Resource is created
			Id<LSPResource> secondReloadingId = Id.create( "ReloadingPoint2", LSPResource.class );
			hubResource = UsecaseUtils.ReloadingPointBuilder.newInstance(secondReloadingId, hubLinkId)
					.setReloadingScheduler( hubScheduler )
					.build();

			//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
			hubElement = LSPUtils.LogisticsSolutionElementBuilder.newInstance(Id.create( "SecondReloadElement", LogisticsSolutionElement.class ))
					.setResource( hubResource )
					.build();
		}


		LSPResource distributionAdapter;
		LogisticsSolutionElement distributionElement;
		{
			//The Carrier for distribution from reloading Point is created
			VehicleType distributionVehicleType = createCarrierVehicleType("DistributionCarrierVehicleType");

			CarrierVehicle distributionCarrierVehicle = CarrierVehicle.Builder.newInstance( Id.createVehicleId( "DistributionVehicle" ), hubLinkId, distributionVehicleType).build();

			Carrier distributionCarrier = CarrierUtils.createCarrier(Id.create( "DistributionCarrier", Carrier.class ));
			distributionCarrier.setCarrierCapabilities(
					CarrierCapabilities.Builder.newInstance()
							.addType( distributionVehicleType )
							.addVehicle( distributionCarrierVehicle )
							.setFleetSize( FleetSize.INFINITE )
							.build() );

			//The distribution adapter i.e. the Resource is created
			distributionAdapter = UsecaseUtils.DistributionCarrierAdapterBuilder.newInstance(
					Id.create("DistributionCarrierAdapter", LSPResource.class ), network )
					.setCarrier( distributionCarrier ).setLocationLinkId(hubLinkId)
					.setDistributionScheduler( UsecaseUtils.createDefaultDistributionCarrierScheduler() )
					.build();
			// (The scheduler is where jsprit comes into play.)

			//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP

			distributionElement = LSPUtils.LogisticsSolutionElementBuilder.newInstance(
					Id.create("DistributionElement", LogisticsSolutionElement.class ) )
					.setResource(distributionAdapter )
					.build();
		}


		//### New (KMT): Carrier for direct distribution from Depot (without 2nd reloading Point)
		LSPResource directDistributionAdapter;
		LogisticsSolutionElement directDistributionElement;
		{
			//The Carrier for distribution from reloading Point is created
			VehicleType directDistributionVehicleType = createCarrierVehicleType("DirectDistributionCarrierVehicleType");

			CarrierVehicle directDistributionCarrierVehicle = CarrierVehicle.Builder.newInstance(
						Id.createVehicleId("DirectDistributionVehicle"), depotLinkId, directDistributionVehicleType)
					.build();

			CarrierCapabilities directDistributionCarrierCapabilities = CarrierCapabilities.Builder.newInstance()
					.addType(directDistributionVehicleType)
					.addVehicle(directDistributionCarrierVehicle)
					.setFleetSize(FleetSize.INFINITE)
					.build();
			Carrier directDistributionCarrier = CarrierUtils.createCarrier(Id.create("DirectDistributionCarrier", Carrier.class));
			directDistributionCarrier.setCarrierCapabilities(directDistributionCarrierCapabilities);

			//The distribution adapter i.e. the Resource is created
			directDistributionAdapter = UsecaseUtils.DistributionCarrierAdapterBuilder.newInstance(
						Id.create("DirectDistributionCarrierAdapter", LSPResource.class), network)
					.setCarrier(directDistributionCarrier).setLocationLinkId(depotLinkId)
					.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
					.build();
			// (The scheduler is where jsprit comes into play.)

			//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
			directDistributionElement = LSPUtils.LogisticsSolutionElementBuilder.newInstance(
						Id.create("DirectDistributionElement", LogisticsSolutionElement.class))
					.setResource(directDistributionAdapter)
					.build();
		}
		//### end new

		//TODO: Beide Lösungen anbieten und "bessere" oder zunächst "eine" auswählen"

		//TODO: Für die Auswahl "CostInfo an die Solutions dran heften.


		//The SolutionElements are now inserted into the only LogisticsSolution of the LSP
		//Die Reihenfolge des Hinzufügens ist egal, da weiter oben die jeweils direkten Vorgänger/Nachfolger bestimmt wurden.

		switch (solutionType) {
			case onePlan_withHub: {
				// ### This is the original solution with mainRun - ReloadingPoint - distributionRun
				log.info("Creating LSP with one plan: reloading at hub");

				LSPPlan lspPlan_Reloading = createLSPPlan_reloading(depotElement, mainRunElement, hubElement, distributionElement);

				return LSPUtils.LSPBuilder.getInstance(Id.create("LSPwithReloading", LSP.class))
						.setInitialPlan(lspPlan_Reloading)
						.setSolutionScheduler(UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(createResourcesListFromLSPPlan(lspPlan_Reloading)))
						.build();

			}
			case onePlan_direct: {
				// ### This is the new solution with  directDistribution from the Depot.
				log.info("Creating LSP with one plan: direct distribution from the depot");

				LSPPlan lspPlan_direct = createLSPPlan_direct(depotElement, directDistributionElement);

				return LSPUtils.LSPBuilder.getInstance(Id.create("LSPdirect", LSP.class))
						.setInitialPlan(lspPlan_direct)
						.setSolutionScheduler(UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(createResourcesListFromLSPPlan(lspPlan_direct)))
						.build();
			}

			case twoPlans_directAndHub: {
				log.info("Creating LSP with two plans: i) direct distribution from the depot ii) reloading at hub");

				log.error("This is totally untested. I can neither say if it will work nor if it will do anything useful - kmt feb22");

				LSPPlan lspPlan_Reloading = createLSPPlan_reloading(depotElement, mainRunElement, hubElement, distributionElement);
				LSPPlan lspPlan_direct = createLSPPlan_direct(depotElement, directDistributionElement);

				//TODO: Müsste nicht eigentlich der SolutionScheduler dann auf Ebene der einzelnen Pläne (mit ihren Solutions) sein?? kmt Feb22
				//So muss ich erst die Ressourcen aus beiden hinzuzufügenden Plänen aufsammeln, damit die dann schon in den Builder können. Irgendwie unschön.
				List<LSPResource> resourcesList = createResourcesListFromLSPPlan(lspPlan_direct);
				resourcesList.addAll(createResourcesListFromLSPPlan(lspPlan_Reloading));

				final LSP lsp = LSPUtils.LSPBuilder.getInstance(Id.create("LSPdirect", LSP.class))
						.setInitialPlan(lspPlan_direct)
						.setSolutionScheduler(UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList))
						.build();

				lsp.addPlan(lspPlan_Reloading); //adding the second plan

				return lsp;
			}
			default:
				throw new IllegalStateException("Unexpected value: " + solutionType);
		}

	}

	private static List<LSPResource> createResourcesListFromLSPPlan(LSPPlan lspPlanWithReloading) {
		log.info("Collecting all LSPResources from the LSPPlan");
		List<LSPResource> resourcesList = new ArrayList<>() ;			//TODO: Mahe daraus ein Set, damit jede Resource nur einmal drin ist? kmt Feb22
		for (LogisticsSolution solution : lspPlanWithReloading.getSolutions()) {
			for (LogisticsSolutionElement solutionElement : solution.getSolutionElements()) {
				resourcesList.add(solutionElement.getResource());
			}
		}
		return resourcesList;
	}

	private static LSPPlan createLSPPlan_direct(LogisticsSolutionElement depotElement, LogisticsSolutionElement directDistributionElement) {
		log.info("");
		log.info("The order of the logisticsSolutionElements is now specified");
		depotElement.connectWithNextElement(directDistributionElement);

		//TODO WIP: KostenInfo an das Element dran hängen.(old) --> brauchen wir das dann noch? (KMT, Feb22)

// 				LSPInfo costInfo = SimulationTrackersUtils.createDefaultCostInfo();
//				SimulationTrackersUtils.getFixedCostFunctionValue(costInfo.getFunction());
//				directDistributionElement.getInfos().add(costInfo);

		log.info("");
		log.info("set up logistic Solution - direct distribution from the depot is created");

		LogisticsSolution completeSolutionDirect = LSPUtils.LogisticsSolutionBuilder.newInstance(
					Id.create("SolutionDirectId", LogisticsSolution.class))
				.addSolutionElement(depotElement)
				.addSolutionElement(directDistributionElement)
				.build();

		log.info("");
		log.info("The initial plan of the lsp is generated and the assigner and the solution from above are added");

		LSPPlan completePlan = LSPUtils.createLSPPlan()
				.setAssigner(UsecaseUtils.createDeterministicShipmentAssigner())
				.addSolution(completeSolutionDirect);
		return completePlan;
	}

	private static LSPPlan createLSPPlan_reloading(LogisticsSolutionElement depotElement, LogisticsSolutionElement mainRunElement, LogisticsSolutionElement hubElement, LogisticsSolutionElement distributionElement) {
		log.info("");
		log.info("set up logistic Solution - original with hub usage solution is created");

		//Das ist wichtig, damit er die Kette zur Verfügung hat.
		depotElement.connectWithNextElement(mainRunElement);
		mainRunElement.connectWithNextElement(hubElement);
		hubElement.connectWithNextElement(distributionElement);

		LogisticsSolution completeSolutionWithReloading = LSPUtils.LogisticsSolutionBuilder.newInstance(
				Id.create("SolutionWithReloadingId", LogisticsSolution.class))
				.addSolutionElement(depotElement)
				.addSolutionElement(mainRunElement)
				.addSolutionElement(hubElement)
				.addSolutionElement(distributionElement)
				.build();


		log.info("");
		log.info("The initial plan of the lsp is generated and the assigner and the solution from above are added");

		LSPPlan lspPlanWithReloading = LSPUtils.createLSPPlan()
				.setAssigner(UsecaseUtils.createDeterministicShipmentAssigner())
				.addSolution(completeSolutionWithReloading);
		return lspPlanWithReloading;
	}

	private static VehicleType createCarrierVehicleType(String vehicleTypeId) {
		return CarrierVehicleType.Builder.newInstance(Id.create(vehicleTypeId, VehicleType.class))
				.setCapacity(10)
				.setCostPerDistanceUnit(0.0004)
				.setCostPerTimeUnit(0.38)
				.setFixCost(49)
				.setMaxVelocity(50 / 3.6)
				.build();
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
			builder.setDeliveryServiceTime(capacityDemand * 60 );
			LSPShipment shipment = builder.build();
			shipmentList.add(shipment);
		}
		return shipmentList;
	}

	private static void printResults(String outputDir, LSP lsp) {
		try ( BufferedWriter writer = IOUtils.getBufferedWriter(  outputDir + "/schedules.txt" ) ){
			for( LSPShipment shipment : lsp.getShipments() ){
				ArrayList<ShipmentPlanElement> elementList = new ArrayList<>( shipment.getShipmentPlan().getPlanElements().values() );
				elementList.sort( new ShipmentPlanElementComparator() );
				final String str1 = "Shipment: " + shipment.getId();
				System.out.println( str1 );
				writer.write( str1 + "\n");
				for( ShipmentPlanElement element : elementList ){
					final String str2 = element.getSolutionElement().getId() + "\t\t" + element.getResourceId() + "\t\t" + element.getElementType() + "\t\t" + element.getStartTime() + "\t\t" + element.getEndTime();
					System.out.println( str2 );
					writer.write(str2);
				}
				System.out.println();
				writer.write("\n");
			}
		} catch( IOException e ){
			e.printStackTrace();
		}
	}

}
