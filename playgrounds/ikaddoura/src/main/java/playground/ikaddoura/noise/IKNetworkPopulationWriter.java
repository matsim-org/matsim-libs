/* *********************************************************************** *
 * project: org.matsim.*
 * HomeLocationFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.ikaddoura.noise;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dhosse, ikaddoura
 *
 */
public class IKNetworkPopulationWriter {
	
	private final static Logger log = Logger.getLogger(IKNetworkPopulationWriter.class);

	private static Scenario scenario;
	private static SimpleFeatureBuilder builder;
	
	static Map<Id<Coord>,Coord> receiverPoints = new HashMap<>();
	
//	private String networkFile = "C:/MA_Noise/Zwischenpraesentation/Testszenarien/input/network01.xml";
//	private static String populationFile = "C:/MA_Noise/Zwischenpraesentation/Testszenarien/input/populationDay.xml";
//	private static String outputPath = 	"C:/MA_Noise/Zwischenpraesentation/Testszenarien/input";
	
//	private static String populationFile = "/Users/Lars/Desktop/noiseInternalization20/SiouxFalls/input/population_5_prozent_homogeneous.xml";
	private static String networkFile = "/Users/Lars/Desktop/VERSUCH/Berlin/Berlin_network.xml";
	private static String populationFile = "/Users/Lars/Desktop/VERSUCH/Berlin/Berlin_population.xml.gz";
//	private static String populationFile = "/Users/Lars/Desktop/noiseInternalization20/SiouxFalls/input/population_20_prozent_A.xml";
	
	private static String outputPath = "/Users/Lars/Desktop/VERSUCH/Berlin/";

	public static void main(String[] args) {
		
		IKNetworkPopulationWriter main = new IKNetworkPopulationWriter();	
		main.run();		
	}
	
	private void run() {
		
		loadScenario();
		
		File file = new File(outputPath);
		file.mkdirs();
		
//		exportNetwork2Shp(scenario.getNetwork());
		exportActivities2Shp();
//		exportReceiverPoints2Shp();
//		exportActivityConnection2DoubleValue();
		
	}
	
	private void loadScenario() {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
	}
	
	private void exportActivities2Shp(){
		
		new PopulationReaderMatsimV5(scenario).readFile(populationFile);
		
		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
		tbuilder.setName("shape");
		tbuilder.add("geometry", Point.class);
		tbuilder.add("type", String.class);
		
		builder = new SimpleFeatureBuilder(tbuilder.buildFeatureType());
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		
		GeometryFactory gf = new GeometryFactory();
		
		int i = 0;
		
		for(Person p : scenario.getPopulation().getPersons().values()){
			
			if(i%1000. == 0.) {log.info(i);}
//			if(Math.random()<0.1){
			
			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){

				if(pe instanceof Activity){
					
					Activity act = (Activity)pe;
					SimpleFeature feature = builder.buildFeature(Integer.toString(i),new Object[]{
						gf.createPoint(MGC.coord2Coordinate(act.getCoord())),
						act.getType()
					});
					i++;
					features.add(feature);
					
				}
				
//			}
			}
		}
		
