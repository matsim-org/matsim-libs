package playground.wrashid.PHEV.parking.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

public class GarageParking {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			
			// open coordinate and capacity file
			
			FileReader fr = new FileReader(
					"C:\\data\\Projekte\\ETH TH-22 07-3 PHEV\\Parkhäuser\\facilities\\input\\garagesZHCity.txt");
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine(); // do not parse first line which just contains column headers
			line = br.readLine();
			GarageParkingData currentParkingData=null;
			String token = null;
			while (line != null) {
				tokenizer = new StringTokenizer(line);
				currentParkingData=new GarageParkingData();
				
				token = tokenizer.nextToken();
				currentParkingData.id=Integer.parseInt(token);
				
				token = tokenizer.nextToken();
				currentParkingData.x=Double.parseDouble(token);

				token = tokenizer.nextToken();
				currentParkingData.y=Double.parseDouble(token);
				
				token = tokenizer.nextToken();
				currentParkingData.capacity=Integer.parseInt(token);
				
				Facility.addGarageParkingData(currentParkingData);
				line = br.readLine();	
			}
			
			fr.close();
			
			
			
			// open opening times
			fr = new FileReader("C:\\data\\Projekte\\ETH TH-22 07-3 PHEV\\Parkhäuser\\facilities\\input\\opening_times_garageZH.txt");
			br = new BufferedReader(fr);
			line = br.readLine();
			tokenizer = null;
			int id = 0;
			String openingTimes=null;
			while (line != null) {
				tokenizer = new StringTokenizer(line);
				
				token = tokenizer.nextToken("@");
				id=Integer.parseInt(token);
				
				token = tokenizer.nextToken("@");
				openingTimes=token;

				Facility.addOpeningTimes(id, openingTimes);
				line = br.readLine();	
			}
			
			fr.close();
			
			
			
			
			// write out data
			Facility.writeToXml("C:\\data\\Projekte\\ETH TH-22 07-3 PHEV\\Parkhäuser\\facilities\\output\\garage_facilities.xml"); 
			

		} catch (Exception ex) {
			System.out.println(ex);
		}

	}

}
