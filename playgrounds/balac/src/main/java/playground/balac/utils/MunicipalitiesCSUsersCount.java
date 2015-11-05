package playground.balac.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;


public class MunicipalitiesCSUsersCount {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
		final BufferedReader readLink1 = IOUtils.getBufferedReader(args[1]);
		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/" + "matrix_interval_peak.txt");

		int[][] mat = new int[12][12];
		
		double centerX = 683217.0; 
		double centerY = 247300.0;
		
		double originX = centerX - 12000.0;
		double endX = centerX + 12000.0;
		double originY = centerY + 12000.0;
		double endY = centerY - 12000.0;
		readLink1.readLine();
		String s = readLink1.readLine();
		int count = 0;
		while( s != null) {
			String[] arr = s.split("\\s");
		
			if (true) {
				
				double x = scenario.getNetwork().getLinks().get(Id.create(arr[3], Link.class)).getCoord().getX();
				double y = scenario.getNetwork().getLinks().get(Id.create(arr[3], Link.class)).getCoord().getY();
				
				if (x >= originX && y <= originY && x <= endX && y >= endY) {
					
					int k = (int)((x - originX) / 2000);
					int j = (int)((-y + originY) / 2000);
					mat[k][j]++;
					count++;
					
				}
			
			}
			
			s = readLink1.readLine();
		}
		for(int i = 0; i < 12; i++) { 
			for(int m = 0; m < 12; m++) {
				
				outLink.write(Double.toString(mat[i][m]) + " ");
				
			}
			outLink.newLine();
		}
		outLink.write(mat[5][5] + " " );
		
		int sum = 0;
		for(int i = 4; i < 7; i++) { 
			for(int m = 4; m < 7; m++) {
				sum += mat[i][m];				
			}
		}
		outLink.write(sum - mat[5][5]  + " " );
		
		int sum1 = 0;
		for(int i = 2; i < 9; i++) { 
			for(int m = 2; m < 9; m++) {
				sum1 += mat[i][m];				
			}
		}
		outLink.write(sum1 - (sum + mat[5][5])  + " " );
		outLink.flush();
		outLink.close();
System.out.println("done");
	}

}
