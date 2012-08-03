/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.acmarmol.microcensus2010;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;


/**
* 
* Parses the zielpersonen.dat file from MZ2010, creates matsim persons and adds them to the matsim population.
* Also fills the population attributes with the microcensus information.
* 
* @see org.matsim.utils.objectattributes 
*
* @author acmarmol
* 
*/
	
public class MZZielPersonParser {
	
//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private Households households;
	private ObjectAttributes householdAttributes;
	private Population population;
	private ObjectAttributes populationAttributes;
	
	private static final String HOME = "home";
	
	private static final String ALWAYS = "always";
	private static final String ARRANGEMENT = "by arrengement";
	private static final String NEVER = "never";
	private static final String NO_ANSWER = "no answer";
	private static final String UNSPECIFIED = "unspecified";
	private static final String NOT_KNOWN = "not known";
	
	private static final String YES = "yes";
	private static final String NO = "no";

//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZZielPersonParser(Population population, ObjectAttributes populationAttributes,  Households households, ObjectAttributes householdAttributes) {
	super();
	this.households = households;
	this.householdAttributes = householdAttributes;
	this.population = population;
	this.populationAttributes = populationAttributes;

}	


//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////
	
	public void parse(String zielpersonenFile) throws Exception{
		
		FileReader fr = new FileReader(zielpersonenFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
				
		while ((curr_line = br.readLine()) != null) {
			
		String[] entries = curr_line.split("\t", -1);
		
		//household number & person number
		String hhnr = entries[0].trim();
		String zielpnr = entries[1].trim();
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "household number", hhnr);
		
		//person weight 
		String person_weight = entries[2];
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "person weight", person_weight);
		
		//person age 
		String age = entries[188];
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "age", age);
		
		//person gender
		String gender = entries[190];
		if(gender.equals("1")){gender = "male";}
		else if(gender.equals("2")){gender = "female";}
		else Gbl.errorMsg("This should never happen!  Gender: " + gender+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "gender", gender);
		
		//day of week
		String dow = entries[10];
		if(dow.equals("1")){dow = "monday";}
		else if(dow.equals("2")){dow = "tuesday";}		else if(dow.equals("3")){dow = "wednesday";}
		else if(dow.equals("4")){dow = "thurdsday";}	else if(dow.equals("5")){dow = "friday";}
		else if(dow.equals("6")){dow = "saturday";}		else if(dow.equals("7")){dow = "sunday";}
		else Gbl.errorMsg("This should never happen!  Day of week: " + dow + " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "day of week", dow);

		
		//employment status
		boolean employed = true;		
		String employment_status = entries[177];
		
		if(!employment_status.equals(" ")){
		if(Integer.parseInt(employment_status)>4){employed = false;}
		}
				
		if(employment_status.equals("1")){employment_status = "independent";}
		else if(employment_status.equals("2")){employment_status = "Mitarbeitendes Familienmitglied";} 	else if(employment_status.equals("3")){employment_status = "employee";}
		else if(employment_status.equals("4")){employment_status = "apprentice-trainee"	;}				else if(employment_status.equals("5")){employment_status = "unemployed";}
		else if(employment_status.equals("6")){employment_status = "not in labor force";}				else if(employment_status.equals("7")){employment_status = "retired";}
		else if(employment_status.equals("8")){employment_status = "disabled";}							else if(employment_status.equals("9")){employment_status = "housewife/hosehusband";}
		else if(employment_status.equals("10")){employment_status = "other inactive";}					else if(employment_status.equals(" ")){employment_status = "unspecified";}
		else Gbl.errorMsg("This should ne ver happen! Employment Status: " + employment_status + " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "work: employment status", employment_status);
		
		//level of employment
		String level_employment = entries[179];
		if(level_employment.equals("1")){level_employment = "90-100%";}
		else if(level_employment.equals("2")){level_employment = "70-89%";}								else if(level_employment.equals("3")){level_employment = "50-69%";}
		else if(level_employment.equals("4")){level_employment = "less than 50%";}						else if(level_employment.equals("99")){level_employment = "part-time unspecified";}
		else if(level_employment.equals("999")){level_employment = "unemployed";}						else if(level_employment.equals(" ")){level_employment = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen! Level of Employment: " + level_employment + " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "work: level of employment", level_employment);
		
		// work location coordinate (round to 1/10 of hectare) - WGS84 (124,125) & CH1903 (126,127)
		if(employed){
		Coord work_location = new CoordImpl(entries[126].trim(),entries[127].trim());
		//work_location.setX(Math.round(work_location.getX()/10.0)*10);
		//work_location.setY(Math.round(work_location.getY()/10.0)*10);
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "work: location coord", work_location);
		} //else?
		
		//car availability
		String car_av = entries[63];
		if(car_av.equals("1")){car_av = ALWAYS;}
		else if(car_av.equals("2")){car_av = ARRANGEMENT;}
		else if(car_av.equals("3")){car_av = NEVER;}
		else if(car_av.equals("-99")){car_av = "???";}// -review
		else if(car_av.equals("-98")){car_av = NO_ANSWER;}
		else if(car_av.equals("-97")){car_av = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen!  Car availability: " + car_av+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "availability: car", car_av);
		
		//motorcycle availability
		String mcycle_av = entries[62];
		if(mcycle_av.equals("1")){mcycle_av = ALWAYS;}
		else if(mcycle_av.equals("2")){mcycle_av = ARRANGEMENT;}
		else if(mcycle_av.equals("3")){mcycle_av = NEVER;}
		else if(mcycle_av.equals("-99")){mcycle_av = "???";}// -review
		else if(mcycle_av.equals("-98")){mcycle_av = NO_ANSWER;}
		else if(mcycle_av.equals("-97")){mcycle_av = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen!  Motorcycle availability: " + mcycle_av+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "availability: motorcycle", mcycle_av);
		
		//small motorcycle availability
		String smcycle_av = entries[61];
		if(smcycle_av.equals("1")){smcycle_av = ALWAYS;}
		else if(smcycle_av.equals("2")){smcycle_av = ARRANGEMENT;}
		else if(smcycle_av.equals("3")){smcycle_av = NEVER;}
		else if(smcycle_av.equals("-99")){smcycle_av = "age less than 16";}
		else if(smcycle_av.equals("-98")){smcycle_av = NO_ANSWER;}
		else if(smcycle_av.equals("-97")){smcycle_av = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen!  Small motorcycle availability: " + smcycle_av+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "availability: small motorcycle ", smcycle_av);
		
		
		//Mofa availability
		String mofa_av = entries[60];
		if(mofa_av.equals("1")){mofa_av = ALWAYS;}
		else if(mofa_av.equals("2")){mofa_av = ARRANGEMENT;}
		else if(mofa_av.equals("3")){mofa_av = NEVER;}
		else if(mofa_av.equals("-99")){mofa_av = "age less than 14";}
		else if(mofa_av.equals("-98")){mofa_av = NO_ANSWER;}
		else if(mofa_av.equals("-97")){mofa_av = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen!  Mofa availability: " + mofa_av+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "availability: mofa", mofa_av);
		
		//Bicycle availability
		String bike_av = entries[59];
		if(bike_av.equals("1")){bike_av = ALWAYS;}
		else if(bike_av.equals("2")){bike_av = ARRANGEMENT;}
		else if(bike_av.equals("3")){bike_av = NEVER;}
		else if(bike_av.equals("-99")){bike_av = UNSPECIFIED;}// -review
		else if(bike_av.equals("-98")){bike_av = NO_ANSWER;}
		else if(bike_av.equals("-97")){bike_av = UNSPECIFIED;}
		else Gbl.errorMsg("This should never happen!  Bike availability: " + bike_av+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "availability: bicycle", bike_av);
		
		//car-sharing membership
		String sharing = entries[56];
		if(sharing .equals("1")){sharing  = YES;}
		else if(sharing.equals("2")){sharing  = NO;}
		else if(sharing.equals("-99")){sharing = "???";}// -review
		else if(sharing.equals("-98")){sharing = NO_ANSWER;}
		else if(sharing.equals("-97")){sharing = NOT_KNOWN;}	
		else Gbl.errorMsg("This should never happen!  Car sharing membership: " + sharing + " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "car sharing membership", sharing);
		
		//HalbTax
		String halbtax = entries[48];
		if(halbtax.equals("1")){halbtax = YES;}
		else if(halbtax.equals("2")){halbtax = NO;}
		else if(halbtax.equals("-98")){halbtax = NO_ANSWER;}
		else if(halbtax.equals("-97")){halbtax = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  Halbtax: " + halbtax+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "abonnement: Halbtax", halbtax);
		
		//GA first class
		String gaFirstClass = entries[49];
		if(gaFirstClass.equals("1")){gaFirstClass = YES;} 
		else if(gaFirstClass.equals("2")){gaFirstClass = NO;}
		else if(gaFirstClass.equals("-98")){gaFirstClass = NO_ANSWER;}
		else if(gaFirstClass.equals("-97")){gaFirstClass = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  GA First Class: " + gaFirstClass+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "abonnement: GA first class", gaFirstClass);
		
		//GA second class
		String gaSecondClass = entries[50];
		if(gaSecondClass.equals("1")){gaSecondClass = YES;}
		else if(gaSecondClass.equals("2")){gaSecondClass = NO;}
		else if(gaSecondClass.equals("-98")){gaSecondClass = NO_ANSWER;}
		else if(gaSecondClass.equals("-97")){gaSecondClass = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  GA Second Class: " + gaSecondClass+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "abonnement: GA second class", gaSecondClass);
		
		
		//verbund abonnement
		String verbund = entries[51];
		if(verbund.equals("1")){verbund = YES;}
		else if(verbund.equals("2")){verbund = NO;}
		else if(verbund.equals("-98")){verbund = NO_ANSWER;}
		else if(verbund.equals("-97")){verbund = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  Verbund abonnement: " + verbund+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "abonnement: Verbund", verbund);
		
		//strecken abonnement
		String strecken = entries[52];
		if(strecken.equals("1")){strecken = YES;}
		else if(strecken.equals("2")){strecken = NO;}
		else if(strecken.equals("-98")){strecken = NO_ANSWER;}
		else if(strecken.equals("-97")){strecken = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  GA Second Class: " + strecken+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "abonnement: Stecken", strecken);
		
		
		//Gleis 7
		String gleis7 = entries[53];
		if(gleis7.equals("1")){gleis7 = YES;}
		else if(gleis7.equals("2")){gleis7 = NO;}
		else if(gleis7.equals("-99")){gleis7 = "not in age";}
		else if(gleis7.equals("-98")){gleis7 = NO_ANSWER;}
		else if(gleis7.equals("-97")){gleis7 = NOT_KNOWN;}
		else Gbl.errorMsg("This should never happen!  Gleis 7: " + gleis7+ " doesn't exist");
		populationAttributes.putAttribute(hhnr.concat(zielpnr), "abonnement: Gleis 7", gleis7);
		
		
		//creating matsim person
		PersonImpl person = new PersonImpl(new IdImpl(hhnr.concat(zielpnr)));
		person.setAge(Integer.parseInt(age));
		person.setEmployed(employed);
		//person.setLicence(licence);
		person.setSex(gender);
		population.addPerson(person);
		}
			
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # persons parsed = "  + population.getPersons().size());
		System.out.println();
	
	}
	
	
}
