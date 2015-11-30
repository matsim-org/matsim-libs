/* *********************************************************************** *
 * project: org.matsim.*
 * EstimateCarOwnership.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.jjoubert.projects.erAfrica;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.utilities.Header;

/**
 * Class to estimate car ownership for households of a given population. The 
 * class assumes that the given population has a {@link Plan} for each person 
 * that indicates the {@link Leg}'s mode.
 * 
 * @author jwjoubert
 */
public class EstimateCarOwnership {
	final private static Logger LOG = Logger.getLogger(EstimateCarOwnership.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(EstimateCarOwnership.class.toString(), args);
		
		/* Parse the population and its attributes. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(args[0]);
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(args[1]);
		new HouseholdsReaderV10(sc.getHouseholds()).parse(args[2]);
		
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(sc.getHouseholds().getHouseholdAttributes());
		oar.putAttributeConverter(Coord.class, new CoordConverter());
		oar.parse(args[3]);
		
		estimateAccessibilityCriteria(sc);
		
		ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(sc.getHouseholds().getHouseholdAttributes());
		oaw.putAttributeConverter(Coord.class, new CoordConverter());
		oaw.writeFile(args[4]);
		
		/* Create an R data set. */
		createRDataset(sc, args[5]);
		
		Header.printFooter();
	}
	
	
	/**
	 * Estimates the following criteria that are used in ERAfrica accessibility
	 * calculations:
	 * <ul>
	 * 		<li> the number of household members with access to a car as 
	 * 		     passenger;
	 * 		<li> the number of household members with access to a car as 
	 * 			 driver; and
	 * 		<li> the number of household members that are school-going.
	 * </ul>
	 *    
	 * @param sc that contains at least the {@link Population} with {@link Plan}s.
	 */
	public static void estimateAccessibilityCriteria(Scenario sc){
		LOG.info("Estimating car ownership...");
		
		int hasCarAccessAsPassenger = 0;
		int hasCarAccessAsDriver = 0;
		int isSchoolGoing = 0;
		
		Counter counter = new Counter("  persons # ");
		for(Id<Person> personId : sc.getPopulation().getPersons().keySet()){
			String householdId = sc.getPopulation().getPersonAttributes().getAttribute(personId.toString(), "householdId").toString();

			Plan plan= sc.getPopulation().getPersons().get(personId).getSelectedPlan();
			
			/* Check basic car access without ownership. */
			if(hasCarAccessAsPassenger(plan)){
				hasCarAccessAsPassenger++;
				updateAttribute(sc, householdId, "membersWithPassengerAccess");
			}
			
			/* Check car access as driver. */
			if(hasCarAccessAsDriver(plan)){
				hasCarAccessAsDriver++;
				updateAttribute(sc, householdId, "membersWithCarAccess");
			}
			
			/* Check if school-going. */
			if(isSchoolGoing(plan)){
				isSchoolGoing++;
				updateAttribute(sc, householdId, "membersThatAreSchoolGoing");
			}
			
			counter.incCounter();
		}
		counter.printCounter();
		
		LOG.info("Done estimating car ownership.");
		LOG.info("---------------------------------------------------------------------");
		LOG.info("Some statistics:");
		LOG.info("       Number of people having driver access to a car: " + hasCarAccessAsDriver);
		LOG.info("    Number of people having passenger access to a car: " + hasCarAccessAsPassenger);
		LOG.info("   Number of people going to primary/secondary school: " + isSchoolGoing);
		LOG.info("---------------------------------------------------------------------");
	}
	
	
	/**
	 * Checks if a person uses a car as passenger. That is, has mode 'ride' in 
	 * its plan.
	 * 
	 * @param plan
	 * @return
	 */
	private static boolean hasCarAccessAsPassenger(Plan plan){
		boolean result = false;
		Iterator<PlanElement> iterator = plan.getPlanElements().iterator();
		while(!result & iterator.hasNext()){
			PlanElement pe = iterator.next();
			if(pe instanceof Leg){
				result = ((Leg)pe).getMode().equalsIgnoreCase("ride");
			}
		}
		return result;
	}

	
	/**
	 * Checks if a person uses a car as driver. That is, has mode 'car' in 
	 * its plan.
	 * 
	 * @param plan
	 * @return
	 */
	private static boolean hasCarAccessAsDriver(Plan plan){
		boolean result = false;
		Iterator<PlanElement> iterator = plan.getPlanElements().iterator();
		while(!result & iterator.hasNext()){
			PlanElement pe = iterator.next();
			if(pe instanceof Leg){
				result = ((Leg)pe).getMode().equalsIgnoreCase("car");
			}
		}
		return result;
	}
	
	
	/**
	 * Checks if a person is school-going. That is, has an activity with type
	 * containing 'e1', which represents primary and secondary education.
	 *   
	 * @param plan
	 * @return
	 */
	private static boolean isSchoolGoing(Plan plan){
		boolean result = false;
		Iterator<PlanElement> iterator = plan.getPlanElements().iterator();
		while(!result & iterator.hasNext()){
			PlanElement pe = iterator.next();
			if(pe instanceof Activity){
				result = ((Activity)pe).getType().contains("e1");
			}
		}
		return result;
	}
	
	
	/**
	 * Checks if a given attribute already exists for a particular hosuehold. 
	 * If not, it is added with value 1. Otherwise it increments the current 
	 * count. If the attribute is known, but with a different {@link Class} 
	 * type, a warning is given.
	 * 
	 * @param sc
	 * @param householdId
	 * @param attributeName
	 */
	private static void updateAttribute(Scenario sc, String householdId, String attributeName){
		Object attribute = sc.getHouseholds().getHouseholdAttributes().getAttribute(householdId, attributeName);
		if(attribute == null){
			sc.getHouseholds().getHouseholdAttributes().putAttribute(householdId, attributeName, 1);
		} else if(attribute instanceof Integer){
			sc.getHouseholds().getHouseholdAttributes().putAttribute(householdId, attributeName, ((Integer)attribute)+1);
		} else{
			LOG.warn("Don't know what to do with '" + attributeName + "' attribute type: " + attribute.getClass().toString());
		}

	}
	
