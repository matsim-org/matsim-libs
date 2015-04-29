package playground.balac.utils.emissions;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class DistanceTravelTimeCalculator {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		BufferedReader reader = IOUtils.getBufferedReader(args[0]);

		String s = reader.readLine();
		double distance = 0.0;
		double travelTime = 0.0;
		
		while (s!=null) {
			
			String[] arr = s.split(";");
			
			for (int i = 0; i < (arr.length - 1) / 4; i ++) {
				
				if (arr[i * 4 + 1].startsWith("K")) {
					
					distance += Double.parseDouble(arr[i * 4 + 4]);
					travelTime += Double.parseDouble(arr[i * 4 + 4]) / Double.parseDouble(arr[i * 4 + 2]);
				}
			}
			
			s = reader.readLine();
		}
		
		reader.close();
		System.out.println(distance);
		System.out.println(travelTime);

		
	}

}
