/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertFacilityCoordinates.java
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

/**
 * 
 */
package playground.jjoubert.projects.capeTownFreight.moveFacilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;

import com.vividsolutions.jts.geom.Geometry;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.analysis.VktEstimator;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

/**
 * Small class with some utilities to build the business cases showing how
 * MATSim can be used when commercial facilities are relocated.
 * 
 * @author jwjoubert
 */
public class CTUtilities {
	final private static Logger LOG = Logger.getLogger(CTUtilities.class);
	final private static String[] affectedFacilities = {
			"2552","2467","2598","2510","2619","2498","2448","2591","2487","2544", // Harbour container yard.
			"2559", "2452", "2561" // Grindrod container terminal
	};

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CTUtilities.class.toString(), args);
		
		int option = Integer.parseInt(args[0]);
		switch (option) {
		case 1:
			ConvertFacilityCoordinates(args);
			break;
		case 2:
			ConvertPlanCoordinates();
			break;
		case 3:
			CheckNumberOfAffectedChains();
			break;
		case 4:
			checkEndTimeDuration();
			break;
		case 5:
			generateFacilitiesFile();
			break;
		case 6:
			calculateAffectedVehicleVkt(args);
			break;
		default:
			break;
		}
		Header.printFooter();
	}

	
	/**
	 * Hard-coded method to convert a facilities file from the standard 
	 * SA_Albers CRS to the Via Webmap-friendly EPSG:3857. However, the 
	 * facilities are to be parsed from the plans, and not a facilities file
	 * explicitly.
	 */
	public static void ConvertFacilityCoordinates(String[] args){
		LOG.info("Converting facility coordinates to EPSG:3857");
		String input = "/Volumes/Nifty/workspace/coct-data/matsim/20150930/output/ITERS/it.0/0.plans.xml.gz";
		String inputCRS = "WGS84_SA_Albers";
		
		String output = "/Volumes/Nifty/workspace/coct-data/matsim/businessCases/facilityMove/facilities.csv";
		String outputCRS = "EPSG:3857";
		
		Map<Id<ActivityFacility>, Coord> map = new TreeMap<Id<ActivityFacility>, Coord>();
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(input);
		for(Id<Person> pId : sc.getPopulation().getPersons().keySet()){
			Plan plan = sc.getPopulation().getPersons().get(pId).getSelectedPlan();
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					Id<ActivityFacility> fId = act.getFacilityId();
					if(fId != null && !map.containsKey(fId)){
						map.put(fId, act.getCoord());
					}
				}
			}
		}
		LOG.info("Found a total of " + map.size() + " unique facility IDs.");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			bw.write("facilityId,lon,lat");
			bw.newLine();
			for(Id<ActivityFacility> fId : map.keySet()){
				Coord c = map.get(fId);
				Coord cc = ct.transform(c);
				bw.write(String.format("%s,%.2f,%.2f\n", fId.toString(), cc.getX(), cc.getY()));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		
		LOG.info("Done converting facility coordinates.");
	}
	
	
	/**
	 * Converts all activity locations of a plan to EPSG:3857. The resulting
	 * plans are written to a new file, with the legs, but without the routes.
	 */
	public static void ConvertPlanCoordinates(){
		LOG.info("Converting plans to EPSG:3857");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse("/Volumes/Nifty/workspace/coct-data/matsim/20150930/output/ITERS/it.0/0.plans.xml.gz");
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "EPSG:3857");
		
		Scenario newSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory pf = sc.getPopulation().getFactory();
		for(Person person : sc.getPopulation().getPersons().values()){
			Person newPerson = pf.createPerson(person.getId());
			Plan newPlan = pf.createPlan();
			
			Plan plan = person.getSelectedPlan();
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					Activity newAct = pf.createActivityFromCoord(act.getType(), ct.transform(act.getCoord()));
					if(act.getFacilityId() != null){
						newAct.setFacilityId(act.getFacilityId());
					}
					newPlan.addActivity(newAct);
				} else if (pe instanceof Leg){
					Leg leg = (Leg)pe;
					Leg newLeg = pf.createLeg(leg.getMode());
					newPlan.addLeg(newLeg);
				} else{
					throw new RuntimeException("What PlanElement is this: " + pe.getClass().toString());
				}
			}
			newPerson.addPlan(newPlan);
			newSc.getPopulation().addPerson(newPerson);
		}
		
		new PopulationWriter(newSc.getPopulation()).write("/Volumes/Nifty/workspace/coct-data/matsim/businessCases/facilityMove/plans.xml.gz");
	}
	
	
	/**
	 * Checks a hard-coded plans file to count the number of affected 
	 * facilities.
	 */
	public static void CheckNumberOfAffectedChains(){
		LOG.info("Checking the number of affected facilities.");
		Map<Integer, Integer> countMap = new TreeMap<>();
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse("/Volumes/Nifty/workspace/coct-data/matsim/20150930/output/ITERS/it.0/0.plans.xml.gz");

		for(Person person : sc.getPopulation().getPersons().values()){
			int affected = countNumberOfAffectedFacilities(person.getSelectedPlan());
			if(affected > 4){
				LOG.info("  Id " + person.getId().toString() + " has " + affected + " affected activities.");
			}
			if(countMap.containsKey(affected)){
				int oldValue = countMap.get(affected);
				countMap.put(affected, oldValue+1);
			} else{
				countMap.put(affected, 1);
			}
		}
		
		LOG.info("Affected facilities:");
		for(Integer i : countMap.keySet()){
			LOG.info(String.format("%3s: %d", i.toString(), countMap.get(i)));
		}
		
		LOG.info("Done checking affected facilities.");
	}
	
	
	public static int countNumberOfAffectedFacilities(Plan plan){
		List<String> facilityList = Arrays.asList(affectedFacilities);
		int num = 0;
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof Activity){
				Activity act = (Activity)pe;
				Id<ActivityFacility> fId = act.getFacilityId();
				if(fId != null && facilityList.contains(fId.toString())){
					num++;
				}
			}
		}
		return num;
	}
	
	public static List<String> getAffectedFacilities(){
		return Arrays.asList(affectedFacilities);
	}
	
	/**
	 * Just a check method to ensure each person has activities with specified
	 * times. The first activity in the plan must have an end time, and the
	 * remainder (except the last) must have a maximum duration. The last 
	 * activity need not have any times set.
	 */
	public static void checkEndTimeDuration(){
		LOG.info("Check that all activities (except last) has either end time or duration.");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse("/Volumes/Nifty/workspace/coct-data/matsim/businessCases/facilityMove/relocatedPlans_Belcon.xml.gz");
		
		for(Person person : sc.getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			for(int i = 0; i < plan.getPlanElements().size()-1; i++){
				PlanElement pe = plan.getPlanElements().get(i);
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					double end = act.getEndTime();
					double duration = act.getMaximumDuration();
					if(end == Time.UNDEFINED_TIME && duration == Time.UNDEFINED_TIME){
						LOG.error("Person " + person.getId().toString() + " has both end time and duration undefined.");
					}
				}
			}
		}
		
		LOG.info("Done checking end time and duration.");
	}
	
	
	/**
	 * Method to read a population file and generate the facilities based on
	 * the (possible) presence of a facility Id. 
	 */
	public static void generateFacilitiesFile(){
		LOG.info("Generating facilities file...");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse("/Volumes/Nifty/workspace/coct-data/matsim/businessCases/facilityMove/relocatedPlans_Belcon.xml.gz");
		new MatsimNetworkReader(sc.getNetwork()).parse("/Volumes/Nifty/workspace/coct-data/matsim/20150930/network.xml.gz");
		
		ActivityFacilities facilities = sc.getActivityFacilities();
		ActivityFacilitiesFactory aff = facilities.getFactory();

		
		for(Person person : sc.getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			for(int i = 0; i < plan.getPlanElements().size()-1; i++){
				PlanElement pe = plan.getPlanElements().get(i);
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					Id<ActivityFacility> fId = act.getFacilityId();
					if(!facilities.getFacilities().containsKey(fId)){
						ActivityFacility facility = aff.createActivityFacility(fId, act.getCoord());
						
						/* Create the facility options. */
						ActivityOption majorOption = aff.createActivityOption("major");
						ActivityOption minorOption = aff.createActivityOption("minor");
						facility.addActivityOption(minorOption);
						facility.addActivityOption(majorOption);

						facilities.addActivityFacility(facility);
					}
				}
			}
		}
		LOG.info("Done generating facilities file.");
		LOG.info("Total number of facilities found: " + facilities.getFacilities().size());
		
		new FacilitiesWriter(facilities).write("/Volumes/Nifty/workspace/coct-data/matsim/businessCases/facilityMove/belconFacilities.xml.gz");
	}
	
	
	/**
	 * Class to read in three populations:
	 * <ol>
	 * 		<li> original population;
	 * 		<li> population where facilities were relocated to Belcon; and
	 * 		<li> population where facilities were relocated to Kraaicon
	 * </ol>
	 * as well as the list of affected vehicles. The method then calculates 
	 * the estimated vehicle kilometres travelled (VKT) and writes the 
	 * comparative values to a single file. 
	 * @throws IOException 
	 */
	public static void calculateAffectedVehicleVkt(String[] args){
		LOG.info("Calculating comparative VKT for affected vehicles.");
		
		String affectedFile = args[1];
		String networkFile = args[2];
		String shapefile = args[3];
		int idField = Integer.parseInt(args[4]);
		String basePopulation = args[5];
		String belconPopulation = args[6];
		String kraaiconPopulation = args[7];
		String outputFile = args[8];
		int numberOfThreads = Integer.parseInt(args[9]);
		
		Map<Id<Person>, Double[]> map = new TreeMap<>();
		BufferedReader br = IOUtils.getBufferedReader(affectedFile);
		try{
			String line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				Id<Person> id = Id.createPersonId(line);
				Double[] da = {0.0, 0.0, 0.0};
				map.put(id, da);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + affectedFile);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + affectedFile);
			}
		}
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).parse(networkFile);
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		try {
			mfr.readMultizoneShapefile(shapefile, idField);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot parse Cape Town shapefile.");
		}
		LOG.info("Cape Town shapefile has " + mfr.getAllZones().size() + " feature(s).");
		Geometry geom = mfr.getAllZones().get(0);

		LOG.info("Ready to process different scenarios (" + map.size() + " affected)...");
		Counter counter = new Counter("  affected # ");
		
		/*================= Base =================*/
		LOG.info("Reading base case...");
		Scenario scBase = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scBase).parse(basePopulation);
		LOG.info("Processing base case...");
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		Map<Id<Person>, Future<Double>> mapOfJobs = new TreeMap<>();
		for(Person person : scBase.getPopulation().getPersons().values()){
			if(map.containsKey(person.getId())){
				Plan plan = person.getSelectedPlan();
				Callable<Double> job = new VktEstimatorCallable(sc.getNetwork(), plan, geom, counter);
				Future<Double> submit = threadExecutor.submit(job);
				mapOfJobs.put(person.getId(), submit);
			}
		}
		scBase = null;
		LOG.info("Waiting for parallel handling of base case...");
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();
		LOG.info("Populating base case results...");
		for(Id<Person> id : mapOfJobs.keySet()){
			try {
				double d = mapOfJobs.get(id).get();
				Double[] da = map.get(id);
				da[0] = d;
				map.put(id, da);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not get callable results for person " + id.toString());
			}
		}
		
		
		/*================= Belcon =================*/
		LOG.info("Reading Belcon case...");
		counter.reset();
		Scenario scBelcon = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scBelcon).parse(belconPopulation);
		LOG.info("Processing Belcon case...");
		threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		mapOfJobs = new TreeMap<>();
		for(Person person : scBelcon.getPopulation().getPersons().values()){
			if(map.containsKey(person.getId())){
				Plan plan = person.getSelectedPlan();
				Callable<Double> job = new VktEstimatorCallable(sc.getNetwork(), plan, geom, counter);
				Future<Double> submit = threadExecutor.submit(job);
				mapOfJobs.put(person.getId(), submit);
			}
		}
		scBelcon = null;
		LOG.info("Waiting for parallel handling of belcon case...");
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();
		LOG.info("Populating belcon case results...");
		for(Id<Person> id : mapOfJobs.keySet()){
			try {
				double d = mapOfJobs.get(id).get();
				Double[] da = map.get(id);
				da[1] = d;
				map.put(id, da);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not get callable results for person " + id.toString());
			}
		}
		
		/*================= Kraaicon =================*/
		LOG.info("Reading Kraaicon case...");
		counter.reset();
		Scenario scKraaicon = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scKraaicon).parse(kraaiconPopulation);
		LOG.info("Processing Kraaicon case...");
		threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		mapOfJobs = new TreeMap<>();
		for(Person person : scKraaicon.getPopulation().getPersons().values()){
			if(map.containsKey(person.getId())){
				Plan plan = person.getSelectedPlan();
				Callable<Double> job = new VktEstimatorCallable(sc.getNetwork(), plan, geom, counter);
				Future<Double> submit = threadExecutor.submit(job);
				mapOfJobs.put(person.getId(), submit);
			}
		}
		scKraaicon = null;
		LOG.info("Waiting for parallel handling of kraaicon case...");
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();
		LOG.info("Populating kraaicon case results...");
		for(Id<Person> id : mapOfJobs.keySet()){
			try {
				double d = mapOfJobs.get(id).get();
				Double[] da = map.get(id);
				da[2] = d;
				map.put(id, da);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not get callable results for person " + id.toString());
			}
		}
		
		/* Write the results. */
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("id,base,belcon,kraaicon");
			bw.newLine();
			for(Id<Person> id : map.keySet()){
				Double[] da = map.get(id);
				bw.write(String.format("%s,%.0f,%.0f,%.0f\n", id.toString(), da[0], da[1], da[2]));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
		LOG.info("Done comparing VKT.");
	}
	
	
	private static class VktEstimatorCallable implements Callable<Double>{
		final Network network;
		final Geometry geom;
		final Plan plan;
		private Counter counter;
		
		public VktEstimatorCallable(Network network, Plan plan, Geometry geom, Counter counter) {
			this.network = network;
			this.plan = plan;
			this.geom = geom;
			this.counter = counter;
		}
		
		@Override
		public Double call() throws Exception {
			double d = VktEstimator.estimateVktFromLegs(network, plan, geom);
			counter.incCounter();
			return d;
		}
	}
}