	/**
	 * Writing the main household attributes to flat (CSV) file so they can be
	 * visualised in R, for example.
	 *  
	 * @param sc
	 * @param output
	 */
	private static void createRDataset(Scenario sc, String output){
		LOG.info("Creating a dataset for visualisation in R...");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		Counter counter = new Counter("   household # ");
		try{
			bw.write("householdId,members,passengerAccess,carAccess,schoolGoing,lon,lat,housingType,mainDwelling");
			bw.newLine();
			
			for(Id<Household> householdId : sc.getHouseholds().getHouseholds().keySet()){
				
				int passengerAccess = 0;
				Object oPassengerAccess = sc.getHouseholds().getHouseholdAttributes().getAttribute(householdId.toString(), "membersWithPassengerAccess");
				if(oPassengerAccess instanceof Integer){
					passengerAccess = (int) oPassengerAccess;
				}
				
				int carAccess = 0;
				Object oCarAccess = sc.getHouseholds().getHouseholdAttributes().getAttribute(householdId.toString(), "membersWithCarAccess");
				if(oCarAccess instanceof Integer){
					carAccess = (int) oCarAccess;
				}
				
				int schoolGoing = 0;
				Object oSchoolgoing = sc.getHouseholds().getHouseholdAttributes().getAttribute(householdId.toString(), "membersThatAreSchoolGoing");
				if(oSchoolgoing instanceof Integer){
					schoolGoing = (int) oSchoolgoing;
				}
				
				Coord wgs = null; 
				Object homeCoord = sc.getHouseholds().getHouseholdAttributes().getAttribute(householdId.toString(), "homeCoord");
				if(homeCoord instanceof Coord){
					Coord saAlbers = (Coord)homeCoord;
					wgs = ct.transform(saAlbers);
				} else{
					throw new RuntimeException("Cannot find/convert home coordinate for household " + householdId.toString());
				}
				
				bw.write(String.format("%s,%d,%d,%d,%d,%.6f,%.6f,%s,%s\n",
						householdId.toString(),
						sc.getHouseholds().getHouseholds().get(householdId).getMemberIds().size(),
						passengerAccess,
						carAccess,
						schoolGoing,
						wgs.getX(),
						wgs.getY(),
						sc.getHouseholds().getHouseholdAttributes().getAttribute(householdId.toString(), "housingType").toString(),
						sc.getHouseholds().getHouseholdAttributes().getAttribute(householdId.toString(), "mainDwellingType").toString() ));
				
				counter.incCounter();
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
		counter.printCounter();
		
		LOG.info("Done with R dataset.");
	}
	
}
