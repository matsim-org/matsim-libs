package playground.qvanheerden.freight;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.util.Solutions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
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
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

public class MyCarrierPlanGenerator {
	private final static Logger log = Logger.getLogger(MyCarrierPlanGenerator.class);
	public static Coord depotCoord;
	public static Id depotLink;
	public CarrierVehicleTypes carrierVehicleTypes;
	public static Carrier carrier;
	public static Network network;
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
		String changeEventsInputFile = args[5];
		
		/* Output */
		String vehicleTypeOutputFile = "./output/freight/vehicleTypes.xml";
		String carrierPlanOutputFile = "./output/freight/carrier.xml";
		
		/* Read network */
		Config config = ConfigUtils.createConfig();
		config.addCoreModules();
		scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(networkFile);
		scenario.getConfig().network().setTimeVariantNetwork(true);
		scenario.getConfig().network().setChangeEventInputFile(changeEventsInputFile);
		network = scenario.getNetwork();
		
		
		/* Set coordinate and linkId of depot */
		depotCoord = new CoordImpl(depotLong, depotLat);
		depotLink = ((NetworkImpl) network).getNearestLink((Coord) depotCoord).getId();
		
		/* Build vehicle types */
		MyCarrierPlanGenerator mcpg = new MyCarrierPlanGenerator();
		mcpg.buildAndWriteVehicleTypes(vehicleTypeOutputFile);
		
		/* Create carrier and assign carrier capabilities to carrier */
		carrier = CarrierImpl.newInstance(new IdImpl("MyCarrier"));
		carrier.setCarrierCapabilities(mcpg.createCarrierCapabilities());

		/* Parse shipments/demand and add to carrier 
		 * -for now these will be services until we figure out how to use shipments
		 */
		mcpg.parseDemand(demandInputFile);
		mcpg.createInitialPlans();
		
		Carriers carriers = new Carriers();
		carriers.addCarrier(carrier);
		CarrierPlanXmlWriterV2 planWriter = new CarrierPlanXmlWriterV2(carriers);
		planWriter.write(carrierPlanOutputFile);
		
		//new Visualiser(config, scenario).visualizeLive(carriers);
		
		Header.printFooter();

	}
	
	public void createInitialPlans(){
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
		NetworkBasedTransportCosts.Builder costsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, carrier.getCarrierCapabilities().getVehicleTypes());
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
				
				Id linkId = ((NetworkImpl) network).getNearestLink(coord).getId();
				
				
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
	
	/**
	 * This method will generate vehicles (also known as {@link CarrierCapabilities}).
	 * <br><br>
	 * 10 vehicles are created:
	 * <ul>
	 * <li> 1 x 3 tonners
	 * <li> 4 x 6 tonners
	 * <li> 2 x 7 tonners
	 * <li> 1 x 12 tonners
	 * <li> 2 x 15 tonners
	 * </ul>
	 * 
	 */
	public CarrierCapabilities createCarrierCapabilities(){
		double earliestStart = 0;
//		double latestEnd = 64800; //6pm (just arbitrary value for now)
		double latestEnd = 32400; //pm (just arbitrary value for now)
		
		CarrierVehicle.Builder builder = CarrierVehicle.Builder.newInstance(new IdImpl("truck_3_1"), depotLink) ;
		builder.setTypeId(new IdImpl("3_tonner")) ;
		builder.setType(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("3_tonner")));
		builder.setEarliestStart(earliestStart);
		builder.setLatestEnd(latestEnd);
		CarrierVehicle truck_3_1 = builder.build();
		
