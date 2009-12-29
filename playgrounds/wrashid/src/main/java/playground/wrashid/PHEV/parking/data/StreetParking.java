package playground.wrashid.PHEV.parking.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

public class StreetParking {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			
			
			FileReader fr = new FileReader(
					"C:\\data\\Projekte\\ETH TH-22 07-3 PHEV\\Parkhäuser\\facilities\\input\\streetParking2007_1.txt");
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just contains column headers
			line = br.readLine();
			StreetParkingData currentParkingData=null;
			String token = null;
			while (line != null) {
				tokenizer = new StringTokenizer(line);
				currentParkingData=new StreetParkingData();
				
				token = tokenizer.nextToken();
				currentParkingData.x=Double.parseDouble(token);

				token = tokenizer.nextToken();
				currentParkingData.y=Double.parseDouble(token);
				
				token = tokenizer.nextToken();
				currentParkingData.id=Integer.parseInt(token);
				
				Facility.addStreetParkingData(currentParkingData);
				line = br.readLine();
				
			}
			
			Facility.finalizeFacilities();
			//Facility.printFacilities();
			Facility.writeToXml("C:\\data\\Projekte\\ETH TH-22 07-3 PHEV\\Parkhäuser\\facilities\\output\\streetpark_facilities.xml"); 
			

		} catch (Exception ex) {
			System.out.println(ex);
		}

	}

	

}
