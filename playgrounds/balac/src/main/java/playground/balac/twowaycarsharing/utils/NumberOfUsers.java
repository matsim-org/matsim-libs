package playground.balac.twowaycarsharing.utils;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class NumberOfUsers {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedReader readLink = IOUtils.getBufferedReader(args[0]);

		String s = readLink.readLine();
		s = readLink.readLine();
		String previous = null;
		int ff = 0;
		int rb = 0;
		boolean addrb = false;
		boolean addff = false;
		int both = 0;
		while (s != null) {
			
			String[] arr = s.split("\t");
			
			if (!arr[0].equals(previous)) {
				if (addff && addrb)
					both++;
				addff = false;
				addrb = false;
				if (arr[1].startsWith("O")) {
					
					ff++;
					addff = true;
				}
				else {
					rb++;
					addrb = true;
				}
			}
			else {
				
				if (arr[1].startsWith("O")) {
					if (!addff){
						ff++;
						addff = true;
					}
				}
				else {
					if (!addrb){
						rb++;
						addrb = true;
					}
				}
			}
			
			
			previous = arr[0];
			s = readLink.readLine();
			
		}
		
		System.out.println(rb);
		System.out.println(ff);
		System.out.println(both);

	}

}
