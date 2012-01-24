package air.scenario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


public class SfUtcOffset {
	
//	public static void main(String args[]) throws Exception {
//		SfUtcOffset test = new SfUtcOffset();
//		test.getUtcOffset(new CoordImpl(52.8,8.08));
//		
//	}
	
	private Map<String, Coord> airportsInOsm;
	
	public void writeUtcOffset(String inputOsmFile, String outputFile) throws IOException, InterruptedException{
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
		SfOsmAerowayParser osmReader = new SfOsmAerowayParser(
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.WGS84));
		osmReader.parse(inputOsmFile);
		this.airportsInOsm = osmReader.airports;
		
		Iterator it = this.airportsInOsm.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			bw.write(pairs.getKey().toString() + "\t" + getUtcOffset(this.airportsInOsm.get(pairs.getKey())));
			bw.newLine();
		}
		bw.close();
	}
	
	public static int getUtcOffset(Coord coord) throws IOException, InterruptedException {
		
		Thread.sleep(2*1000);
		
		String inputURL = "http://www.earthtools.org/timezone/"+coord.getX()+"/"+coord.getY();	
		
		URL oracle = new URL(inputURL);
		BufferedReader in = new BufferedReader(
		new InputStreamReader(oracle.openStream()));
		
		String inputLine;
		
		int offset = 0;
		
		while ((inputLine = in.readLine()) != null) {
			
			if (inputLine.contains("offset")) {
				inputLine = inputLine.replaceAll("<offset>", "");
				inputLine = inputLine.replaceAll("</offset>", "");
				inputLine = inputLine.replaceAll(" ", "");
				offset = Integer.parseInt(inputLine);
//				System.out.println(offset);
			}
			else if (inputLine.contains("dst")) {
				inputLine = inputLine.replaceAll("<dst>", "");
				inputLine = inputLine.replaceAll("</dst>", "");
				inputLine = inputLine.replaceAll(" ", "");
				if (inputLine.equalsIgnoreCase("true")) offset++;
//				System.out.println("neu"+offset);
//				System.out.println(inputLine);
			}
			
		}
		
		in.close();
	    	    

		return offset;
		
		
		
	}

}
