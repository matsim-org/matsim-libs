/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityLocationAdjuster.java                                                                        *
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
/**
 * 
 */
package playground.southafrica.projects.erAfrica;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.Household;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * This class reads in the population with its allocated activity chains, and 
 * adjusts each activity's location by considering the leg's distance, and the
 * location of actual parsed activity facilities (OpenStreetMap, Cape Town 
 * landuse, or Cape Town's informal settlements).
 *  
 * @author jwjoubert
 */
public class ERAfricaActivityLocationAdjuster {
	final private static Logger LOG = Logger.getLogger(ERAfricaActivityLocationAdjuster.class);
	private Scenario sc;
	private QuadTree<ActivityFacility> qtHomeFormal;
	private QuadTree<ActivityFacility> qtHomeInformal;
	private QuadTree<ActivityFacility> qtEducation;
	private QuadTree<ActivityFacility> qtWork;
	private QuadTree<ActivityFacility> qtShopping;
	private QuadTree<ActivityFacility> qtLeisure;
	private QuadTree<ActivityFacility> qtMedical;
	private QuadTree<ActivityFacility> qtOther;
	
	private static List<Tuple<Double, Double>> tuplesWork = new ArrayList<>();
	private static List<Tuple<Double, Double>> tuplesEducation = new ArrayList<>();
	private static List<Tuple<Double, Double>> tuplesSecondary = new ArrayList<>();
	
	private static List<Integer> triesSingle = new ArrayList<>();
	private static List<Integer> triesRing = new ArrayList<>();
	private static List<Integer> triesEllipse = new ArrayList<>();
 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ERAfricaActivityLocationAdjuster.class.toString(), args);

		String treasuryPopulation = args[0];
		String populationFolder = args[1];
		String osmFacilitiesFile = args[2];
		String ctFacilitiesFile = args[3];
		String ctInformalHousing = args[4];
		String finalPopulationFolder = args[5];
		String sample = args[6];
		
		setup(treasuryPopulation, populationFolder, sample);

		ComprehensivePopulationReader cpr = new ComprehensivePopulationReader();
		cpr.parse(populationFolder);
		Scenario sc = cpr.getScenario();

		ERAfricaActivityLocationAdjuster ala = new ERAfricaActivityLocationAdjuster(sc);
		ala.buildQuadTreeFromFacilities(osmFacilitiesFile, ctFacilitiesFile, ctInformalHousing);
		ala.processHouseholds();
		
		writeTuples(finalPopulationFolder);
		writeTries(finalPopulationFolder);
		writeUpdatedScenario(sc, finalPopulationFolder);

		Header.printFooter();
	}
	
	private static void writeTuples(String folder){
		BufferedWriter bw = IOUtils.getBufferedWriter(folder + "tuples.csv");
		try{
			bw.write("type,old,new");
			bw.newLine();
			
			for(Tuple<Double, Double> t : tuplesWork){
				bw.write(String.format("w,%.0f,%.0f\n", t.getFirst(), t.getSecond()));
			}
			for(Tuple<Double, Double> t : tuplesEducation){
				bw.write(String.format("e,%.0f,%.0f\n", t.getFirst(), t.getSecond()));
			}
			for(Tuple<Double, Double> t : tuplesSecondary){
				bw.write(String.format("secondary,%.0f,%.0f\n", t.getFirst(), t.getSecond()));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write tuples to file.");
		}finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close tuples file.");
			}
		}
	}
	
	
	private static void writeTries(String folder){
		BufferedWriter bw = IOUtils.getBufferedWriter(folder + "tries.csv");
		try{
			bw.write("type,tries");
			bw.newLine();
			
			for(Integer i : triesSingle){
				bw.write(String.format("single,%d\n", i));
			}
			for(Integer i : triesRing){
				bw.write(String.format("ring,%d\n", i));
			}
			for(Integer i : triesEllipse){
				bw.write(String.format("ellipse,%d\n", i));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write tries to file.");
		}finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close tries file.");
			}
		}
	}
	
	
	private static void writeUpdatedScenario(Scenario sc, String folder){
		folder += folder.endsWith("/") ? "" : "/";
		
		/* Updated household coordinates. */
		ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(sc.getHouseholds().getHouseholdAttributes());
		oaw.putAttributeConverter(Coord.class, new CoordConverter());
		oaw.writeFile(folder + "householdAttributes.xml.gz");
		
		/* Updated activity locations of population. */
		new PopulationWriter(sc.getPopulation()).writeV5(folder + "persons.xml.gz");
		
		/* Write all the (used) facilities. */
		ActivityFacilities afs = FacilitiesUtils.createActivityFacilities();
		for(Person person : sc.getPopulation().getPersons().values()){
			for(Plan plan : person.getPlans()){
				for(PlanElement pe : plan.getPlanElements()){
					if(pe instanceof Activity){
						Activity activity = (Activity)pe;
						
						Id<ActivityFacility> fid = activity.getFacilityId();
						if(!afs.getFacilities().containsKey(fid)){
							afs.addActivityFacility(sc.getActivityFacilities().getFacilities().get(fid)); 
						}
					}
				}
			}
		}
		LOG.info("Total number of facilities used: " + afs.getFacilities().size());
		new FacilitiesWriter(afs).write(folder + "facilities_used.xml.gz");
		
		/* Write all the facilities. */
		LOG.info("Total number of facilities: " + sc.getActivityFacilities().getFacilities().size());
		new FacilitiesWriter(sc.getActivityFacilities()).write(folder + "facilities_all.xml.gz");
	}
	
	
	public static void setup(String treasurePopulation, String populationFolderName, String sample){
		LOG.info("Setting up the population files...");

		/* Delete and create the temporary folder for population files. */
		treasurePopulation += treasurePopulation.endsWith("/") ? "" : "/";
		File populationFolder = new File(populationFolderName);
		if(populationFolder.exists()){
			LOG.warn("Population folder will be deleted and overwritten.");
			LOG.warn("Deleting " + populationFolder.getAbsolutePath());
			FileUtils.delete(populationFolder);
		}
		populationFolder.mkdirs();
		
		String folder = null;
		switch (sample) {
		case "001":
			folder = treasurePopulation + "sample/001/CapeTown/";
			break;
		case "010":
			folder = treasurePopulation + "sample/010/CapeTown/";
			break;
		case "100":
			folder = treasurePopulation + "full/CapeTown/";
			break;
		default:
			LOG.error("Don't know how to interpret sample " + sample);
			break;
		}
		
		/* Copying the files from the original treasure data set. */
		try {
			FileUtils.copyFile(
					new File(folder + "population_withPlans.xml.gz"),
					new File(populationFolderName + "population.xml.gz"));
			FileUtils.copyFile(
					new File(folder + "populationAttributes.xml.gz"),
					new File(populationFolderName + "populationAttributes.xml.gz"));
			FileUtils.copyFile(
					new File(folder + "households.xml.gz"),
					new File(populationFolderName + "households.xml.gz"));
			FileUtils.copyFile(
					new File(folder + "householdAttributes_withPlanHome.xml.gz"),
					new File(populationFolderName + "householdAttributes.xml.gz"));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot copy input files for setup.");
		}
		
		LOG.info("Done setting up.");
	}

	public ERAfricaActivityLocationAdjuster(Scenario sc) {
		this.sc = sc;
	}


	public void buildQuadTreeFromFacilities(String osmFile, String ctLanduseFile, String ctInformalFile){
		LOG.info("Building the necessary QuadTree objects...");
//		Scenario thisSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		/* Determine the extent of the QuadTree. */
		double xMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;

		new FacilitiesReaderMatsimV1(sc).readFile(osmFile);
		new FacilitiesReaderMatsimV1(sc).readFile(ctLanduseFile);
		new FacilitiesReaderMatsimV1(sc).readFile(ctInformalFile);
		for(ActivityFacility facility : sc.getActivityFacilities().getFacilities().values()){
			Coord c = facility.getCoord();
			xMin = Math.min(xMin, c.getX());
			xMax = Math.max(xMax, c.getX());
			yMin = Math.min(yMin, c.getY());
			yMax = Math.max(yMax, c.getY());
		}
		
		/* Housing. */
		LOG.info("  ... home...");
		qtHomeFormal = new QuadTree<>(xMin, yMin, xMax, yMax);
		qtHomeInformal = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : sc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.HOME).keySet()){
			ActivityFacility facility = sc.getActivityFacilities().getFacilities().get(fId);
			if(fId.toString().startsWith("gdb")){
				qtHomeFormal.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			} else if(fId.toString().startsWith("inf")){
				qtHomeInformal.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			}
		}

		/* Education. */
		LOG.info("  ... education...");
		qtEducation = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : sc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.EDUCATION).keySet()){
			ActivityFacility facility = sc.getActivityFacilities().getFacilities().get(fId);
			qtEducation.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		
		/* Work. */
		LOG.info("  ... work...");
		qtWork = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : sc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.WORK).keySet()){
			ActivityFacility facility = sc.getActivityFacilities().getFacilities().get(fId);
			qtWork.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		
		/* Shopping. */
		LOG.info("  ... shopping...");
		qtShopping = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : sc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.SHOPPING).keySet()){
			ActivityFacility facility = sc.getActivityFacilities().getFacilities().get(fId);
			qtShopping.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		
		/* Leisure. */
		LOG.info("  ... leisure...");
		qtLeisure = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : sc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.LEISURE).keySet()){
			ActivityFacility facility = sc.getActivityFacilities().getFacilities().get(fId);
			qtLeisure.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		
		/* Medical. */
		LOG.info("  ... medical...");
		qtMedical = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : sc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.MEDICAL).keySet()){
			ActivityFacility facility = sc.getActivityFacilities().getFacilities().get(fId);
			qtMedical.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		for(Id<ActivityFacility> fId : sc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.HOSPITAL).keySet()){
			ActivityFacility facility = sc.getActivityFacilities().getFacilities().get(fId);
			qtMedical.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		
		/* Other. */
		LOG.info("  ... other...");
		qtOther = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : sc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.OTHER).keySet()){
			ActivityFacility facility = sc.getActivityFacilities().getFacilities().get(fId);
			qtOther.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		LOG.info("Done building QuadTree objects.");
	}



	public void processHouseholds(){
		LOG.info("Handle each member of each household...");
		
		/*FIXME Households' home coordinate were initially hard coded using
		 * WGS84_SA_ALbers as CRS. The SurveyPlanPicker then converts it to 
		 * EPSG:3857. The facilities (from land use and OSM) are all parsed 
		 * and converted to SA_Lo19. */
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:3857", "SA_Lo19");
		
		Counter counter = new Counter("  households # ");
		for(Household hh : sc.getHouseholds().getHouseholds().values()){
			
			/* Find an appropriate home for the household. */
			Coord censusCoord = ct.transform((Coord) sc.getHouseholds().getHouseholdAttributes().getAttribute(hh.getId().toString(), "homeCoord"));
			String mainDwelling = sc.getHouseholds().getHouseholdAttributes().getAttribute(hh.getId().toString(), "mainDwellingType").toString();
			ActivityFacility homeFacility = null;
			if(mainDwelling.equalsIgnoreCase("Informal")){
				homeFacility = sampleSingleActivityFacility(qtHomeInformal, censusCoord, "homeFormal");
				
				/* Informal housing units are removed once selected. */
//				qtHomeInformal.remove(homeFacility.getCoord().getX(), homeFacility.getCoord().getY(), homeFacility);
			} else{
				homeFacility = sampleSingleActivityFacility(qtHomeFormal, censusCoord, "homeInformal");
				
				/* Remove single houses with capacity of 1. */
				if(homeFacility.getActivityOptions().get(FacilityTypes.HOME).getCapacity() == 1.0){
					qtHomeFormal.remove(homeFacility.getCoord().getX(), homeFacility.getCoord().getY(), homeFacility);
				}
			}
			
			/* Handle each member of the household. */
			for(Id<Person> pId : hh.getMemberIds()){
				Plan plan = this.sc.getPopulation().getPersons().get(pId).getSelectedPlan();
				relocatePlanActivities(plan, homeFacility);
			}
			
			/* Finally, update the household's homeCoord attribute. */
			sc.getHouseholds().getHouseholdAttributes().putAttribute(hh.getId().toString(), "homeCoord", homeFacility.getCoord());
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done handling households.");
	}
	
	public ActivityFacility sampleSingleActivityFacility(QuadTree<ActivityFacility> qt, Coord c, String type){
		ActivityFacility facility = null;
		double radius = 1000.0;
		Collection<ActivityFacility> col = qt.getDisk(c.getX(), c.getY(), radius);
		int tries = 1;
		while(col.size() == 0 && tries <= 100){
//		while(col.size() == 0){
			radius *= 2.0;
			col = qt.getDisk(c.getX(), c.getY(), radius);
			tries++;
		}
		List<ActivityFacility> list = new ArrayList<>(col);
		
		if(list.size() > 0){
			facility = list.get(MatsimRandom.getLocalInstance().nextInt(list.size()));
		} else{
			LOG.error("Could not sample a facility for activity type '" + type + "'.");
			LOG.error("Facilities in QuadTree: " + qt.size());
			LOG.warn("Getting the closest facility.");
			facility = qt.getClosest(c.getX(), c.getY());
		}
		triesSingle.add(tries);
		return facility;
	}
	
	
	public ActivityFacility sampleActivityFacilityInRing(
			QuadTree<ActivityFacility> qt, Coord c, double radius, String type){
		ActivityFacility facility = null;
		double factor = 0.1;
		double lower = radius/(1.0 + factor);
		double upper = radius*(1.0 + factor);
		Collection<ActivityFacility> col = qt.getRing(c.getX(), c.getY(), lower, upper);
		int tries = 1;
		while(col.size() == 0 && tries <= 100){
//		while(col.size() == 0){
			factor *= 2.0;
			lower = radius/(1.0 + factor);
			upper = radius*(1.0 + factor);
			col = qt.getRing(c.getX(), c.getY(), lower, upper);
			tries++;
		}
		
		List<ActivityFacility> list = new ArrayList<>(col);
		
		if(list.size() > 0){
			facility = list.get(MatsimRandom.getLocalInstance().nextInt(list.size()));
		} else{
			LOG.error("Could not sample a facility for activity type '" + type + "'.");
			LOG.error("Facilities in QuadTree: " + qt.size());
			LOG.warn("Getting the closest facility.");
			facility = qt.getClosest(c.getX(), c.getY());
		}
		triesRing.add(tries);
		return facility;
	}
	
	public ActivityFacility sampleActivityFacilityBetweenPrimaries(
			QuadTree<ActivityFacility> qt, Coord c1, Coord c2, double dist, String type){
		ActivityFacility facility = null;
		double primaryDistance = CoordUtils.calcEuclideanDistance(c1, c2);
		
		if(primaryDistance == 0.0){
			/* It is an activity from a single point. Sample it from a ring
			 * instead. */
			facility = sampleActivityFacilityInRing(qt, c1, dist, type);
			return facility;
		}
		
		primaryDistance = Math.max(primaryDistance, dist);

		double factor = 0.1;
		double distance = primaryDistance * (1.0 + factor);
		Collection<ActivityFacility> col = qt.getElliptical(c1.getX(), c1.getY(), c2.getX(), c2.getY(), distance);
		int tries = 1;
		while(col.size() == 0 && tries <= 100){
//		while(col.size() == 0){
			factor *= 2.0;
			distance = primaryDistance * (1.0 + factor);
			col = qt.getElliptical(c1.getX(), c1.getY(), c2.getX(), c2.getY(), distance);
			tries++;
		}
		
		List<ActivityFacility> list = new ArrayList<>(col);
		
		if(list.size() > 0){
			facility = list.get(MatsimRandom.getLocalInstance().nextInt(list.size()));
		} else{
			LOG.error("Could not sample a facility for activity type '" + type + "'.");
			LOG.error("Facilities in QuadTree: " + qt.size());
			LOG.warn("Getting the closest facility to c1.");
			facility = qt.getClosest(c1.getX(), c1.getY());
		}
		triesEllipse.add(tries);
		return facility;
	}
	
	
	public void relocatePlanActivities(Plan plan, ActivityFacility home){
		List<PlanElement> elements = plan.getPlanElements();
		
		/* Fix the home location(s). */
		Coord oldHome = null;
		Iterator<PlanElement> peIterator = elements.iterator();
		while(peIterator.hasNext()){
			PlanElement pe = peIterator.next();
			if(pe instanceof Activity){
				Activity activity = (Activity)pe;
				if(activity.getType().startsWith("h")){
					if(oldHome == null){
						oldHome = activity.getCoord();
					}
					activity.setCoord(home.getCoord());
					activity.setFacilityId(home.getId());
				}
			}
		}

		/* Fix the work location(s). */
		List<Activity> workList = getActivityType(plan, "w");
		if(workList.size() > 0){
			for(Activity activity : workList){
				double distanceFromHome = CoordUtils.calcEuclideanDistance(
						oldHome, 
						activity.getCoord());
				ActivityFacility af = sampleActivityFacilityInRing(qtWork, home.getCoord(), distanceFromHome, "w");
				activity.setCoord(af.getCoord());
				activity.setFacilityId(af.getId());
				double newDistanceFromHome = CoordUtils.calcEuclideanDistance(home.getCoord(), af.getCoord());
				Tuple<Double, Double> tuple = new Tuple<Double, Double>(distanceFromHome, newDistanceFromHome);
				tuplesWork.add(tuple);
//				LOG.info(String.format("(old: %.0f; new: %.0f)", distanceFromHome, newDistanceFromHome));
			}
		}
		
		/* Fix the education location(s). */
		List<Activity> educationList = getActivityType(plan, "e");
		if(educationList.size() > 0){
			for(Activity activity : educationList){
				double distanceFromHome = CoordUtils.calcEuclideanDistance(
						oldHome, 
						activity.getCoord());
				ActivityFacility af = sampleActivityFacilityInRing(qtEducation, home.getCoord(), distanceFromHome, "e");
				activity.setCoord(af.getCoord());
				activity.setFacilityId(af.getId());
				double newDistanceFromHome = CoordUtils.calcEuclideanDistance(home.getCoord(), af.getCoord());
				Tuple<Double, Double> tuple = new Tuple<Double, Double>(distanceFromHome, newDistanceFromHome);
				tuplesEducation.add(tuple);
//				LOG.info(String.format("(old: %.0f; new: %.0f)", distanceFromHome, newDistanceFromHome));
			}
		}
		
		/* Fix the secondary activities. */
		Activity firstPrimary = null;
		Activity secondPrimary = null;
		List<Activity> secondaries = new ArrayList<>();
		Iterator<PlanElement> iterator = elements.iterator();
		while(iterator.hasNext()){
			PlanElement pe = iterator.next();
			if(pe instanceof Activity){
				Activity act = (Activity)pe;
				boolean isPrimary = isPrimary(act);
				if(isPrimary){
					if(firstPrimary == null){
						firstPrimary = act;
					} else if(secondPrimary == null){
						/*TODO Process the facilities in between. */
						secondPrimary = act;
						if(secondaries.size() > 0){
							for(Activity a : secondaries){
								QuadTree<ActivityFacility> qt = getQuadtreeFromActivityType(a.getType());
								double distanceFromHome = CoordUtils.calcEuclideanDistance(
										oldHome, 
										a.getCoord());
								ActivityFacility af = sampleActivityFacilityBetweenPrimaries(
										qt, firstPrimary.getCoord(), secondPrimary.getCoord(), distanceFromHome, a.getType());
								a.setCoord(af.getCoord());
								a.setFacilityId(af.getId());
								double newDistanceFromHome = CoordUtils.calcEuclideanDistance(home.getCoord(), af.getCoord());
								Tuple<Double, Double> tuple = new Tuple<Double, Double>(distanceFromHome, newDistanceFromHome);
								tuplesSecondary.add(tuple);
							}
							secondaries = new ArrayList<>();
						} else{
							/* Ignore it - there are no secondaries. */
						}
						firstPrimary = secondPrimary;
						secondPrimary = null;
					} else{
						LOG.error("Can this happen?!");
					}
				} else{
					if(firstPrimary == null){
						/*TODO This is a loose-standing activity, just sample
						 * any activity location within the given radius. */
						QuadTree<ActivityFacility> qt = getQuadtreeFromActivityType(act.getType());
						double distanceFromHome = CoordUtils.calcEuclideanDistance(
								oldHome, 
								act.getCoord());
						ActivityFacility af = sampleActivityFacilityInRing(qt, home.getCoord(), distanceFromHome, act.getType());
						act.setCoord(af.getCoord());
						act.setFacilityId(af.getId());
						double newDistanceFromHome = CoordUtils.calcEuclideanDistance(home.getCoord(), af.getCoord());
						Tuple<Double, Double> tuple = new Tuple<Double, Double>(distanceFromHome, newDistanceFromHome);
						tuplesSecondary.add(tuple);
					} else{
						secondaries.add(act);
					}
				}
			}
		}
		
		/* Handle the remaining secondary activities. */
		for(Activity act : secondaries){
			QuadTree<ActivityFacility> qt = getQuadtreeFromActivityType(act.getType());
			double distanceFromHome = CoordUtils.calcEuclideanDistance(
					oldHome, 
					act.getCoord());
			ActivityFacility af = sampleActivityFacilityInRing(qt, firstPrimary.getCoord(), distanceFromHome, act.getType());
			act.setCoord(af.getCoord());
			act.setFacilityId(af.getId());
			double newDistanceFromPrimary = CoordUtils.calcEuclideanDistance(firstPrimary.getCoord(), af.getCoord());
			Tuple<Double, Double> tuple = new Tuple<Double, Double>(distanceFromHome, newDistanceFromPrimary);
			tuplesSecondary.add(tuple);
		}
	}
	
	
	private boolean isPrimary(Activity activity){
		boolean isPrimary = false;
		String type = activity.getType().substring(0, 1);
		if(type.equalsIgnoreCase("h") ||
		   type.equalsIgnoreCase("e") ||
		   type.equalsIgnoreCase("w")){
			isPrimary = true;
		}
		return isPrimary;
	}
	
	
	private List<Activity> getActivityType(Plan plan, String typePrefix){
		List<Activity> list = new ArrayList<>();
		Iterator<PlanElement> iterator = plan.getPlanElements().iterator();
		while(iterator.hasNext()){
			PlanElement pe = iterator.next();
			if(pe instanceof Activity){
				Activity activity = (Activity)pe;
				if(activity.getType().startsWith(typePrefix)){
					list.add(activity);
				}
			}
		}

		return list;
	}
	
	
	private boolean hasActivityType(Plan plan, String typePrefix){
		boolean hasActivity = false;
		
		Iterator<PlanElement> iterator = plan.getPlanElements().iterator();
		while(!hasActivity && iterator.hasNext()){
			PlanElement pe = iterator.next();
			if(pe instanceof Activity){
				Activity activity = (Activity)pe;
				if(activity.getType().startsWith(typePrefix)){
					hasActivity = true;
				}
			}
		}
		
		return hasActivity;
	}
	
	
	private QuadTree<ActivityFacility> getQuadtreeFromActivityType(String type){
		QuadTree<ActivityFacility> qt = null;
		String prefix = type.substring(0,1);
		switch (prefix) {
		case "e":
			qt = qtEducation;
			break;
		case "w":
			qt = qtWork;
			break;
		case "s":
			qt = qtShopping;
			break;
		case "l":
			qt = qtLeisure;
			break;
		case "m":
			qt = qtMedical;
			break;
		case "o":
			qt = qtOther;
			break;
		case "v":
			qt = qtHomeFormal;
			break;
		default:
			break;
		}
		
		if(qt == null){
			LOG.error("Could not find an appropriate QuadTree for activity type ''" + type + "''");
		}
		return qt;
	}
	

}
