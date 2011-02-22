/* *********************************************************************** *
 * project: org.matsim.*
 * CalculateTravelTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.agglobern;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Counter;
import org.matsim.withinday.trafficmonitoring.FreeSpeedTravelTimeCalculator;

public class CalculateTravelTimes {

	final private static Logger log = Logger.getLogger(CalculateTravelTimes.class);

	private String networkFile = "../../matsim/mysimulations/Reisezeiten_Metropolitanraum_Basel/network_updated.xml";
	private String inFile = "../../matsim/mysimulations/Reisezeiten_Metropolitanraum_Basel/Gemeinde_MetroBasel_CHXY_GSC_WGS84_Coord.csv";
	private String outFile = "../../matsim/mysimulations/Reisezeiten_Metropolitanraum_Basel/TravelTimesMatrix.csv";
	
	private String separator = ",";
	private Charset charset = Charset.forName("ISO-8859-1");
//	private Charset charset = Charset.forName("UTF-8");
	
	private Map<Integer, Data> dataLines;
	private double[][] travelTimes;
	
	private Scenario scenario;
	private TravelTime travelTime;
	private TravelCost travelCost;
	private LeastCostPathCalculator leastCostPathCalculator;
	
	private double connectorSpeed = 45 / 3.6;	// 45 km/h to reach the network
	
	public static void main(String[] args) throws Exception {
		new CalculateTravelTimes(new ScenarioImpl());
	}
	
	public CalculateTravelTimes(Scenario scenario) throws IOException {
		this.scenario = scenario;
		
		log.info("Read Network File...");
		readNetworkFile(scenario);
		log.info("done.");

		log.info("Initialize Calculators...");
		initCalulators();
		log.info("done.");
		
		log.info("Reading Zones File...");
		readFile();
		log.info("done.");
				
		log.info("Calculate TravelTimes...");
		doCalculation();
		log.info("done.");
		
		log.info("Write TravelTimes...");
		writeFile();
		log.info("done.");
	}
	
	private void initCalulators() {
		travelTime = new FreeSpeedTravelTimeCalculator();
		travelCost = new FreeSpeedTravelCost(travelTime);
		leastCostPathCalculator = new AStarLandmarksFactory(scenario.getNetwork(), (FreeSpeedTravelCost)travelCost).createPathCalculator(scenario.getNetwork(), travelCost, travelTime);
	}
	
	private void readNetworkFile(Scenario scenario) {
		new MatsimNetworkReader(scenario).readFile(networkFile);
	}
	
	private void doCalculation() {
		travelTimes = new double[dataLines.size()][];
		
		Counter counter = new Counter("Calculated Travel Times: ");
		
		int from = 0;
		for (Entry<Integer, Data> fromEntry : dataLines.entrySet()) {
			int fromId = fromEntry.getKey();
			Data fromData = fromEntry.getValue();
			
			Link fromLink = ((NetworkImpl)scenario.getNetwork()).getNearestLink(fromData.coord1903);
			double fromDistance = ((CoordImpl)fromLink.getCoord()).calcDistance(fromData.coord1903);

			travelTimes[from] = new double[dataLines.size()];
			int to = 0;
			for (Entry<Integer, Data> toEntry : dataLines.entrySet()) {
				int toId = toEntry.getKey();
				Data toData = toEntry.getValue();

				/*
				 * Interzonal Trip -> no Travel Time
				 */
				if (fromId == toId) {
					travelTimes[from][to] = 0.0;
					to++;
					counter.incCounter();
					continue;
				}
				
				Link toLink = ((NetworkImpl)scenario.getNetwork()).getNearestLink(toData.coord1903);
				double toDistance = ((CoordImpl)toLink.getCoord()).calcDistance(toData.coord1903);
				
				/*
				 * If both coordinates were mapped to the same Link.
				 * Create a Warning - there may be a Problem with the network resolution.
				 * Use direct distance between coordinates.
				 */
				if (fromLink.getId().equals(toLink.getId())) {
					log.warn("Two different zones were mapped to the same Link - there may be a Problem with the Network resolution. "
							+ "FromId: " + fromId + ", ToId: " + toId);
					
					double distance = ((CoordImpl)fromData.coord1903).calcDistance(toData.coord1903);
					travelTimes[from][to] = distance/connectorSpeed;
					to++;
					counter.incCounter();
					continue;
				}
				
				/*
				 * If the Route starts and ends at the same Node but the Links are different.
				 * Use half Travel Costs of both Links + Travel Costs to reach the Links.
				 */
				if (fromLink.getFromNode().getId().equals(toLink.getToNode().getId())) {
					
					double fromCosts = 0.5 * travelCost.getLinkGeneralizedTravelCost(fromLink, 0.0);
					double toCosts = 0.5 * travelCost.getLinkGeneralizedTravelCost(fromLink, 0.0);
					double costs = fromDistance/connectorSpeed + fromCosts + toCosts + toDistance/connectorSpeed;
					travelTimes[from][to] = costs;
					to++;
					counter.incCounter();
					continue;
				}
				
				/*
				 * By Default:
				 * Calculate Path from To-Node to To-Node. We assume that the trip starts
				 * on the middle of the From-Link and ends on the middle of the To-Link.
				 * Further we assume that both links have a comparable TravelTime.
				 */
				Path path = leastCostPathCalculator.calcLeastCostPath(fromLink.getFromNode(), toLink.getToNode(), 0.0);
				List<Link> links = path.links;
				
				double pathTravelCost = path.travelCost;
				if (links != null) {
					boolean fromLinkIncluded = links.get(0).getId().equals(fromLink.getId());
					if (fromLinkIncluded) pathTravelCost = pathTravelCost - 0.5 * travelCost.getLinkGeneralizedTravelCost(fromLink, 0.0);
					else pathTravelCost = pathTravelCost + 0.5 * travelCost.getLinkGeneralizedTravelCost(fromLink, 0.0);
						
					boolean toLinkIncluded = links.get(links.size() - 1).getId().equals(toLink.getId());
					if (toLinkIncluded) pathTravelCost = pathTravelCost - 0.5 * travelCost.getLinkGeneralizedTravelCost(toLink, 0.0);
					else pathTravelCost = pathTravelCost + 0.5 * travelCost.getLinkGeneralizedTravelCost(toLink, 0.0);
				}
				
				double costs = fromDistance/connectorSpeed + pathTravelCost + toDistance/connectorSpeed;
							
