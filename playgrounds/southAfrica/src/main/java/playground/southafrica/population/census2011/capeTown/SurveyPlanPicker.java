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

package playground.southafrica.population.census2011.capeTown;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import playground.southafrica.population.capeTownTravelSurvey.PersonEnums;
import playground.southafrica.population.census2011.containers.Income2011;
import playground.southafrica.population.demographics.SaDemographicsAge;
import playground.southafrica.population.demographics.SaDemographicsEmployment;
import playground.southafrica.population.demographics.SaDemographicsHouseholdSize;
import playground.southafrica.population.demographics.SaDemographicsIncome;
import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.RandomPermutation;

/**
 * Class to assign the travel demand, i.e. activity chains, observed in the 
 * Travel Survey of 2013 for City of Cape Town, to a synthetic population of
 * agents for the same area. 
 * 
 * <h5>Note:</h5> The output coordinate reference system of the population is
 * changed to that of the travel diary. If that is a problem, you need to 
 * manually transform the coordinates outside of this class.
 *
 * @author jwjoubert
 */
public class SurveyPlanPicker {
	private final static Logger LOG = Logger.getLogger(SurveyPlanPicker.class);
	
	private ComprehensivePopulationReader surveyPopulation;
	private ComprehensivePopulationReader censusPopulation;
	
	private double[] qtExtent;
	private Map<String, QuadTree<Plan>> qtMap = new TreeMap<String, QuadTree<Plan>>();
	
	/*TODO Remove the dummy check variables. */
	int nullQtCount = 0;
	private Map<String, Integer> hammingDistanceChanges = new TreeMap<String, Integer>();
	private List<Double> distanceList = new ArrayList<Double>();

