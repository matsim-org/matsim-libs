/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Point;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.scenarioSetup.PatnaCalibrationUtils.PatnaDemandLabels;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils.PatnaUrbanActivityTypes;
import playground.agarwalamit.utils.GeometryUtils;
/**
 * The number of trips from trip diaries are 2% (not 1%).
 * @author amit
 */
public class UrbanDemandGenerator {

	private static final Logger LOG = Logger.getLogger(UrbanDemandGenerator.class);
	private static final Random RAND = MatsimRandom.getLocalInstance();

	private Scenario scenario;
	private Collection<SimpleFeature> features ;
	private final int cloningFactor ;

	public UrbanDemandGenerator() {
		this(1);
	}

	public UrbanDemandGenerator(final int cloningFactor) {
		this.cloningFactor = cloningFactor;
	}

	public static void main (String []args) {
		UrbanDemandGenerator pudg = new UrbanDemandGenerator();
		pudg.startProcessing();
		pudg.writePlans(PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/urban/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/");
	}

	public void startProcessing(String inputFilesDir) {
		String wardFile = inputFilesDir + "/raw/others/wardFile/Wards.shp";
		this.features = readZoneFilesAndReturnFeatures(wardFile);

		String planFile1 = inputFilesDir+"/raw/plans/tripDiaryDataIncome/nonSlum_allZones_cleanedData.txt"; //Urban_PlanFile.CSV; // urban plans for all zones except 27 to 42.
		//		String planFile2 = PatnaUtils.INPUT_FILES_DIR+"/27TO42zones.CSV";// urban plans for zones 27 to 42
		String planFile3 = inputFilesDir+"/raw/plans/tripDiaryDataIncome/slum_allZones_cleanedData.txt";//"/Slum_PlanFile.CSV";	

		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);

		filesReader(planFile1, "nonSlum_");
		//		filesReader(planFile2, "nonSlum_");
		filesReader(planFile3, "slum_");
	}
	
	public void startProcessing() {
		String inputFilesDir =  PatnaUtils.INPUT_FILES_DIR;
		startProcessing(inputFilesDir);
	}

	public void writePlans(final String outputDir){
		new PopulationWriter(scenario.getPopulation()).write(outputDir+"/initial_urban_plans_"+cloningFactor+"pct.xml.gz");
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile(outputDir+"/initial_urban_persionAttributes_"+cloningFactor+"pct.xml.gz");
		LOG.info("Writing Plan and person attributes files are finished.");
	}

	public Population getPopulation(){
		return scenario.getPopulation();
	}

	private Collection<SimpleFeature> readZoneFilesAndReturnFeatures(String wardFile) {
		ShapeFileReader reader = new ShapeFileReader();
		return reader.readFileAndInitialize(wardFile);
	}

