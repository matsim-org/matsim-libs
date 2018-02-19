/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

/**
 * 
 */
package ft.cemdap4H.planspreprocessing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.vsp.demandde.cemdap.output.Cemdap2MatsimUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ReassignZonesByAttractiveness {
	Map<String, Geometry> zoneMap = new HashMap<>();
	Map<String, WVIZonalAttractiveness> wobAttractiveness;
	Map<String, WVIZonalAttractiveness> bsAttractiveness;
	
	
	public static void main(String[] args) {
		new ReassignZonesByAttractiveness().run("/Volumes/Volume/cemdap-vw/add_data/shp/wvi-zones.shp", "/Volumes/Volume/cemdap-vw/add_data/wvi/wobattractivities.txt", "/Volumes/Volume/cemdap-vw/add_data/wvi/bsattractivities.txt", "/Volumes/Volume/cemdap-vw/cemdap_output/mergedPlans_filtered_0.1.xml.gz", "/Users/jb/Desktop/mergedPlans_filtered_1.0_attr.xml.gz");
	}
	
	
	void run(String shapeFile, String attFileWob, String attFileBs, String inputPlansFile, String outputPlansFile){
		readShape (shapeFile, "NO");
		wobAttractiveness = WVIZonalAttractiveness.readZonalAttractiveness(attFileWob, zoneMap);
		bsAttractiveness = WVIZonalAttractiveness.readZonalAttractiveness(attFileBs, zoneMap);
		StreamingPopulationReader spr = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		StreamingPopulationWriter spw = new StreamingPopulationWriter();
		spw.startStreaming(outputPlansFile);
		spr.addAlgorithm(new PersonAlgorithm() {
			
			@Override
			public void run(Person person) {
				for (Plan plan : person.getPlans()){
				int counter = 0;
				for (PlanElement pe : plan.getPlanElements()){
					
					if (pe instanceof Activity){
						Activity act = (Activity) pe;
						String activityShort = act.getType().split("_")[0];

						if (!activityShort.equals("home")){
							String zone = findZone(act.getCoord(),activityShort);
							if (zone!=null){
								Coord preceedingCoord = findPreceedingAnchorActivity(plan);
								Coord proceedingCoord = findProceedingAnchorActivity(plan, counter);
								Map<String, WVIZonalAttractiveness> attractiveness = findAttractiveness(zone);
								Coord newActivityCoord = findNewActivityCoord(preceedingCoord,proceedingCoord,attractiveness,activityShort);
								act.setCoord(newActivityCoord);
							}
						}
					}
					counter++;
				}
				}
			}

			private Coord findNewActivityCoord(Coord preceedingCoord, Coord proceedingCoord,
					Map<String, WVIZonalAttractiveness> attractiveness, String activityType) {
				WeightedRandomSelection<String> wrs = new WeightedRandomSelection<>();
				for (WVIZonalAttractiveness a : attractiveness.values()){
					double v;
					switch (activityType){
						case "work":
							v = a.getAttractivenessWork();
							break;
						case "other":
							v = a.getAttractivenessOther();
							break;
						case "shopping":
							v = a.getAttractivenessShop();
							break;
						case "leisure":
							v = a.getAttractivenessLeisure();
							break;
						case "education":
							v = a.getAttractivenessEducation();
							break;
						default:
							throw new RuntimeException("activitytype unknown: " + activityType);
					}
				double distance = CoordUtils.calcEuclideanDistance(preceedingCoord, a.getZoneCentroid())+CoordUtils.calcEuclideanDistance(proceedingCoord, a.getZoneCentroid());
				double avalue = v/(distance*distance);
				wrs.add(a.getZoneId(), avalue);
				}
				String selectedZone = wrs.select();
				Geometry geometry = zoneMap.get(selectedZone);
				Envelope envelope = geometry.getEnvelopeInternal();
				while (true) {
					Point point = Cemdap2MatsimUtils.getRandomCoordinate(envelope);
					if (point.within(geometry)) {
						return new Coord(point.getX(), point.getY());
					}
				}
				
			}

			private Map<String, WVIZonalAttractiveness> findAttractiveness(String zone) {
				
				if (zone.startsWith("3")) return wobAttractiveness;
				else if ( zone.startsWith("1")) return bsAttractiveness;
				else throw new RuntimeException();
				
			}

			private Coord findProceedingAnchorActivity(Plan plan, int counter) {
				int actc = counter-2;
				Coord c = null;
				if (actc<0){
					actc = 0;
					}
					Activity act = (Activity) plan.getPlanElements().get(counter-2);
					c = act.getCoord();
				
				return c;
			}

			private Coord findPreceedingAnchorActivity(Plan plan) {
				Activity actL = (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-1);
				return actL.getCoord();
			}
			
			private String findZone(Coord coord, String actTypeShort) {
				Point p = MGC.coord2Point(coord);
				for (Entry<String, Geometry> e : zoneMap.entrySet()){
					if (e.getValue().contains(p)){
						if (e.getKey().equals("350")&&actTypeShort.equals("work")){
							return null;
							//VW Werk, change nothing
						}
						return e.getKey();
					}
				}
				
				return null;
			}
		});
		spr.addAlgorithm(spw);
		spr.readFile(inputPlansFile);
		spw.closeStreaming();
		
	}
	
	
	
	private void readShape(String shapeFile, String featureKeyInShapeFile) {
		Collection <SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id =  feature.getAttribute(featureKeyInShapeFile).toString();
			
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zoneMap.put(id, geometry);
		}
	}
}
