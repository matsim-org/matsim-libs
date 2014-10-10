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

package playground.acmarmol.matsim2030.microcensus1994;

import java.io.BufferedReader;
import java.io.FileReader;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.acmarmol.matsim2030.microcensus2010.MZConstants;


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
	
public class MZ1994ZielPersonParser {
	
//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private Households households;
	private ObjectAttributes householdAttributes;
	private Population population;
	private ObjectAttributes populationAttributes;
	

//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZ1994ZielPersonParser(Population population, ObjectAttributes populationAttributes,  Households households, ObjectAttributes householdAttributes) {
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
		
		//household number & interview number
		//interview number is used as person ID
		String hhnr = entries[0].trim();
		String zp = entries[12].trim();
		//String intnr = entries[0].trim();
		String intnr = hhnr.concat(zp);
		populationAttributes.putAttribute(intnr, MZConstants.HOUSEHOLD_NUMBER, hhnr);
		
		//person weight 
		String person_weight = entries[11];
		populationAttributes.putAttribute(intnr, MZConstants.PERSON_WEIGHT, person_weight);
		
		//person age 
		String age = entries[18];
		populationAttributes.putAttribute(intnr, MZConstants.AGE, age);
		
		//person gender
		String gender = entries[19];
		if(gender.equals("1")){gender = MZConstants.MALE;}
		else if(gender.equals("2")){gender = MZConstants.FEMALE;} else
			throw new RuntimeException("This should never happen!  Gender: " + gender+ " doesn't exist");
		populationAttributes.putAttribute(intnr, MZConstants.GENDER, gender);
		
		//day of week
		String dow = entries[14];
		if(dow.equals("1")){dow = MZConstants.MONDAY;}
		else if(dow.equals("2")){dow = MZConstants.TUESDAY;}
		else if(dow.equals("3")){dow = MZConstants.WEDNESDAY;}
		else if(dow.equals("4")){dow = MZConstants.THURSDAY;}
		else if(dow.equals("5")){dow = MZConstants.FRIDAY;}
		else if(dow.equals("6")){dow = MZConstants.SATURDAY;}
		else if(dow.equals("7")){dow = MZConstants.SUNDAY;} else
			throw new RuntimeException("This should never happen!  Day of week: " + dow + " doesn't exist");
		populationAttributes.putAttribute(intnr, MZConstants.DAY_OF_WEEK, dow);
		
		
		//employment status
		boolean employed = false;		
		String employment_status = entries[20];
			
		if(!employment_status.equals("1")){
				employed = true;
					
			
		}
				
//		if(employment_status.equals("1")){employment_status = MZConstants.INDEPENDENT;}
//		else if(employment_status.equals("2")){employment_status = MZConstants.MITARBEITENDES;}
//		else if(employment_status.equals("3")){employment_status = MZConstants.EMPLOYEE;}
//		else if(employment_status.equals("4")){employment_status = MZConstants.TRAINEE	;}
//		else if(employment_status.equals("5")){employment_status = MZConstants.UNEMPLOYED;}
//		else if(employment_status.equals("6")){employment_status = MZConstants.NOT_LABOR_FORCE;}	
//		else if(employment_status.equals("7")){employment_status = MZConstants.RETIRED;}
//		else if(employment_status.equals("8")){employment_status = MZConstants.DISABLED;}
//		else if(employment_status.equals("9")){employment_status = MZConstants.HOUSEWIFE_HOUSEHUSBAND;}
//		else if(employment_status.equals("10")){employment_status = MZConstants.OTHER_INACTIVE;}
//		else if(employment_status.equals("-97")){employment_status = MZConstants.UNSPECIFIED;}
//		else Gbl.errorMsg("This should ne ver happen! Employment Status: " + employment_status + " doesn't exist");
//		populationAttributes.putAttribute(intnr, "work: employment status", employment_status);
		
//		//level of employment
//		String level_employment = entries[179];
//		if(level_employment.equals("1")){level_employment = "90-100%";}
//		else if(level_employment.equals("2")){level_employment = "70-89%";}
//		else if(level_employment.equals("3")){level_employment = "50-69%";}
//		else if(level_employment.equals("4")){level_employment = "less than 50%";}
//		else if(level_employment.equals("99")){level_employment = "part-time unspecified";}
//		else if(level_employment.equals("999")){level_employment = MZConstants.UNEMPLOYED;}
//		else if(level_employment.equals(" ")){level_employment = MZConstants.UNSPECIFIED;}
//		else Gbl.errorMsg("This should never happen! Level of Employment: " + level_employment + " doesn't exist");
//		populationAttributes.putAttribute(intnr, "work: level of employment", level_employment);
		
