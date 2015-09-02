package playground.balac.allcsmodestest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

public class UsersOfBothServices {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedReader readLink1 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/1.250.RT_CS_1");

		final BufferedReader readLink2 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/1.250.RT_CS");

		Set<String> bla = new HashSet<String>();

		readLink1.readLine();
		readLink2.readLine();
		String s = readLink1.readLine();
		
		while(s != null) {
			String[] arr = s.split("\\s");
			
			bla.add((arr[7]));
			
			s = readLink1.readLine();
		
			
		}
		int count = 0;
		s = readLink2.readLine();
		
		while(s != null) {
			String[] arr = s.split("\\s");
			
			if (bla.contains((arr[7]))) {
				count++;
				bla.remove(arr[7]);
			}
			s = readLink2.readLine();
		
			
		}
		
		System.out.println(count);

	}

}
