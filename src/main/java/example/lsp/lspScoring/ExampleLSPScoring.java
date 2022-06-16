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

package example.lsp.lspScoring;

import lsp.*;
import lsp.controler.LSPModule;
import lsp.replanning.LSPReplanningModule;
import lsp.replanning.LSPReplanningModuleImpl;
import lsp.LSPResource;
import lsp.scoring.LSPScoringModule;
import lsp.scoring.LSPScoringModuleDefaultImpl;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

import java.util.*;

/* Example for customized scoring. Each customer that is visited will give a random tip between zero and five
 *
 *
 */


/*package-private*/ final class ExampleLSPScoring {

	private ExampleLSPScoring(){
	}
	private static LSP createLSPWithScorer( Network network ) {

		//The Carrier for the resource of the sole LogisticsSolutionElement of the LSP is created
		var carrierVehicleType = CarrierVehicleType.Builder.newInstance( Id.create("CollectionCarrierVehicleType", VehicleType.class ) )
											  .setCapacity(10).setCostPerDistanceUnit(0.0004).setCostPerTimeUnit(0.38).setFixCost(49).setMaxVelocity(50/3.6).build();

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");

		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance( Id.createVehicleId("CollectionVehicle" ), collectionLinkId, carrierVehicleType );

		CarrierCapabilities capabilities = CarrierCapabilities.Builder.newInstance()
//									      .addType(carrierVehicleType )
									      .addVehicle(carrierVehicle ).setFleetSize(FleetSize.INFINITE ).build();

		Carrier carrier = CarrierUtils.createCarrier( Id.create("CollectionCarrier", Carrier.class ) );
		carrier.setCarrierCapabilities(capabilities);

		//The Adapter i.e. the Resource is created
		//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		LSPResource lspResource = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(
									    Id.create("CollectionCarrierAdapter", LSPResource.class ), network )
											    .setCollectionScheduler( UsecaseUtils.createDefaultCollectionCarrierScheduler() )
											    .setCarrier(carrier ).setLocationLinkId(collectionLinkId ).build();

		//The adapter is now inserted into the only LogisticsSolutionElement of the only LogisticsSolution of the LSP
		LogisticsSolutionElement logisticsSolutionElement = LSPUtils.LogisticsSolutionElementBuilder.newInstance(
				Id.create("CollectionElement", LogisticsSolutionElement.class ) ).setResource(lspResource ).build();

		//The LogisticsSolutionElement is now inserted into the only LogisticsSolution of the LSP
		LogisticsSolution logisticsSolution = LSPUtils.LogisticsSolutionBuilder.newInstance( Id.create("CollectionSolution", LogisticsSolution.class ) )
											.addSolutionElement(logisticsSolutionElement ).build();

		//The initial plan of the lsp is generated and the assigner and the solution from above are added
		LSPPlan lspPlan = LSPUtils.createLSPPlan().setAssigner( UsecaseUtils.createDeterministicShipmentAssigner() ).addSolution(logisticsSolution );

		//The exogenous list of Resoruces for the SolutionScheduler is compiled and the Scheduler is added to the LSPBuilder
		LSP lsp = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class ) )
					     .setInitialPlan(lspPlan )
					     .setSolutionScheduler( UsecaseUtils.createDefaultSimpleForwardSolutionScheduler( Collections.singletonList( lspResource ) ) )
					     .build();

//		TipScorer.TipSimulationTracker tracker = new TipScorer.TipSimulationTracker();

		//add SimulationTracker to the Resource
//		lspResource.addSimulationTracker(tracker);

		//Create the Scorer and add it to the lsp
		final TipScorer scorer = new TipScorer();
		lsp.addSimulationTracker( scorer );
		lsp.setScorer( scorer );

		// yyyyyy there is almost surely something wrong with the design if you cannot set the
		// scorer in the builder. kai, sep'18

		return lsp;
	}

	private static Collection<LSPShipment> createInitialLSPShipments(Network network){
		List<LSPShipment> shipmentList = new ArrayList<>();
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


	public static void main(String []args) {

		Config config = prepareConfig();

		Scenario scenario = prepareScenario( config );

		Controler controler = prepareControler( scenario );

		controler.run();

		for( LSP lsp2 : LSPUtils.getLSPs( scenario ).getLSPs().values() ){
			System.out.println("The tip of all customers was: " + lsp2.getSelectedPlan().getScore());
		}


	}
	static Controler prepareControler( Scenario scenario ){
		//Start the Mobsim one iteration is sufficient for scoring
		Controler controler = new Controler( scenario );
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				install( new LSPModule() );
				this.bind( LSPReplanningModule.class ).to( LSPReplanningModuleImpl.class );
				this.bind( LSPScoringModule.class ).to( LSPScoringModuleDefaultImpl.class );
			}
		});
		return controler;
	}
	static Scenario prepareScenario( Config config ){
		Scenario scenario = ScenarioUtils.loadScenario( config );

		//Create LSP and shipments
		LSP lsp = createLSPWithScorer( scenario.getNetwork() );
		Collection<LSPShipment> shipments =  createInitialLSPShipments( scenario.getNetwork() );

		//assign the shipments to the LSP
		for(LSPShipment shipment : shipments) {
			lsp.assignShipmentToLSP(shipment);
		}

		//schedule the LSP with the shipments and according to the scheduler of the Resource
		lsp.scheduleSolutions();

		//Prepare LSPModule and add the LSP
		LSPs lsps = new LSPs( Collections.singletonList( lsp ));
		LSPUtils.addLSPs( scenario, lsps );
		return scenario;
	}
	static Config prepareConfig(){
		//Set up required MATSim classes
		Config config = ConfigUtils.createConfig();

		config.network().setInputFile( "scenarios/2regions/2regions-network.xml" );

		config.controler().setLastIteration( 0 );
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		var freightConfig = ConfigUtils.addOrGetModule( config, FreightConfigGroup.class );
		freightConfig.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.ignore );
		return config;
	}


}
