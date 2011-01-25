package air;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;

public class SfAirNetworkBuilder {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		String output = "/home/soeren/workspace/testnetzwerk";

		NetworkImpl network = NetworkImpl.createNetwork();
		
		BufferedReader brAirports = new BufferedReader(new FileReader(new File("/home/soeren/workspace/OsmTest.txt")));
		BufferedReader brRoutes = new BufferedReader(new FileReader(new File("/home/soeren/workspace/cityPairs.txt")));
		
		while (brAirports.ready()) {
			String oneLine = brAirports.readLine();
			String[] lineEntries = new String[2];
			lineEntries = oneLine.split("\t");
			String airportCode = lineEntries[0];
			String[] coordinates = new String[2];
			coordinates = lineEntries[1].split("y");
			String xValue = coordinates[0].substring(3);
			String yValue = coordinates[1].substring(1);
			xValue = xValue.replaceAll("\\]", "");
			xValue = xValue.replaceAll("\\[", "");
			yValue = yValue.replaceAll("\\[", "");
			yValue = yValue.replaceAll("\\]", "");
			
			Coord airportCoord = new CoordImpl(Double.parseDouble(xValue), Double.parseDouble(yValue));
			
			new MATSimAirport(new IdImpl(airportCode), airportCoord).createRunways(network);			
		}
		
		int ii = 0;
		
		while (brRoutes.ready()) {
			String oneLine = brRoutes.readLine();
			String origin = oneLine.substring(0, 3);
			String destination = oneLine.substring(3, 6);
			System.out.println(oneLine);
			Id originRunway = new IdImpl(origin+"runwayOutbound");
			Id destinationRunway = new IdImpl(destination+"runwayInbound");
			network.addLink(network.getFactory().createLink(new IdImpl(origin+destination), originRunway, destinationRunway));
			ii++;

		}
		

		
		new NetworkWriter(network).write(output + ".xml.gz");
		System.out.println("Done! Unprocessed MATSim Network saved as " + output + ".xml.gz");
		
		brAirports.close();
		brRoutes.close();
		
		
	}

}
