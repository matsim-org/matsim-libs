package playground.andreas.utils.net.geocode;

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

import playground.andreas.bln.pop.generate.TabReader;

public class GoogleGeocode {
	
	private static final Logger log = Logger.getLogger(TabReader.class);
	private final static String ENCODING = "UTF-8";
	private final static String KEY = "xyz";

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
		
		public String getGK4(){
			CoordinateTransformation coorTransform = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
					TransformationFactory.DHDN_GK4);
			Coord coord = coorTransform.transform(new CoordImpl(this.lon, this.lat));
			return new String(coord.getX() + ", " + coord.getY());
			
		}
	}


	public static Location getLocation(String address) throws IOException {
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new URL("http://maps.google.com/maps/geo?q=" + URLEncoder.encode(address, ENCODING) + "&output=csv&key=" + KEY).openStream()));
		
		String resultingLine;
		Location location = null;
		int exitCode = -1;
		
		while ((resultingLine = reader.readLine()) != null) {
			
			// Format: 200,6,42.730070,-73.690570
			exitCode = Integer.parseInt(resultingLine.substring(0, 3));
			
			if (resultingLine.contains("403")){
				exitCode = 403;
			}
			
			if (exitCode == 200) {
				location = new Location(resultingLine.substring("200,6,".length(), resultingLine.indexOf(',', "200,6,".length())),
						resultingLine.substring(resultingLine.indexOf(',', "200,6,".length()) + 1, resultingLine.length()));
			}
		}
		
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
			}
		}
		
		return location;
	}

	/**
	 * 
	 * @param argv 0 - input file, 1 - 0 if first run else 1, 2 - entry to start from
	 * @throws Exception
	 */
	public static void main(String[] argv) throws Exception {
		
		int cntFinished = 0;
		int cntCoded = 0;
		int cntReturnedError = 0;
		int cntDelayed = 0;
		int cntBadRequest = 0;
		int cntRequests = 0;
		
		int startAtEntry = Integer.parseInt(argv[2]);
		int cntEntriesHandled = 0;
		
		String personsFileName = argv[0];
//		String personsFileName = "z:/population/input/googlemapstest.csv";
		log.info("Start reading file " + personsFileName);
		ArrayList<String[]> personData = TabReader.readFile(personsFileName);
		log.info("...finished reading " + personData.size() + " entries.");
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(personsFileName + ".out")));
		
		for (String[] strings : personData) {			
			
			if(cntEntriesHandled < startAtEntry){
				// dump line til start
				writer.write(strings[0]);
				for (int i = 1; i < strings.length; i++) {
					writer.write(", " + strings[i]);
				}
				writer.newLine();
//				writer.flush();

			} else {

				try {
					Double.parseDouble(strings[2]);
					Double.parseDouble(strings[3]);

					// dump already geocoded lines
					writer.write(strings[0]);
					for (int i = 1; i < strings.length; i++) {
						writer.write(", " + strings[i]);
					}
					writer.newLine();
					//				writer.flush();
					cntFinished++;
				} catch (Exception e) {
					//				!strings[2].equalsIgnoreCase("Bad Request") || 
					if(!strings[2].equalsIgnoreCase("Bad Request - 400")){
						if(cntRequests < 14950){

							String ort = "";
							for (int i = 2 + Integer.parseInt(argv[1]); i < strings.length; i++) {
								if(!strings[i].trim().equalsIgnoreCase("wohnung")){
									ort = ort + strings[i] + ", ";
								}
							}

							try {
								cntRequests++;
								String googleOut = GoogleGeocode.getLocation(ort).getGK4();
								writer.write(strings[0] + ", " + strings[1] + ", " + googleOut + ", " + ort);
								writer.newLine();
								writer.flush();
								cntCoded++;

							} catch (IOException e2) {

								writer.write(strings[0] + ", " + strings[1] + ", " + e2.getMessage() + ", " + ort);
								writer.newLine();
								writer.flush();
								cntReturnedError++;

							}
						} else {
							// limit exceeded - finish run
							writer.write(strings[0]);
							for (int i = 1; i < strings.length; i++) {
								writer.write(", " + strings[i]);
							}
							writer.newLine();
							//						writer.flush();
							cntDelayed++;
						}
					} else {
						// dump bad request line
						writer.write(strings[0]);
						for (int i = 1; i < strings.length; i++) {
							writer.write(", " + strings[i]);
						}
						writer.newLine();
						//					writer.flush();
						cntBadRequest++;
					}
				}				
			}
			cntEntriesHandled++;
		}
		
		writer.flush();
		writer.close();

		log.info(cntRequests + " requests done");
		log.info(cntEntriesHandled + " entries handled");
		log.info(cntFinished + " finished + " + cntBadRequest + " bad request + " + cntReturnedError + " returned error + " + cntCoded + " newly coded + " + cntDelayed + " delayed = " + (cntFinished + cntBadRequest + cntReturnedError + cntCoded + cntDelayed));
		log.info("Finished");
	}
}