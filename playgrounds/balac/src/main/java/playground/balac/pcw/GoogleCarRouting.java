package playground.balac.pcw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

public class GoogleCarRouting {

	
	public static void main(String[] args) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader("C:\\LocalDocuments\\Projects\\PCW\\Routing\\clean_weg_correction1_geocoded.csv"));
		reader.readLine();
		final BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\LocalDocuments\\Projects\\PCW\\Routing\\clean_weg_correction1_car.csv"));
		String s = reader.readLine();
		GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBEkVBy4u7UVyvavAfzoeQmczD_CdGw7pA");
		
		int i = 1;
		while (s != null) {
			 String[] arr = s.split(";");
			 if (!arr[1].equals("-99")) {
					double m = Integer.parseInt(arr[12]);
			 
			 int h = (int) (m / 60);
			 int min = (int)m - h * 60;
				DateTime time = new DateTime(2016, 12, 29, h, min, DateTimeZone.getDefault());

				

			
			
		
			double distance = 0.0;
					
	
					double travelTime = 0.0;
					DirectionsRoute[] route = DirectionsApi.getDirections(context, arr[1] + " " + arr[2] ,
							arr[3] + " " + arr[4]).mode(TravelMode.DRIVING).departureTime(time).await();
					if (route == null || route.length == 0) {
//						System.out.println("");
						writer.write(arr[0] + ";-99;-99");
						writer.newLine();
					}
					else if (route.length > 0){
					for (DirectionsLeg l : route[0].legs) {
						
						travelTime += l.duration.inSeconds;
						distance += l.distance.inMeters;
					}
					
					writer.write(arr[0] + ";" +  travelTime + ";"+ distance);
					writer.newLine();
					}
				

			
			 }
		
		else {
			writer.write(i + ";-99;-99");
			writer.newLine();
			
		}
			 i++;
				s = reader.readLine();

		}
		writer.flush();
		writer.close();
		reader.close();
		

	}
}