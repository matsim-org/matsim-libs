package playground.southafrica.sandboxes.qvanheerden.freight;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.util.Solutions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.*;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.filesampler.MyFileFilter;
import playground.southafrica.utilities.filesampler.MyFileSampler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MyCarrierPlanGenerator {
	private final static Logger log = Logger.getLogger(MyCarrierPlanGenerator.class);
	public static Coord depotCoord;
	public static Id<Link> depotLink;
	//	public CarrierVehicleTypes carrierVehicleTypes;
	//	public static Carrier carrier;
	//	public static Scenario scenario;
	//	public static String initialPlanAlgorithm;
	/**
	 * Trying to create carrier plans from the freight contrib [Oct 2013, update Feb 2014]
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
		String demandInputDir = args[3];
		String initialPlanAlgorithm = args[4];
		String changeEventsInputFile = args[5];
		String vehicleTypesFile = args[6];
		String carrierInput = args[7];
		String outputDir = args[8];

		/* Read network */
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("./output/");
		config.network().setInputFile(networkFile);
		config.network().setTimeVariantNetwork(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		//Add congestion
		int speed = 80; //in kmph
//		MyCarrierPlanGenerator.getNetworkChangeEvents(scenario, 7, 10, 16, 19, speed, true);

		/* Set coordinate and linkId of depot */
		depotCoord = new Coord(depotLong, depotLat);
		//		depotLink = ((NetworkImpl) network).getNearestLink((Coord) depotCoord).getId();
		depotLink = NetworkUtils.getNearestLink(((NetworkImpl) scenario.getNetwork()), depotCoord).getId();

		MyFileSampler mfs = new MyFileSampler(demandInputDir);
		List<File> files = mfs.sampleFiles(Integer.MAX_VALUE, new MyFileFilter(".csv"));
		for(File demandInputFile : files){
			String filename = demandInputFile.getName().substring(0, demandInputFile.getName().indexOf("."));
			/* Build vehicle types */
			MyCarrierPlanGenerator mcpg = new MyCarrierPlanGenerator();
			Carriers carriers = new Carriers();
			/* Create carrier and assign carrier capabilities to carrier */
			carriers.addCarrier(CarrierImpl.newInstance(Id.create("MyCarrier", Carrier.class)));
//			new CarrierPlanXmlReaderV2(carriers).read(carrierInput);

			CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
			new CarrierVehicleTypeReader(carrierVehicleTypes).read(vehicleTypesFile);

			new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(carrierVehicleTypes);

			Carrier carrier = carriers.getCarriers().get(Id.create("MyCarrier", Carrier.class));

//					mcpg.buildAndWriteVehicleTypes(vehicleTypeOutputFile, true);

					MyCarrierCapabilityGenerator mccg = new MyCarrierCapabilityGenerator();
//					carrier.setCarrierCapabilities(mccg.createVehicles(carrierVehicleTypes, depotLink, scenario.getNetwork()));
					carrier.setCarrierCapabilities(mccg.createVehicles(carrierVehicleTypes, depotLink));


			/* Parse shipments/demand and add to carrier 
			 * -for now these will be services until we figure out how to use shipments
			 */
//			int day = Integer.parseInt(demandInputFile.getName().substring(demandInputFile.getName().indexOf(".")-1,demandInputFile.getName().indexOf(".")));

			List<CarrierService> services = mcpg.parseDemand(demandInputFile.getAbsolutePath(), network, carrier);
			carrier.getServices().addAll(services);
			mcpg.createInitialPlans(carrier, network, initialPlanAlgorithm, carrierVehicleTypes);

			//			carriers.getCarriers().clear();
			carriers.getCarriers().put(Id.create("MyCarrier", Carrier.class), carrier);
			CarrierPlanXmlWriterV2 planWriter = new CarrierPlanXmlWriterV2(carriers);
			planWriter.write(outputDir + filename + "_" + speed +  ".xml");

			//			CarrierVehicleTypes types = CarrierVehicleTypes.getVehicleTypes(carriers);
			//			CarrierVehicleTypeWriter typeWriter = new CarrierVehicleTypeWriter(types);
			//			typeWriter.write(outputDir);
		}

		//new Visualiser(config, scenario).visualizeLive(carriers);

		Header.printFooter();

	}

	public void createInitialPlans(Carrier carrier, Network network, String initialPlanAlgorithm, CarrierVehicleTypes vehicleTypes){
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
//		vrpBuilder.setFleetSize(FleetSize.valueOf( carrier.getCarrierCapabilities().getFleetSize().name() ));
		
		
//				vrpBuilder.addPenaltyVehicles(10000, 100000);
		NetworkBasedTransportCosts.Builder costsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, vehicleTypes.getVehicleTypes().values());
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

	public static void getNetworkChangeEvents(Scenario scenario, double amStart, double amEnd, double pmStart, double pmEnd, int kmph, boolean allLinks) {
		
		//first create bounding box for area to limit congestion to
		double top=-33.5379, left=25.168, bottom=-34.0788, right=25.8986;

		Coordinate leftTop = new Coordinate(left, top);
		Coordinate rightTop = new Coordinate(right, top);
		Coordinate leftBottom = new Coordinate(left, bottom);
		Coordinate rightBottom = new Coordinate(right, bottom);

		Coordinate[] coordArray = {leftTop, rightTop, rightBottom, leftBottom, leftTop};

		GeometryFactory gf = new GeometryFactory();
		Polygon poly = gf.createPolygon(coordArray);
		
		//now apply congestion
		NetworkChangeEventFactory cef = new NetworkChangeEventFactoryImpl();

		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			boolean contains = false;
			
			if(!allLinks){
				Coordinate coordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
				Point p = gf.createPoint(coordinate);
				contains = poly.contains(p);
			}else{
				contains = true;
			}
			
			if(contains){

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
		log.info("Added congestion...");
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
	 */
	public List<CarrierService> parseDemand(String demandFile, Network network, Carrier carrier){
		List<CarrierService> services = new ArrayList<CarrierService>();
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
				double start = Double.parseDouble(array[7]);
				double end = Double.parseDouble(array[8]);

				Coord coord = new Coord(longi, lati);
				Id<Link> linkId = NetworkUtils.getNearestLink(((NetworkImpl) network), coord).getId();

				CarrierService serv = CarrierService.Builder.newInstance(Id.create(i, CarrierService.class), linkId).
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
		return services;
	}



}
