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
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.UsecaseUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

/**
 * This is an academic example for the 2-echelon problem.
 * It uses the 9x9-grid network from the matsim-examples.
 *
 * The depot is located at the outer border of the network, while the jobs are located in the middle area.
 * The {@link lsp.LSP} has two different {@link lsp.LSPPlan}s:
 * 1) direct delivery from the depot
 * 2) Using a TransshipmentHub: All goods were brought from the depot to the hub, reloaded and then brought from the hub to the customers
 *
 * The decision which of these plans is chosen should be made via the Score of the plans.
 * We will modify the costs of the vehicles and/or for using(having) the Transshipment hub. Depending on this setting,
 * the plan selection should be done accordingly.
 *
 * Please note: This example is in part on existing examples, but I start from the scratch for a) see, if this works and b) have a "clean" class :)
 *
 * @author Kai Martins-Turner (kturner)
 */
final class ExampleTwoEchelonGrid {


	private static final Logger log = Logger.getLogger(ExampleTwoEchelonGrid.class);
	private static final Id<Link> DEPOT_LINK_ID = Id.createLinkId("i(5,0)");

	private static final VehicleType VEH_TYPE_LARGE_10 = CarrierVehicleType.Builder.newInstance(Id.create("large10", VehicleType.class))
			.setCapacity(10)
			.setMaxVelocity(10)
			.setFixCost(130)
			.setCostPerDistanceUnit(0.001)
			.setCostPerTimeUnit(0.01)
			.build();