//				for (Link link : path.links) {
//					log.info("from " + link.getFromNode().getId() + " to " + link.getToNode().getId() + " travelcost " + travelCost.getLinkTravelCost(link, 0.0));
//				}
//				log.info("");
				
				travelTimes[from][to] = costs;
				to++;
				counter.incCounter();
			}
			from++;
		}
	}
	
	private void readFile() throws IOException {
		dataLines = new TreeMap<Integer, Data>();
		
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
  		
		fis = new FileInputStream(inFile);
		isr = new InputStreamReader(fis, charset);
		br = new BufferedReader(isr);
		
		// skip first Line
		br.readLine();
		 
		String line;
		while((line = br.readLine()) != null) {
			Data data = new Data();
			
			String[] cols = line.split(separator);
			
			data.id = parseInteger(cols[0]);
			data.city = cols[1];
			data.country = cols[2];
			
			double x1903 = parseDouble(cols[3]);
			double y1903 = parseDouble(cols[4]);
			data.coord1903 = scenario.createCoord(x1903, y1903);
			
			double xWGS84 = parseDouble(cols[5]);
			double yWGS84 = parseDouble(cols[6]);
			data.coordWGS84 = scenario.createCoord(xWGS84, yWGS84);
							
			dataLines.put(data.id, data);
		}
		
		br.close();
		isr.close();
		fis.close();		
	}
	
	public void writeFile() throws IOException {
		FileOutputStream fos = null; 
		OutputStreamWriter osw = null; 
	    BufferedWriter bw = null;
	    
		fos = new FileOutputStream(outFile);
		osw = new OutputStreamWriter(fos, charset);
		bw = new BufferedWriter(osw);
		
		// write Header
		String header = "";
		for (int id : dataLines.keySet()) {
			header = header + separator + id; 
		}
		bw.write(header);
		bw.write("\n");
		
		Iterator<Integer> rows = dataLines.keySet().iterator();
		// write Values
		for (double[] array : travelTimes)
		{	
			bw.write(String.valueOf(rows.next()));
			for (double value : array) {
				bw.write(separator);
				bw.write(String.valueOf(value));
			}
			bw.write("\n");
		}
		
		bw.close();
		osw.close();
		fos.close();
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
		
	public static class Data {
		int id;
		String city;
		String country;
		Coord coord1903;
		Coord coordWGS84;
	}
		
	public class FreeSpeedTravelCost implements PersonalizableTravelCost, TravelMinCost {
		private TravelTime travelTime;
		
		public FreeSpeedTravelCost(TravelTime travelTime) {
			this.travelTime = travelTime;
		}
		
		public void setPerson(Person person) {
			// nothing to do here
		}

		public double getLinkGeneralizedTravelCost(Link link, double time) {
			return travelTime.getLinkTravelTime(link, time);
		}

		public double getLinkMinimumTravelCost(Link link) {
			return travelTime.getLinkTravelTime(link, 0.0);
		}
	}
}