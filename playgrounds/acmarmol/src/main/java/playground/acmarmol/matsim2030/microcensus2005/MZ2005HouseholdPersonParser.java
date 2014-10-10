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

package playground.acmarmol.matsim2030.microcensus2005;

import java.io.BufferedReader;
import java.io.FileReader;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
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
public class MZ2005HouseholdPersonParser {
	
//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private Households households;
	private ObjectAttributes householdAttributes;
	private ObjectAttributes householdpersonsAttributes;

//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZ2005HouseholdPersonParser(Households households, ObjectAttributes householdAttributes, ObjectAttributes householdpersonsAttributes) {
	super();
	this.households = households;
	this.householdAttributes = householdAttributes;
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
			String hpnr = entries[2].trim();
			
			
			//age
			String age = entries[4].trim();
			this.householdpersonsAttributes.putAttribute(hhnr.concat(hpnr), MZConstants.AGE, age);
			
			//gender
			String gender = entries[5].trim();
			if(gender.equals("1")){gender=MZConstants.MALE;}
			else if(gender.equals("2")){gender=MZConstants.FEMALE;}
			else{
				throw new RuntimeException("Unknown gender: "+ gender);
			}
			this.householdpersonsAttributes.putAttribute(hhnr.concat(hpnr), MZConstants.GENDER, gender);
			
			//car driving license
			String driving_license = entries[6].trim();
			if(driving_license.equals("1")){driving_license = MZConstants.YES;}
			else{driving_license = MZConstants.NO;}
			this.householdpersonsAttributes.putAttribute(hhnr.concat(hpnr), MZConstants.DRIVING_LICENCE, driving_license);
			
			//motorbike driving license
			String mbike_license = entries[7].trim();
			if(mbike_license.equals("1")){mbike_license = MZConstants.YES;}
			else{mbike_license = MZConstants.NO;}
			this.householdpersonsAttributes.putAttribute(hhnr.concat(hpnr), MZConstants.MOTORBIKE_DRIVING_LICENCE, mbike_license);
					
			
			//filling person data into matsim households
			Id<Household> hhid = Id.create(hhnr, Household.class);
			if(!this.households.getHouseholds().containsKey(hhid)){
				throw new RuntimeException("This should never happen!  Household hhnr: " + hhnr+ " doesn't exist");
			}		
			this.households.getHouseholds().get(hhid).getMemberIds().add(Id.create(hhnr.concat(hpnr), Person.class));  // id = hhnr + hpnr??
		}
		
			
		
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # households people parsed = "  + nr_persons);
		System.out.println();
		
	}
	
}
