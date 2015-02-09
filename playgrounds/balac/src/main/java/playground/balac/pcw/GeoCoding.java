package playground.balac.pcw;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;

public class GeoCoding {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		 BufferedReader readLink = IOUtils.getBufferedReader("P:/Projekte/SNF/SNF Post-Car World/STATEDCHOICE/SC_pretest_referencevalues_new.csv");
		 
		BufferedWriter outLink = IOUtils.getBufferedWriter("P:/Projekte/SNF/SNF Post-Car World/STATEDCHOICE/geo_coded_pretest_referencevalues_new.txt");

		WGS84toCH1903LV03 transformation = new WGS84toCH1903LV03();
		
		 readLink.readLine();
		 String s = readLink.readLine();
		 int i = 1;
		 while(s!= null) {
			 
			 String[] arr = s.split(";");
			 
			 
			String addressStart = arr[8];
			String addressEnd = arr[13];
			String streetNumberStart = arr[9];
			String streetNumberEnd = arr[14];
			String cityStart = arr[11];
			String cityEnd = arr[16];
			
			GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBEkVBy4u7UVyvavAfzoeQmczD_CdGw7pA");
			GeocodingResult[] results =  GeocodingApi.geocode(context,
			    addressStart
			    + " " +	 streetNumberStart 
			    + ", " + cityStart).await();
			System.out.println(results[0].formattedAddress);
			System.out.println(results[0].geometry.location.lat + " " + results[0].geometry.location.lng);
			GeocodingResult[] results1 =  GeocodingApi.geocode(context,
					addressEnd
				    + " " +	 streetNumberEnd 
				    + ", " + cityEnd).await();
			
			System.out.println(results1[0].formattedAddress);
			System.out.println(results1[0].geometry.location.lat + " " + results1[0].geometry.location.lng);
			
			Coord coordStartT = new CoordImpl(Double.toString(results[0].geometry.location.lng), Double.toString(results[0].geometry.location.lat));
			
			CoordImpl coordStart = (CoordImpl) transformation.transform(coordStartT);
					
			Coord coordEndT = new CoordImpl(Double.toString(results1[0].geometry.location.lng), Double.toString(results1[0].geometry.location.lat));
			
			CoordImpl coordEnd = (CoordImpl) transformation.transform(coordEndT);
			
			int Radius = 6371;
			
			double lat1 = Math.toRadians(results[0].geometry.location.lat);
	        double lat2 = Math.toRadians(results1[0].geometry.location.lat);
	        double lon1 = Math.toRadians(results[0].geometry.location.lng);
	        double lon2 = Math.toRadians(results1[0].geometry.location.lng);
			
			double x = Math.cos(lat1);
	        double y = Math.cos(lat2);
	        
	        double z = (lon2 - lon1) / 2.0;
	        
	        double k = (lat2 - lat1) / 2.0;
	        
	        double l = Math.sin(z) * Math.sin(z);
	        
			double distance = 2 * Radius * Math.asin(Math.sqrt(Math.sin((k) * Math.sin(k)) + x * y * l));
			
			outLink.write(Integer.toString(i) + ";" + results[0].geometry.location.lat + ";" + results[0].geometry.location.lng 
					+ ";" + results1[0].geometry.location.lat + ";" + results1[0].geometry.location.lng);
			
			outLink.write(";" + Double.toString(coordStart.getX()) + ";" + Double.toString(coordStart.getY()) 
					+ ";" + Double.toString(coordEnd.getX()) + ";" + Double.toString(coordEnd.getY()) + ";" + Double.toString(distance) + ";" + arr[20]);
			outLink.newLine();
			i++;
			s = readLink.readLine();
		 }
		 
		 outLink.flush();
		 outLink.close();
		 readLink.close();
		

	}

}
