package playground.balac.twowaycarsharing.utils;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class AverageRentalTimes {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedReader readLink = IOUtils.getBufferedReader(args[0]);

		
		String s = readLink.readLine();
		s = readLink.readLine();
		String previous = null;
		int ff = 0;
		int rb = 0;
		int rentff = 0;
		int rentrb = 0;
		double timerb = 0.0;
		double timeff = 0.0;
		while (s != null) {
			
			String[] arr = s.split("\t");
			
			if (!arr[3].equals("NaN") && !arr[2].equals("NaN")) {
			if (arr[1].contains("c")) {
				
				timerb += (Double.parseDouble(arr[3]) - Double.parseDouble(arr[2]));
				rb++;
			}
			else if (arr[1].contains("W")) {
				
				timeff += (Double.parseDouble(arr[3]) - Double.parseDouble(arr[2]));
				ff++;
			}}
			s = readLink.readLine();
		}
		System.out.println(timerb/rb);
		System.out.println(timeff/ff);
		

	}

}
