/* *********************************************************************** *
 * project: org.matsim.*
 * ReplaceFacilities.java
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

package playground.christoph.energyflows.population;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class IdentifyActivitiesToRelocate {

	final private static Logger log = Logger.getLogger(IdentifyActivitiesToRelocate.class);
	
	private String networkFile = "../../matsim/mysimulations/2kw/network/network.xml.gz";
	private String facilitiesFile = "../../matsim/mysimulations/2kw/facilities/facilities.xml.gz";
	private String populationFile = "../../matsim/mysimulations/2kw/population/plans_100pct_dilZh30km.xml.gz";
	private String cityZurichSHPFile = "../../matsim/mysimulations/2kw/gis/Zurich_City.shp";
	
	private String homeActivitiesToRelocate =  "../../matsim/mysimulations/2kw/gis/homeActivities.csv";
	private String leisureActivitiesToRelocate = "../../matsim/mysimulations/2kw/gis/leisureActivities.csv";
	private String shopActivitiesToRelocate = "../../matsim/mysimulations/2kw/gis/shopActivities.csv";
	private String workSector2ActivitiesToRelocate = "../../matsim/mysimulations/2kw/gis/workSector2Activities.csv";
	private String workSector3ActivitiesToRelocate = "../../matsim/mysimulations/2kw/gis/workSector3Activities.csv";
	private String educationPrimaryActivitiesToRelocate = "../../matsim/mysimulations/2kw/gis/educationPrimaryActivities.csv";
	private String educationSecondaryActivitiesToRelocate = "../../matsim/mysimulations/2kw/gis/educationSecondaryActivities.csv";
	private String educationKindergartenActivitiesToRelocate = "../../matsim/mysimulations/2kw/gis/educationKindergartenActivities.csv";
	private String educationHigherActivitiesToRelocate = "../../matsim/mysimulations/2kw/gis/educationHigherActivities.csv";
	private String educationOtherActivitiesToRelocate = "../../matsim/mysimulations/2kw/gis/educationOtherActivities.csv";
	
	private String delimiter = ",";
	private Charset charset = Charset.forName("UTF-8");
	
	private Scenario scenario;
	private MultiPolygon cityZurichPolygon;
	
	public static void main(String[] args) throws Exception {
		new IdentifyActivitiesToRelocate();
	}
	
	public IdentifyActivitiesToRelocate() throws Exception {
		
		readSHPFile();
		
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseKnowledge(true);
		config.scenario().setUseHouseholds(true);
		
		config.plans().setInputFile(populationFile);
		config.facilities().setInputFile(facilitiesFile);
		config.network().setInputFile(networkFile);
		scenario = ScenarioUtils.loadScenario(config);
		
		identifyHomeActivities();
		identifyOtherActivities();
	}
	
	// [x] home
	// [x] leisure
	// [x] shop
	// [x] work_sector2
	// [x] work_sector3
	// [x] education_primary
	// [x] education_secundary
	// [x] education_kindergarten
	// [x] education_higher
	// [x] education_other
	
	protected void identifyHomeActivities() throws Exception {
		GeometryFactory geoFac = new GeometryFactory();
		
		Counter counter = new Counter("People with a home activity inside the city of Zurich: ");
		
		/*
		 * Identify persons that have to be relocated
		 */
		List<Person> personToRelocate = new ArrayList<Person>();
		List<String> gisData = new ArrayList<String>();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			
			for (PlanElement planElement : plan.getPlanElements()) {
				
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;

					if (activity.getType().equalsIgnoreCase("home")) {
						Coord coord = activity.getCoord();
						Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
						Point point = geoFac.createPoint(coordinate);
						
						if (cityZurichPolygon.contains(point)) {
							personToRelocate.add(person);
							StringBuffer sb = new StringBuffer();
							sb.append(person.getId());
							sb.append(delimiter);
							sb.append(coord.getX());
							sb.append(delimiter);
							sb.append(coord.getY());
							gisData.add(sb.toString());
							counter.incCounter();
							break;
						}
					}
				}
			}
		}
		// print final count
		counter.printCounter();
		log.info("");
		
		// write text file
		writeOutputFile(this.homeActivitiesToRelocate, gisData);
		
