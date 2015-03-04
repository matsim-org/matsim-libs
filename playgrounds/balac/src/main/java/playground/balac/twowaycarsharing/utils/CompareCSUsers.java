package playground.balac.twowaycarsharing.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.core.utils.io.IOUtils;

public class CompareCSUsers {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedReader readLink1 = IOUtils.getBufferedReader(args[0]);
		final BufferedReader readLink2 = IOUtils.getBufferedReader(args[1]);

		Set<String> a1 = new TreeSet<String>();
		Set<String> a2 = new TreeSet<String>();
		String s = readLink1.readLine();
		s = readLink1.readLine();
		String previous = null;
		int ff = 0;
		int rb = 0;
		int rentff = 0;
		int rentrb = 0;
		while (s != null) {
			
			String[] arr = s.split("\t");
			
			
			a1.add(arr[0]);
			s = readLink1.readLine();
			
		}
		
		String s1 = readLink2.readLine();
		s1 = readLink2.readLine();
		previous = null;
		
		while (s1 != null) {
			
			String[] arr = s1.split("\t");
			
			if (arr[1].contains("W"))
				a2.add(arr[0]);
			s1 = readLink2.readLine();
			
		}
		
		int same = 0;
		int not = 0;
		for(String s2:a1){
			if (a2.contains(s2))
				same++;
			else
				not++;
		}
		
		System.out.println(same);
		System.out.println(not);
		

	}

}