		log.info("Writing out activity points shapefile... ");
		ShapeFileWriter.writeGeometries(features, outputPath + "activities_withType_100.shp");
		log.info("Writing out activity points shapefile... Done.");		
	}
	
	public static void getReceiverPoints(Scenario scenario) {
		double receiverPointGap = 10.; //TODO
		double xMin = Double.MAX_VALUE;
		double xMax = Double.MIN_VALUE;
		double yMin = Double.MAX_VALUE;
		double yMax = Double.MIN_VALUE;
		
		for(Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
			Link link = scenario.getNetwork().getLinks().get(linkId);
			Coord fromNodeCoord = link.getFromNode().getCoord();
			Coord toNodeCoord = link.getToNode().getCoord();
			
			if(fromNodeCoord.getX()<xMin) {
				xMin = fromNodeCoord.getX();
			}
			if(toNodeCoord.getX()<xMin) {
				xMin = toNodeCoord.getX();
			}
			if(fromNodeCoord.getY()<yMin) {
				yMin = fromNodeCoord.getY();
			}
			if(toNodeCoord.getY()<yMin) {
				yMin = toNodeCoord.getY();
			}
			
			if(fromNodeCoord.getX()>xMax) {
				xMax = fromNodeCoord.getX();
			}
			if(toNodeCoord.getX()>xMax) {
				xMax = toNodeCoord.getX();
			}
			if(fromNodeCoord.getY()>yMax) {
				yMax = fromNodeCoord.getY();
			}
			if(toNodeCoord.getY()>yMax) {
				yMax = toNodeCoord.getY();
			}
		}
		
//		int counter = 0;
//		for(double y = yMax ; y > yMin ; y = y - receiverPointGap) {
//			for(double x = xMin ; x < xMax ; x = x + receiverPointGap) {
//				Coord newCoord = new CoordImpl(x, y);
//				receiverPoints.put(Id.create("coordId"+counter), newCoord);
//				counter++;
//			}
//		}
		
		// SiouxFalls
//		int counter = 0;
//		for(double y = 4832500. ; y > 4818500 ; y = y - receiverPointGap) {
//			for(double x = 678000 ; x < 688000 ; x = x + receiverPointGap) {
//				Coord newCoord = new CoordImpl(x, y);
//				receiverPoints.put(Id.create("coordId"+counter), newCoord);
//				counter++;
//			}
//		}
		
		int counter = 0;
		for(double y = 4828500. ; y > 4827500 ; y = y - receiverPointGap) {
			for(double x = 682000 ; x < 683000 ; x = x + receiverPointGap) {
				Coord newCoord = new CoordImpl(x, y);
				receiverPoints.put(Id.create("coordId"+counter, Coord.class), newCoord);
				counter++;
			}
		}

		//TODO: only for samples
		
		new PopulationReaderMatsimV5(scenario).readFile(populationFile);
		List<Coord> coordList = new ArrayList<Coord>();
		for(Person p : scenario.getPopulation().getPersons().values()){
			
			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
		
				if(pe instanceof Activity){
					Coord coord = ((Activity) pe).getCoord();
					coordList.add(coord);
				}
			}
		}
		for(int j = 0 ; j < 10 ; j++) {
			log.info(coordList.get(j));
		}
		log.info("111");
		
		Map<Coord,Integer> coords2number = new HashMap<Coord, Integer>();
		List<Coord> coordListDeleting = new ArrayList<Coord>();
		for(Coord coordx : coordList) {
			coordListDeleting.add(coordx);
		}
		for(int j = 0 ; j < 10 ; j++) {
			log.info(coordListDeleting.get(j));
		}		
		for (Coord newCoord : coordList) {
				
			double x1 = newCoord.getX();
			double y1 = newCoord.getY();
					
			Coord coordTmp = null;
			double minDistance = Double.MAX_VALUE;
			for(Coord coordy : coordListDeleting) {
				if(!(coordy==newCoord)) {
					if((Math.sqrt(Math.pow(x1-coordy.getX(), 2)+Math.pow(y1-coordy.getY(), 2)))<minDistance) {
						minDistance = Math.sqrt(Math.pow(x1-coordy.getX(), 2)+Math.pow(y1-coordy.getY(), 2));
						coordTmp = coordy;
					}
				}
			}
//			log.info(minDistance);
//			log.info("+++"+coordTmp);
			if(minDistance<10.) {
				if(coords2number.containsKey(coordTmp)) {
					coords2number.put(coordTmp, coords2number.get(coordTmp)+1);
				} else {
					coords2number.put(coordTmp, 1);
				}
				coordListDeleting.remove(newCoord);
			} else {
				if(coords2number.containsKey(newCoord)) {
					coords2number.put(newCoord, coords2number.get(newCoord)+1);
				} else {
					coords2number.put(newCoord, 1);
				}
			}
		}
		for(int j = 0 ; j < 10 ; j++) {
			log.info(coords2number.get(j));
		}
		log.info("222");
		
		//von oben nach unten sortieren
		List<Coord> sortedList = new ArrayList<Coord>();
		int sizeNumber = coords2number.size();
		for(int i = 0 ; i<sizeNumber ; i++) {
			int maxValue = 0;
			Coord maxCoord = null;
			for (Coord coord : coords2number.keySet()) {
				if(coords2number.get(coord)>maxValue) {
					maxCoord = coord;
				}
			}
			sortedList.add(maxCoord);
		}
		for(int j = 0 ; j < 10 ; j++) {
			log.info(sortedList.get(j));
		}
		log.info("333");
		
		for(Coord coord : sortedList) {
			double minDistance = Double.MAX_VALUE;
			for (Id<Coord> coordId : receiverPoints.keySet()) {
				double x1 = receiverPoints.get(coordId).getX();
				double y1 = receiverPoints.get(coordId).getY();
				double x2 = coord.getX();
				double y2 = coord.getY();
				double distance = (Math.sqrt((Math.pow((x1-x2),2)+Math.pow((y1-y2),2))));
				if(distance<minDistance) {
					minDistance = distance;
				}
				
				if(distance<150.0) {
					receiverPoints.put(Id.create("coordId"+counter+"_"+coord.getX()+"_"+coord.getY(), Coord.class), coord);
					counter++;
					System.out.println("receiverPointCounter: "+counter);
				}
			}
		}
		
		
					
