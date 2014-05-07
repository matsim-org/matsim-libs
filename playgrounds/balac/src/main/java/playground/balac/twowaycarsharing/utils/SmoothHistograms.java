package playground.balac.twowaycarsharing.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class SmoothHistograms {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedReader readLink = IOUtils.getBufferedReader(args[0]);
		final BufferedWriter outLinkd = IOUtils.getBufferedWriter(args[1]);
		final BufferedWriter outLinka = IOUtils.getBufferedWriter(args[2]);
		final BufferedWriter outLinke = IOUtils.getBufferedWriter(args[3]);
		String s = readLink.readLine();
		s = readLink.readLine();
		s = readLink.readLine();
		
		int[] dep = new int[300];
		int[] arr = new int[300];
		int[] en = new int[300];
		int i = 1;
		while (s != null && i <= 300) {
			
			String[] array = s.split("\t");
		
			dep[i - 1] = Integer.parseInt(array[18]);
			arr[i - 1] = Integer.parseInt(array[19]);
			en[i - 1] = Integer.parseInt(array[21]);
			s = readLink.readLine();
			i++;
			
		}
		double l = 0.5;
		for(int j = 0; j < 295; j += 6) {
			int sumd = 0;
			int suma = 0;
			int sume = 0;
			
			for(int k = j; k < j + 6; k++) {
				
				sumd += dep[k];
				suma += arr[k];
				sume += en[k];
			}
			
			outLinkd.write(String.valueOf(sumd));
			outLinka.write(String.valueOf(suma));
			outLinke.write(String.valueOf(sume));
			outLinkd.newLine();
			outLinka.newLine();
			outLinke.newLine();
			l +=0.5;
		}
		outLinkd.flush(); outLinkd.close();
		outLinka.flush(); outLinka.close();
		outLinke.flush(); outLinke.close();

	}

}
