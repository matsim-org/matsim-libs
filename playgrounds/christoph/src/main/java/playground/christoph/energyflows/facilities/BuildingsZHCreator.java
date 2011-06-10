/* *********************************************************************** *
 * project: org.matsim.*
 * ResidentalBuildingsZHCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.christoph.energyflows.facilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Counter;

public class BuildingsZHCreator {
	
	final private static Logger log = Logger.getLogger(BuildingsZHCreator.class);
	
//	private String buildingsTextFile = "../../matsim/mysimulations/2kw/facilities/Wilke_Gebäude_100709.csv";
//	private String apartmentsTextFile = "../../matsim/mysimulations/2kw/facilities/Wilke_Wohnungen_100709.csv";
	
//	private String apartmentBuildingsTextFile =  "../../matsim/mysimulations/2kw/gis/apartmentBuildings.csv";
//	private String facilitiesZHFile = "../../matsim/mysimulations/2kw/facilities/facilitiesZH.xml.gz";
	
	private String delimiter = ",";
	private Charset charset = Charset.forName("UTF-8");
	private int totalResidentialArea;
	private Set<Id> residentialBuildings;
	private Map<Id, BuildingData> data;
	private Config config;
	private Scenario scenario;
	
	/**
	 * Expects 4 Strings as input parameters:
	 * - buildingsTextFile (input)
	 * - apartmentsTextFile (input)
	 * - apartmentBuildingsTextFile (output)
	 * - facilitiesZHFile (output)
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 4) return;
		
		String buildingsTextFile = args[0];
		String apartmentsTextFile = args[1];
		String apartmentBuildingsTextFile = args[2];
		String facilitiesZHFile = args[3];
		
		BuildingsZHCreator creator = new BuildingsZHCreator();
		creator.parseBuildingFile(buildingsTextFile);
		creator.parseApartmentFile(apartmentsTextFile);
		creator.writeApartmentBuildingsFile(apartmentBuildingsTextFile);
		creator.createFacilities();
		creator.writeFacilitiesFile(facilitiesZHFile);
	}
	
	/*
	 * Creates basic residential facilities that are not connected to the
	 * network and have no capacities set.
	 */
	public BuildingsZHCreator() throws Exception {
		config = ConfigUtils.createConfig();	
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
	}
	
	public void parseBuildingFile(String buildingsTextFile) throws Exception {
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
  		
		fis = new FileInputStream(buildingsTextFile);
		isr = new InputStreamReader(fis, charset);
		br = new BufferedReader(isr);
		Counter counter = new Counter("parsed buildings in ZH: ");
		data = new TreeMap<Id, BuildingData>();
		
		// skip first Line with the Header
		br.readLine();
		
//		Counter noFlats = new Counter("Buildings with a residential area but where the number of flats is 0: ");
//		Counter toLargeFlats = new Counter("Buildings where the residential area per flat is > 150 m2: ");
		String line;
		while((line = br.readLine()) != null) {
			String[] cols = line.split(delimiter);
			
//			BuildingData d = new BuildingData(cols, noFlats, toLargeFlats);
			BuildingData d = new BuildingData(cols);
			
			data.put(scenario.createId("egid" + d.egid), d);
			counter.incCounter();
			
//			/*
//			 * Some facilities have a residential area of 0.0 but still some flats.
//			 * However, we ignore them. This seems reasonable because the amount of
//			 * remaining valid facilities approximately fits the amount of removed  
//			 * residential facilities.
//			 */
//			if (d.residentialArea > 0.0) {
//				data.put(scenario.createId("egid" + d.egid), d);
//				counter.incCounter();				
//			}
		}
		counter.printCounter();
//		noFlats.printCounter();
//		toLargeFlats.printCounter();
		
		br.close();
		isr.close();
		fis.close();
	}
	
	public void parseApartmentFile(String apartmentsTextFile) throws Exception {
		
		residentialBuildings = new HashSet<Id>();
		
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
  		
		fis = new FileInputStream(apartmentsTextFile);
		isr = new InputStreamReader(fis, charset);
		br = new BufferedReader(isr);
		Counter counter = new Counter("parsed apartments in ZH: ");
		
		// skip first Line with the Header
		br.readLine();
		
		String line;
		while((line = br.readLine()) != null) {
			line = line.replace("\"", "");
			String[] cols = line.split(delimiter);
			
			ApartmentData a = new ApartmentData(cols);
			Id id = scenario.createId("egid" + a.egid);
			BuildingData d = data.get(id);
			
			if (d != null) {
				d.getApartmentData().add(a);
				counter.incCounter();
				residentialBuildings.add(id);
				totalResidentialArea += a.residentialArea;
			} else log.error("No Building with egid " + a.egid + " found. Skipping apartment.");
		}
		counter.printCounter();
		log.info("total residential area: " + totalResidentialArea);
		log.info("total residential buildings: " + residentialBuildings.size());
		log.info("mean residential area per apartment: " + totalResidentialArea / counter.getCounter());
		log.info("mean residential area per residential building: " + totalResidentialArea / residentialBuildings.size());
		
		br.close();
		isr.close();
		fis.close();
	}
	
	public void writeApartmentBuildingsFile(String apartmentBuildingsTextFile) throws Exception {
		/*
		 * Write textfile
		 */
		FileOutputStream fos = new FileOutputStream(apartmentBuildingsTextFile);
		OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
		BufferedWriter bw = new BufferedWriter(osw);
		
		Counter counter = new Counter("Write apartment buildings in ZH to file: ");
		
		// write Header
		bw.write("id" + delimiter + "x" + delimiter + "y" + delimiter + "capacity" + "\n");
		
		int totalCapacity = 0;
		for (Id id : residentialBuildings) {
			BuildingData building = data.get(id);
			
			int capacity = building.getApartmentCapacity();
			totalCapacity += capacity;
			
			StringBuffer sb = new StringBuffer();
			sb.append(id);
			sb.append(delimiter);
			sb.append(building.x);
			sb.append(delimiter);
			sb.append(building.y);
			sb.append(delimiter);
			sb.append(capacity);
			
			bw.write(sb.toString() + "\n");
			counter.incCounter();
		}
		counter.printCounter();
		log.info("Total residential capacity " + totalCapacity);
		
		bw.close();
		osw.close();
		fos.close();
	}
	
	public void createFacilities() {
		for (Entry<Id, BuildingData> entry :data.entrySet()) {
			Id id = entry.getKey();
			BuildingData building = entry.getValue();
			
			Coord coord = scenario.createCoord(building.x, building.y);
			ActivityFacilityImpl facility = ((ScenarioImpl) scenario).getActivityFacilities().createFacility(id, coord);

			int capacity = building.getApartmentCapacity();
			if (capacity > 0) {
				ActivityOptionImpl activityOption = facility.createActivityOption("home");
				activityOption.setCapacity(building.getApartmentCapacity() * 1.0);
			}
		}
	}
	
	public void writeFacilitiesFile(String facilitiesZHFile) {
		new FacilitiesWriter(((ScenarioImpl) scenario).getActivityFacilities()).write(facilitiesZHFile);
	}
	