//		/*
//		 * Write textfile
//		 */
//		FileOutputStream fos = new FileOutputStream(homeActivitiesToRelocate);
//		OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
//		BufferedWriter bw = new BufferedWriter(osw);
//		
//		// write Header
//		bw.write("id" + delimiter + "x" + delimiter + "y" + "\n");
//		
//		for (String string : gisData) {
//			bw.write(string + "\n");
//		}
//		
//		bw.close();
//		osw.close();
//		fos.close();
	}
	
	protected void identifyOtherActivities() throws Exception {
		GeometryFactory geoFac = new GeometryFactory();
		
		Counter leisureCounter = new Counter("People with a leisure activity inside the city of Zurich: ");
		Counter shopCounter = new Counter("People with a shop activity inside the city of Zurich: ");
		Counter workSector2Counter = new Counter("People with a work sector 2 activity inside the city of Zurich: ");
		Counter workSector3Counter = new Counter("People with a work sector 3 activity inside the city of Zurich: ");
		Counter educationPrimaryCounter = new Counter("People with a education primary activity inside the city of Zurich: ");
		Counter educationSecondaryCounter = new Counter("People with a education secondary activity inside the city of Zurich: ");
		Counter educationKindergartenCounter = new Counter("People with a education kindergarten activity inside the city of Zurich: ");
		Counter educationHigherCounter = new Counter("People with a education higher activity inside the city of Zurich: ");
		Counter educationOtherCounter = new Counter("People with a education other activity inside the city of Zurich: ");
		
		double leisureDuration = 0.0;
		double shopDuration = 0.0;
		double workSector2Duration = 0.0;
		double workSector3Duration = 0.0;
		double educationPrimaryDuration = 0.0;
		double educationSecondaryDuration = 0.0;
		double educationKindergartenDuration = 0.0;
		double educationHigherDuration = 0.0;
		double educationOtherDuration = 0.0;
		
		/*
		 * Identify persons that have to be relocated
		 */
//		List<Person> personToRelocate = new ArrayList<Person>();
		List<String> leisureGisData = new ArrayList<String>();
		List<String> shopGisData = new ArrayList<String>();
		List<String> workSector2GisData = new ArrayList<String>();
		List<String> workSector3GisData = new ArrayList<String>();
		List<String> educationPrimaryGisData = new ArrayList<String>();
		List<String> educationSecondaryGisData = new ArrayList<String>();
		List<String> educationKindergartenGisData = new ArrayList<String>();
		List<String> educationHigherGisData = new ArrayList<String>();
		List<String> educationOtherGisData = new ArrayList<String>();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			
			for (PlanElement planElement : plan.getPlanElements()) {
				
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;

					if (activity.getType().equalsIgnoreCase("home")) {
						continue;
					} else {
						Coord coord = activity.getCoord();
						Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
						Point point = geoFac.createPoint(coordinate);
						
						if (cityZurichPolygon.contains(point)) {
//							personToRelocate.add(person);
							StringBuffer sb = new StringBuffer();
							sb.append(person.getId());
							sb.append(delimiter);
							sb.append(coord.getX());
							sb.append(delimiter);
							sb.append(coord.getY());
							
							if (activity.getType().equalsIgnoreCase("leisure")) {
								leisureGisData.add(sb.toString());
								leisureDuration += activity.getEndTime() - activity.getStartTime();
								leisureCounter.incCounter();
							} else if (activity.getType().equalsIgnoreCase("shop")) {
								shopGisData.add(sb.toString());
								shopDuration += activity.getEndTime() - activity.getStartTime();
								shopCounter.incCounter();
							} else if (activity.getType().equalsIgnoreCase("work_sector2")) {
								workSector2GisData.add(sb.toString());
								workSector2Duration += activity.getEndTime() - activity.getStartTime();
								workSector2Counter.incCounter();
							} else if (activity.getType().equalsIgnoreCase("work_sector3")) {
								workSector3GisData.add(sb.toString());
								workSector3Duration += activity.getEndTime() - activity.getStartTime();
								workSector3Counter.incCounter();
							} else if (activity.getType().equalsIgnoreCase("education_primary")) {
								educationPrimaryGisData.add(sb.toString());
								educationPrimaryDuration += activity.getEndTime() - activity.getStartTime();
								educationPrimaryCounter.incCounter();
							} else if (activity.getType().equalsIgnoreCase("education_secondary")) {
								educationSecondaryGisData.add(sb.toString());
								educationSecondaryDuration += activity.getEndTime() - activity.getStartTime();
								educationSecondaryCounter.incCounter();
							} else if (activity.getType().equalsIgnoreCase("education_kindergarten")) {
								educationKindergartenGisData.add(sb.toString());
								educationKindergartenDuration += activity.getEndTime() - activity.getStartTime();
								educationKindergartenCounter.incCounter();
							} else if (activity.getType().equalsIgnoreCase("education_higher")) {
								educationHigherGisData.add(sb.toString());
								educationHigherDuration += activity.getEndTime() - activity.getStartTime();
								educationHigherCounter.incCounter();
							} else if (activity.getType().equalsIgnoreCase("education_other")) {
								educationOtherGisData.add(sb.toString());
								educationOtherDuration += activity.getEndTime() - activity.getStartTime();
								educationOtherCounter.incCounter();
							} else Log.warn("unknown activity type: " + activity.getType());
						}
					}
				}
			}
		}
		
		// print final counts
		log.info("");
		leisureCounter.printCounter();
		shopCounter.printCounter();
		workSector2Counter.printCounter();
		workSector3Counter.printCounter();
		educationPrimaryCounter.printCounter();
		educationSecondaryCounter.printCounter();
		educationKindergartenCounter.printCounter();
		educationHigherCounter.printCounter();
		educationOtherCounter.printCounter();
		
		log.info("");
		log.info("Total leisure duration " + Time.writeTime(leisureDuration));
		log.info("Total shop duration " + Time.writeTime(shopDuration));
		log.info("Total work sector 2 duration " + Time.writeTime(workSector2Duration));
		log.info("Total work sector 3 duration " + Time.writeTime(workSector3Duration));
		log.info("Total education primary duration " + Time.writeTime(educationPrimaryDuration));
		log.info("Total education secondary duration " + Time.writeTime(educationSecondaryDuration));
		log.info("Total education kindergarten duration " + Time.writeTime(educationKindergartenDuration));
		log.info("Total education higher duration " + Time.writeTime(educationHigherDuration));
		log.info("Total education other duration " + Time.writeTime(educationOtherDuration));
				
		// write text files
		writeOutputFile(this.leisureActivitiesToRelocate, leisureGisData);
		writeOutputFile(this.shopActivitiesToRelocate, shopGisData);
		writeOutputFile(this.workSector2ActivitiesToRelocate, workSector2GisData);
		writeOutputFile(this.workSector3ActivitiesToRelocate, workSector3GisData);
		writeOutputFile(this.educationPrimaryActivitiesToRelocate, educationPrimaryGisData);
		writeOutputFile(this.educationSecondaryActivitiesToRelocate, educationSecondaryGisData);
		writeOutputFile(this.educationKindergartenActivitiesToRelocate, educationKindergartenGisData);
		writeOutputFile(this.educationHigherActivitiesToRelocate, educationHigherGisData);
		writeOutputFile(this.educationOtherActivitiesToRelocate, educationOtherGisData);
	}
	
	private void writeOutputFile(String outputFile, List<String> gisData) throws Exception {
		/*
		 * Write textfile
		 */
		FileOutputStream fos = new FileOutputStream(outputFile);
		OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
		BufferedWriter bw = new BufferedWriter(osw);
		
		// write Header
		bw.write("id" + delimiter + "x" + delimiter + "y" + "\n");
		
		for (String string : gisData) {
			bw.write(string + "\n");
		}
		
		bw.close();
		osw.close();
		fos.close();
	}
	
	protected void readSHPFile() throws Exception {
		FeatureSource featureSource = ShapeFileReader.readDataFile(cityZurichSHPFile);
		for (Object o : featureSource.getFeatures()) {
			Feature feature = (Feature) o;
			cityZurichPolygon = (MultiPolygon)feature.getAttribute(0);
		}
	}

}
