package playground.balac.utils.csstations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

public class ZoneStationsCreator {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedReader readLink1 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/Stations_GreaterZurich.txt");

		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/Stations_GreaterZurich_zone10km.txt");
		
		double centerX = 683217.0; 
		double centerY = 247300.0;
		Coord coord = new Coord(centerX, centerY);
		//String para = readLink1.readLine();
		//outLink.write(para);
		outLink.newLine();
		int total = 0;
		String s = readLink1.readLine();
		boolean write =true;
		while(s!=null) {		
			
			String[] arr1 = s.split("\t");
			
			
			Coord coordS = new Coord(Double.parseDouble(arr1[2]), Double.parseDouble(arr1[3]));
			
			if (CoordUtils.calcDistance(coordS, coord) < 10000) {
				if (write) {
				outLink.write(arr1[0] + "\t" + arr1[1] + "\t" +arr1[2] + "\t" +arr1[3] + "\t" +arr1[4] + "\t" +arr1[5] + "\t" + "1");
				outLink.newLine();
				outLink.flush();
				total += Integer.parseInt(arr1[6]);
				write = false;
				}
				else
					write = true;
				
			}
			
			
			s = readLink1.readLine();

		}
		System.out.println(total);
		outLink.close();

	}

}
