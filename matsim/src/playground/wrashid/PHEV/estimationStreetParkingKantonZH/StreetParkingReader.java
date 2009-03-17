package playground.wrashid.PHEV.estimationStreetParkingKantonZH;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.utils.geometry.CoordImpl;

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
			StreetParkingReader currentParkingData = null;
			String token = null;
			Coord coord=null;
			while (line != null) {
				coord=new CoordImpl(0,0);
				
				
				tokenizer = new StringTokenizer(line);
				currentParkingData = new StreetParkingReader();

				token = tokenizer.nextToken();
				coord.setX(Double.parseDouble(token));
				
				token = tokenizer.nextToken();
				coord.setY(Double.parseDouble(token));

				
				streetData.add(coord);
				line = br.readLine();

			}

		
		} catch (Exception ex) {
			System.out.println(ex);
		}

		return streetData;
	}
}
