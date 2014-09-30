package playground.balac.utils.carsharingusers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

public class UsersAnalysis {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedReader readLink1 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/STRC_Temp/CSTW_Stats_1.txt");

		final BufferedReader readLink2 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/STRC_Temp/CSTW_Stats_2.txt");
		
		final BufferedReader readLink3 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/STRC_Temp/FF_Stats.txt");

		Set<String> bla = new HashSet<String>();
		Set<String> bla1 = new HashSet<String>();
		Set<String> bla2 = new HashSet<String>();


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
				bla1.add((arr[0]));
			s = readLink2.readLine();
		
			
		}
		s = readLink3.readLine();
		System.out.println(bla1.size());
		while(s != null) {
			String[] arr = s.split("\\s");
			
			if (bla.contains((arr[0])))
				bla2.add((arr[0]));
			s = readLink3.readLine();
		
			
		}
		
		for (String s1 : bla1) {
			
			if (bla2.contains(s1))
				count++;
			
		}
		
		System.out.println(count);

	}

}