		// work location coordinate (round to 1/10 of hectare) - WGS84 (124,125) & CH1903 (126,127)
//		if(employed){
//		Coord work_location = new CoordImpl(entries[34].trim(),entries[35].trim());
//		//work_location.setX(Math.round(work_location.getX()/10.0)*10);
//		//work_location.setY(Math.round(work_location.getY()/10.0)*10);
//		populationAttributes.putAttribute(intnr, "work: location coord", work_location);
//		} //else?
		
//		//total nr wege inland
//		String t_wege = entries[2];
//		populationAttributes.putAttribute(intnr, MZConstants.TOTAL_TRIPS_INLAND, t_wege);
//		
//		//total wege time
//		String wege_time = entries[140];
//		populationAttributes.putAttribute(intnr, MZConstants.TOTAL_TRIPS_DURATION, wege_time);
//		
//		//total wege distance
//		String wege_dist = entries[139];
//		populationAttributes.putAttribute(intnr, MZConstants.TOTAL_TRIPS_DISTANCE, wege_dist);
		
		
		
		//car driving license
		String licence = entries[22];
		if(licence.equals("1")){
			licence = MZConstants.YES;
		}else {
			licence = MZConstants.NO;
		}
		populationAttributes.putAttribute(intnr, MZConstants.DRIVING_LICENCE, licence);
		
		//car availability
		String car_av = entries[39];
		if(car_av.equals("1")){car_av = MZConstants.ALWAYS;}
		else if(car_av.equals("2")){car_av =MZConstants.ARRANGEMENT;}
		else if(car_av.equals("3")){car_av = MZConstants.NEVER;}
		else if(car_av.equals("9") | car_av.equals(" ")){car_av = MZConstants.NO_ANSWER;} else
			throw new RuntimeException("This should never happen!  Car availability: " + car_av+ " doesn't exist");
		populationAttributes.putAttribute(intnr, MZConstants.CAR_AVAILABILITY, car_av);
		
		//motorcycle availability
		String mcycle_av = entries[38];
		if(mcycle_av.equals("1")){mcycle_av = MZConstants.ALWAYS;}
		else if(mcycle_av.equals("2")){mcycle_av = MZConstants.ARRANGEMENT;}
		else if(mcycle_av.equals("3")){mcycle_av = MZConstants.NEVER;}
		else if(mcycle_av.equals("9") | mcycle_av.equals(" ")){mcycle_av = MZConstants.NO_ANSWER;} else
			throw new RuntimeException("This should never happen!  Motorcycle availability: " + mcycle_av+ " doesn't exist");
		populationAttributes.putAttribute(intnr, MZConstants.MOTORCYCLE_AVAILABILITY, mcycle_av);
		
//		//small motorcycle availability
//		String smcycle_av = entries[65];
//		if(smcycle_av.equals("1")){smcycle_av = MZConstants.ALWAYS;}
//		else if(smcycle_av.equals("2")){smcycle_av = MZConstants.ARRANGEMENT;}
//		else if(smcycle_av.equals("3")){smcycle_av = MZConstants.NEVER;}
//		else if(smcycle_av.equals("4") | smcycle_av.equals("-97") ){smcycle_av = MZConstants.NO_ANSWER;}
//		else if(smcycle_av.equals("-99")){smcycle_av = "???";}
//		else Gbl.errorMsg("This should never happen!  Small motorcycle availability: " + smcycle_av+ " doesn't exist");
//		populationAttributes.putAttribute(intnr, "availability: small motorcycle ", smcycle_av);
//		
		
		//Mofa availability
		String mofa_av = entries[37];
		if(mofa_av.equals("1")){mofa_av = MZConstants.ALWAYS;}
		else if(mofa_av.equals("2")){mofa_av = MZConstants.ARRANGEMENT;}
		else if(mofa_av.equals("3")){mofa_av = MZConstants.NEVER;}
		else if(mofa_av.equals("9") | mofa_av.equals(" ")){mofa_av = MZConstants.NO_ANSWER;} else
			throw new RuntimeException("This should never happen!  Mofa availability: " + mofa_av+ " doesn't exist");
		populationAttributes.putAttribute(intnr, MZConstants.MOFA_AVAILABILITY, mofa_av);
		