	private void filesReader (final String planFile, final String idPrefix) {
		Iterator<SimpleFeature> iterator = features.iterator();

		String line;
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(planFile)) ) {
			line = bufferedReader.readLine();
			List<String> labels = new ArrayList<>();

			while (line != null ) {
				String row [] = line.split("\t");
				List<String> strs = Arrays.asList(row);

				if( row[0].substring(0, 1).matches("[A-Za-z]") // labels 
						&& !row[0].startsWith("NA") // "NA" could also be inside the data 
						) {
					for (String s : strs){ 
						labels.add(s); 
					}
				} else { // main data

					//				String[] parts = line.split("\t");
					String fromZoneId = strs.get(labels.indexOf(PatnaDemandLabels.originZone.toString())); //parts [5];
					String toZoneId = strs.get(labels.indexOf(PatnaDemandLabels.destinationZone.toString())); //parts [6]; 
					String tripPurpose = strs.get(labels.indexOf(PatnaDemandLabels.tripPurpose.toString())); //parts [7];

					double monthlyIncome = PatnaUtils.getAverageIncome( strs.get(labels.indexOf(PatnaDemandLabels.monthlyIncome.toString())) );
					double dailyTransportCost = PatnaUtils.getAverageDailyTranportCost( strs.get(labels.indexOf(PatnaDemandLabels.dailyTransportCost.toString())) ) ;

					Coord homeZoneCoordTransform = null ;
					Coord workZoneCoordTransform = null ;
					Point p=null;
					Point q = null;

					Population population = scenario.getPopulation();
					PopulationFactory factory = population.getFactory();

					toZoneId = getCorrectZoneNumber(toZoneId);

					for (int ii = 0; ii< cloningFactor; ii++){ // this will give random points for activities in the plan of every cloned person

						if (fromZoneId.equals(toZoneId)) {
							// intraZonal trips
							while (iterator.hasNext()){

								SimpleFeature feature = iterator.next();

								p = GeometryUtils.getRandomPointsInsideFeature(feature);
								Coord fromZoneCoord = new Coord(p.getX(), p.getY());
								homeZoneCoordTransform = PatnaUtils.COORDINATE_TRANSFORMATION.transform(fromZoneCoord);

								q = GeometryUtils.getRandomPointsInsideFeature(feature);
								Coord toZoneCoord = new Coord(q.getX(), q.getY());
								workZoneCoordTransform= PatnaUtils.COORDINATE_TRANSFORMATION.transform(toZoneCoord);
							}
						} else {														
							while (iterator.hasNext()){
								SimpleFeature feature = iterator.next();
								int id = (Integer) feature.getAttribute("ID1");
								String zoneId  = String.valueOf(id);

								if(fromZoneId.equals(zoneId) ) {
									p = GeometryUtils.getRandomPointsInsideFeature(feature);
									Coord fromZoneCoord = new Coord(p.getX(), p.getY());
									homeZoneCoordTransform = PatnaUtils.COORDINATE_TRANSFORMATION.transform(fromZoneCoord);
								}
								else if (toZoneId.equals(zoneId)){
									q = GeometryUtils.getRandomPointsInsideFeature(feature);
									Coord toZoneCoord = new Coord(q.getX(), q.getY());
									workZoneCoordTransform= PatnaUtils.COORDINATE_TRANSFORMATION.transform(toZoneCoord);
								}
							}
						}  

						Person person = factory.createPerson(Id.createPersonId(idPrefix+population.getPersons().size()));
						population.addPerson(person);

						//					double beelineDist = CoordUtils.calcEuclideanDistance(homeZoneCoordTransform, workZoneCoordTransform);
						String travelMode = strs.get(labels.indexOf(PatnaDemandLabels.mode.toString())); //getTravelMode(parts [8], beelineDist);

						Plan plan = createPlan( workZoneCoordTransform, homeZoneCoordTransform, travelMode, tripPurpose);
						person.addPlan(plan);

						// attributes
						population.getPersonAttributes().putAttribute(person.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE, monthlyIncome);
						population.getPersonAttributes().putAttribute(person.getId().toString(), PatnaUtils.TRANSPORT_COST_ATTRIBUTE, dailyTransportCost);
					}
				}
				line = bufferedReader.readLine();
				iterator = features.iterator();
			}
		} catch (IOException e) {
			throw new RuntimeException("File is not read. Reason : "+e);
		}
	}

	private String getCorrectZoneNumber(final String zoneId) {
		// for trips terminating in zone 73,74,75 and 76 are replaced by zone 6,1,3 and 3 respectively
		String outZoneId = "NULL" ;
		switch(zoneId) {
		case "73" : 
			outZoneId = "6"; break;
		case "74" : 
			outZoneId = "1"; break;
		case "75" : 
		case "76" : 
			outZoneId = "3"; break;
		default : outZoneId = zoneId; break;
		}
		return outZoneId;
	}

	private Plan createPlan (final Coord toZoneFeatureCoord, final Coord fromZoneFeatureCoord, final String mode, final String tripPurpose) {
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();
		Plan plan = populationFactory.createPlan();

		double homeActEndTime=0.; 
		double secondActEndTimeLeaveTime =0.;
		String secondActType = null;

		switch (tripPurpose) {

		case "work": { //case "1" : {//work act starts between 8 to 9:30 and duration is 8 hours
			homeActEndTime = 8.0*3600. + RAND.nextInt(91)*60.; 
			secondActEndTimeLeaveTime = homeActEndTime + 8*3600.; 
			secondActType = PatnaUrbanActivityTypes.work.toString();
			break; 
		}  
		case "educational" : {//case "2" : { // educational act starts between between 6:30 to 8:30 hours and duration is assumed about 7 hours
			homeActEndTime = 6.5*3600. + RAND.nextInt(121)*60.; 
			secondActEndTimeLeaveTime = homeActEndTime + 7*3600.;
			secondActType = PatnaUrbanActivityTypes.educational.toString();
			break;
		}  
		case "social" : {//case "3" : {// social duration between 5 to 7 hours
			homeActEndTime= 10.*3600. ; 
			secondActEndTimeLeaveTime = homeActEndTime+ 5.*3600. + RAND.nextInt(121)*60.; 
			secondActType = PatnaUrbanActivityTypes.social.toString();
			break;
		}  
		case "other" : { //case "4" : { // other act duration between 5 to 7 hours
			homeActEndTime = 8.*3600 ; 
			secondActEndTimeLeaveTime= homeActEndTime + 5.*3600. + RAND.nextInt(121)*60.; 
			secondActType = PatnaUrbanActivityTypes.other.toString();
			break;
		} 
		case "unknown" : { //case "9999" : { // no data
			homeActEndTime = 8.*3600. + RAND.nextInt(121)*60.; 
			secondActEndTimeLeaveTime= homeActEndTime + 7*3600.; 
			secondActType = PatnaUrbanActivityTypes.unknown.toString();
			break;
		} 
		default : throw new RuntimeException("Trip purpose input code "+tripPurpose+" is not recognized. Aborting ...");
		}

		Activity homeAct = populationFactory.createActivityFromCoord(PatnaUrbanActivityTypes.valueOf("home").toString(), fromZoneFeatureCoord);
		homeAct.setEndTime(homeActEndTime); 								
		plan.addActivity(homeAct);
		plan.addLeg(populationFactory.createLeg(mode));

		Activity secondAct = populationFactory.createActivityFromCoord(secondActType, toZoneFeatureCoord);
		secondAct.setEndTime(secondActEndTimeLeaveTime); 
		plan.addActivity(secondAct);
		plan.addLeg(populationFactory.createLeg(mode));

		Activity homeActII = populationFactory.createActivityFromCoord("home", homeAct.getCoord());	
		plan.addActivity(homeActII);
		return plan;
	}
}