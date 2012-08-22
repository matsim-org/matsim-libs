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

package playground.southafrica.population.utilities;

import java.io.BufferedReader;
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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
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

import playground.southafrica.population.containers.Race;
import playground.southafrica.population.containers.Schooling;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.RandomVariateGenerator;
import playground.southafrica.utilities.SouthAfricaInflationCorrector;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class NmbmQTBuilder {
	private final static Logger LOG = Logger.getLogger(NmbmQTBuilder.class);
	private final String inputFolder;
	private Map<Id, QuadTree<Plan>> qtMap; 
	private ComprehensivePopulationReader cr;
	
	private final String[] genderClasses = {"m","f"};
	private final String[] ageClasses = {"5", "12", "23", "45", "68","120"};
	private final String[] incomeClasses = {"800", "3200", "12800", "51200", "500000"};
	private final String[] householdSizeClasses = {"2", "10", "30"};//{"1","2","5","15","50"};
	private final int ipfYear = 2001;
	private final int travelActivityYear = 2004;
	private final int populationYear = 2011; 
	
	private Population inputPopulation;
	private Households inputHouseholds;
	private Population outputPopulation;
	private Households outputHouseholds;
	private Map<Id, MyZone> zones;
	
	/*TODO Remove the dummy check variables. */
	int nullQtCount = 0;
	private int fixedByReducingIncome = 0;
	private int fixedByReducingHouseholdSize = 0;
	private int fixedByReducingBoth = 0;
	private int fixedByIncreasingAge = 0;
	private int fixedByIncreasingAgeReducingBoth = 0;
	private int fixedByIncreasingIncome = 0;
	private int fixedByIncreasingIncomeReducingSize = 0;
	private int fixedByPuttingKidsOutOfWork = 0;
	private Map<Integer, Integer> distanceMap = new TreeMap<Integer, Integer>();


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(NmbmQTBuilder.class.toString(), args);
		
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
			Gbl.errorMsg("Could not find the population input file.");
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
		this.inputHouseholds = cr.getHouseholds();
		this.qtMap = new TreeMap<Id, QuadTree<Plan>>();
		
		/* Parse the zones for which the population is being generated. */
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		try {
			mfr.readMultizoneShapefile(shapefile, shapefileIdField);
		} catch (IOException e) {
			throw new RuntimeException("Could not parse the zones for the region from " + shapefile);
		}
		this.zones = new TreeMap<Id, MyZone>();
		for(MyZone zone : mfr.getAllZones()){
			this.zones.put(zone.getId(), zone);
		}
	}
	
	
	public void buildProfiledQuadTree(){
		/*TODO Remove dummy variables and counters */
		Map<Id, Integer> localQtMap = new HashMap<Id, Integer>();
		
		/* Determine the extent of the QuadTree. */
		LOG.info("Determine QuadTree extent...");
		double xMin = Double.POSITIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		
		for(Id id : this.zones.keySet()){
			Geometry envelope = zones.get(id).getEnvelope();
			xMin = Math.min(xMin, (envelope.getCoordinates())[0].x);
			xMax = Math.max(xMax, (envelope.getCoordinates())[2].x);
			yMin = Math.min(yMin, (envelope.getCoordinates())[0].y);
			yMax = Math.max(yMax, (envelope.getCoordinates())[2].y);
		}	
		
		/* Identify the correct QT for each person. */
		LOG.info("Process each person...");
		Counter counter = new Counter("  person # ");
		
		for(Id householdId : inputHouseholds.getHouseholds().keySet()){
			Household household = inputHouseholds.getHouseholds().get(householdId);
			for(Id personId : inputHouseholds.getHouseholds().get(householdId).getMemberIds()){
				PersonImpl person = (PersonImpl) inputPopulation.getPersons().get(personId);
				if(person != null){
					String gender = person.getSex();
					int householdSize = household.getMemberIds().size();
					int age = person.getAge();
					Income surveyIncome = household.getIncome();
					if(surveyIncome != null){
						/* Check for working kids. */
						if(age < 12 && hasWork(person)){
							/* Ignore working kids. */
						} else{
							
							Income currentIncome = new IncomeImpl(
									SouthAfricaInflationCorrector.convert(surveyIncome.getIncome(), travelActivityYear, populationYear), 
									surveyIncome.getIncomePeriod());
							
							Id qtId = getQtId(person.isEmployed(), householdSize, age, currentIncome);
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
		for(Id id : localQtMap.keySet()){
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
		
		
		/* Ensure that the population file exist. */
		File pf = new File(populationFile);
		if(!pf.exists()){
			throw new FileNotFoundException("The household file " + populationFile + " does not exist.");
		}
		
		/* Parse persons. */
		Map<Id,List<String>> personMap = parsePersons(pf.getAbsolutePath());
		
		LOG.info("Generating population...");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		outputPopulation = sc.getPopulation();
		outputHouseholds = new HouseholdsImpl();
		int personCounter = 0;
		for(Id hhId : personMap.keySet()){
			/* Create the household. */
			Household hh = outputHouseholds.getFactory().createHousehold(hhId);
			boolean firstMember = true;
			
			Coord homeCoord = null;

			List<String> persons = personMap.get(hhId);
			/* Create each individual */
			for(String s : persons){
				String[] sa = s.split(",");
				PersonImpl p = (PersonImpl) sc.getPopulation().getFactory().createPerson(new IdImpl(personCounter++));
				if(firstMember){
					/* Income */
					double censusIncome = getRandomCensusIncomeMonthly(Integer.parseInt(sa[5]));
					Income income = new IncomeImpl(
							SouthAfricaInflationCorrector.convert(censusIncome,	ipfYear, populationYear),
							IncomePeriod.month);
					hh.setIncome(income);
					
					/*TODO Living quarter type */
					
					/* Get the home location of the household. */
					Point homePoint = this.zones.get(new IdImpl(sa[12])).getInteriorPoint();
					homeCoord = new CoordImpl(homePoint.getX(), homePoint.getY());
					
					firstMember = false;
				}
				/* Add the person's Id to the household. */
				hh.getMemberIds().add(p.getId());
				/* Add original IPF person number. */
				p.getCustomAttributes().put("ipfId", sa[1]);
				/* Link individual to its household. */
				p.getCustomAttributes().put("householdId", hhId.toString());
				/* Set the person's age. */
				p.setAge(Integer.parseInt(sa[7]));
				/* Set the person's gender. */
				p.setSex(sa[8].equalsIgnoreCase("1") ? "m" : "f");
				/* Set the person's race. */
				p.getCustomAttributes().put("race", Race.getDescription(Race.getRace(Integer.parseInt(sa[4]))));
				/* Set person's employment status. */
				p.setEmployed(Integer.parseInt(sa[10]) == 1 ? true : false);
				/* Set person's schooling status. */
				p.getCustomAttributes().put("school", Schooling.getDescription(Schooling.getSchool(Integer.parseInt(sa[11]))));
				
				/* To make things easier, set the household's income as a custom attribute */
				p.getCustomAttributes().put("householdIncome", hh.getIncome().getIncome());
								
				/*===== Now pick a plan for the person =====*/
				/* Get the QT id */
				Id qtId = getQtId(p.isEmployed(), Integer.parseInt(sa[2]), p.getAge(), hh.getIncome());

				if(!qtMap.containsKey(qtId)){
					Id tryNewId = nmbmRulesToChangeQtId(qtId);
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
				} 
				
				/* Starting with 500m, search and sample from the associated 
				 * QuadTree. If not found, double the distance. */
				if(qtMap.containsKey(qtId)){
					int distance = 250;
					
					Plan plan = null;
					do {
						Collection<Plan> plans = qtMap.get(qtId).get(homeCoord.getX(), homeCoord.getY(), distance);
						if(plans.size() == 0){
							distance *= 2;
						} else{
							plan = ((List<Plan>) plans).get((int) Math.rint(Math.random() * (plans.size()-1)));
							/* Keep track of the distance for which plans were selected */
							if(distanceMap.containsKey(distance)){
								int oldValue = distanceMap.get(distance);
								distanceMap.put(distance, oldValue+1);
							} else{
								distanceMap.put(distance, 1);
							}
						}
					} while (plan == null);

					/* Should have a plan now. Change its activity locations. */
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
				}
				outputPopulation.addPerson(p);
			}
			outputHouseholds.getHouseholds().put(hhId, hh);
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
		LOG.info("Fixes applied:");
		LOG.info("   Number of income-reducing-fixes: " + fixedByReducingIncome);
		LOG.info("   Number of household size-reducing-fixes: " + fixedByReducingHouseholdSize);
		LOG.info("   Number of combination-fixes: " + fixedByReducingBoth);
		LOG.info("   Number of age-increase-fixes: " + fixedByIncreasingAge);
		LOG.info("   Number of age-increase-combination-reduction-fixes: " + fixedByIncreasingAgeReducingBoth);
		LOG.info("   Number of income-increasing-fixes: " + fixedByIncreasingIncome);
		LOG.info("   Number of income-increasing-size-reduction-fixes: " + fixedByIncreasingIncomeReducingSize);
		LOG.info("   Number of child-liberating-fixes: " + fixedByPuttingKidsOutOfWork);
		/*=====================================================================*/
		LOG.info("Distances within which a plan was found in the QuadTree:");
		for(int i : distanceMap.keySet()){
			LOG.info("   " + i + "m: " + distanceMap.get(i));
		}
		/*=====================================================================*/
	}
	

	/**
	 * Write the households and population files to the given output folder.
	 * @param ouputFolder
	 * @param networkFile
	 */
	public void writePopulation(String outputFolder, String networkFile){
		LOG.info("Writing output...");
		
		/* Parse the network */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc);
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
			return getRandomRighTailedTriangularIncome(4915200, 1737786);
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

	
	private Map<Id,List<String>> parsePersons(String filename){
		int personCounter = 0;
		int householdCounter = 0;
		
		Map<Id,List<String>> personMap = new TreeMap<Id, List<String>>();
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try {
			String line = br.readLine(); /* Header */
			while((line=br.readLine()) != null){
				String[] sa = line.split(",");
				if(sa.length == 13){
					Id hhID = new IdImpl(householdCounter++);
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
	
		
	private Id getQtId(boolean isEmployed, int householdSize, int age, Income income){
		/* Gender */
//		String genderCode = gender.equalsIgnoreCase("m") ? "m" : "f";
//		genderCode = "u"; // Override
		
		/* Employment */
		String employment = isEmployed ? "y" : "n";
		
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
		String householdSizeCode = String.format("%02d", Integer.parseInt(householdSizeClasses[i]));
		
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
		String ageCode = String.format("%03d", Integer.parseInt(ageClasses[i]));
		
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
				LOG.error("Could not find an income class for the value " + income);
			}
			String incomeCode = incomeClasses[i];
			return new IdImpl(employment + "_" + ageCode + "_" + incomeCode + "_" + householdSizeCode) ;
		} else{
			return null;
		}			
	}
	
	
	public Id nmbmRulesToChangeQtId(Id id){
		Id newId = null;
		String[] sa = id.toString().split("_");
		
		getGenderIndex(sa[0]); // Currently we don't do anything with it.
		
		/* Get one level lower income class. */
		if(newId == null){ // this may seem silly, but eases up the moving around of rules. 
			int incomeIndex = getHouseholdIncomeIndex(sa[2]);
			if(incomeIndex > 0){
				Id tempId = new IdImpl(sa[0] + "_" + sa[1] + "_" + incomeClasses[incomeIndex-1] + "_" + sa[3]);
				if(qtMap.containsKey(tempId)){
					newId = tempId;
					fixedByReducingIncome++;
				}
			}
		}
		
		/* Reduce household size class by one level. */
		if(newId == null){
			int hhsIndex = getHouseholdSizeIndex(sa[3]);
			if(hhsIndex > 0){
				Id tempId = new IdImpl(sa[0] + "_" + sa[1] + "_" + sa[2] + "_" + String.format("%02d", Integer.parseInt(householdSizeClasses[hhsIndex-1])) );
				if(qtMap.containsKey(tempId)){
					newId = tempId;
					fixedByReducingHouseholdSize++;
				}
			}
		}

		/* Next, try moving both income and household size down (but only by ONE class). */
		if(newId == null){
			int incomeIndex = getHouseholdIncomeIndex(sa[2]);
			int hhsIndex = getHouseholdSizeIndex(sa[3]);
			if(incomeIndex > 0 && hhsIndex > 0){
				Id tempId = new IdImpl(sa[0] + "_" + sa[1] + "_" + incomeClasses[incomeIndex-1] + "_" + String.format("%02d", Integer.parseInt(householdSizeClasses[hhsIndex-1])) );
				if(qtMap.containsKey(tempId)){
					newId = tempId;
					fixedByReducingBoth++;
				}
			}
		}	

		/* Next increase age category by one class. This is the result of seeing the only remaining 
		 * unidentified groups being up-to-six-year olds. */
		if(newId == null){
			int ageIndex = getAgeIndex(sa[1]);
			if(ageIndex < (ageClasses.length-1) ){
				Id tempId = new IdImpl(sa[0] + "_" + String.format("%03d", Integer.parseInt(ageClasses[ageIndex+1])) + "_" + sa[2] + "_" + sa[3]);
				if(qtMap.containsKey(tempId)){
					newId = tempId;
					fixedByIncreasingAge++;
				}
			}			
		}
		
		/* Move all to the average: increase age by one class, reduce income 
		 * and household size by one. */
 		if(newId == null){
			int ageIndex = getAgeIndex(sa[1]);
			int incomeIndex = getHouseholdIncomeIndex(sa[2]);
			int hhsIndex = getHouseholdSizeIndex(sa[3]);
			if(ageIndex < (ageClasses.length-1) && incomeIndex > 0 && hhsIndex > 0){
				Id tempId = new IdImpl(sa[0] + "_" + String.format("%03d", Integer.parseInt(ageClasses[ageIndex+1])) + "_" + incomeClasses[incomeIndex-1] + "_" + String.format("%02d", Integer.parseInt(householdSizeClasses[hhsIndex-1])) );
				if(qtMap.containsKey(tempId)){
					newId = tempId;
					fixedByIncreasingAgeReducingBoth++;
				}
			}
		}
 		
 		/* Increase low-income by one class. */
 		if(newId == null){
 			int incomeIndex = getHouseholdIncomeIndex(sa[2]);
 			if(incomeIndex < (incomeClasses.length-2) ){
 				Id tempId = new IdImpl(sa[0] + "_" + sa[1] + "_" + incomeClasses[incomeIndex+1] + "_" + sa[3] );
 				if(qtMap.containsKey(tempId)){
 					newId = tempId;
 					fixedByIncreasingIncome++;
 				}
 			}
 		}
 		
 		/* If it happens to be a low-income, large(est) family size, and we've 
 		 * STILL not been able to get a QT, increase the income by one class, 
 		 * and decrease the family size by one. */
 		if(newId == null){
			int incomeIndex = getHouseholdIncomeIndex(sa[2]);
			int hhsIndex = getHouseholdSizeIndex(sa[3]);
			if(incomeIndex < (incomeClasses.length-2) && hhsIndex == (householdSizeClasses.length-1) ){
				Id tempId = new IdImpl(sa[0] + "_" + sa[1] + "_" + incomeClasses[incomeIndex+1] + "_" + String.format("%02d", Integer.parseInt(householdSizeClasses[hhsIndex-1])) );
				if(qtMap.containsKey(tempId)){
					newId = tempId;
					fixedByIncreasingIncomeReducingSize++;
				}
			}
 		}
 		
 		/* Employed children, up to and including 16 years, have their
 		 * employment status changed. Household size reduced one class; 
 		 */
 		if(newId == null){
 			int ageIndex = getAgeIndex(sa[1]);
			int hhsIndex = getHouseholdSizeIndex(sa[3]);
 			if(ageIndex < 3 && sa[0].equalsIgnoreCase("y")){
 				Id tempId = new IdImpl("n" + "_" + sa[1] + "_" + sa[2] + "_" + String.format("%02d", Integer.parseInt(householdSizeClasses[hhsIndex-1])) );
				if(qtMap.containsKey(tempId)){
					newId = tempId;
					fixedByPuttingKidsOutOfWork++;
				}
 			}
 		}
		
		if(newId == null){
			nullQtCount++;
		}
		
		return newId;
	}
	
	
	private int getHouseholdIncomeIndex(String income){
		int index = -1;
		for(int i = incomeClasses.length-1; i >= 0; i--){
			if(Double.parseDouble(incomeClasses[i]) == Double.parseDouble(income)){
				index = i;
				break;
			}
		}
		if(index == -1){
			LOG.error("Couldn't find the income index for the value " + income);
		}
		return index;
	}
	
	
	private int getHouseholdSizeIndex(String size){
		int index = -1;
		for(int i = householdSizeClasses.length-1; i >= 0; i--){
			if(Double.parseDouble(householdSizeClasses[i]) == Double.parseDouble(size)){
				index = i;
				break;
			}
		}
		if(index == -1){
			LOG.error("Couldn't find the household size index for the value " + size);
		}
		return index;

	}

	
	private int getAgeIndex(String age){
		int index = -1;
		for(int i = ageClasses.length-1; i >= 0; i--){
			if(Double.parseDouble(ageClasses[i]) == Double.parseDouble(age)){
				index = i;
				break;
			}
		}
		if(index == -1){
			LOG.error("Couldn't find the age index for the value " + age);
		}
		return index;

	}

	
	private int getGenderIndex(String gender){
		int index = -1;
		for(int i = genderClasses.length-1; i >= 0; i--){
			if(genderClasses[i].equalsIgnoreCase(gender)){
				index = i;
				break;
			}
		}
		if(index == -1){
//			LOG.error("Couldn't find the age index for the value " + gender);
		}
		return index;

	}

}