	public static void main(String[] args) {
		log.info("Prepare Config");
		Config config = prepareConfig();

		log.info("Prepare scenario");
		Scenario scenario = prepareScenario(config);

		log.info("Prepare Controler");
		Controler controler = new Controler(scenario);
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				install( new LSPModule() );
			}
		} );

		log.info("Run MATSim");
		controler.run();

		log.info("Done.");
	}

	private static Config prepareConfig() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(String.valueOf(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9" ), "grid9x9.xml")));
		config.controler().setOutputDirectory("output/2echelon/");
		config.controler().setLastIteration(2);

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule( config, FreightConfigGroup.class );
		freightConfig.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.ignore );


		return config;
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//Change speed on all links to 30 km/h (8.33333 m/s) for easier computation --> Freeflow TT per link is 2min
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setFreespeed(30/3.6);
		}

		log.info("Add LSP to the scenario");
		LSPUtils.addLSPs( scenario, new LSPs( Collections.singletonList( createLSP(scenario.getNetwork()) ) ) );

		return scenario;
	}

	private static LSP createLSP(Network network) {
		log.info("create LSP");
	

//		//TODO: Brauchen wir das hier wirklich?
		//Scheint so, weil nur 1 Element nicht geht, aktuell. --> die direkte Beliferung ist es irgendwie nötig
		LogisticsSolutionElement depotElement;
		{
			log.info( "Create depot" );

			//The scheduler for the first reloading point is created --> this will be the depot in this use case
			LSPResourceScheduler depotScheduler = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance()
					.setCapacityNeedFixed(10) //Time needed, fixed (for Scheduler)
					.setCapacityNeedLinear(1) //additional time needed per shipmentSize (for Scheduler)
					.build();

			//The scheduler is added to the Resource and the Resource is created
			LSPResource depotResource = UsecaseUtils.TransshipmentHubBuilder.newInstance( Id.create( "Depot", LSPResource.class ), DEPOT_LINK_ID )
					.setTransshipmentHubScheduler( depotScheduler )
					.build();

			depotElement = LSPUtils.LogisticsSolutionElementBuilder.newInstance(Id.create( "DepotElement", LogisticsSolutionElement.class ))
					.setResource( depotResource )
					.build(); //Nicht unbedingt nötig, aber nehme den alten Hub nun als Depot. Waren werden dann dort "Zusammengestellt".
		}



		Carrier directCarrier = CarrierUtils.createCarrier(Id.create("directCarrier", Carrier.class));
				directCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

		CarrierUtils.addCarrierVehicle(directCarrier, CarrierVehicle.newInstance(Id.createVehicleId("directTruck"), DEPOT_LINK_ID, VEH_TYPE_LARGE_10));
		LSPResource directCarrierRessource = UsecaseUtils.DistributionCarrierAdapterBuilder.newInstance(Id.create("directCarrierRes", LSPResource.class), network)
				.setCarrier(directCarrier)
				.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
				.build();

		LogisticsSolutionElement directCarrierElement = LSPUtils.LogisticsSolutionElementBuilder.newInstance(Id.create("directCarrierSE", LogisticsSolutionElement.class))
				.setResource(directCarrierRessource)
				.build();

		//Kettenbildung per hand, damit dann klar ist, wie das Scheduling ablaufen soll. TODO: Vielleicht bekommt man das noch eleganter hin.
		// z.B. in der Reihenfolge in der die solutionsElements der LogisticsSolution zugeordnet werden: ".addSolutionElement(..)"
		depotElement.connectWithNextElement(directCarrierElement);

		LogisticsSolution solution_direct = LSPUtils.LogisticsSolutionBuilder.newInstance(Id.create("directSolution", LogisticsSolution.class))
				.addSolutionElement(depotElement)
				.addSolutionElement(directCarrierElement)
				.build();

		LSPPlan lspPlan = LSPUtils.createLSPPlan()
				.addSolution(solution_direct)
				.setAssigner(UsecaseUtils.createSinglesolutionShipmentAssigner());

		LSPUtils.LSPBuilder lspBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
				.setInitialPlan(lspPlan)
//				.setSolutionScheduler(LSPUtils.createForwardSolutionScheduler())  //Does not work, because of "null" pointer in predecessor.. TODO: Have a look into it later... kmt jul22
				.setSolutionScheduler(UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(createResourcesListFromLSPPlan(lspPlan)))
				.setSolutionScorer(new MyLSPScorer());

		LSP lsp = lspBuilder.build();

		log.info("create initial LSPShipments");
		log.info("assign the shipments to the LSP");
		for(LSPShipment shipment : createInitialLSPShipments() ) {
			lsp.assignShipmentToLSP(shipment);
		}

		log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
		lsp.scheduleSolutions();

		return lsp;
	}

	private static Collection<LSPShipment> createInitialLSPShipments() {
		ArrayList<LSPShipment> shipmentList = new ArrayList<>();

		Random rand = MatsimRandom.getRandom();
		int i = 1;
//		for(int i = 1; i < 6; i++) {
			Id<LSPShipment> id = Id.create("Shipment_" + i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id );
			int capacityDemand = rand.nextInt(10);
			builder.setCapacityDemand(capacityDemand);

			builder.setFromLinkId(DEPOT_LINK_ID);
			builder.setToLinkId(Id.createLinkId("i(5,5)R"));

			builder.setEndTimeWindow(TimeWindow.newInstance(0,(24*3600)));
			builder.setStartTimeWindow(TimeWindow.newInstance(0,(24*3600)));
			builder.setDeliveryServiceTime(capacityDemand * 60 );

			shipmentList.add(builder.build());
//		}
		return shipmentList;
	}

	//TODO: This is maybe something that can go into a utils class ... KMT jul22
	private static List<LSPResource> createResourcesListFromLSPPlan(LSPPlan lspPlanWithReloading) {
		log.info("Collecting all LSPResources from the LSPPlan");
		List<LSPResource> resourcesList = new ArrayList<>() ;			//TODO: Mache daraus ein Set, damit jede Resource nur einmal drin ist? kmt Feb22
		for (LogisticsSolution solution : lspPlanWithReloading.getSolutions()) {
			for (LogisticsSolutionElement solutionElement : solution.getSolutionElements()) {
				resourcesList.add(solutionElement.getResource());
			}
		}
		return resourcesList;
	}

}
