package playground.balac.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class ScalingOfStations {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		final BufferedReader readLink1 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/Stations_GreaterZurich.txt");

		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/Stations_GreaterZurich_10%.txt");
		//String para = readLink1.readLine();
		//outLink.write(para);
		outLink.newLine();
		int total = 0;
		String s = readLink1.readLine();
		while(s!=null) {
		
			
			String[] arr1 = s.split("\t");
			
			outLink.write(arr1[0] + "\t" + arr1[1] + "\t" +arr1[2] + "\t" +arr1[3] + "\t" +arr1[4] + "\t" +arr1[5] + "\t");
			
			int number = Integer.parseInt(arr1[6]);
			number /= 10;
			number++;
			total += number;
			//if (number < 5) 
			//	outLink.write(Integer.toString(1));
			//else
		//		outLink.write(Integer.toString(2));
			outLink.write(Integer.toString(number));
			outLink.newLine();
			outLink.flush();
			s = readLink1.readLine();

		}
		outLink.close();

	}

}
