package playground.qvanheerden.freight;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.util.Solutions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeWriter;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

public class MyCarrierPlanGenerator {
	private final static Logger log = Logger.getLogger(MyCarrierPlanGenerator.class);
	public static Coord depotCoord;
	public static Id depotLink;
//	public CarrierVehicleTypes carrierVehicleTypes;
	public static Carrier carrier;
	public static Scenario scenario;
	public static String initialPlanAlgorithm;
	/**
	 * Trying to create carrier plans from the freight contrib [Oct 2013]
	 * 
	 * The output from this class includes the following files:
	 * <ul>
	 * <li> vehicleTypes.xml (containing the vehicle types for the carrier)
	 * <li> carrier.xml (containing the carrier plans)
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(MyCarrierPlanGenerator.class.toString(), args);
		
		/* Input */
		String networkFile = args[0];
		Double depotLong = Double.parseDouble(args[1]);
		Double depotLat = Double.parseDouble(args[2]);
		String demandInputFile = args[3];
		initialPlanAlgorithm = args[4];
//		String changeEventsInputFile = args[5];
		String vehicleTypesFile = args[6];
		String carrierInput = args[7];
		
		/* Output (check for consistency)*/
		String vehicleTypeOutputFile = "./output/freight/vehicleTypes2.xml"; 
		String carrierPlanOutputFile = "./output/freight/carrier2.xml";
		
		/* Read network */
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("./output/");
		config.network().setInputFile(networkFile);
		config.network().setTimeVariantNetwork(true);
		scenario = ScenarioUtils.loadScenario(config);
		
		//Add congestion
//		MyCarrierPlanGenerator.getNetworkChangeEvents(scenario, 7, 8, 16, 17, 60);
		
		/* Set coordinate and linkId of depot */
		depotCoord = new CoordImpl(depotLong, depotLat);
//		depotLink = ((NetworkImpl) network).getNearestLink((Coord) depotCoord).getId();
		depotLink = ((NetworkImpl) scenario.getNetwork()).getNearestLink((Coord) depotCoord).getId();
		
		/* Build vehicle types */
		MyCarrierPlanGenerator mcpg = new MyCarrierPlanGenerator();
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(carrierVehicleTypes).read(vehicleTypesFile);
//		mcpg.buildAndWriteVehicleTypes(vehicleTypeOutputFile, true);
		
		/* Create carrier and assign carrier capabilities to carrier */
//		carrier = CarrierImpl.newInstance(new IdImpl("MyCarrier"));
		Carriers carriers = new Carriers();
		new CarrierPlanXmlReaderV2(carriers).read(carrierInput);
		
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(carrierVehicleTypes);
		
		carrier = carriers.getCarriers().get(new IdImpl("MyCarrier"));

//		MyCarrierCapabilityGenerator mccg = new MyCarrierCapabilityGenerator();
//		carrier.setCarrierCapabilities(mccg.createVehicles(carrierVehicleTypes, depotLink, scenario.getNetwork()));
		
		
		/* Parse shipments/demand and add to carrier 
		 * -for now these will be services until we figure out how to use shipments
		 */
		mcpg.parseDemand(demandInputFile);
		mcpg.createInitialPlans(carrierVehicleTypes);
		
		carriers.getCarriers().clear();
		carriers.getCarriers().put(new IdImpl("MyCarrier"), carrier);
		CarrierPlanXmlWriterV2 planWriter = new CarrierPlanXmlWriterV2(carriers);
		planWriter.write(carrierPlanOutputFile);

		CarrierVehicleTypes types = CarrierVehicleTypes.getVehicleTypes(carriers);
		CarrierVehicleTypeWriter typeWriter = new CarrierVehicleTypeWriter(types);
		typeWriter.write(vehicleTypeOutputFile);
		
		//new Visualiser(config, scenario).visualizeLive(carriers);
		
		Header.printFooter();

	}
	
	public void createInitialPlans(CarrierVehicleTypes carrierVehicleTypes){
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork());
		vrpBuilder.setFleetSize(FleetSize.FINITE);
