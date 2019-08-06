package commercialtraffic.examples;/* *********************************************************************** *
				* project: org.matsim.*
				*                                                                         *
				* *********************************************************************** *
				*                                                                         *
				* copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import commercialtraffic.deliveryGeneration.PersonDelivery;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;

import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;

/**
 * @author thiel
 */
public class AttributesParcelExample {
	static String shapeFile = "D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Cemdap\\add_data\\shp\\Hannover_Stadtteile.shp";
	static String shapeFeature = "NO";
	static Set<String> zones = new HashSet<>();
	static Map<String, Geometry> zoneMap = new HashMap<>();
	static Map<Id<Person>, Coord> deliveredPersons = new HashMap<>();






	public static void main(String[] args) {
		Config config = createConfig();
		Scenario scenario = createScenario(config);
		String personsFile = "D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Simulation\\output\\vw243_cadON_ptSpeedAdj.0.1\\vw243_cadON_ptSpeedAdj.0.1.output_plans.xml.gz";

		new PopulationReader(scenario).readFile(personsFile);

		int deliveryCounter = 0;
		int personCounter = 0;
		//int maxParcels = 18541/10;

		// Servicezeiten des Unternehmens
		int deliveryBusinessStart = 9 * 3600;
		int deliveryBusinessEnd = 18 * 3600;
		// TODO random Verteilung der Zustellungen
		double deliveryRate = 0.05;

		// Sollte so umgesetzt werden, dass man die einzelnen Attribute der Pakete
		// festlegen kann (Volumen, Gewicht,...)
		String parcelSmall = "smallParcel";
		// String parcelMid ="midParcel";
		// String parcelBig ="BigParcel";
		
		readShape(shapeFile, shapeFeature);


		for (Person p : scenario.getPopulation().getPersons().values()) {
			personCounter++;
			Plan plan=p.getSelectedPlan();

				for (PlanElement pe : plan.getPlanElements()) {

					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						// Prüfe ob aktuelle Activität die erste ist
						boolean firstAct = false;
						if (act == PopulationUtils.getFirstActivity(plan)) {
							firstAct = true;
						}
						double r=Math.random();
						// Wenn Home-Activity erste Aktivität ist hat diese auch eine End-Time
						if (act.getType().startsWith("home") && r <= deliveryRate && isWithinZone(act.getCoord(), zoneMap)) {
							if (act.getEndTime() > deliveryBusinessStart && firstAct) {
								act.getAttributes().putAttribute("deleteActivity", "None");
								act.getAttributes().putAttribute("deliveryAmount", "1");
								act.getAttributes().putAttribute("deliveryType", parcelSmall);
								act.getAttributes().putAttribute("deliveryTimeStart", (deliveryBusinessStart));
								act.getAttributes().putAttribute("deliveryTimeEnd",
										Math.min(deliveryBusinessEnd, act.getEndTime()));
								deliveryCounter++;

								break;
							}
							// Wenn Home-Actiovity nicht erste Act. dann evtl. End-Time=-inf -> ANpassung
							// der Delivery zeiten
							else if (firstAct == false) {
								Leg prevLeg = PopulationUtils.getPreviousLeg(plan, act);
								Activity prevAct = PopulationUtils.getPreviousActivity(plan, prevLeg);
//								double prevActEndtime = prevAct.getEndTime(); // this also can be -Inf so it is better to use the expected arrival time out of the prevLeg. see comment below
//								if (prevActEndtime < deliveryBusinessEnd) {
                                if(prevLeg.getDepartureTime()+prevLeg.getTravelTime() < deliveryBusinessEnd){
									act.getAttributes().putAttribute("deleteActivity", "None");
									act.getAttributes().putAttribute("deliveryAmount", "1");
									act.getAttributes().putAttribute("deliveryType", parcelSmall);
									act.getAttributes().putAttribute("deliveryTimeStart",
//											Math.max(deliveryBusinessStart, prevActEndtime)); //as activity end time most often is not set for prevAct, this almost always leads to deliveryTimeStart = deliveryBusinessStart
																								// even if home activity is in the evening. But we can derive expected arrival time at home activity out of the leg.tschlenther 6.8.2019
											Math.max(deliveryBusinessStart,prevLeg.getDepartureTime()+prevLeg.getTravelTime()) ); //one could think of inserting a buffer here..

									if (act.getEndTime() < 0) {
                                        if(PopulationUtils.getLastActivity(plan).equals(act)){
                                            act.getAttributes().putAttribute("deliveryTimeEnd", deliveryBusinessEnd);
                                        } else {
                                            act.getAttributes().putAttribute("deliveryTimeEnd",
                                                    Math.min(PopulationUtils.getNextLeg(plan,act).getDepartureTime(), deliveryBusinessEnd));
                                        }
                                        deliveryCounter++;
                                        break;
									} else {
										act.getAttributes().putAttribute("deliveryTimeEnd",
												Math.min(act.getEndTime(), deliveryBusinessEnd));
										deliveryCounter++;
										break;
									}
								}
							}

						deliveredPersons.put(p.getId(), act.getCoord());

						}
					}

				}

		}

		System.out.println("Es werden:" + deliveryCounter + " von " + personCounter + " beliefert.");
		new PopulationWriter(scenario.getPopulation())
				.write("D:\\Thiel\\Programme\\MatSim\\05_Commercial_H\\Parcel\\Daten\\DHL\\Dummy_Pläne\\parcel_demand_0.1.xml.gz");


	}

	public static void readShape(String shapeFile, String featureKeyInShapeFile) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id = feature.getAttribute(featureKeyInShapeFile).toString();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.add(id);
			zoneMap.put(id, geometry);
		}
	}

	public static boolean isWithinZone(Coord coord, Map<String, Geometry> zoneMap) {
		// Function assumes Shapes are in the same coordinate system like MATSim
		// simulation

		for (String zone : zoneMap.keySet()) {
			Geometry geometry = zoneMap.get(zone);
			if (geometry.intersects(MGC.coord2Point(coord))) {
				// System.out.println("Coordinate in "+ zone);
				return true;
			}
		}

		return false;
	}

}
