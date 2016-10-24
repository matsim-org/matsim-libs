package playground.wrashid.PHEV.estimationStreetParkingKantonZH;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.matsim.api.core.v01.Coord;

public class StreetParkingReader {
	public static LinkedList<Coord> readData(String path) {
		LinkedList<Coord> streetData = new LinkedList<Coord>();

		try {

			FileReader fr = new FileReader(path);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just
									// contains column headers
			line = br.readLine();
			Coord coord=null;
			while (line != null) {
				
				
				tokenizer = new StringTokenizer(line);

				String token1 = tokenizer.nextToken();
				
				String token2 = tokenizer.nextToken();

				coord = new Coord( Double.parseDouble(token1), Double.parseDouble(token2));

				
				streetData.add(coord);
				line = br.readLine();

			}

		
		} catch (Exception ex) {
			System.out.println(ex);
		}

		return streetData;
	}
}
