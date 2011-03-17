package playground.jbischoff.waySplitter;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


public class JBGoogleGeocode {
	
	private static final Logger log = Logger.getLogger(JBGoogleGeocode.class);
	private final static String ENCODING = "UTF-8";
	private final static String KEY = "xyz";
	private int count = 0;

	public static class Location {

		public String lon;
		public String lat;

		Location(String lat, String lon) {
			this.lon = lon;
			this.lat = lat;
		}

		@Override
		public String toString() {
			return ("Lat: " + this.lat + ", Lon: " + this.lon);
		}
		
		public Coord getGK4(){
			CoordinateTransformation coorTransform = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
					TransformationFactory.DHDN_GK4);
			Coord coord = coorTransform.transform(new CoordImpl(this.lon, this.lat));
			return coord;
			
		}
	}


	public Location getLocation(String address) throws IOException, InterruptedException {
		
		int exitCode = -1;
		Location location = null;
		

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new URL("http://maps.google.com/maps/geo?q=" + URLEncoder.encode(address, ENCODING) + "&output=csv&key=" + KEY).openStream()));
		
		String resultingLine;

		while ((resultingLine = reader.readLine()) != null) {
			
			exitCode = Integer.parseInt(resultingLine.substring(0, 3));

				count++;
			// Format: 200,6,42.730070,-73.690570
			
			if (resultingLine.contains("403")){
				exitCode = 403;
			}
			
			if (exitCode == 200) {
				location = new Location(resultingLine.substring("200,6,".length(), resultingLine.indexOf(',', "200,6,".length())),
						resultingLine.substring(resultingLine.indexOf(',', "200,6,".length()) + 1, resultingLine.length()));
			}
		}
//		}
//		while (exitCode == 620 || exitCode == 602 || exitCode == 403 );
		
		if  (exitCode == 620 ||  exitCode == 403 ) {location = new Location("7","7");
		log.info("wrote mock");}
		try{
		if (location == null) {
			
			switch (exitCode) {
				case 400:
					throw new IOException("Bad Request - 400");
				case 403:
					throw new IOException("Refused - 403");
				case 500:
					throw new IOException("Unknown error from Google Encoder - 500");
				case 601:
					throw new IOException("Missing query - 601");
				// don't know what that does mean
				case 602:
					throw new IOException("null - 602");
				case 603:
					throw new IOException("Legal problem - 603");
				case 604:
					throw new IOException("No route - 604");
				case 610:
					throw new IOException("Bad key - 610");
				case 620:
					throw new IOException("Too many queries - 620");
					// limit is 15000 request per 24h
			}}} catch (IOException e) {
				location = new Location("0", "0");
//				e.printStackTrace();
			}
		
		
		return location;
	}

	/**
	 * 
	 * @param argv 0 - input file, 1 - 0 if first run else 1, 2 - entry to start from
	 * @throws Exception
	 */
	public  Coord readGC(String loc) throws Exception {
		

		int cntRequests = 0;

			

			
								cntRequests++;
								Coord googleOut = this.getLocation(loc).getGK4();
								log.info(googleOut + " for " + loc);
			
								
								return  googleOut;
	}
}