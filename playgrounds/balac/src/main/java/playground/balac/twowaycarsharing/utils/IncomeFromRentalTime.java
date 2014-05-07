package playground.balac.twowaycarsharing.utils;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class IncomeFromRentalTime {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		final BufferedReader readLink = IOUtils.getBufferedReader(args[0]);

		
		String s = readLink.readLine();
		s = readLink.readLine();
		int ff = 0;
		int rb = 0;
		int rentff = 0;
		int rentrb = 0;
		double timerb = 0.0;
		double timeff = 0.0;
		double timeffo = 0.0;
		double timeffi = 0.0;
		while (s != null) {
			
			String[] arr = s.split("\t");
			
			if (!arr[3].equals("NaN") && !arr[2].equals("NaN")) {
				if (arr[1].contains("c")) {
					if (Double.parseDouble(arr[3]) - Double.parseDouble(arr[2]) > 11 * 3600) {
						timerb += 11 * 3600;
					}
					else
						timerb += (Double.parseDouble(arr[3]) - Double.parseDouble(arr[2]));
					rb++;
				}
				else if (arr[1].contains("W")) {
					if (Double.parseDouble(arr[3]) < 36000 && Double.parseDouble(arr[2]) < 36000) {
						
						timeffo += (Double.parseDouble(arr[3]) - Double.parseDouble(arr[2]));
						
						
					}
					else if (Double.parseDouble(arr[3]) > 57600 && Double.parseDouble(arr[2]) > 57600) {
						
						timeffo += (Double.parseDouble(arr[3]) - Double.parseDouble(arr[2]));
						
						
					}
					else if (Double.parseDouble(arr[2]) < 36000 && Double.parseDouble(arr[3]) < 57600) {
						
						timeffo += (36000 -  Double.parseDouble(arr[2]));
						timeffi += (Double.parseDouble(arr[3]) - 36000);
						
						
					}
					else if (Double.parseDouble(arr[2]) > 36000 && Double.parseDouble(arr[3]) > 57600) {
						
						timeffo += (-57600 +  Double.parseDouble(arr[3]));
						timeffi += (57600 - Double.parseDouble(arr[2]));
						
						
					} 
					else if (Double.parseDouble(arr[2]) > 36000 && Double.parseDouble(arr[3]) < 57600) {
						
						
						timeffi += (Double.parseDouble(arr[3]) - Double.parseDouble(arr[2]));
						
						
					}
					else if (Double.parseDouble(arr[2]) < 36000 && Double.parseDouble(arr[3]) > 57600) {
						
						
						timeffi += 21600.0;
						
						
					}
						
					timeff += (Double.parseDouble(arr[3]) - Double.parseDouble(arr[2]));
					ff++;
				}}
				s = readLink.readLine();
			
			
		}
		
		System.out.println(timerb * 4.52 /3600.0 + " rb");
		System.out.println(timeff * 14.26 /3600.0+ " ff");
		System.out.println((timeffi * 7.23 + timeffo * 14.26) / 3600);
		
	}

}
