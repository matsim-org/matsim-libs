package air.scenario;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

public class SfUtcOffset {
	
	public static void main(String args[]) throws Exception {
		SfUtcOffset test = new SfUtcOffset();
		test.getUtcOffset(new CoordImpl(52.8,8.08));
		
	}
	
	public int getUtcOffset(Coord coord) throws IOException, InterruptedException {
		
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