	public static void main(String[] args){
		Header.printHeader(SurveyPlanPicker.class.toString(), args);
		MatsimRandom.reset();
		
		String surveyPopulationFolder = args[0];
		String populationFolder = args[1];
		String areaShapefile = args[2]; 
		String populationCRS = args[3];
		String surveyCRS = args[4];
		
		SurveyPlanPicker spp = new SurveyPlanPicker(areaShapefile);
		spp.buildQuadTreeFromSurvey(surveyPopulationFolder);
		spp.pickActivityChainsForPopulation(populationFolder, populationCRS, surveyCRS);
		
		/* Write the adapted population. Only the people's activity chains have
		 * been adapted, so we need not write all of the files. */
		PopulationWriter pw = new PopulationWriter(spp.censusPopulation.getScenario().getPopulation());
		pw.write(populationFolder + (populationFolder.endsWith("/") ? "" : "/") + "population_withPlans.xml.gz");

		Header.printFooter();
	}
	
	
	/**
	 * Class to build a {@link QuadTree} of plans for different population
	 * profiles.
	 * 
	 * @param areaShapefile the path to the shapefile that will be used as 
	 * extent for the {@link QuadTree}s of different demographic signatures. 
	 * It is important that this shapefile must be in the same Coordinate 
	 * Reference System (CRS) as the plans that represent the travel diary.
	 */
	public SurveyPlanPicker(String areaShapefile) {
		/* Get the QuadTree extent. */
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(areaShapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		for(SimpleFeature f : features){
			Object o = f.getDefaultGeometry();
			if(o instanceof Geometry){
				Geometry g = (Geometry)o;
				Coordinate[] ca = g.getEnvelope().getCoordinates();
				minX = Math.min(minX, ca[0].x);
				minY = Math.min(minY, ca[0].y);
				maxX = Math.max(maxX, ca[2].x);
				maxY = Math.max(maxY, ca[2].y);
			}
		}
		double[] extent = {minX, minY, maxX, maxY};
		this.qtExtent = extent;
	}
	
	
	/**
	 * Parses the survey population and builds QuadTrees for different 
	 * demographic classifications.
	 * 
	 * @param surveyPopulationFolder
	 */
	public void buildQuadTreeFromSurvey(String surveyPopulationFolder){
		/* Parse the survey population */
		LOG.info("Parse survey population...");
		this.surveyPopulation = new ComprehensivePopulationReader();
		this.surveyPopulation.parse(surveyPopulationFolder);
		
		/* Put each person's surveyed plan in an appropriate QuadTree. */
		LOG.info("Building QuadTree from survey population...");
		Counter counter = new Counter("  persons placed # ");
		for(Id<Person> personId : this.surveyPopulation.getScenario().getPopulation().getPersons().keySet()){
			/* Get the selected plan (and make a copy of it) */
			Person person = this.surveyPopulation.getScenario().getPopulation().getPersons().get(personId);
			Plan plan = person.createCopyOfSelectedPlanAndMakeSelected();
			
			Coord home = getQtPlanHomeCoordinate(plan);
			
			/* Get the person's demographic 'signature' */
			ObjectAttributes personAttributes = this.surveyPopulation.getScenario().getPopulation().getPersonAttributes();
			Object oEmplyment = personAttributes.getAttribute(person.getId().toString(), "employment");
			String employment = null;
			if(oEmplyment instanceof String){
				employment = (String)oEmplyment;
			} else{
				LOG.error("Could not get an appropriate String for employment status: person " + person.getId().toString());
			}
			String a = SaDemographicsEmployment.convertCapeTown2011Employment(employment).toString();
			
			Object oAge = personAttributes.getAttribute(person.getId().toString(), "yearOfBirth");
			String age = null;
			if(oAge instanceof String){
				age = (String)oAge;
			} else{
				LOG.error("Could not get an appropriate String for age: person " + person.getId().toString());
			}
			String ageGroup = PersonEnums.AgeGroup.parseFromBirthYear(age).getDescription();
			String b = SaDemographicsAge.getCapeTown2013AgeClass(ageGroup).toString();
			
			String sId = person.getId().toString();
			String[] sa = sId.split("_");
			Id<Household> hhid = Id.create(sa[0], Household.class);
			Household household = surveyPopulation.getScenario().getHouseholds().getHouseholds().get(hhid);
			String c = SaDemographicsHouseholdSize.getHouseholdSizeClass( household.getMemberIds().size() ).toString();
			
			ObjectAttributes hhAttr = this.surveyPopulation.getScenario().getHouseholds().getHouseholdAttributes();
			String incomeString = null;
			String class2String = null;
			Object oIncome = hhAttr.getAttribute(hhid.toString(), "income");
			if(oIncome instanceof String){
				incomeString = (String) oIncome;
			} else{
				LOG.error("Could not get an appropriate String for monthly income: household " + hhid.toString());
			}
			Object oClass2 = hhAttr.getAttribute(hhid.toString(), "assetClassMethod2");
			if(oClass2 instanceof String){
				class2String = (String) oClass2;
			} else{
				LOG.error("Could not get an appropriate String for asset class 2: household " + hhid.toString());
			}
			String d = SaDemographicsIncome.convertCapeTown2013Income(incomeString, class2String).toString();
			String signature = String.format("%s_%s_%s_%s", a, b, c, d);

			/* Put the person's plan in the 'signature' QuadTree, but only if 
			 * it has a home location. */
			if(home != null){
				if(!qtMap.containsKey(signature)){
					qtMap.put(signature, new QuadTree<Plan>(qtExtent[0], qtExtent[1], qtExtent[2], qtExtent[3]));
				}
				qtMap.get(signature).put(home.getX(), home.getY(), plan);
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done building QuadTree. Total of " + qtMap.size() + " signature QuadTrees");
		for(String s : qtMap.keySet()){
			LOG.info("  |_ " + s + ": " + qtMap.get(s).size() + " observations");
		}
	}
	
	
	public void pickActivityChainsForPopulation(String populationFolder, String originalCRS, String targetCRS){
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(originalCRS, targetCRS);
		/* Parse the population for which plans are sought. */
		LOG.info("Parsing population that must be assigned plans...");
		censusPopulation = new ComprehensivePopulationReader();
		censusPopulation.parse(populationFolder);
		
		/*TODO Variables for debugging. Can be removed once validated. */
		Map<String, Integer> noQtMap = new TreeMap<String, Integer>();
		
		/* Get each person's best-match plan. */
		Counter counter = new Counter(" plans picked # ");
		LOG.info("Picking plans for persons...");
		for(Id<Person> personId : censusPopulation.getScenario().getPopulation().getPersons().keySet()){
			Person person = censusPopulation.getScenario().getPopulation().getPersons().get(personId);
			/* Remove all the existing plans the person may have. */
			person.getPlans().clear();
			
			/* Get the household's home coordinate. */
			Id<Household> hhid = Id.create( (String) censusPopulation.getScenario().getPopulation().getPersonAttributes().getAttribute(personId.toString(), "householdId") , Household.class);
			Coord home = ct.transform( (Coord) censusPopulation.getScenario().getHouseholds().getHouseholdAttributes().getAttribute(hhid.toString(), "homeCoord") );
			
			/* Get person's demographic 'signature' */
			String a = SaDemographicsEmployment.convertCensus2011Employment( PersonUtils.isEmployed(person) ).toString();
			String b = SaDemographicsAge.getAgeClass( PersonUtils.getAge(person) ).toString();
			Household household = censusPopulation.getScenario().getHouseholds().getHouseholds().get(hhid);
			String c = SaDemographicsHouseholdSize.getHouseholdSizeClass( household.getMemberIds().size() ).toString();
			String d = SaDemographicsIncome.convertCensus2011Income( Income2011.getIncomeEnum(household.getIncome()) ).toString();
			String signature = String.format("%s_%s_%s_%s", a, b, c, d);
			
			/* Find the QuadTree that best match the demographic 'signature'. */
			QuadTree<Plan> qt = null;
			String closestSignature = findClosestSignature(signature);
			if(closestSignature != null){
				qt = qtMap.get(closestSignature);
				List<Tuple<Plan, Double>> closestPlans = this.getClosestPlans(home, qt, 20);

				/* Randomly pick any of the closest plans, and make a COPY of it. */
				Tuple<Plan, Double> randomTuple = closestPlans.get( RandomPermutation.getRandomPermutation(closestPlans.size())[0]-1 );
				PlanImpl plan = new PlanImpl();
				plan.copyFrom(randomTuple.getFirst());
			
				distanceList.add(randomTuple.getSecond());

				/* Should have a plan now. Change its home locations. */
				for(PlanElement pe : plan.getPlanElements()){
					if(pe instanceof ActivityImpl){
						ActivityImpl activity = (ActivityImpl) pe;
						/* Set the home location */
						if(activity.getType().equalsIgnoreCase("h")){
							activity.setCoord(home);
						}
					}
				}
				person.addPlan(plan);
				person.setSelectedPlan(plan);
			} else{
				/* FIXME Why are there persons without a valid demographic signature. */
				{
					/*TODO Debugging: can be removed once validated. */
					if(!noQtMap.containsKey(signature)){
						noQtMap.put(signature, 0);
					}
					noQtMap.put(signature, noQtMap.get(signature)+1);
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done picking plans.");
		
		/*=====================================================================*/
		/* TODO Keep, or remove, these dummy statistics used for fault-finding. */
		/*=====================================================================*/
		LOG.info("Number of times no QT class was found: " + nullQtCount);
		int noQtCounter = 1;
		for(String s : noQtMap.keySet()){
			LOG.info("   " + noQtCounter++ + ". " +  s + " (" + noQtMap.get(s) + ")");
		}
		/*=====================================================================*/
		LOG.info("Hamming distance fixes applied:");
		for(int i = 0; i < hammingDistanceChanges.size(); i++){
			LOG.info("   " + i + ": " + hammingDistanceChanges.get(String.valueOf(i)) );
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
			/* The closest plan's home coordinate. */
			Coord closestHome = getQtPlanHomeCoordinate(closestPlan);
			
			double radius = CoordUtils.calcEuclideanDistance(c, closestHome );
			Collection<Plan> plans = qt.getDisk(c.getX(), c.getY(), radius);
			while(plans.size() < number){
				/* Double the radius. If the radius happens to be zero (0), 
				 * then you stand the chance of running into an infinite loop.
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
			/* Get the plan's home coordinate. */
			Coord planHome = getQtPlanHomeCoordinate(plan);
			
			double d = CoordUtils.calcEuclideanDistance(c, planHome);
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
	 * The person plans from the Survey data were not put in QuadTree at the 
	 * location of the plan's first activity. Rather, it was placed at the 
	 * location of the first home ('h') activity. This class searches for the
	 * first home activity in the plan.
	 * 
	 * @param closestPlan
	 * @return the {@link Coord}inate of the first home activity, or 
	 * <code>null</code> if the plan does not contain a home activity.
	 */
	private Coord getQtPlanHomeCoordinate(Plan closestPlan) {
		Coord closestHome = null;
		Iterator<PlanElement> iterator = closestPlan.getPlanElements().iterator();
		while(closestHome == null && iterator.hasNext()){
			PlanElement pe = iterator.next();
			if(pe instanceof Activity){
				Activity act = (Activity)pe;
				if(act.getType().equalsIgnoreCase("h")){
					closestHome = act.getCoord();
				}
			}
		}
		return closestHome;
	}
	
			
	/**
	 * The demographic 'signature' is made up of the following elements:
	 * <ol>
	 * 		<li> employment class;
	 * 		<li> age class;
	 * 		<li> household size class; and
	 * 		<li> household income class;
	 * </ol>
	 * This method takes an existing signature and makes (zero or more) 
	 * perturbations until a valid signature is found for which a {@link QuadTree}
	 * exists.
	 * 
	 * @param signature
	 * @return
	 */
	public String findClosestSignature(String signature){
		/* If the signature is already associated with a QuadTree, no change 
		 * is required. Return the current signature as is. */
		if(qtMap.containsKey(signature)){
			if(!hammingDistanceChanges.containsKey("0")){
				hammingDistanceChanges.put("0", new Integer(0));
			}
			hammingDistanceChanges.put("0", hammingDistanceChanges.get("0")+1);
			return signature;
		}
		
		/* Perform perturbations to the signature. */
		String newSignature = null;
		int maximumTriesAtCurrentHammingDistance = 500;
		int hammingDistance = 1;

		/* Get the demographics from the current signature. */
		String[] sa = signature.toString().split("_");
		SaDemographicsEmployment employment = SaDemographicsEmployment.valueOf(sa[0]);
		SaDemographicsAge age = SaDemographicsAge.valueOf(sa[1]);
		SaDemographicsHouseholdSize householdSize = SaDemographicsHouseholdSize.valueOf(sa[2]);
		SaDemographicsIncome income = SaDemographicsIncome.valueOf(sa[3]);
		
		String currentSignature;
		do {
			int triesAtCurrentHammingDistance = 0;
			do{
				/* Get a random permutation of the Hamming dimensions. */
				int[] permutation = RandomPermutation.getRandomPermutation(sa.length);
				
				/* Make random changes. The number of changes should be the
				 * same as the current Hamming distance. */
				for(int stepChange = 0; stepChange < hammingDistance; stepChange++){
					int classToChange = permutation[stepChange];
					switch (classToChange) {
					case 0:
						employment = SaDemographicsEmployment.getEmploymentPerturbation(employment);
					case 1:
						age = SaDemographicsAge.getAgePerturbation(age);
					case 2:
						householdSize = SaDemographicsHouseholdSize.getHouseholdSizePerturbation(householdSize);
					case 3:
						income = SaDemographicsIncome.getIncomePerturbation(income);
					}
				}
				
				/* Check if the perturbation has resulted in a known QuadTree. */
				currentSignature = employment.toString() + "_" +
						age + "_" + householdSize + "_" + income;
				newSignature = qtMap.containsKey(currentSignature) ? currentSignature : null;
			} while (newSignature == null &&
					triesAtCurrentHammingDistance++ < maximumTriesAtCurrentHammingDistance);
			
		} while (newSignature == null && ++hammingDistance <= sa.length);

		if(newSignature == null){
			nullQtCount++;
		} else{
			if(!hammingDistanceChanges.containsKey(String.valueOf(hammingDistance))){
				hammingDistanceChanges.put(String.valueOf(hammingDistance), new Integer(0));
			}
			hammingDistanceChanges.put(
					String.valueOf(hammingDistance), 
					hammingDistanceChanges.get(String.valueOf(hammingDistance))+1);
		}
		
		return newSignature;
	}
	
	
}