//	/*
//	 * Use a two step approach:
//	 * Ensure, that at least one facility is connected to each link that
//	 * hosted residential facilities before.
//	 * Then connect the remaining facilities to the next link, that hosted
//	 * residential facilities before. 
//	 */
//	public void connectToNetwork(Network network, Map<Id, List<ActivityFacility>> hostedFacilities) {
////		new WorldConnectLocations(config).connectFacilitiesWithLinks((ActivityFacilitiesImpl) this.getResidentialFacilities(), (NetworkImpl) network);
//
//		// Create a quadtree with all links that hosted the removed facilities.
//		log.info("building link quad tree...");
//		double minx = Double.POSITIVE_INFINITY;
//		double miny = Double.POSITIVE_INFINITY;
//		double maxx = Double.NEGATIVE_INFINITY;
//		double maxy = Double.NEGATIVE_INFINITY;
//		for (Id linkId : hostedFacilities.keySet()) {
//			Link link = network.getLinks().get(linkId);
//			if (link.getCoord().getX() < minx) { minx = link.getCoord().getX(); }
//			if (link.getCoord().getY() < miny) { miny = link.getCoord().getY(); }
//			if (link.getCoord().getX() > maxx) { maxx = link.getCoord().getX(); }
//			if (link.getCoord().getY() > maxy) { maxy = link.getCoord().getY(); }
//		}
//		minx -= 1.0;
//		miny -= 1.0;
//		maxx += 1.0;
//		maxy += 1.0;
//		log.info("xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
//		QuadTree<Id> homeQuadTree = new QuadTree<Id>(minx, miny, maxx, maxy);	
//		
//		for (Id linkId : hostedFacilities.keySet()) {
//			Link link = network.getLinks().get(linkId);
//			
//			/*
//			 * Check whether the link hosts at least one home facility. If yes,
//			 * add it to the quadtree.
//			 */
//			List<ActivityFacility> list = hostedFacilities.get(linkId);
//			for (ActivityFacility facility : list) {
//				ActivityOption activityOption = facility.getActivityOptions().get("home");
//				if (activityOption != null) {
//					homeQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), linkId);
//					break;
//				}
//			}
//		}
//		log.info("found " + homeQuadTree.size() + " links that host removed residential facilities.");
//		log.info("done.");
//		
//		
////		//Create a quadtree with all new residential facilities.
////		log.info("building residential facilities quad tree...");
////		minx = Double.POSITIVE_INFINITY;
////		miny = Double.POSITIVE_INFINITY;
////		maxx = Double.NEGATIVE_INFINITY;
////		maxy = Double.NEGATIVE_INFINITY;
////		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
////			if (facility.getCoord().getX() < minx) { minx = facility.getCoord().getX(); }
////			if (facility.getCoord().getY() < miny) { miny = facility.getCoord().getY(); }
////			if (facility.getCoord().getX() > maxx) { maxx = facility.getCoord().getX(); }
////			if (facility.getCoord().getY() > maxy) { maxy = facility.getCoord().getY(); }
////		}
////		minx -= 1.0;
////		miny -= 1.0;
////		maxx += 1.0;
////		maxy += 1.0;
////		log.info("xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
////		QuadTree<Id> facilitiesQuadTree = new QuadTree<Id>(minx, miny, maxx, maxy);
////		
////		// fill the quadtree
////		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
////			facilitiesQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility.getId());
////		}
////
////		residentialFacilities = new TreeMap<Id, List<ActivityFacility>>();
////		
////		// Assign one facility to each link
////		double maxDistance = Double.MIN_VALUE;
////		for (Id linkId : homeQuadTree.values()) {
////			Link link = network.getLinks().get(linkId);
////			Id nextFacilityId = facilitiesQuadTree.get(link.getCoord().getX(), link.getCoord().getY());
////			ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(nextFacilityId);
////			((ActivityFacilityImpl) facility).setLinkId(linkId);
////			
////			// remove the facility from the quadtree so it is not selected again
////			facilitiesQuadTree.remove(facility.getCoord().getX(), facility.getCoord().getY(), nextFacilityId);
////			
////			double distance = CoordUtils.calcDistance(facility.getCoord(), link.getCoord());
////			if (distance > maxDistance) maxDistance = distance;
////		}
////		log.info("maximum distance between links and their facilities: " + maxDistance);
//			
//		// Assign the facilities
//		facilities = new TreeMap<Id, List<ActivityFacility>>();
//		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
//			// connect the facility to the next link using the homeQuadTree
//			((ActivityFacilityImpl)facility).setLinkId(homeQuadTree.get(facility.getCoord().getX(), facility.getCoord().getY()));
//
//			List<ActivityFacility> list = facilities.get(facility.getLinkId());
//			if (list == null) {
//				list = new ArrayList<ActivityFacility>();
//				facilities.put(facility.getLinkId(), list);
//			}
//			list.add(facility);
//		}
//		log.info("number of links that host residential facilities: " + facilities.size());
//	}
	
	public ActivityFacilities getResidentialFacilities() {
		return ((ScenarioImpl) this.scenario).getActivityFacilities();
	}
	
	/*
	 * The Map contains those facilities, that have been removed and
	 * therefore have been replaced. We replace them with the new residential
	 * facilities using an area based approach.
	 * -> residentialCapacity = totalCapacity * facilityResidentialArea/totalResidentialArea 
	 */
	public void calcCapacities(Map<String, Double> removedCapacities) {
		
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			ActivityOption activityOption = facility.getActivityOptions().get("home");
			BuildingData d = data.get(facility.getId());
			double capacity = removedCapacities.get("home") * d.residentialArea / totalResidentialArea;
//			capacity = Math.round(capacity);
			if (capacity <= 1.0) {
				log.warn("Capacity of facility " + facility.getId().toString() + " would be < 1.0! Using 1.0 instead.");
				capacity = 1.0;
			}
			activityOption.setCapacity(capacity);
		}
