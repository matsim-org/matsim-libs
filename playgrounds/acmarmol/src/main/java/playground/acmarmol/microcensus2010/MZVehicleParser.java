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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.Vehicles;

/**
* 
* Parses the fahrzeuge.dat file from MZ2010, creates matsim vehicles and adds them to matsim households.
* Also fills the vehicle attributes with the microcensus information.
* 
* @see org.matsim.utils.objectattributes 
*
* @author acmarmol
* 
*/

public class MZVehicleParser {
	

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private Households households;
	private ObjectAttributes householdAttributes;
	private Vehicles vehicles;
	private ObjectAttributes vehiclesAttributes;

	private static final String NOT_KNOWN = "not known";
	private static final String UNSPECIFIED = "unspecified";

//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZVehicleParser(Vehicles vehicles, ObjectAttributes vehiclesAttributes, Households households, ObjectAttributes householdAttributes) {
	super();
	this.households = households;
	this.householdAttributes = householdAttributes;
	this.vehicles = vehicles;
	this.vehiclesAttributes = vehiclesAttributes;
	
	}	


//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////
	
	public void parse(String fahrzeugeFile) throws Exception{
		
		FileReader fr = new FileReader(fahrzeugeFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		
			while ((curr_line = br.readLine()) != null) {
				
			String[] entries = curr_line.split("\t", -1);
			
			//household number
			String hhnr = entries[0].trim();
			
			//vehicle number
			String fznr = entries[2].trim();
			
			//type
			String type = entries[3].trim();
			if(type.equals("1")){type = "Auto";}
			else if(type.equals("2")){type = "Motorcycle";}
			else Gbl.errorMsg("This should never happen!  Vehicle type: " + type+ " doesn't exist");
	
			vehiclesAttributes.putAttribute(hhnr.concat(fznr), "type", type);
			
			//type of fuel
			String fuel = entries[4].trim();
			if(fuel.equals("1")){fuel = "benzin";}
			else if(fuel.equals("2")){fuel = "diesel";}
			else if(fuel.equals("3")){fuel = "hybridE85Gas";}
			else if(fuel.equals("4")){fuel = "other";}
			else if(fuel.equals("-98")){fuel = UNSPECIFIED;}
			else if(fuel.equals("-97")){fuel = NOT_KNOWN;}
			else if(fuel.equals("-99")){fuel = "FrageNurBeiAuto";} // -review
			else Gbl.errorMsg("This should never happen!  Fuel type: " + fuel+ " doesn't exist");
			vehiclesAttributes.putAttribute(hhnr.concat(fznr), "fuel type", fuel);
			
			//year of registration
			String year = entries[6].trim();
			if(year.equals("-98")){year = UNSPECIFIED;}
			else if(year.equals("-97")){year = NOT_KNOWN;}
			vehiclesAttributes.putAttribute(hhnr.concat(fznr), "year of registration", year);
			
			//month of registration
			String month = entries[7].trim();
			if(month.equals("1")){month = "january";}else if(month.equals("2")){month = "febrary";}
			else if(month.equals("3")){month = "march";}else if(month.equals("4")){month = "april";}
			else if(month.equals("5")){month = "may";}else if(month.equals("6")){month = "june";}
			else if(month.equals("7")){month = "july";}else if(month.equals("8")){month = "august";}
			else if(month.equals("9")){month = "september";}else if(month.equals("10")){month = "october";}
			else if(month.equals("11")){month = "november";}else if(month.equals("12")){month = "december";}
			else if(month.equals("-98")){month = UNSPECIFIED;} 
			else if(month.equals("-97")){month = NOT_KNOWN;}
			else if(month.equals("-99")){month = "NichtÜberMofis";} // -review
			else Gbl.errorMsg("This should never happen!  Month: " + month+ " doesn't exist");
			vehiclesAttributes.putAttribute(hhnr.concat(fznr), "month of registration", month);
			
						
			// creating matsim vehicle
			Vehicle vehicle = vehicles.getFactory().createVehicle(new IdImpl(hhnr.concat(fznr)), new VehicleTypeImpl(new IdImpl(type))); 
			vehicles.getVehicles().put(vehicle.getId(), vehicle);
			
			
			//filling vehicles data into matsim households
			IdImpl hhid = new IdImpl(hhnr);
			if(!this.households.getHouseholds().containsKey(hhid)){
				Gbl.errorMsg("This should never happen!  Household hhnr: " + hhnr+ " doesn't exist");
			}		
			this.households.getHouseholds().get(hhid).getVehicleIds().add(new IdImpl(hhnr.concat(fznr)));  // id = hhnr + fznr??
			
			
		}//end while
		
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # vehicles parsed = "  + vehicles.getVehicles().size());
		System.out.println();
		
	}

}