//					Version GAMMA					
//					int b = 0;
//					int c = 0;
//					int d = 0;
//					for (Id coordId : receiverPoints.keySet()) {
//						double x1 = receiverPoints.get(coordId).getX();
//						double y1 = receiverPoints.get(coordId).getY();
//						double x2 = newCoord.getX();
//						double y2 = newCoord.getY();
//						double distance = (Math.sqrt((Math.pow((x1-x2),2)+Math.pow((y1-y2),2))));
//						
//						if(distance<50.0) {
//							b = 1;
//						}else if(distance<100.0) {
//							c = 1;
//						}else if(distance<200.0) {
//							d = 1;
//						}
//					}
//					if ((b==0)&&(c==0)&&(d==0)) {
//						receiverPoints.put(Id.create("coordId"+counter+"_"+newCoord.getX()+"_"+newCoord.getY()), newCoord);
//						counter++;
//						System.out.println("receiverPointCounter: "+counter);
//					} else if ((b==0)&&(c==1)) {
//						double random = Math.random();
//						if(random<0.02) {
//							receiverPoints.put(Id.create("coordId"+counter+"_"+newCoord.getX()+"_"+newCoord.getY()), newCoord);
//							counter++;
//							System.out.println("receiverPointCounter: "+counter);
//						}
//					} else if ((b==0)&&(c==0)&&(d==1)) {
//						double random = Math.random();
//						if(random<0.005) {
//							receiverPoints.put(Id.create("coordId"+counter+"_"+newCoord.getX()+"_"+newCoord.getY()), newCoord);
//							counter++;
//							System.out.println("receiverPointCounter: "+counter);
//						}
//					}
	}
	
	private void exportReceiverPoints2Shp(){
		
		getReceiverPoints(scenario);
		
		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
		tbuilder.setName("shape");
		tbuilder.add("geometry", Point.class);
		tbuilder.add("id", String.class);
		
		builder = new SimpleFeatureBuilder(tbuilder.buildFeatureType());
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		
		GeometryFactory gf = new GeometryFactory();
		
		int i = 0;
		
		for(Id<Coord> receiverPointId : receiverPoints.keySet()){
	
			SimpleFeature feature = builder.buildFeature(receiverPointId.toString(),new Object[]{
				gf.createPoint(MGC.coord2Coordinate(receiverPoints.get(receiverPointId))),
				receiverPointId
			});
			i++;
			features.add(feature);
		
		}
		
		log.info("Writing out receiver points shapefile... ");
		ShapeFileWriter.writeGeometries(features, outputPath + "receiverPoints1000.shp");
		log.info("Writing out activity points shapefile... Done.");		
	}
	
