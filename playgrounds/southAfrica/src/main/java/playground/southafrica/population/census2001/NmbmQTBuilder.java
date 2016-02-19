/* *********************************************************************** *
 * project: org.matsim.*
 * NmbmQTBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.population.census2001;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.households.IncomeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.census2001.containers.Race;
import playground.southafrica.population.census2001.containers.Schooling;
import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.RandomVariateGenerator;
import playground.southafrica.utilities.SouthAfricaInflationCorrector;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class NmbmQTBuilder {
	private final static Logger LOG = Logger.getLogger(NmbmQTBuilder.class);
	private final String inputFolder;
	private Map<Id<QuadTree>, QuadTree<Plan>> qtMap; 
	private ComprehensivePopulationReader cr;
	
	private final String[] employmentClasses = {"1", "0"};
	private final String[] ageClasses = {"5", "12", "23", "45", "68","120"};
	private final String[] incomeClasses = {"800", "3200", "12800", "51200", "5000000"};
	private final String[] householdSizeClasses = {"2", "10", "30"};//{"1","2","5","15","50"};
	private List<String[]> qtSpace;
	private final int ipfYear = 2001;
	private final int travelActivityYear = 2004;
	private final int populationYear = 2011; 
	
	private Population inputPopulation;
	private Households inputHouseholds;
	private Population outputPopulation;
	private Households outputHouseholds;
	private Map<Id<MyZone>, MyZone> zones;
	
	/*TODO Remove the dummy check variables. */
	int nullQtCount = 0;
	private List<Integer> hammingDistanceChanges;
	private List<Double> distanceList = new ArrayList<Double>();
	private Map<Id<MyZone>, Tuple<Integer, Integer>> zoneCounts = new HashMap<>();
	private List<Coord> homeCoords2 = new ArrayList<Coord>();
	private List<Coord> homeCoords = new ArrayList<Coord>();
	private Coord checkCoord = null;
	private boolean created = false;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(NmbmQTBuilder.class.toString(), args);
		MatsimRandom.reset();
		
		String inputFolder = args[0];
		String populationFile = args[1];
		String shapefile = args[2]; 
		String shapefileIdField = args[3];
		String outputFolder = args[4];
		String networkFile = args[5];
		
		NmbmQTBuilder nqtb = new NmbmQTBuilder(inputFolder, shapefile, Integer.parseInt(shapefileIdField));
		nqtb.buildProfiledQuadTree();
		try {
			nqtb.buildPopulation(populationFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Could not find the population input file.");
		}
		
		nqtb.writePopulation(outputFolder, networkFile);
		Header.printFooter();
	}
	
	/**
	 * Class to build a {@link QuadTree} of plans for different population
	 * profiles.
	 * @param inputFolder containing the four population files (See {@link 
	 * ComprehensivePopulationReader}).
	 */
	public NmbmQTBuilder(String inputFolder, String shapefile, int shapefileIdField) {
		this.inputFolder = inputFolder;
		this.cr = new ComprehensivePopulationReader();
		cr.parse(this.inputFolder);
		
		this.inputPopulation = cr.getScenario().getPopulation();
		this.inputHouseholds = cr.getScenario().getHouseholds();
		this.qtMap = new TreeMap<>();
		
		/* Parse the zones for which the population is being generated. */
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		try {
			mfr.readMultizoneShapefile(shapefile, shapefileIdField);
		} catch (IOException e) {
			throw new RuntimeException("Could not parse the zones for the region from " + shapefile);
		}
		this.zones = new TreeMap<>();
		for(MyZone zone : mfr.getAllZones()){
			this.zones.put(zone.getId(), zone);
		}
		LOG.info("Total number of subplaces: " + this.zones.size());
		
		// ---------------------------------------------------------//
		// The following is hard-coded and implementation-specific. //
		// The remainder of the method should be generic and not    //
		// require any changes to work. 							//
		// ---------------------------------------------------------//
		qtSpace = new ArrayList<String[]>();						//
		qtSpace.add(employmentClasses);								//
		qtSpace.add(ageClasses);									//
		qtSpace.add(incomeClasses);									//
		qtSpace.add(householdSizeClasses);							//
		/*__________________________________________________________*/
		
		hammingDistanceChanges = new ArrayList<Integer>();
		for(int i = 0; i <= qtSpace.size(); i++){
			hammingDistanceChanges.add(new Integer(0));
		}

	}
	
	
	public void buildProfiledQuadTree(){
		/*TODO Remove dummy variables and counters */
		Map<Id<QuadTree>, Integer> localQtMap = new HashMap<>();
		
		/* Determine the extent of the QuadTree. */
		LOG.info("Determine QuadTree extent...");
		double xMin = Double.POSITIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		
		for(Id<MyZone> id : this.zones.keySet()){
			Geometry envelope = zones.get(id).getEnvelope();
			xMin = Math.min(xMin, (envelope.getCoordinates())[0].x);
			xMax = Math.max(xMax, (envelope.getCoordinates())[2].x);
			yMin = Math.min(yMin, (envelope.getCoordinates())[0].y);
			yMax = Math.max(yMax, (envelope.getCoordinates())[2].y);
		}	
		
		/* Identify the correct QT for each person. */
		LOG.info("Process each person...");
		Counter counter = new Counter("  person # ");
		
		for(Id<Household> householdId : inputHouseholds.getHouseholds().keySet()){
			Household household = inputHouseholds.getHouseholds().get(householdId);
			for(Id<Person> personId : inputHouseholds.getHouseholds().get(householdId).getMemberIds()){
				Person person = inputPopulation.getPersons().get(personId);
				if(person != null){
					int householdSize = household.getMemberIds().size();
					int age = PersonUtils.getAge(person);
					Income surveyIncome = household.getIncome();
					if(surveyIncome != null){
						/* Check for working kids. */
						if(age < 12 && hasWork(person)){
							/* Ignore working kids. */
						} else{
							
							Income currentIncome = new IncomeImpl(
									SouthAfricaInflationCorrector.convert(surveyIncome.getIncome(), travelActivityYear, populationYear), 
									surveyIncome.getIncomePeriod());
							
							Id<QuadTree> qtId = getQtId(PersonUtils.isEmployed(person), householdSize, age, currentIncome);
							/* Check that there is a viable QuadTree definition */
							if(qtId != null){
								/* Create the QuadTree if it doesn't exist yet */
								if(!qtMap.containsKey(qtId) ){
									QuadTree<Plan> qt = new QuadTree<Plan>(xMin, yMin, xMax, yMax);		
									qtMap.put(qtId, qt);
								}						
								
								/* Add the person's selected plan to the QuadTree */
								QuadTree<Plan> thisQt = qtMap.get(qtId);
								Plan plan = person.getSelectedPlan();
								ActivityImpl firstActivity = (ActivityImpl) plan.getPlanElements().get(0);
								Coord c = null;
								if(firstActivity.getType().equalsIgnoreCase("h")){
									c = firstActivity.getCoord();
								}
								if(c == null){
									/* Ignore the person. S/he does not start their chain at home. */
								} else{
									thisQt.put(c.getX(), c.getY(), plan);
								}
								
								/*TODO Remove later */
								if(!localQtMap.containsKey(qtId)){
									localQtMap.put(qtId, 1);
								} else{
									int oldValue = localQtMap.get(qtId);
									localQtMap.put(qtId, oldValue+1);
								}
							}						
						}
					} else{
						/* There is no household income specified. Person is omitted. */
					}
				}
				counter.incCounter();
			}
		}
		counter.printCounter();
		LOG.info("QuadTree map size: " + qtMap.size());
		
		int j = 1;
		for(Id<QuadTree> id : localQtMap.keySet()){
			LOG.info("   " + (j++) + ". " + id.toString() + " (" + localQtMap.get(id) + ")");
		}
	}
	
	private boolean hasWork(Person person){
		for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
			if(pe instanceof ActivityImpl){
				ActivityImpl activity = (ActivityImpl) pe;
				if(activity.getType().contains("w")){
					return true;
				}
			}
		}
		return false;
	}
	
	public void buildPopulation(String populationFile) throws FileNotFoundException{
		/*TODO Remove these dummy variables and counters */
		Map<Id, Integer> noQtMap = new HashMap<Id, Integer>();
		int dummy = 0;
		
		
		/* Ensure that the population file exist. */
		File pf = new File(populationFile);
		if(!pf.exists()){
			throw new FileNotFoundException("The household file " + populationFile + " does not exist.");
		}
		
		/* Parse persons. */
		Map<Id<Household>,List<String>> personMap = parsePersons(pf.getAbsolutePath());
		
		LOG.info("Generating population...");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		outputPopulation = sc.getPopulation();
		outputHouseholds = new HouseholdsImpl();
		int personCounter = 0;
		for(Id<Household> hhId : personMap.keySet()){
			/* Create the household. */
			Household hh = outputHouseholds.getFactory().createHousehold(hhId);
			boolean firstMember = true;
			

			List<String> persons = personMap.get(hhId);
			/* Create each individual */
			for(String s : persons){
				String[] sa = s.split(",");

				/* Check household counts, and add first if not. */
				Id<MyZone> homeZone = Id.create(sa[12], MyZone.class);
				if (!zoneCounts.containsKey(homeZone)){
					zoneCounts.put(homeZone, new Tuple<Integer, Integer>(0, 0));
				}
				
				
				/* Get the home location of the household. */
				Point homePoint = this.zones.get(Id.create(sa[12], MyZone.class)).getInteriorPoint();
				Point altHomePoint = getRandomInteriorPoint(this.zones.get(Id.create(sa[12], MyZone.class)));
				Coord homeCoord = new Coord(altHomePoint.getX(), altHomePoint.getY());
				
				Person p = sc.getPopulation().getFactory().createPerson(Id.create(personCounter++, Person.class));
				
				
				
				if(firstMember){
					/* Income */
					double censusIncome = getRandomCensusIncomeMonthly(Integer.parseInt(sa[5]));
					Income income = new IncomeImpl(
							SouthAfricaInflationCorrector.convert(censusIncome,	ipfYear, populationYear),
							IncomePeriod.month);
					if(income.getIncome() < 0){
						LOG.error("Negative income.");
					}
					
					hh.setIncome(income);
					
					/*TODO Living quarter type */
					
					firstMember = false;
					
					/* Add to person and household counts. */
					zoneCounts.put(homeZone, new Tuple<Integer, Integer>(
							zoneCounts.get(homeZone).getFirst()+1, 
							zoneCounts.get(homeZone).getSecond()) );
					homeCoords.add(homeCoord);
					/* TODO Remove after debugging. */
					String checkZone = "27508018";
					if(homeZone.toString().equalsIgnoreCase(checkZone) && dummy < 10){
						LOG.warn(checkZone + ": " + p.getId().toString());
						dummy++;
					}
				}
				/* Add the person's Id to the household. */
				hh.getMemberIds().add(p.getId());
				/* Add original IPF person number. */
				p.getCustomAttributes().put("ipfId", sa[1]);
				/* Link individual to its household. */
				p.getCustomAttributes().put("householdId", hhId.toString());
				/* Set the person's age. */
				PersonUtils.setAge(p, Integer.parseInt(sa[7]));
				/* Set the person's gender. */
				PersonUtils.setSex(p, sa[8].equalsIgnoreCase("1") ? "m" : "f");
				/* Set the person's race. */
				p.getCustomAttributes().put("race", Race.getDescription(Race.getRace(Integer.parseInt(sa[4]))));
				/* Set person's employment status. */
				PersonUtils.setEmployed(p, Integer.parseInt(sa[10]) == 1 ? true : false);
				/* Set person's schooling status. */
				p.getCustomAttributes().put("school", Schooling.getDescription(Schooling.getSchool(Integer.parseInt(sa[11]))));
				
				/* To make things easier, set the household's income as a custom attribute */
				p.getCustomAttributes().put("householdIncome", hh.getIncome().getIncome());
								
				/*===== Now pick a plan for the person =====*/
				/* Get the QT id */
				Id qtId = getQtId(PersonUtils.isEmployed(p), Integer.parseInt(sa[2]), PersonUtils.getAge(p), hh.getIncome());
				Id tryNewId = searchForQtId(qtId);
				if(tryNewId == null){
					if(!noQtMap.containsKey(qtId)){
						noQtMap.put(qtId, 1);
					} else{
						noQtMap.put(qtId, noQtMap.get(qtId)+1);
					}
					/* FIXME Must still decide what to do. Not necessary for NMBM */						
				} else{
					qtId = tryNewId;
				}
				
				/* Get the closest 20 people to the person's home location. */				
				if(qtMap.containsKey(qtId)){
					/* For each person you have a different sample size - choice set.
					 * If you have 200 observations, and you pick one randomly.
					 * 1. Get all persons in QT;
					 * 2. Pick the twenty nearest (choice);
					 *    - randomly choose one of them.
					 * (The distance distribution is (typically) consistent with the
					 * distribution of the observations.)
					 * ...  
					 */
					List<Tuple<Plan,Double>> closestPlans = getClosestPlans(homeCoord, qtMap.get(qtId), 20);
					/* Randomly pick any of the closest plans. and make a COPY of it. */
					Tuple<Plan, Double> randomTuple = closestPlans.get(getRandomPermutation(closestPlans.size())[0]);
					PlanImpl plan = new PlanImpl();
					plan.copyFrom(randomTuple.getFirst());
				
					distanceList.add(randomTuple.getSecond());

					/* Should have a plan now. Change its home locations. */
					for(PlanElement pe : plan.getPlanElements()){
						if(pe instanceof ActivityImpl){
							ActivityImpl activity = (ActivityImpl) pe;
							/* Set the home location */
							if(activity.getType().equalsIgnoreCase("h")){
								activity.setCoord(homeCoord);
							}
							
							/*TODO What should be done to the other activity locations? Scrambled? */
						}
					}
					p.addPlan(plan);
					
					/*TODO Remove after debugging */
					if(p.getId().equals(Id.create("48548", Person.class))){
						checkCoord = ((ActivityImpl)p.getSelectedPlan().getPlanElements().get(0)).getCoord();
						created = true;
					}
					
					/* TODO Remove after debugging. Add to person and household counts. */
					zoneCounts.put(homeZone, new Tuple<Integer, Integer>(
							zoneCounts.get(homeZone).getFirst(), 
							zoneCounts.get(homeZone).getSecond()+1) );
					
				}
				outputPopulation.addPerson(p);
			}
			outputHouseholds.getHouseholds().put(hhId, hh);
			
			/* TODO Remove after debugging. Add to person and household counts. */
			Person oneMember = outputPopulation.getPersons().get(hh.getMemberIds().get(0));
			PlanElement firstActivity = oneMember.getSelectedPlan().getPlanElements().get(0);
			if(firstActivity instanceof ActivityImpl){
				ActivityImpl ai = (ActivityImpl)firstActivity;
				if(ai.getType().equalsIgnoreCase("h")){
					homeCoords2.add(ai.getCoord());
				}
			}
			
			if(created){
				if(!((ActivityImpl) sc.getPopulation().getPersons().get(Id.create(48548, Person.class)).getSelectedPlan().getPlanElements().get(0)).getCoord().equals(checkCoord)){
					LOG.error("GOTCHA!!");
				}
			}
		}
		
		/*=====================================================================*/
		/* TODO Keep, or remove, these dummy statistics used for fault-finding. */
		/*=====================================================================*/
		LOG.info("Number of times no QT class was found: " + nullQtCount);
		int counter = 1;
		for(Id id : noQtMap.keySet()){
			LOG.info("   " + counter++ + ". " +  id.toString() + " (" + noQtMap.get(id) + ")");
		}
		/*=====================================================================*/
		LOG.info("Hamming distance fixes applied:");
		for(int i = 0; i < hammingDistanceChanges.size(); i++){
			LOG.info("   " + i + ": " + hammingDistanceChanges.get(i));
		}
		/*=====================================================================*/
	}
	
	
	private List<Tuple<Plan, Double>> getClosestPlans(Coord c, QuadTree<Plan> qt, int number){
		List<Tuple<Plan, Double>> list = new ArrayList<Tuple<Plan,Double>>();
		List<Tuple<Plan, Double>> tuples = new ArrayList<Tuple<Plan,Double>>();
		
		/* Quickly scan distance in QuadTree to limit the ranking later-on. */ 
		Collection<Plan> plansToRank = null;
		if(qt.values().size() > number){
		 /* Start the search radius with the distance to the closest person. */
			Plan closestPlan = qt.getClosest(c.getX(), c.getY());
			double radius = CoordUtils.calcEuclideanDistance(c, ((ActivityImpl) closestPlan.getPlanElements().get(0)).getCoord());
			Collection<Plan> plans = qt.getDisk(c.getX(), c.getY(), radius);
			while(plans.size() < number){
				/* Double the radius. If the radius happens to be zero (0), 
				 * then you stand the chase of running into an infinite loop.
				 * Hence, add a minimum of 1m to move on. */
				radius += Math.max(radius, 1.0);
				plans = qt.getDisk(c.getX(), c.getY(), radius);
			}
			plansToRank = plans;
		} else{
			plansToRank = qt.values();
		}
		
		/* Rank the plans based on distance. */
		for(Plan plan : plansToRank){
			double d = CoordUtils.calcEuclideanDistance(c, ((ActivityImpl) plan.getPlanElements().get(0)).getCoord());
			Tuple<Plan, Double> thisTuple = new Tuple<Plan, Double>(plan, d);
			if(tuples.size() == 0){
				tuples.add(thisTuple);
			} else{
				int index = 0;
				boolean found = false;
				while(!found && index < tuples.size()){
					if(d <= tuples.get(index).getSecond()){
						found = true;
					} else{
						index++;
					}
				}
				if(found){
					tuples.add(index, thisTuple);
				} else{
					tuples.add(thisTuple);
				}
			}
		}
		
		/* Add the number of plans requested, or the  number of the plans in 
		 * the QuadTree, whichever is less, to the results, and return. */
		for(int i = 0; i < Math.min(number, tuples.size()); i++){
			list.add(tuples.get(i));
		}
		return list;
	}
	

	/**
	 * Write the households and population files to the given output folder.
	 * @param ouputFolder
	 * @param networkFile
	 */
	public void writePopulation(String outputFolder, String networkFile){
		LOG.info("Writing output...");
		
		/*TODO Remove after debugging. */
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFolder + "check2.csv");
		try {
			bw.write("SPCode,Hhs2,Persons2");
			bw.newLine();
			for(Id id : zoneCounts.keySet()){
				bw.write(id.toString());
				bw.write(",");
				bw.write(String.valueOf(zoneCounts.get(id).getFirst()));
				bw.write(",");
				bw.write(String.valueOf(zoneCounts.get(id).getSecond()));
				bw.newLine();				
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + outputFolder + "check2.csv");
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + outputFolder + "check2.csv");
			}
		}
		
		BufferedWriter bw2 = IOUtils.getBufferedWriter(outputFolder + "check3.csv");
		try {
			bw2.write("Long,Lat");
			bw2.newLine();
			for(Coord c : homeCoords){
				bw2.write(String.format("%.0f,%.0f\n", c.getX(), c.getY()));
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + outputFolder + "check3.csv");
		} finally {
			try {
				bw2.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + outputFolder + "check3.csv");
			}
		}
		
		BufferedWriter bw3 = IOUtils.getBufferedWriter(outputFolder + "check4.csv");
		try {
			bw3.write("Long,Lat");
			bw3.newLine();
			for(Coord c : homeCoords2){
				bw3.write(String.format("%.0f,%.0f\n", c.getX(), c.getY()));
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + outputFolder + "check4.csv");
		} finally {
			try {
				bw3.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + outputFolder + "check4.csv");
			}
		}

		
		/* Parse the network */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc.getNetwork());
		nr.parse(networkFile);

		/* Write the population */
		PopulationWriter pw = new PopulationWriter(this.outputPopulation, sc.getNetwork());
		pw.write(outputFolder + "population.xml.gz");
		
		/* If there are custom attributes, write them separately as object 
		 * attributes */
		ObjectAttributes personAttributes = new ObjectAttributes();
		for(Person person : this.outputPopulation.getPersons().values()){
			for(String s : person.getCustomAttributes().keySet()){
				personAttributes.putAttribute(person.getId().toString(), s, person.getCustomAttributes().get(s));
			}
		}
		ObjectAttributesXmlWriter paw = new ObjectAttributesXmlWriter(personAttributes);
		paw.writeFile(outputFolder + "populationAttributes.xml.gz");
		
		/* Write the households.*/
		HouseholdsWriterV10 hw = new HouseholdsWriterV10(outputHouseholds);
		hw.writeFile(outputFolder + "households.xml.gz");
		
		/* Write the distances of the selected plans. This is purely for 
		 * statistical purposes and will only be relevant if the population
		 * and the survey data are for the same area, e.g. Nelson Mandela Bay.
		 */
		String bufferedWriterName = outputFolder + "distances.txt";
		bw = IOUtils.getBufferedWriter(bufferedWriterName);
		try{
			for(double d : distanceList){
				bw.write(String.format("%.0f\n", d));
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to " + bufferedWriterName);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter for " + bufferedWriterName);
			}
		}
	}
	
		
	/**
	 * Converting the income code as used during the Iterative Proportional
	 * Fitting (IPF) into an actual annual income value. The value is a
	 * randomly generated value between the lower and upper limits specified
	 * by the income code.
	 * @param code the income code used. See {@link IpfWriter#getHouseholdIncomeCode(Income)}
	 * for the income class limits used.
	 * @return the annual income, or <code>null</code> if an invalid income code was used.
	 */
	public Double getRandomCensusIncomeAnnual(int code){
		Double income = null;
		switch (code) {
		case 1:
			return 0.0;
		case 3:
			return getRandomLeftTailedTriangularIncome(0, 7200);
		case 5:
			return getRandomUniformIncome(7200, 26152);
		case 7:
			return getRandomUniformIncome(26152, 108612);
		case 9:
			return getRandomUniformIncome(108612, 434446);
		case 11:
			return getRandomRighTailedTriangularIncome(434446, 1737786);
		case 12:
			return getRandomRighTailedTriangularIncome(1737786, 4915200);
		default:
			break;
		}
		LOG.error("Couldn't find an income for code " + code);
		return income;
	}
	
	/**
	 * Converting the income code as used during the Iterative Proportional
	 * Fitting (IPF) into an actual monthly income value. The value is a
	 * randomly generated value between the lower and upper limits specified
	 * by the income code.
	 * <h5>
	 * Note:<br></h5>
	 * The monthly income is merely the annual income ({@link #getRandomCensusIncomeAnnual(int)}) 
	 * divided by 12.<br><br>
	
	 * @param code the income code used. See {@link IpfWriter#getHouseholdIncomeCode(Income)}
	 * for the income class limits used.
	 * @return the annual income, or <code>null</code> if an invalid income code was used.
	 */
	private double getRandomCensusIncomeMonthly(int code){
		return (getRandomCensusIncomeAnnual(code) / 12);
	}
	
	private double getRandomUniformIncome(double lower, double upper){
		return lower + Math.random()*(upper - lower);
	}
	
	private double getRandomRighTailedTriangularIncome(double lower, double upper){
		return RandomVariateGenerator.getTriangular(lower, lower, upper);
	}

	private double getRandomLeftTailedTriangularIncome(double lower, double upper){
		return RandomVariateGenerator.getTriangular(lower, upper, upper);
	}

	
	private Map<Id<Household>,List<String>> parsePersons(String filename){
		int personCounter = 0;
		int householdCounter = 0;
		
		Map<Id<Household>,List<String>> personMap = new TreeMap<>();
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try {
			String line = br.readLine(); /* Header */
			while((line=br.readLine()) != null){
				String[] sa = line.split(",");
				if(sa.length == 13){
					Id<Household> hhID = Id.create(householdCounter++, Household.class);
					int numberOfHouseholdMembers = Integer.parseInt(sa[2]);
					List<String> memberList = new ArrayList<String>(numberOfHouseholdMembers);
					memberList.add(line);
					for(int i = 0; i < numberOfHouseholdMembers-1; i++){
						memberList.add(br.readLine());
					}
					personMap.put(hhID, memberList);
					personCounter += numberOfHouseholdMembers;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedReader "
					+ filename);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedReader "
						+ filename);
			}
		}
		LOG.info("Done parsing persons (" + personCounter + " found)");
		return personMap;
	}
	
		
	private Id<QuadTree> getQtId(boolean isEmployed, int householdSize, int age, Income income){
		/* Gender */
//		String genderCode = gender.equalsIgnoreCase("m") ? "m" : "f";
//		genderCode = "u"; // Override
		
		/* Employment */
		String employment = isEmployed ? "1" : "0";
		
		/* Household size */
		boolean foundHouseholdSizeClass = false;
		int i = 0;
		while(i < this.householdSizeClasses.length && !foundHouseholdSizeClass){
			if(Integer.parseInt(householdSizeClasses[i]) >= householdSize){
				foundHouseholdSizeClass = true;
			} else{
				i++;
			}
		}
		if(!foundHouseholdSizeClass){
			LOG.error("Could not find a household size class for the value " + householdSize);
		}
		String householdSizeCode = String.format("%d", Integer.parseInt(householdSizeClasses[i]));
		
		/* Age */
		boolean foundAgeClass = false;
		i = 0;
		while(i < this.ageClasses.length && !foundAgeClass){
			if(Integer.parseInt(ageClasses[i]) >= age){
				foundAgeClass = true;
			} else{
				i++;
			}
		}
		if(!foundAgeClass){
			LOG.error("Could not find an age class for the value " + age);
		}
		String ageCode = String.format("%d", Integer.parseInt(ageClasses[i]));
		
		/* Income, assuming the income is already in the population year, and expressed as monthly. */
		if(income != null){
			Double incomeValue = income.getIncome();
			boolean foundIncomeClass = false;
			i = 0;
			while(i < this.incomeClasses.length && !foundIncomeClass){
				if(Double.parseDouble(incomeClasses[i]) >= incomeValue){
					foundIncomeClass = true;
				} else{
					i++;
				}
			}
			if(!foundIncomeClass){
				LOG.error("Could not find an income class for the value " + income.getIncome() + ". Using highest value.");
				i = incomeClasses.length-1;
			}
			String incomeCode = incomeClasses[i];
			return Id.create(employment + "_" + ageCode + "_" + incomeCode + "_" + householdSizeCode, QuadTree.class) ;
		} else{
			return null;
		}			
	}
	
	
	/**
	 * The {@link Id} used in the {@link QuadTree} is made up of the following
	 * elements:
	 * <ol>
	 * 		<li> employment status;
	 * 		<li> age class;
	 * 		<li> household income class; and
	 * 		<li> household size class.
	 * </ol>
	 * @param id
	 * @return
	 */
	public Id<QuadTree> searchForQtId(Id<QuadTree> id){
		/* TODO Remove after debugging. */
//		if(id.toString().equalsIgnoreCase("0_12_800_30")){
//			String s = "";
//		}
		
		
		/* If the Id is already associated with a QuadTree, no change is 
		 * required. Return the current Id as is. */
		if(qtMap.containsKey(id)){
			hammingDistanceChanges.set(0, hammingDistanceChanges.get(0)+1);
			return id;
		}
		
		/* Otherwise, attempt perturbations to the Id. */
		Id newId = null;
		int maximumTriesAtCurrentHammingDistance = 500;
		int hammingDistance = 1;

		/* Get the index for each of the QuadTree dimensions of the given Id. */
		String[] sa = id.toString().split("_");
		int[] indices = new int[qtSpace.size()];
		for(int i = 0; i < qtSpace.size(); i++){
			indices[i] = getIndex(sa[i], qtSpace.get(i));
		}
		
		do {
			int triesAtCurrentHammingDistance = 0;
			do{
				int[] tmpIndices = indices.clone();

				/* Get a random permutation of the QuadTree dimensions. */
				int[] permutation = getRandomPermutation(qtSpace.size());
				
				/* Make random changes. The number of changes should be the
				 * same as the current Hamming distance. */
				for(int stepChange = 0; stepChange < hammingDistance; stepChange++){
					int classToChange = permutation[stepChange];
					/* Randomly change the class. */
					int change = 0;
					if(indices[classToChange] == 0){
						/* It is the lowest class, so can only increase. */
						change = +1;
					} else if(indices[classToChange] == qtSpace.get(classToChange).length-1){
						/* It is the highest class, so can only decrease. */
						change = -1;
					} else{
						double d = MatsimRandom.getRandom().nextDouble();
						change =  d < 0.5 ? -1 : +1;
					}
					tmpIndices[classToChange] = indices[classToChange] + change;
				}
				
				/* Check if the perturbation has resulted in a known QuadTree. */
				String tmpIdString = "";
				for(int i = 0; i < tmpIndices.length-1; i++){
					tmpIdString += qtSpace.get(i)[tmpIndices[i]] + "_";
				}
				tmpIdString += qtSpace.get(tmpIndices.length-1)[tmpIndices[tmpIndices.length-1]];
				Id<QuadTree> tmpId = Id.create(tmpIdString, QuadTree.class);
				
				newId = qtMap.containsKey(tmpId) ? tmpId : null; 
				
			} while (newId == null &&
					triesAtCurrentHammingDistance++ < maximumTriesAtCurrentHammingDistance);
			
		} while (newId == null && ++hammingDistance <= qtSpace.size());

		if(newId == null){
			nullQtCount++;
		} else{
			hammingDistanceChanges.set(hammingDistance, hammingDistanceChanges.get(hammingDistance)+1);
		}
		
		return newId;
	}
	
	private int[] getRandomPermutation(int number){
		/* Add the sequential integers to an array. */
		int[] a = new int[number];
		for(int i = 0; i < number; i++){
			a[i] = i;
		}

		/* Shuffle each position in the array with a random other position. */
		int[] b = a.clone();
		for(int c = b.length-1; c >= 0; c--){
			int d = (int)Math.floor(MatsimRandom.getRandom().nextDouble() * (c+1));
			int tmp = b[d];
			b[d] = b[c];
			b[c] = tmp;
		}
		return b;
	}
	
	
	private int getIndex(String value, String[] classes){
		int index = -1;
		for(int i = 0; i < classes.length; i++){
			if(Double.parseDouble(classes[i]) == Double.parseDouble(value)){
				index = i;
				break;
			}
		}
		if(index == -1){
			LOG.error("Couldn't find the index for the value " + value + " in the array " + classes);
		}
		return index;
	}
	
	
	private Point getRandomInteriorPoint(Geometry geometry){
		/* First get the radius as the distance from the centroid to the
		 * farthest from the four envelope corners. */
		Coordinate c = geometry.getCentroid().getCoordinate(); 
		double radius = Double.NEGATIVE_INFINITY;
		Polygon envelope = (Polygon) geometry.getEnvelope();
		Coordinate c1 = envelope.getCoordinates()[0];
		Coordinate c2 = envelope.getCoordinates()[1];
		Coordinate c3 = envelope.getCoordinates()[2];
		Coordinate c4 = envelope.getCoordinates()[3];
		radius = Math.max(radius, c.distance(c1));
		radius = Math.max(radius, c.distance(c2));
		radius = Math.max(radius, c.distance(c3));
		radius = Math.max(radius, c.distance(c4));
		
		Point p = null;
		boolean found = false;
		while(!found){
			/* Radius and angle. */
//			double randomAngle = MatsimRandom.getRandom().nextDouble()*2*Math.PI;
//			double randomRadius = MatsimRandom.getRandom().nextDouble()*radius;
//			double x = c.x + randomRadius*Math.cos(randomAngle);
//			double y = c.y + randomRadius*Math.sin(randomAngle);

			/* Just delta x and y. */
			double x = c1.x + MatsimRandom.getRandom().nextDouble()*(c3.x - c1.x);
			double y = c1.y + MatsimRandom.getRandom().nextDouble()*(c3.y - c1.y);

			p = geometry.getFactory().createPoint(new Coordinate(x, y));
			if(geometry.contains(p)){
				found = true;
			}
		}
		
		return p;
	}

}

