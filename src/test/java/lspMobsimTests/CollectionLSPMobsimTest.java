package lspMobsimTests;

import static lsp.usecase.UsecaseUtils.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import lsp.*;
import lsp.replanning.LSPReplanningUtils;
import lsp.scoring.LSPScoringUtils;
import lsp.shipment.*;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import lsp.controler.LSPModule;
import org.matsim.contrib.freight.events.eventsCreator.LSPEventCreatorUtils;
import lsp.resources.LSPCarrierResource;
import lsp.resources.LSPResource;

public class CollectionLSPMobsimTest {
	private static final Logger log = Logger.getLogger( CollectionLSPMobsimTest.class );

	@Rule public final MatsimTestUtils utils = new MatsimTestUtils();

	private LSP collectionLSP;
	private Carrier carrier;
	private LSPResource collectionAdapter;

	@Before
	public void initialize() {

		// create config:
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile( "scenarios/2regions/2regions-network.xml" );
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setFirstIteration( 0 );
		config.controler().setLastIteration( 0 );

		// load scenario:
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// define vehicle type:
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();

		// define starting link (?):
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Link collectionLink = scenario.getNetwork().getLinks().get(collectionLinkId );
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(collectionVehicleId, collectionLink.getId());
		carrierVehicle.setType( collectionType );

		// define carrier:
		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();
		carrier = CarrierUtils.createCarrier( carrierId );
		carrier.setCarrierCapabilities(capabilities);


		Id<LSPResource> adapterId = Id.create("CollectionCarrierAdapter", LSPResource.class);
		CollectionCarrierAdapterBuilder adapterBuilder = CollectionCarrierAdapterBuilder.newInstance(adapterId, scenario.getNetwork() );
		adapterBuilder.setCollectionScheduler( createDefaultCollectionCarrierScheduler() );
		adapterBuilder.setCarrier(carrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		collectionAdapter = adapterBuilder.build();

		final LogisticsSolutionElement collectionElement;
		{
			Id<LogisticsSolutionElement> elementId = Id.create( "CollectionElement", LogisticsSolutionElement.class );
			LSPUtils.LogisticsSolutionElementBuilder collectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance( elementId );
			collectionElementBuilder.setResource( collectionAdapter );
			collectionElement = collectionElementBuilder.build();
		}
		final LogisticsSolution collectionSolution;
		{
			Id<LogisticsSolution> collectionSolutionId = Id.create( "CollectionSolution", LogisticsSolution.class );
			LSPUtils.LogisticsSolutionBuilder collectionSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance( collectionSolutionId );
			collectionSolutionBuilder.addSolutionElement( collectionElement );
			collectionSolution = collectionSolutionBuilder.build();
		}
		final LSPPlan collectionPlan;
		{
			ShipmentAssigner assigner = createDeterministicShipmentAssigner();
			collectionPlan = LSPUtils.createLSPPlan();
			collectionPlan.setAssigner( assigner );
			collectionPlan.addSolution( collectionSolution );
		}
		{
			final LSPUtils.LSPBuilder collectionLSPBuilder;
			ArrayList<LSPResource> resourcesList = new ArrayList<>();
			collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
			collectionLSPBuilder.setInitialPlan( collectionPlan );
			resourcesList.add( collectionAdapter );
			SolutionScheduler simpleScheduler = createDefaultSimpleForwardSolutionScheduler( resourcesList );
			simpleScheduler.setBufferTime( 300 );
			collectionLSPBuilder.setSolutionScheduler( simpleScheduler );
			collectionLSP = collectionLSPBuilder.build();
		}
		{
			ArrayList<Link> linkList = new ArrayList<>( scenario.getNetwork().getLinks().values() );
			for( int i = 1 ; i < 2 ; i++ ){
				Id<LSPShipment> id = Id.create( i, LSPShipment.class );
				ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance( id );
				//Random random = new Random(1);
				int capacityDemand = 1 + new Random().nextInt( 4 );
				builder.setCapacityDemand( capacityDemand );

				while( true ){
					Collections.shuffle( linkList );
					Link pendingFromLink = linkList.get( 0 );
					if( pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
							    pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
							    pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
							    pendingFromLink.getToNode().getCoord().getY() <= 4000 ){
						builder.setFromLinkId( pendingFromLink.getId() );
						break;
					}
				}

				builder.setToLinkId( collectionLinkId );
				TimeWindow endTimeWindow = TimeWindow.newInstance( 0, (24 * 3600) );
				builder.setEndTimeWindow( endTimeWindow );
				TimeWindow startTimeWindow = TimeWindow.newInstance( 0, (24 * 3600) );
				builder.setStartTimeWindow( startTimeWindow );
				builder.setDeliveryServiceTime( capacityDemand * 60 );
				LSPShipment shipment = builder.build();
				collectionLSP.assignShipmentToLSP( shipment );
			}
			collectionLSP.scheduleSolutions();
		}
		final LSPs lsps;
		{
			ArrayList<LSP> lspList = new ArrayList<>();
			lspList.add( collectionLSP );
			lsps = new LSPs( lspList );
		}
		Controler controler = new Controler(config);
		controler.getEvents().addHandler( new BasicEventHandler(){
			@Override public void handleEvent( Event event ){
				log.warn(event);
			}
		} );

		controler.addOverridingModule( new LSPModule(lsps, LSPReplanningUtils.createDefaultLSPReplanningModule(lsps ),
				LSPScoringUtils.createDefaultLSPScoringModule(lsps ), LSPEventCreatorUtils.getStandardEventCreators()) );

		controler.run();
	}

	@Test
	public void testCollectionLSPMobsim() {
		for(LSPShipment shipment : collectionLSP.getShipments()) {
			assertFalse(shipment.getLog().getPlanElements().isEmpty());

			log.warn("");
			log.warn("shipment schedule plan elements:" );
			for( ShipmentPlanElement planElement : shipment.getShipmentPlan().getPlanElements().values() ){
				log.warn( planElement );
			}
			log.warn("");
			log.warn("shipment log plan elements:");
			for( ShipmentPlanElement planElement : shipment.getLog().getPlanElements().values() ){
				log.warn( planElement );
			}
			log.warn("");

			assertEquals(shipment.getShipmentPlan().getPlanElements().size(), shipment.getLog().getPlanElements().size());
			ArrayList<ShipmentPlanElement> scheduleElements = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			scheduleElements.sort(new ShipmentPlanElementComparator());
			ArrayList<ShipmentPlanElement> logElements = new ArrayList<>(shipment.getLog().getPlanElements().values());
			logElements.sort(new ShipmentPlanElementComparator());

			//Das muss besser in den SchedulingTest rein
			assertSame(collectionLSP.getResources().iterator().next(), collectionAdapter);
			LSPCarrierResource carrierResource = (LSPCarrierResource) collectionAdapter;
			assertSame(carrierResource.getCarrier(), carrier);
			assertEquals(1, carrier.getServices().size());

			for(ShipmentPlanElement scheduleElement : scheduleElements){
				ShipmentPlanElement logElement = logElements.get(scheduleElements.indexOf(scheduleElement));
				assertEquals(scheduleElement.getElementType(), logElement.getElementType());
				assertSame(scheduleElement.getResourceId(), logElement.getResourceId());
				assertSame(scheduleElement.getSolutionElement(), logElement.getSolutionElement());
				assertEquals(scheduleElement.getStartTime(), logElement.getStartTime(), 300);
			}
		}
	}
}