//	public static void exportReceiverPoints2Shp(Map<Id,Double> receiverPointId2noiseImmission){
//		
////		getReceiverPoints(scenario);
//		
//		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
//		tbuilder.setName("shape");
//		tbuilder.add("geometry", Point.class);
//		tbuilder.add("id", String.class);
//		tbuilder.add("noiseImmission", Double.class);
//		
//		builder = new SimpleFeatureBuilder(tbuilder.buildFeatureType());
//		
//		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
//		
//		GeometryFactory gf = new GeometryFactory();
//		
//		int i = 0;
//		
//		for(Id receiverPointId : GetNearestReceiverPoint.receiverPoints.keySet()){
//	
//			SimpleFeature feature = builder.buildFeature(receiverPointId.toString(),new Object[]{
//				gf.createPoint(MGC.coord2Coordinate(GetNearestReceiverPoint.receiverPoints.get(receiverPointId))),
//				receiverPointId,
//				receiverPointId2noiseImmission.get(receiverPointId)
//			});
//			i++;
//			features.add(feature);
//		
//		}
//		
//		log.info("Writing out receiver points shapefile... ");
//		ShapeFileWriter.writeGeometries(features, outputPath + "receiverPoints10_noiseImmissionTest.shp");
//		log.info("Writing out activity points shapefile... Done.");		
//	}
	
//	public static void exportActivityConnection2DoubleValue(Scenario scenario , Map<Id,Double> personId2value , String type){
//		
//		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
//		tbuilder.setName("shape");
//		tbuilder.add("geometry", LineString.class);
//		tbuilder.add("personId", String.class);
//		tbuilder.add("value", Double.class);
//		
//		builder = new SimpleFeatureBuilder(tbuilder.buildFeatureType());
//		
//		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
//		
//		GeometryFactory gf = new GeometryFactory();
//		
//		Map<Id,Tuple<Coord,Coord>> personId2CoordTuple = new HashMap<Id, Tuple<Coord,Coord>>();
//		
//		for(Person p : scenario.getPopulation().getPersons().values()){
//			
//			double x1 = 0.;
//			double y1 = 0.;
//			double x2 = 0.;
//			double y2 = 0.;
//			
//			int i = 0;
//			
//			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
//				
//				if(i<2) {
//						
//					if(pe instanceof Activity){
//							
//						Activity act = (Activity)pe;
//						if(i == 0) {
//							x1 = act.getCoord().getX();
//							y1 = act.getCoord().getY();
//						} else if(i==1) {
//							x2 = act.getCoord().getX();
//							y2 = act.getCoord().getY();
//						}
//						i++;
//					}
//				}
//			}
//			Coord first = new CoordImpl(x1, y1);
//			Coord second = new CoordImpl(x2, y2);
//			Tuple <Coord,Coord> tupleTmp = new Tuple<Coord, Coord>(first, second);
//			personId2CoordTuple.put(p.getId(), tupleTmp);
//		}
//			
//		for(Id personId : scenario.getPopulation().getPersons().keySet()){
//		
//			SimpleFeature feature = builder.buildFeature(personId.toString(),new Object[]{
//				
//				gf.createLineString(new Coordinate[]{
//						new Coordinate(MGC.coord2Coordinate(personId2CoordTuple.get(personId).getFirst())),
//						new Coordinate(MGC.coord2Coordinate(personId2CoordTuple.get(personId).getSecond()))
//				}),
//				personId,
//				personId2value.get(personId),
////				(int) ((Math.random()-0.5)*1000),
//			});
//			features.add(feature);
//			
//		}
//			
//		log.info("Writing out receiver points shapefile... ");
////		ShapeFileWriter.writeGeometries(features, outputPath + "lineStrings.shp");
//		ShapeFileWriter.writeGeometries(features, outputPath + "lineStrings_"+type+".shp");
//		log.info("Writing out activity points shapefile... Done.");		
//	}
	