//		CarrierVehicle.Builder builder2 = CarrierVehicle.Builder.newInstance(null, null)
//				.setEarliestStart(10.).setLatestEnd(20.) ;
//		CarrierVehicle truck_test = builder2.build() ;

		CarrierCapabilities capabilities = CarrierCapabilities.Builder.
				newInstance().
				addType(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("3_tonner"))).
				addVehicle(truck_3_1).build();
		
		CarrierVehicle truck_6_1 = CarrierVehicle.Builder.newInstance(new IdImpl("truck_6_1"), depotLink).
				setTypeId(new IdImpl("6_tonner")).
				setType(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("3_tonner"))).
				setEarliestStart(earliestStart).
				setLatestEnd(latestEnd).
				build();
		
		capabilities.getCarrierVehicles().add(truck_6_1);
		capabilities.getVehicleTypes().add(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("6_tonner")));
				
		CarrierVehicle truck_6_2 = CarrierVehicle.Builder.newInstance(new IdImpl("truck_6_2"), depotLink).
				setTypeId(new IdImpl("6_tonner")).
				setType(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("6_tonner"))).
				setEarliestStart(earliestStart).
				setLatestEnd(latestEnd).
				build();
		
		capabilities.getCarrierVehicles().add(truck_6_2);
		//capabilities.getVehicleTypes().add(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("6_tonner")));
		
		CarrierVehicle truck_6_3 = CarrierVehicle.Builder.newInstance(new IdImpl("truck_6_3"), depotLink).
				setTypeId(new IdImpl("6_tonner")).
				setType(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("6_tonner"))).
				setEarliestStart(earliestStart).
				setLatestEnd(latestEnd).
				build();
		
		capabilities.getCarrierVehicles().add(truck_6_3);
		//capabilities.getVehicleTypes().add(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("6_tonner")));

		CarrierVehicle truck_6_4 = CarrierVehicle.Builder.newInstance(new IdImpl("truck_6_4"), depotLink).
				setTypeId(new IdImpl("6_tonner")).
				setType(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("6_tonner"))).
				setEarliestStart(earliestStart).
				setLatestEnd(latestEnd).
				build();
		capabilities.getCarrierVehicles().add(truck_6_4);
		//capabilities.getVehicleTypes().add(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("6_tonner")));

		CarrierVehicle truck_7_1 = CarrierVehicle.Builder.newInstance(new IdImpl("truck_7_1"), depotLink).
				setTypeId(new IdImpl("7_tonner")).
				setType(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("7_tonner"))).
				setEarliestStart(earliestStart).
				setLatestEnd(latestEnd).
				build();
		capabilities.getCarrierVehicles().add(truck_7_1);
		capabilities.getVehicleTypes().add(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("7_tonner")));
		
		CarrierVehicle truck_7_2 = CarrierVehicle.Builder.newInstance(new IdImpl("truck_7_2"), depotLink).
				setTypeId(new IdImpl("7_tonner")).
				setType(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("7_tonner"))).
				setEarliestStart(earliestStart).
				setLatestEnd(latestEnd).
				build();
		capabilities.getCarrierVehicles().add(truck_7_2);
		//capabilities.getVehicleTypes().add(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("7_tonner")));
		
		CarrierVehicle truck_12_1 = CarrierVehicle.Builder.newInstance(new IdImpl("truck_12_1"), depotLink).
				setTypeId(new IdImpl("12_tonner")).
				setType(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("12_tonner"))).
				setEarliestStart(earliestStart).
				setLatestEnd(latestEnd).
				build();
		capabilities.getCarrierVehicles().add(truck_12_1);
		capabilities.getVehicleTypes().add(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("12_tonner")));
		
		CarrierVehicle truck_15_1 = CarrierVehicle.Builder.newInstance(new IdImpl("truck_15_1"), depotLink).
				setTypeId(new IdImpl("15_tonner")).
				setType(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("15_tonner"))).
				setEarliestStart(earliestStart).
				setLatestEnd(latestEnd).
				build();
				capabilities.getCarrierVehicles().add(truck_15_1);
		capabilities.getVehicleTypes().add(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("15_tonner")));
		
		CarrierVehicle truck_15_2 = CarrierVehicle.Builder.newInstance(new IdImpl("truck_15_2"), depotLink).
				setTypeId(new IdImpl("15_tonner")).
				setType(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("15_tonner"))).
				setEarliestStart(earliestStart).
				setLatestEnd(latestEnd).
				build();
		capabilities.getCarrierVehicles().add(truck_15_2);
		//capabilities.getVehicleTypes().add(carrierVehicleTypes.getVehicleTypes().get(new IdImpl("15_tonner")));
		
		return capabilities;
	}
	
	/**
	 * This method builds vehicle types and writes a file containing the vehicle
	 *  types.  Five vehicle types are created for now with arbitrary values for 
	 *  the variable and fixed costs.
	 */
	public void buildAndWriteVehicleTypes(String vehicleTypeFile){
		/* Build vehicle types */
		
		CarrierVehicleType typeThree = CarrierVehicleType.Builder.
				newInstance(new IdImpl("3_tonner")).
				setCapacity(3000).
				setCostPerDistanceUnit(3).
				setFixCost(1000).
				build();
		CarrierVehicleType typeSix = CarrierVehicleType.Builder.
				newInstance(new IdImpl("6_tonner")).
				setCapacity(6000).
				setCostPerDistanceUnit(6).
				setFixCost(1000).
				build();
		CarrierVehicleType typeSeven = CarrierVehicleType.Builder.
				newInstance(new IdImpl("7_tonner")).
				setCapacity(7000).
				setCostPerDistanceUnit(7).
				setFixCost(1000).
				build();
		CarrierVehicleType typeTwelve = CarrierVehicleType.Builder.
				newInstance(new IdImpl("12_tonner")).
				setCapacity(12000).
				setCostPerDistanceUnit(12).
				setFixCost(1000).
				build();
		CarrierVehicleType typeFifteen = CarrierVehicleType.Builder.
				newInstance(new IdImpl("15_tonner")).
				setCapacity(15000).
				setCostPerDistanceUnit(15).
				setFixCost(1000).
				build();

		carrierVehicleTypes = new CarrierVehicleTypes();
		carrierVehicleTypes.getVehicleTypes().put(typeThree.getId(), typeThree);
		carrierVehicleTypes.getVehicleTypes().put(typeSix.getId(), typeSix);
		carrierVehicleTypes.getVehicleTypes().put(typeSeven.getId(), typeSeven);
		carrierVehicleTypes.getVehicleTypes().put(typeTwelve.getId(), typeTwelve);
		carrierVehicleTypes.getVehicleTypes().put(typeFifteen.getId(), typeFifteen);
		
		CarrierVehicleTypeWriter typeWriter = new CarrierVehicleTypeWriter(carrierVehicleTypes);
		typeWriter.write(vehicleTypeFile);
	}
}
