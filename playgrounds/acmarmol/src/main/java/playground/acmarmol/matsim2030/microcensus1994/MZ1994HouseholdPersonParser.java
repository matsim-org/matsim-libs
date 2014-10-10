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
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.acmarmol.matsim2030.microcensus2010.MZConstants;

/**
* 
* Parses the haushaltepersonen.dat file from MZ2010,  and adds member ids to matsim households.
*
* @author acmarmol
* 
*/
public class MZ1994HouseholdPersonParser {
	
//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private Households households;
	private ObjectAttributes populationAttributes;
	ObjectAttributes householdpersonsAttributes;
	private Population population;
	

//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZ1994HouseholdPersonParser(Households households,Population population, ObjectAttributes populationAttributes, ObjectAttributes householdpersonsAttributes) {
	super();
	this.households = households;
	this.populationAttributes = populationAttributes;
	this.population = population;
	this.householdpersonsAttributes = householdpersonsAttributes;
	}	


//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////
	
	public void parse(String haushaltspersonenFile) throws Exception{
		
		FileReader fr = new FileReader(haushaltspersonenFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		int nr_persons = 0; 
		
		while ((curr_line = br.readLine()) != null) {
			nr_persons++;	
			String[] entries = curr_line.split("\t", -1);
			
			//household number
			String hhnr = entries[0].trim();
			
			//household person number (hpnr)
			String hpnr = entries[1].trim();
			
			String zp = entries[8];
			String zid = hhnr.concat(zp);
			Id<Household> hhid = Id.create(hhnr, Household.class);
			
			//age
			String age = entries[10].trim();
			this.householdpersonsAttributes.putAttribute(zid, MZConstants.AGE, age);
			
			//gender
			String gender = entries[11].trim();
			if(gender.equals("1")){gender=MZConstants.MALE;}
			else if(gender.equals("2")){gender=MZConstants.FEMALE;}
			else{
				throw new RuntimeException("Unknown gender: "+ gender);
			}
			this.householdpersonsAttributes.putAttribute(zid, MZConstants.GENDER, gender);
			
			//car driving license
			String driving_license = entries[16].trim();
			if(driving_license.equals("1")){driving_license = MZConstants.YES;}
			else{driving_license = MZConstants.NO;}
			this.householdpersonsAttributes.putAttribute(zid, MZConstants.DRIVING_LICENCE, driving_license);
			
			//motorbike driving license
			String mbike_license = entries[17].trim();
			if(mbike_license.equals("1")){mbike_license = MZConstants.YES;}
			else{mbike_license = MZConstants.NO;}
			this.householdpersonsAttributes.putAttribute(zid, MZConstants.MOTORBIKE_DRIVING_LICENCE, mbike_license);
			
			//filling person data into matsim households

			if(!this.households.getHouseholds().containsKey(hhid)){
				throw new RuntimeException("This should never happen!  Household hhnr: " + hhnr+ " doesn't exist");
			}		
			this.households.getHouseholds().get(hhid).getMemberIds().add(Id.create(zid, Person.class));  // id = hhnr + hpnr??
			
			
			if(this.population.getPersons().containsKey(Id.create(zid, Person.class))){
				//HalbTax
				String halbtax = entries[11];
				if(halbtax.equals("1") | halbtax.equals("2")| halbtax.equals("3")){halbtax = MZConstants.YES;}
				else {halbtax = MZConstants.NO;} 
				//else Gbl.errorMsg("This should never happen!  Halbtax: " + halbtax+ " doesn't exist");
				populationAttributes.putAttribute(zid, MZConstants.ABBO_HT, halbtax);
				
				//GA (no distinction first/second class, will be stores as second class)
				String gaSecondClass = entries[13];
				if(gaSecondClass.equals("1")){gaSecondClass = MZConstants.YES;}
				else {gaSecondClass = MZConstants.NO;}
				//else Gbl.errorMsg("This should never happen!  GA Second Class: " + gaSecondClass+ " doesn't exist");
				populationAttributes.putAttribute(zid, MZConstants.ABBO_GA2, gaSecondClass);
				populationAttributes.putAttribute(zid, MZConstants.ABBO_GA1, MZConstants.NO); //completeness
				
				//verbund abonnement
				String verbund = entries[14];
				if(verbund.equals("1")){
					verbund = MZConstants.YES;
					}
				else {verbund = MZConstants.NO;}
				//else Gbl.errorMsg("This should never happen!  Verbund abonnement: " + verbund+ " doesn't exist");
				populationAttributes.putAttribute(zid, MZConstants.ABBO_VERBUND, verbund);
				
				populationAttributes.putAttribute(zid, MZConstants.TOTAL_TRIPS_DISTANCE, "0");
				populationAttributes.putAttribute(zid, MZConstants.TOTAL_TRIPS_DURATION, "0");
				populationAttributes.putAttribute(zid, MZConstants.TOTAL_TRIPS_INLAND, "0");
			}
			
		}
			
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # households people parsed = "  + nr_persons);
		System.out.println();
		
	}
	
}