		//Bicycle availability
		String bike_av = entries[36];
		if(bike_av.equals("1")){bike_av = MZConstants.ALWAYS;}
		else if(bike_av.equals("2")){bike_av = MZConstants.ARRANGEMENT;}
		else if(bike_av.equals("3")){bike_av = MZConstants.NEVER;}
		else if(bike_av.equals("9") | bike_av.equals(" ")){bike_av = MZConstants.NO_ANSWER;} else
			throw new RuntimeException("This should never happen!  Bike availability: " + bike_av+ " doesn't exist");
		populationAttributes.putAttribute(intnr, MZConstants.BICYCLE_AVAILABILITY, bike_av);
		
//		//car-sharing membership
//		String sharing = entries[56];
//		if(sharing .equals("1")){sharing  = MZConstants.YES;}
//		else if(sharing.equals("2")){sharing  = MZConstants.NO;}
//		else if(sharing.equals("-99")){sharing = "???";}// -review
//		else if(sharing.equals("-98")){sharing = MZConstants.NO_ANSWER;}
//		else if(sharing.equals("-97")){sharing = MZConstants.NOT_KNOWN;}	
//		else Gbl.errorMsg("This should never happen!  Car sharing membership: " + sharing + " doesn't exist");
//		populationAttributes.putAttribute(intnr, "car sharing membership", sharing);
		
//		
//		//GA first class
//		String gaFirstClass;
//		if(abonnement.equals("2")){gaFirstClass = MZConstants.YES;} 
//		else {gaFirstClass = MZConstants.NO;}
//		//else Gbl.errorMsg("This should never happen!  GA First Class: " + gaFirstClass+ " doesn't exist");
//		populationAttributes.putAttribute(intnr, "abonnement: GA first class", gaFirstClass);
//		
//		//GA second class
//		String gaSecondClass = entries[50];
//		if(abonnement.equals("3")){gaSecondClass = MZConstants.YES;}
//		else {gaSecondClass = MZConstants.NO;}
//		//else Gbl.errorMsg("This should never happen!  GA Second Class: " + gaSecondClass+ " doesn't exist");
//		populationAttributes.putAttribute(intnr, "abonnement: GA second class", gaSecondClass);
//		
//		
//		//verbund abonnement
//		String verbund;
//		if(abonnement.equals("4") | abonnement.equals("5") | abonnement.equals("6")|
//				abonnement.equals("12") | abonnement.equals("13") | abonnement.equals("14")){
//			verbund = MZConstants.YES;
//			}
//		else {verbund = MZConstants.NO;}
//		//else Gbl.errorMsg("This should never happen!  Verbund abonnement: " + verbund+ " doesn't exist");
//		populationAttributes.putAttribute(intnr, "abonnement: Verbund", verbund);
//		
//		//strecken abonnement
//		String strecken = entries[52];
//		if(strecken.equals("1")){strecken = MZConstants.YES;}
//		else if(strecken.equals("2")){strecken = MZConstants.NO;}
//		else if(strecken.equals("-98")){strecken = MZConstants.NO_ANSWER;}
//		else if(strecken.equals("-97")){strecken = MZConstants.NOT_KNOWN;}
//		else Gbl.errorMsg("This should never happen!  GA Second Class: " + strecken+ " doesn't exist");
//		populationAttributes.putAttribute(intnr, "abonnement: Stecken", strecken);
//		
//		
//		//Gleis 7
//		String gleis7 = entries[53];
//		if(gleis7.equals("1")){gleis7 = MZConstants.YES;}
//		else if(gleis7.equals("2")){gleis7 = MZConstants.NO;}
//		else if(gleis7.equals("-99")){gleis7 = "not in age";}
//		else if(gleis7.equals("-98")){gleis7 = MZConstants.NO_ANSWER;}
//		else if(gleis7.equals("-97")){gleis7 = MZConstants.NOT_KNOWN;}
//		else Gbl.errorMsg("This should never happen!  Gleis 7: " + gleis7+ " doesn't exist");
//		populationAttributes.putAttribute(intnr, "abonnement: Gleis 7", gleis7);
		
		
		//creating matsim person
		PersonImpl person = new PersonImpl(Id.create(intnr, Person.class));
		person.setAge(Integer.parseInt(age));
		person.setEmployed(employed);
		person.setLicence(licence);
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