//		for (Data d : data) {
//			double capacity =
//		}
		
//		Counter remappingNotPossible = new Counter("1:1 remapping not possible: ");
//		
//		for (Entry<Id, List<ActivityFacility>> entry : hostedFacilities.entrySet()) {
//			Id linkId = entry.getKey();
//			if (residentialFacilities.containsKey(linkId)) {
//				
//			} else {
//				remappingNotPossible.incCounter();
////				log.warn("1:1 remapping is not possible for Link: " + linkId.toString());
//			}
//		}
//		remappingNotPossible.printCounter();	
	}
	
	private static class ApartmentData {
		/*
		 * Structure of the data set:
		 *	[0] "Anzahl Zimmer"
		 *	[1] "Wohnflaeche [m2]"
		 *	[2] "Erfassungsdatum Wohnung"
		 *	[3] "Stockwerk Level 2"
		 *  [4] "Lage Stockwerk"
		 *  [5] "EGID"
		 *  [6] "EWID" 
		 */
		
		/*package*/ int roomCount;
		/*package*/ int residentialArea;
		/*package*/ int egid;
		/*package*/ int ewid;
		
		public ApartmentData(String[] cols) {
			roomCount = parseInteger(cols[0]);
			residentialArea = parseInteger(cols[1]);
			egid = parseInteger(cols[5]);
			ewid = parseInteger(cols[6]);
		}
		
		private int parseInteger(String string) {
			if (string == null) return 0;
			else if (string.trim().equals("")) return 0;
			else return Integer.valueOf(string);
		}
	}
	
	private static class BuildingData {	
		/*
		 * Structure of the data set:
		 * [0]	EGID Gebäude
		 * [1]	"Strassenname Gebäude"
		 * [2]	"Hausnummer Gebäude"
		 * [3]	"Hinweiscode Gebäude"
		 * [4]	X-Koordinate Gebäude
		 * [5]	Y-Koordinate Gebäude
		 * [6]	"Statistische Zone Gebäude"
		 * [7]	"Statistische Zone Gebäude benannt"
		 * [8]	"Vermessungsbezirk Gebäude"
		 * [9]	Postleitzahl
		 * [10]	"Zonenart Level 1 Gebäude"
		 * [11]	"Zonenart Level 2 Gebäude"
		 * [12]	"Zonenart Level 3 Gebäude"
		 * [13]	"Zonenart Level 4 Gebäude"
		 * [14]	"Eigentümerart Gebäude Level 1"
		 * [15]	"Eigentümerart Gebäude Level 2"
		 * [16]	"Eigentümerart Gebäude Level 3"
		 * [17]	"Eigentümerart Gebäude Level 4"
		 * [18]	"Überbaute"
		 * [19]	"Gebäudebezeichnung"
		 * [20]	"Gebäude Niveau"
		 * [21]	"Lift"
		 * [22]	"Dachform"
		 * [23]	"Bauliche Verbindung"
		 * [24]	"Gebäudeart Level 1"
		 * [25]	"Gebäudeart Level 2"
		 * [26]	"Gebäudeart Level 3"
		 * [27]	"Gebäudeart Level 4"
		 * [28]	"Energiestandard"
		 * [29]	"Heizungsart"
		 * [30]	"Energieträger der Heizung"
		 * [31]	"Energieträger Warmwasser"
		 * [32]	"Lüftung/Kühlung"
		 * [33]	"Energieträger Lüftung/Kühlung"
		 * [34]	"Warmwasser Aufbereitung"
		 * [35]	Baujahr
		 * [36]	Anzahl Geschosse unter Niveau
		 * [37]	Anzahl Geschosse über Niveau
		 * [38]	Fläche [m2] auf Niveau
		 * [39]	Fläche [m2] unter Niveau
		 * [40]	Fläche [m2] über Niveau
		 * [41]	Gebäude Parkplätze
		 * [42]	Anzahl Zimmer
		 * [43]	Anzahl 1-Zimmerwohnungen
		 * [44]	Anzahl 2-Zimmerwohnungen
		 * [45]	Anzahl 3-Zimmerwohnungen
		 * [46]	Anzahl 4-Zimmerwohnungen
		 * [47]	Anzahl 5-Zimmerwohnungen
		 * [48]	Anzahl 6-Zimmerwohnungen
		 * [49]	Anzahl 7-Zimmerwohnungen
		 * [50]	Anzahl 8-(und mehr) Zimmerwohnungen
		 * [51]	Anzahl sep. Wohnräume
		 * [52]	Anzahl Wohnungen
		 * [53]	Nutzfläche [m2]
		 * [54]	Nutzfläche Wohnen [m2]
		 * [55]	Nutzfläche Büro [m2]
		 * [56]	Nutzfläche Verkauf [m2]
		 * [57]	Nutzfläche Produktion [m2]
		 * [58]	Nutzfläche Lager [m2]
		 * [59]	Nutzfläche Parkierung [m2]
		 * [60]	"Nutzfläche Sondernutzung [m2]"
		 * [61]	"Nutzfläche Gemischte Nutzung [m2]"
		 * [62]	"Nutzfläche Nicht nutzbar [m2]"
		 * [63]	"Nutzfläche Pendent [m2]"
		 * [64]	"Nutzfläche Zu bestimmen [m2]"
		 * [65]	"Nutzfläche Unbekannt [m2]"
		 * [66]	"Erfassungsdatum Gebäude"
		 * [67]	"Umbaudatum Gebäude"
		 * [68]	"Neubaudatum Gebäude"
		 * [69]	"Nutzungsänderungsdatum Gebäude"
		 */
		
		/*package*/ int egid;
		/*package*/ double x;
		/*package*/ double y;
		/*package*/ int flatCount;
		/*package*/ double residentialArea;
		/*package*/ List<ApartmentData> apartments;
		
//		public BuildingData(String[] cols, Counter noFlats, Counter toLargeFlats) {
		public BuildingData(String[] cols) {
			
			egid = parseInteger(cols[0]);
			x = parseDouble(cols[4]);
			y = parseDouble(cols[5]);
			flatCount = parseInteger(cols[52]);
			residentialArea = parseDouble(cols[54]);
			apartments = new ArrayList<ApartmentData>();
			
			/*
			 * Fix probably wrong entries in the dataset.
			 */
			/*
			 * If the flatCount is 0, we set the residential area also to 0.
			 */
//			if (flatCount == 0 && residentialArea > 0.0) {
//				residentialArea = 0.0;
//				log.warn("The number of flats is 0. Therefore, set the residential area also to 0.");
//				noFlats.incCounter();
//			}
			/*
			 * If the area per flat is > 150.0 we assume that this might
			 * be an error. Therefore we reduce it to 150.0 m2 per flat.
			 */
//			else if (residentialArea / flatCount > 150.0) {
//				log.warn("Residential area for building " + egid + " might be wrong. Reducing it from " 
//						+ residentialArea + " to " + flatCount * 150.0);
//				residentialArea = flatCount * 150.0;
//				toLargeFlats.incCounter();
//			}
		}
		
		/*package*/ List<ApartmentData> getApartmentData() {
			return this.apartments;
		}
		
		/*package*/ int getApartmentArea() {
			int area = 0;
			for (ApartmentData apartment : apartments) {
				area += apartment.residentialArea;
			}
			return area;
		}
		
		/*package*/ int getApartmentCapacity() {
			int capacity = 0;
			for (ApartmentData apartment : apartments) {
				if (apartment.roomCount == 0) ;	// nothing to do here 
				else if (apartment.roomCount == 1) capacity += 1;
				else if (apartment.roomCount == 2) capacity += 1;
				else if (apartment.roomCount == 3) capacity += 2;
				else if (apartment.roomCount == 4) capacity += 3;
				else if (apartment.roomCount == 5) capacity += 4;
				else if (apartment.roomCount == 6) capacity += 5;
				else capacity += 6;
			}
			return capacity;
		}
		
		private int parseInteger(String string) {
			if (string == null) return 0;
			else if (string.trim().equals("")) return 0;
			else return Integer.valueOf(string);
		}
		
		private double parseDouble(String string) {
			if (string == null) return 0.0;
			else if (string.trim().equals("")) return 0.0;
			else return Double.valueOf(string);
		}
	}
}
