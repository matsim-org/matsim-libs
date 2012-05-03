package air.scenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


public class SfAirNetworkBuilder {

	public static final String NETWORK_FILENAME = "air_network.xml";
	private static final double MACH_2 = 686.0;	

	public void createNetwork(String Airports, String cityPairs, String networkOutputFilename) throws IOException {
		int airportcounter = 0;
		int linkcounter = 0;
		
		Set<String> allowedModes = new HashSet<String>();
		allowedModes.add("pt");
		allowedModes.add("car");

		NetworkImpl network = NetworkImpl.createNetwork();
		network.setCapacityPeriod(60.0);	
		
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
			new SfMatsimAirport(new IdImpl(airportCode), airportCoord).createRunways(network);			
		}
		
		
		while (brRoutes.ready()) {
			String oneLine = brRoutes.readLine();
			String[] lineEntries = oneLine.split("\t");
			String[] airportCodes = lineEntries[0].split("_");
			double length = Double.parseDouble(lineEntries[1])*1000;	//distance between O&D in meters
			double groundSpeed = MACH_2;	
			String origin = airportCodes[0];
			String destination = airportCodes[1];
			
			Id originRunway = new IdImpl(origin+"runwayOutbound");
			Id destinationRunway = new IdImpl(destination+"runwayInbound");
			Link originToDestination = network.getFactory().createLink(new IdImpl(origin+destination), network.getNodes().get(originRunway), network.getNodes().get(destinationRunway));
			originToDestination.setAllowedModes(allowedModes);
			originToDestination.setCapacity(1.0);
			originToDestination.setFreespeed(groundSpeed);
			originToDestination.setLength(length);
			network.addLink(originToDestination);
			linkcounter++;
		}
			
		new NetworkWriter(network).write(networkOutputFilename);
		System.out.println("Done! Unprocessed MATSim Network saved as " + networkOutputFilename);
		
		System.out.println("Anzahl Flughäfen: "+airportcounter);
		System.out.println("Anzahl Links: "+linkcounter);
		
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
