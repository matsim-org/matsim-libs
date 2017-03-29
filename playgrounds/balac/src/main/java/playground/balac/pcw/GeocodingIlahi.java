package playground.balac.pcw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import org.matsim.core.utils.io.IOUtils;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;

public class GeocodingIlahi {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		 BufferedReader readLink = IOUtils.getBufferedReader("C:\\LocalDocuments\\Projects\\Geocoding\\Ilahi\\Work_School Location_170317_semi.txt");
		 
		BufferedWriter outLink = IOUtils.getBufferedWriter("C:\\LocalDocuments\\Projects\\Geocoding\\Ilahi\\Work_School Location_170317_geocoded_1_2000.txt");
		outLink.write("id;xcoord;ycoord");
		outLink.newLine();
		
		 readLink.readLine();
		 String s = readLink.readLine();
		 int i = 1;
		 while(s!= null && i <= 2000) {
			 if (i > 0) {
				 String[] arr = s.split(";");			
				 if (arr.length == 7) {
					 GeoApiContext context = new GeoApiContext().setApiKey(args[0]);
					 GeocodingResult[] results =  GeocodingApi.geocode(context,
					    arr[1]
					    + " " +	 arr[2] + " " +	 arr[3] + " " +	 arr[4] + " " +	 arr[5] + " " +	 arr[6] + " " +	 arr[6]).await();
					
					
					 if (results.length > 0) {			
					
						outLink.write(arr[0] + ";" + results[0].geometry.location.lat + ";" + results[0].geometry.location.lng);				
						outLink.newLine();
					
					 }
					 else {
						
						outLink.write(arr[0] + ";" + "-99" + ";" + "-99" );
						outLink.newLine();
					 }
				 }
				 else {
					outLink.write(arr[0] + ";" + "-99" + ";" + "-99" );
					outLink.newLine();
					 
				 }
			 }
			 i++;

			s = readLink.readLine();
		 }
		 
		 outLink.flush();
		 outLink.close();
		 readLink.close();
		

	}

}
