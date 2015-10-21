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
		
			dep[i - 1] = Integer.parseInt(array[10]);
			arr[i - 1] = Integer.parseInt(array[11]);
			en[i - 1] = Integer.parseInt(array[13]);
			s = readLink.readLine();
			i++;
			
		}
		for(int j = 0; j < 300; j ++) {
			
			outLinkd.write(String.valueOf(dep[j]));
			outLinka.write(String.valueOf(arr[j]));
			outLinke.write(String.valueOf(en[j]));
			outLinkd.newLine();
			outLinka.newLine();
			outLinke.newLine();
		}
		outLinkd.flush(); outLinkd.close();
		outLinka.flush(); outLinka.close();
		outLinke.flush(); outLinke.close();

	}

}
