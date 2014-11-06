package air.scenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


public class SfAirNetworkBuilder {
	
	private static final Logger log = Logger.getLogger(SfAirNetworkBuilder.class);
	
	public static final String NETWORK_FILENAME = "air_network.xml";
	private static final double MACH_2 = 686.0;
	public static final double CAP_PERIOD = 3600.0;
	
	public final Map<String, Double> STARoffset = new HashMap<String, Double>();

	public void createNetwork(String Airports, String cityPairs, String networkOutputFilename) throws IOException {
		int airportcounter = 0;
		int linkcounter = 0;
		
		Set<String> allowedModes = new HashSet<String>();
		allowedModes.add("pt");
		allowedModes.add("car");

		NetworkImpl network = NetworkImpl.createNetwork();
		network.setCapacityPeriod(CAP_PERIOD);	
		
		BufferedReader brAirports = new BufferedReader(new FileReader(new File(Airports)));
		BufferedReader brRoutes = new BufferedReader(new FileReader(new File(cityPairs)));
		
		CoordinateTransformation coordtransform =
			TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3395");
		
		while (brAirports.ready()) {
			String oneLine = brAirports.readLine();
			String[] lineEntries = oneLine.split("\t");
			String airportCode = lineEntries[0];
			double xValue = Double.parseDouble(lineEntries[1]);
			double yValue = Double.parseDouble(lineEntries[2]);		
			Coord coord = new CoordImpl(xValue, yValue);
			Coord airportCoord = coordtransform.transform(coord);
			airportcounter++;
			if (DgCreateSfFlightScenario.NUMBER_OF_RUNWAYS==2)
				new SfMatsimAirport(Id.create(airportCode, Node.class), airportCoord, null).createTwoRunways(network);
			else
				new SfMatsimAirport(Id.create(airportCode, Node.class), airportCoord, null).createOneRunway(network);
		}
		
		while (brRoutes.ready()) {
			String oneLine = brRoutes.readLine();
			String[] lineEntries = oneLine.split("\t");
			String[] airportCodes = lineEntries[0].split("_");
			double length = Double.parseDouble(lineEntries[1])*1000;	//distance between O&D in meters
			double groundSpeed = MACH_2;	
//			double duration = Double.parseDouble(lineEntries[2]);
//			groundSpeed = Math.round(100*length/(duration-SfMatsimAirport.TAXI_TOL_TIME))/100.;	//set for older MATSim version, where max. speed in VehicleType ist not supported
			String origin = airportCodes[0];
			String destination = airportCodes[1];
			
			Id originRunwayOutNodeId;
			if (DgCreateSfFlightScenario.NUMBER_OF_RUNWAYS==2) {
				originRunwayOutNodeId = Id.create(origin+"runwayOutbound", Node.class);
			}
			else {
				originRunwayOutNodeId = Id.create(origin+"runway", Node.class);
			}
			
			Node destinationNode = null;
			if (DgCreateSfFlightScenario.doCreateStars) {
				Id<Node> destinationStar = Id.create(destination+"star", Node.class);
				destinationNode = network.getNodes().get(destinationStar);
			}
			else {
				if (DgCreateSfFlightScenario.NUMBER_OF_RUNWAYS == 2){
					destinationNode = network.getNodes().get(Id.create(destination + "runwayInbound", Node.class));
				}
				else {
					destinationNode = network.getNodes().get(Id.create(destination + "runway", Node.class));
				}
			}
			
			Link originToDestination = network.getFactory().createLink(Id.create(origin+destination, Link.class), 
					network.getNodes().get(originRunwayOutNodeId), destinationNode);
			originToDestination.setAllowedModes(allowedModes);

			originToDestination.setCapacity(1.0*CAP_PERIOD);
			originToDestination.setFreespeed(groundSpeed);
			originToDestination.setLength(length);
			network.addLink(originToDestination);
			linkcounter++;
		}
			
		new NetworkWriter(network).write(networkOutputFilename);
		
		log.info("Done! Unprocessed MATSim Network saved as " + networkOutputFilename);
		
		log.info("Anzahl Flughäfen: "+airportcounter);
		log.info("Anzahl Links: "+linkcounter);
		
		brAirports.close();
		brRoutes.close();
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String baseDirectory = "/home/dgrether/shared-svn/studies/countries/eu/flight/sf_oag_flight_model/";
				
		String output = baseDirectory + NETWORK_FILENAME;
		String osmAirports = baseDirectory + SfAirScheduleBuilder.AIRPORTS_OUTPUT_FILE;
		String cityPairs = baseDirectory + SfAirScheduleBuilder.CITY_PAIRS_OUTPUT_FILENAME;
		SfAirNetworkBuilder builder = new SfAirNetworkBuilder();
		builder.createNetwork(osmAirports, cityPairs, output);
	}

	
}
