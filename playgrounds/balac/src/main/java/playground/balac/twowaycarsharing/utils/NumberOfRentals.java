package playground.balac.twowaycarsharing.utils;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class NumberOfRentals {

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
		while (s != null) {
			
			String[] arr = s.split("\t");
			
			if (!arr[1].equals(previous)) {
				if (arr[1].startsWith("O")) {
					
					ff++;
				}
				else 
					rb++;
			}
			
			if (arr[1].startsWith("O"))
				rentff++;
			else rentrb++;
			previous = arr[1];
			s = readLink.readLine();
			
		}
		
		System.out.println(rb);
		System.out.println(ff);
		System.out.println(rentrb);
		System.out.println(rentff);

	}

}
