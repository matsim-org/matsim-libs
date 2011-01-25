package air;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.xml.sax.SAXException;

public class SfAirScheduleBuilder {

	/**
	 * @param args
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		
		SfAirScheduleBuilder builder = new SfAirScheduleBuilder();
		builder.filterEurope("/home/soeren/workspace/airports.osm", "/home/soeren/Downloads/OAGSEP09.CSV", 
				"/home/soeren/workspace/OsmTest.txt", "/home/soeren/workspace/OagTest.txt", "/home/soeren/workspace/missingAirports.txt", "/home/soeren/workspace/cityPairs.txt");

	}
	
	protected Map<String, Coord> airportsInOsm = new HashMap<String, Coord>();
	protected Map<String, Coord> airportsInOag = new HashMap<String, Coord>();
	protected Map<String, Integer> routes = new HashMap<String, Integer>();
	protected Map<String, Integer> missingAirports = new HashMap<String, Integer>();
	
	
//	AIRPORTS AUS OAG FILTERN
	
	public void filterEurope(String inputOsm, String inputOag, String outputOsm, String outputOag, String outputMissingAirports, String cityPairs) throws IOException, SAXException, ParserConfigurationException {
		
		
		OsmAerowayParser osmReader = new OsmAerowayParser(TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
				TransformationFactory.WGS84));
		osmReader.parse(inputOsm);
		
		long counter = 0;
		
		this.airportsInOsm = osmReader.airports;
		
		String[] euroCountries = {"AD","AL","AM","AT","AX","AZ","BA","BE","BG","BY","CH","CY","CZ",
				"DE","DK","EE","ES","FI","FO","FR","GB","GI","GE","GG","GR","HR","HU","IE","IM","IS","IT",
				"JE","KZ","LI","LT","LU","LV","MC","MD","ME","MK","MT","NL","NO","PL","PT","RO","RS", 
				"RU","SE","SI","SJ","SK","SM","TR","UA","VA" };
		
		BufferedReader br = new BufferedReader(new FileReader(new File(inputOag)));
		BufferedWriter bwOag = new BufferedWriter(new FileWriter(new File(outputOag)));
		BufferedWriter bwOsm = new BufferedWriter(new FileWriter(new File(outputOsm)));
		BufferedWriter bwMissing = new BufferedWriter(new FileWriter(new File(outputMissingAirports)));
		BufferedWriter bwcityPairs = new BufferedWriter(new FileWriter(new File(cityPairs)));
		Map<String, String> flights = new HashMap<String, String>();
		String flight = "";
		int lines = 0;

		
		while (br.ready()) {
			String oneLine = br.readLine();
			String[] lineEntries = new String[81];
			lineEntries = oneLine.split(",");
			
		if (lines>0) {
			

			for (int jj=0; jj<81; jj++){
				lineEntries[jj]=lineEntries[jj].replaceAll("\"", "");
			}
			
				String originCountry = lineEntries[6];
				String destinationCountry = lineEntries[9];
				String hours = lineEntries[13].substring(1, 3);
				String minutes = lineEntries[13].substring(3);
				String carrier = lineEntries[0];
				double durationMinutes = Double.parseDouble(minutes)*60;	//convert flight dur minutes into seconds
				double durationHours = Double.parseDouble(hours)*3600;
				double duration = durationHours+durationMinutes;
				boolean origin = false; boolean destination = false;

			
				for (int ii=0; ii<euroCountries.length; ii++) {
					if (originCountry.contains(euroCountries[ii])) origin=true;
					if (destinationCountry.contains(euroCountries[ii])) destination=true;
				}
			
			
				if (origin && destination) {
					
				
				if (lineEntries[47].contains("O") || lineEntries[43].equalsIgnoreCase("")) {
								
						flight=lineEntries[0]+lineEntries[1];
						int seatsAvail = Integer.parseInt(lineEntries[23]);
						String originAirport = lineEntries[4];
						String destinationAirport = lineEntries[7];
						this.missingAirports.put(originAirport, 1);
						this.missingAirports.put(destinationAirport, 1);
						String route = lineEntries[4]+lineEntries[7];
						

						
						if (lineEntries[14].contains("2") && !flights.containsKey(flight) && seatsAvail>0 && 
												this.airportsInOsm.containsKey(originAirport) && this.airportsInOsm.containsKey(destinationAirport)) {
							
							if (this.routes.containsKey(route)) {
								int num = this.routes.get(route);
								num++;
								this.routes.put(route, num);
							}
							else if (!this.routes.containsKey(route)){
								this.routes.put(route, 1);
							}
							
							bwOag.write(
									route+"\t"+		//TransitRoute
									lineEntries[4]+lineEntries[7]+"_"+lineEntries[0]+"\t"+	//TransitLine
									lineEntries[0]+lineEntries[1]+"\t"+		//vehicleId
									lineEntries[10]+"\t"+	//departure time (24h)
									lineEntries[11]+"\t"+	//arrival time
									duration+"\t"+	//journey time (HHHMM)
									lineEntries[21]+"\t"+	//aircraft
									lineEntries[23]+"\t"+	//seats avail
									lineEntries[42]);		//distance (statute miles)
							flights.put(flight, "");
							bwOag.newLine();
							counter++;
							this.airportsInOag.put(originAirport, this.airportsInOsm.get(originAirport));
							this.airportsInOag.put(destinationAirport, this.airportsInOsm.get(destinationAirport));
						}
					}
				}
			}
		
		lines++;
		
		}
		
		Iterator it = this.airportsInOag.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        bwOsm.write(pairs.getKey().toString()+"\t"+pairs.getValue().toString());
	        this.missingAirports.remove(pairs.getKey().toString());
	        bwOsm.newLine();
	    }
	    
		Iterator it2 = this.missingAirports.entrySet().iterator();
	    while (it2.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it2.next();
	        bwMissing.write(pairs.toString());
	        bwMissing.newLine();
	    }
	    
		Iterator it3 = this.routes.entrySet().iterator();
	    while (it3.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it3.next();
	        bwcityPairs.write(pairs.getKey().toString());
	        bwcityPairs.newLine();
	    }
	    
	    System.out.println("Anzahl der Airports: "+this.airportsInOag.size());
	    System.out.println("Anzahl der City Pairs: "+this.routes.size());
	    System.out.println("Anzahl der Flüge: "+counter);
	    System.out.println("Anzahl der fehlenden Airport: "+this.missingAirports.size());
	    
	    bwOsm.flush();
	    bwOsm.close();
		bwMissing.flush();
		bwMissing.close();
		bwcityPairs.flush();
		bwcityPairs.close();
		br.close();
		bwOag.flush();
		bwOag.close();
		
	}
	
	
	

}
