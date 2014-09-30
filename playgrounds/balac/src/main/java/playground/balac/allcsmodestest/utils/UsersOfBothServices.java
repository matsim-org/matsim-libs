package playground.balac.allcsmodestest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

public class UsersOfBothServices {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedReader readLink1 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/STRC_Temp/FF_Stats.txt");

		final BufferedReader readLink2 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/STRC_Temp/CSTW_Stats.txt");

		Set<String> bla = new HashSet<String>();

		readLink1.readLine();
		readLink2.readLine();
		String s = readLink1.readLine();
		
		while(s != null) {
			String[] arr = s.split("\\s");
			
			bla.add((arr[0]));
			
			s = readLink1.readLine();
		
			
		}
		int count = 0;
		s = readLink2.readLine();
		
		while(s != null) {
			String[] arr = s.split("\\s");
			
			if (bla.contains((arr[0])))
				count++;
			s = readLink2.readLine();
		
			
		}
		
		System.out.println(count);

	}

}
