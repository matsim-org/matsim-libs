package playground.balac.utils.parking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.io.IOUtils;

public class GarageParkingCounts {

	public static void main(String[] args) throws IOException {
		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/parkingGarageCountsCityZH27-April-2011_enriched.txt");
		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/parkingGarageCountsCityZH27-April-2011_enriched_v1.txt");
		final BufferedWriter outTotalOccupancy = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/parkingGarageTotalCounts.txt");
		final BufferedWriter outTotalOccupancyV2 = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/parkingGarageTotalCounts_v2.txt");

		int[][] jj = new int[23][4287];
		boolean[] closed = new boolean[23];
		Map<String, String> parkingMap = new HashMap<String, String>();
		int total = 0;
		int k = 0;
		int n = 0;
		for(int i =0; i < 34; i++) {
			
			String s = readLink.readLine();
			
			String[] arr = s.split("\t");
			String[] arrNumber = s.split("/");

			if (!arr[0].equals("-1")) {
				outLink.write(s);
				outLink.newLine();
				parkingMap.put(arr[2], arr[0]);
				if (!arrNumber[2].contains("---") ) {
					String str = arrNumber[2].replaceAll("[^\\d.]", "");
					total += Integer.parseInt(str);
					jj[k][n] = Integer.parseInt(str);
					closed[k] = false;
				}
				else {
					
					closed[k] = true;
				}
				k++;
				
			}		

			
		}
		n++;
		outTotalOccupancy.write(Integer.toString(total));
		outTotalOccupancy.newLine();
		String s =readLink.readLine();
		int j = 0;
		total = 0;
		while (s!=null) {
			
			String[] arr = s.split("\t");
			String[] arrNumber = s.split("/");

			if (arr.length < 2)
				System.out.println();
			if (parkingMap.containsKey(arr[1])) {
				
				outLink.write(parkingMap.get(arr[1]) + "\t" + s);
				outLink.newLine();
				if (j < 22) {
					j++;
					if (!arrNumber[2].contains("---") && !arrNumber[2].equals("")) {
						String str = arrNumber[2].replaceAll("[^\\d.]", "");
						jj[j][n] = Integer.parseInt(str);
						
						if (closed[j]) {
							
							for (int h = 0; h < n; h++) {
								
								if (jj[j][h] == -1)
									jj[j][h] = Integer.parseInt(str);
								
							}
							closed[j] = false;
							
						}
						total += Integer.parseInt(str);
					}
					else {
						
						if (!closed[j]) {	
							jj[j][n] = jj[j][n - 1];
							
						}
						else jj[j][n] = -1;
					}
				}
				else {
					j=0;
					outTotalOccupancy.write(Integer.toString(total));
					outTotalOccupancy.newLine();
					total = 0;
					n++;
				}
				
			}
			
			s = readLink.readLine();
			
			
		}
		
		
		
		for (int i = 0; i < 4287; i++) {
			int totala = 0;
			for (j = 0; j < 23; j++)
				totala += jj[j][i];
			outTotalOccupancyV2.write(Integer.toString(totala));
			outTotalOccupancyV2.newLine();
		}
			
		outLink.flush();
		outLink.close();
		outTotalOccupancy.flush();
		outTotalOccupancy.close();
		outTotalOccupancyV2.flush();
		outTotalOccupancyV2.close();
		readLink.close();
		
	}

}