//	public static void exportActivityConnections(){
//		
//		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
//		tbuilder.setName("shape");
//		tbuilder.add("geometry", LineString.class);
//		tbuilder.add("personId", String.class);
//		tbuilder.add("value", Double.class);
//		
//		builder = new SimpleFeatureBuilder(tbuilder.buildFeatureType());
//		
//		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
//		
//		GeometryFactory gf = new GeometryFactory();
//		
//		Map<Id,Tuple<Coord,Coord>> personId2CoordTuple = new HashMap<Id, Tuple<Coord,Coord>>();
//		
//		for(Person p : scenario.getPopulation().getPersons().values()){
//			
//			double x1 = 0.;
//			double y1 = 0.;
//			double x2 = 0.;
//			double y2 = 0.;
//			
//			int i = 0;
//			
//			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
//				
//				if(i<2) {
//						
//					if(pe instanceof Activity){
//							
//						Activity act = (Activity)pe;
//						if(i == 0) {
//							x1 = act.getCoord().getX();
//							y1 = act.getCoord().getY();
//						} else if(i==1) {
//							x2 = act.getCoord().getX();
//							y2 = act.getCoord().getY();
//						}
//						i++;
//					}
//				}
//			}
//			Coord first = new CoordImpl(x1, y1);
//			Coord second = new CoordImpl(x2, y2);
//			Tuple <Coord,Coord> tupleTmp = new Tuple<Coord, Coord>(first, second);
//			personId2CoordTuple.put(p.getId(), tupleTmp);
//		}
//			
//		for(Id personId : scenario.getPopulation().getPersons().keySet()){
//		
//			SimpleFeature feature = builder.buildFeature(personId.toString(),new Object[]{
//				
//				gf.createLineString(new Coordinate[]{
//						new Coordinate(MGC.coord2Coordinate(personId2CoordTuple.get(personId).getFirst())),
//						new Coordinate(MGC.coord2Coordinate(personId2CoordTuple.get(personId).getSecond()))
//				}),
//				personId,
////				personId2value.get(personId),
//				(int) ((Math.random()-0.5)*1000),
//			});
//			features.add(feature);
//			
//		}
//			
//		log.info("Writing out receiver points shapefile... ");
//		ShapeFileWriter.writeGeometries(features, outputPath + "lineStrings.shp");
////		ShapeFileWriter.writeGeometries(features, outputPath + "lineStrings_"+type+".shp");
//		log.info("Writing out activity points shapefile... Done.");		
//	}
	
	public static void exportNetwork2Shp(Network network){

		if (scenario.getNetwork().getLinks().size() == 0) {
			new NetworkReaderMatsimV1(scenario).parse(networkFile);
		}
				
		SimpleFeatureTypeBuilder tbuilder = new SimpleFeatureTypeBuilder();
		tbuilder.setName("shape");
		tbuilder.add("geometry", LineString.class);
		tbuilder.add("id", String.class);
		tbuilder.add("length", Double.class);
		tbuilder.add("capacity", Double.class);
		tbuilder.add("freespeed", Double.class);
		tbuilder.add("modes", String.class);
		
		builder = new SimpleFeatureBuilder(tbuilder.buildFeatureType());
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		
		GeometryFactory gf = new GeometryFactory();
		
		for(Link link : scenario.getNetwork().getLinks().values()){
			SimpleFeature feature = builder.buildFeature(link.getId().toString(), new Object[]{
					gf.createLineString(new Coordinate[]{
							new Coordinate(MGC.coord2Coordinate(link.getFromNode().getCoord())),
							new Coordinate(MGC.coord2Coordinate(link.getToNode().getCoord()))
					}),
					link.getId(),
					link.getLength(),
					link.getCapacity(),
					link.getFreespeed(),
					link.getAllowedModes().toString(),
			});
			features.add(feature);
		}
		
		log.info("Writing out network lines shapefile... ");
		ShapeFileWriter.writeGeometries(features, outputPath + "network.shp");
		log.info("Writing out network lines shapefile... Done.");
	}

}
