package playground.balac.twowaycarsharing.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.core.utils.io.IOUtils;

public class UsersCSTable {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedReader readLink1 = IOUtils.getBufferedReader(args[0]);
		final BufferedReader readLink2 = IOUtils.getBufferedReader(args[1]);
		final BufferedReader readLink3 = IOUtils.getBufferedReader(args[2]);
		final BufferedReader readLink4 = IOUtils.getBufferedReader(args[3]);
		final BufferedReader readLink5 = IOUtils.getBufferedReader(args[4]);
		final BufferedWriter outLinkff = IOUtils.getBufferedWriter(args[5]);
		final BufferedWriter outLinkrb = IOUtils.getBufferedWriter(args[6]);
		Set<String> a1 = new TreeSet<String>();
		
		Set<String> aff1 = new TreeSet<String>();
		Set<String> arb1 = new TreeSet<String>();
		Set<String> aff2 = new TreeSet<String>();
		Set<String> arb2 = new TreeSet<String>();
		Set<String> aff3 = new TreeSet<String>();
		Set<String> arb3 = new TreeSet<String>();
		Set<String> aff4 = new TreeSet<String>();
		Set<String> arb4 = new TreeSet<String>();
		Set<String> aff5 = new TreeSet<String>();
		Set<String> arb5 = new TreeSet<String>();
		String s = readLink1.readLine();
		s = readLink1.readLine();
		while (s != null) {
			
			String[] arr = s.split("\t");

			a1.add(arr[0]);
			
			if (arr[1].contains("W"))
				aff1.add(arr[0]);
			else
				arb1.add(arr[0]);
			s = readLink1.readLine();
		}
		
		s = readLink2.readLine();
		s = readLink2.readLine();
		while (s != null) {
			
			String[] arr = s.split("\t");
			if (arr[1].contains("W"))
				aff2.add(arr[0]);
			else
				arb2.add(arr[0]);
			a1.add(arr[0]);
			s = readLink2.readLine();
		}
		
		s = readLink3.readLine();
		s = readLink3.readLine();
		while (s != null) {
			
			String[] arr = s.split("\t");
			if (arr[1].contains("W"))
				aff3.add(arr[0]);
			else
				arb3.add(arr[0]);
			a1.add(arr[0]);
			s = readLink3.readLine();
		}
		
		s = readLink4.readLine();
		s = readLink4.readLine();
		while (s != null) {
			
			String[] arr = s.split("\t");
			if (arr[1].contains("W"))
				aff4.add(arr[0]);
			else
				arb4.add(arr[0]);
			a1.add(arr[0]);
			s = readLink4.readLine();
		}
		
		s = readLink5.readLine();
		s = readLink5.readLine();
		while (s != null) {
			
			String[] arr = s.split("\t");
			if (arr[1].contains("W"))
				aff5.add(arr[0]);
			else
				arb5.add(arr[0]);
			a1.add(arr[0]);
			s = readLink5.readLine();
		}
		

		for (String ss: a1){
			
			outLinkff.write(ss + " ");
			if (aff1.contains(ss))
				outLinkff.write("1 ");
			else
				outLinkff.write("0 ");
			if (aff2.contains(ss))
				outLinkff.write("1 ");
			else
				outLinkff.write("0 ");
			if (aff3.contains(ss))
				outLinkff.write("1 ");
			else
				outLinkff.write("0 ");
			if (aff4.contains(ss))
				outLinkff.write("1 ");
			else
				outLinkff.write("0 ");
			if (aff5.contains(ss))
				outLinkff.write("1");
			else
				outLinkff.write("0");
			outLinkff.newLine();
			
		}
		for (String ss: a1){
			
			outLinkrb.write(ss + " ");
			if (arb1.contains(ss))
				outLinkrb.write("1 ");
			else
				outLinkrb.write("0 ");
			if (arb2.contains(ss))
				outLinkrb.write("1 ");
			else
				outLinkrb.write("0 ");
			if (arb3.contains(ss))
				outLinkrb.write("1 ");
			else
				outLinkrb.write("0 ");
			if (arb4.contains(ss))
				outLinkrb.write("1 ");
			else
				outLinkrb.write("0 ");
			if (arb5.contains(ss))
				outLinkrb.write("1");
			else
				outLinkrb.write("0");
			outLinkrb.newLine();
			
		}
		
		outLinkff.close();
		outLinkrb.close();
	}

}
