package playground.dhosse.gap.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.genericUtils.RecursiveStatsContainer;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PointFeatureFactory.Builder;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.gap.Global;

public class CsAnalysis {
	
	private static final String it = "90";
	
	public static void main(String args[]){
		
//		SpatialAnalysis.writePopulationToShape(Global.runInputDir + "output_plans.xml.gz", "/home/danielhosse/population.shp");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(Global.runInputDir + "merged-networkV2_20150929.xml");
		
		BufferedReader reader = IOUtils.getBufferedReader("/home/danielhosse/run11/output/ITERS/it." + it + "/" + it + ".OW_CS");
		Set<String> userIds = new HashSet<>();
		
		RecursiveStatsContainer durationStats = new RecursiveStatsContainer();
		RecursiveStatsContainer distanceStats = new RecursiveStatsContainer();
		RecursiveStatsContainer accessStats = new RecursiveStatsContainer();
		RecursiveStatsContainer egressStats = new RecursiveStatsContainer();
		
		Map<String, Tuple<Double,Double>> linkId2PassengerInteractions = new HashMap<>();
		
		try {
			
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null){
				
				String[] lineParts = line.split(" ");
				
				String personId = lineParts[0];
				String startTime = lineParts[1];
				String endTime = lineParts[2];
				String startLinkId = lineParts[3];
				String endLinkId = lineParts[4];
				String distance = lineParts[5];
				String accessTime = lineParts[6];
				String egressTime = lineParts[7];
				String vehicleId = lineParts[8];
				
				double d = Double.parseDouble(distance);
				
				if(d == 0 || vehicleId.equals("null")) continue;
				
				double duration = Double.parseDouble(endTime) - Double.parseDouble(startTime);
				
				userIds.add(personId);
				
				distanceStats.handleNewEntry(d);
				durationStats.handleNewEntry(duration);
				accessStats.handleNewEntry(Double.parseDouble(accessTime));
				egressStats.handleNewEntry(Double.parseDouble(egressTime));
				
				if(!linkId2PassengerInteractions.containsKey(startLinkId)){
					
					linkId2PassengerInteractions.put(startLinkId, new Tuple<Double, Double>(0., 0.));
					
				} else {
					
					double nBoardings = linkId2PassengerInteractions.get(startLinkId).getFirst() + 1;
					double nAlightings = linkId2PassengerInteractions.get(startLinkId).getSecond();
					
					linkId2PassengerInteractions.put(startLinkId, new Tuple<Double, Double>(nBoardings, nAlightings));
					
				}
				
				if(!linkId2PassengerInteractions.containsKey(endLinkId)){
					
					linkId2PassengerInteractions.put(endLinkId, new Tuple<Double, Double>(0., 0.));
					
				} else {
					
					double nBoardings = linkId2PassengerInteractions.get(endLinkId).getFirst();
					double nAlightings = linkId2PassengerInteractions.get(endLinkId).getSecond() + 1;
					
					linkId2PassengerInteractions.put(endLinkId, new Tuple<Double, Double>(nBoardings, nAlightings));
					
				}
				
			}
			
			reader.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		BufferedWriter writer = IOUtils.getBufferedWriter("/home/danielhosse/userIds.csv");
		
		try {
			
			writer.write("personId");
			
			for(String id : userIds){
				writer.newLine();
				writer.write(id);
			}
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		System.out.println(distanceStats.getNumberOfEntries() + " persons used car sharing");
		System.out.println("###########################################################################");
		System.out.println("distance stats:");
		System.out.println("mean: " + distanceStats.getMean() + "\tmax: " + distanceStats.getMax() + "\tmin: " + distanceStats.getMin());
		System.out.println("###########################################################################");
		System.out.println("duration stats:");
		System.out.println("mean: " + Time.writeTime(durationStats.getMean()) + "\tmax: " + Time.writeTime(durationStats.getMax()) + "\tmin: " + Time.writeTime(durationStats.getMin()));
		System.out.println("###########################################################################");
		System.out.println("access stats:");
		System.out.println("mean: " + Time.writeTime(accessStats.getMean()) + "\tmax: " + Time.writeTime(accessStats.getMax()) + "\tmin: " + Time.writeTime(accessStats.getMin()));
		System.out.println("###########################################################################");
		System.out.println("egress stats:");
		System.out.println("mean: " + Time.writeTime(egressStats.getMean()) + "\tmax: " + Time.writeTime(egressStats.getMax()) + "\tmin: " + Time.writeTime(egressStats.getMin()));
		
		Builder builder = new Builder();
		builder.setCrs(MGC.getCRS(Global.toCrs));
		builder.addAttribute("boardings", Double.class);
		builder.addAttribute("alightings", Double.class);
		PointFeatureFactory pff = builder.create();
		
		Collection<SimpleFeature> features = new ArrayList<>();
		
		for(Entry<String, Tuple<Double,Double>> entry : linkId2PassengerInteractions.entrySet()){
			
			Coord coordinate = scenario.getNetwork().getLinks().get(Id.createLinkId(entry.getKey())).getCoord();
			Object[] attributeValues = new Double[]{entry.getValue().getFirst(), entry.getValue().getSecond()};
			
			features.add(pff.createPoint(coordinate, attributeValues, null));
			
		}
		
		ShapeFileWriter.writeGeometries(features, "/home/danielhosse/passengerInteractions.shp");
		
	}
	
}