//		vrpBuilder.addPenaltyVehicles(10000, 100000);
		NetworkBasedTransportCosts.Builder costsBuilder = NetworkBasedTransportCosts.Builder.newInstance(scenario.getNetwork(), carrier.getCarrierCapabilities().getVehicleTypes());
		costsBuilder.setTimeSliceWidth(1800);
		NetworkBasedTransportCosts costs = costsBuilder.build();
		vrpBuilder.setRoutingCost(costs);
		VehicleRoutingProblem vrp = vrpBuilder.build();
		
		VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, initialPlanAlgorithm);
		Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();
		
		CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, Solutions.bestOf(solutions));
		NetworkRouter.routePlan(plan, costs);
		carrier.setSelectedPlan(plan);
		log.info("Initial plans created...");
	}
	
	public static void getNetworkChangeEvents(Scenario scenario, double amStart, double amEnd, double pmStart, double pmEnd, int kmph) {
		NetworkChangeEventFactory cef = new NetworkChangeEventFactoryImpl();

		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			double speed = link.getFreespeed() ;
//			double speed = 0 ;
			double speedKmph = kmph;
			final double threshold = speedKmph/3.6; //convert to m/s
			if ( speed > threshold ) {
				{//morning peak starts
					NetworkChangeEvent event = cef.createNetworkChangeEvent(amStart*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  threshold ));
					event.addLink(link);
//					ni.addNetworkChangeEvent(event);
					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//					events.add(event);
				}
				{//morning peak ends
					NetworkChangeEvent event = cef.createNetworkChangeEvent(amEnd*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  speed ));
					event.addLink(link);
//					ni.addNetworkChangeEvent(event);
					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//					events.add(event);
				}
				{//afternoon peak starts
					NetworkChangeEvent event = cef.createNetworkChangeEvent(pmStart*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  threshold ));
					event.addLink(link);
//					ni.addNetworkChangeEvent(event);
					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//					events.add(event);
				}
				{//afternoon peak ends
					NetworkChangeEvent event = cef.createNetworkChangeEvent(pmEnd*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  speed ));
					event.addLink(link);
//					ni.addNetworkChangeEvent(event);
					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//					events.add(event);
				}
			}
		}

	}
	
	/**
	 * This method reads in a file with shipments in the format:
	 * <br> customer, long, lat, product, mass, sale, duration, start, end
	 * <br><br>
	 * Where the fields refer to:
	 * <ul>
	 * <li> customer: name of customer with demand
	 * <li> long: longitude of customer
	 * <li> lat: latitude of customer
	 * <li> product: name of product requested
	 * <li> mass: mass of product requested
	 * <li> sale: sales value of product requested
	 * <li> duration: duration of service activity
	 * <li> start: start of time window
	 * <li> end: end of time window
	 * </ul>
	 * This format may change once we use shipments. (E.g. shipments have both 
	 * pickup and delivery time windows).
	 */
	public void parseDemand(String demandFile){

		BufferedReader br = IOUtils.getBufferedReader(demandFile);
		
		try {
			br.readLine();//skip header
			
			String input;
			int i = 1;
			while((input = br.readLine()) != null){
				String[] array = input.split(",");
				
				String customer = array[0];
				double longi = Double.parseDouble(array[1]);
				double lati = Double.parseDouble(array[2]);
				String product = array[3];
				double mass = Double.parseDouble(array[4]);
				double sale = Double.parseDouble(array[5]);
				double duration = Double.parseDouble(array[6]);
				int start = Integer.parseInt(array[7]);
				int end = Integer.parseInt(array[8]);
				
				Coord coord = new CoordImpl(longi, lati);
				
				Id linkId = ((NetworkImpl) scenario.getNetwork()).getNearestLink(coord).getId();
				
				
				//Shipments seem to not yet be supported in the vehicle routing builder
//				CarrierShipment ship = CarrierShipment.Builder.newInstance(new IdImpl(i), linkId, mass)
//						.setPickupServiceTime(500)
//						.setDeliveryServiceTime(500)
//						.setPickupTimeWindow(TimeWindow.newInstance(start, end))
//						.setDeliveryTimeWindow(TimeWindow.newInstance(start, end))
//						.build();
				
				CarrierService serv = CarrierService.Builder.newInstance(new IdImpl(i), linkId).
						setCapacityDemand((int) mass).
						setServiceDuration(duration).
						setName(customer).
						setServiceStartTimeWindow(TimeWindow.newInstance(start, end)).
						build();
				
				carrier.getServices().add(serv);
				
				i++;
			}
			
			
		} catch (IOException e) {
			log.error("Could not read shipments file");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				log.error("Could not close shipment file");
			}
		}
	}
	
	
	
}
