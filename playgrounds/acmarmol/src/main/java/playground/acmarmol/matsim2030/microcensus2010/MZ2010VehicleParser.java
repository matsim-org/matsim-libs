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

package playground.acmarmol.matsim2030.microcensus2010;

import java.io.BufferedReader;
import java.io.FileReader;

import org.matsim.api.core.v01.Id;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
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

public class MZ2010VehicleParser {
	

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private Households households;
	private ObjectAttributes householdAttributes;
	private Vehicles vehicles;
	private ObjectAttributes vehiclesAttributes;


//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZ2010VehicleParser(Vehicles vehicles, ObjectAttributes vehiclesAttributes, Households households, ObjectAttributes householdAttributes) {
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
			if(type.equals("1")){type = MZConstants.CAR;}
			else if(type.equals("2")){type = MZConstants.MOTORCYCLE;} else
				throw new RuntimeException("This should never happen!  Vehicle type: " + type+ " doesn't exist");
			vehiclesAttributes.putAttribute(hhnr.concat(fznr), MZConstants.TYPE, type);
			
			//type of fuel
			String fuel = entries[4].trim();
			if(fuel.equals("1")){fuel = MZConstants.BENZIN;}
			else if(fuel.equals("2")){fuel = MZConstants.DIESEL;}
			else if(fuel.equals("3")){fuel = MZConstants.HYBRIDE85GAS;}
			else if(fuel.equals("4")){fuel = MZConstants.OTHER;}
			else if(fuel.equals("-98")){fuel = MZConstants.UNSPECIFIED;}
			else if(fuel.equals("-97")){fuel = MZConstants.NOT_KNOWN;}
			else if(fuel.equals("-99")){fuel = MZConstants.JUST_FOR_CAR;} else
				throw new RuntimeException("This should never happen!  Fuel type: " + fuel+ " doesn't exist");
			vehiclesAttributes.putAttribute(hhnr.concat(fznr), MZConstants.FUEL_TYPE, fuel);
			
			//year of registration
			String year = entries[6].trim();
			if(year.equals("-98")){year = MZConstants.UNSPECIFIED;}
			else if(year.equals("-97")){year = MZConstants.NOT_KNOWN;}
			vehiclesAttributes.putAttribute(hhnr.concat(fznr), MZConstants.YEAR_REGISTRATION, year);
			
			//month of registration
			String month = entries[7].trim();
			if(month.equals("1")){month = MZConstants.JANUARY;}
			else if(month.equals("2")){month = MZConstants.FEBRUARY;}
			else if(month.equals("3")){month = MZConstants.MARCH;}
			else if(month.equals("4")){month = MZConstants.APRIL;}
			else if(month.equals("5")){month = MZConstants.MAY;}
			else if(month.equals("6")){month = MZConstants.JUNE;}
			else if(month.equals("7")){month = MZConstants.JULY;}
			else if(month.equals("8")){month = MZConstants.AUGUST;}
			else if(month.equals("9")){month = MZConstants.SEPTEMBER;}
			else if(month.equals("10")){month = MZConstants.OCTOBER;}
			else if(month.equals("11")){month = MZConstants.NOVEMBER;}
			else if(month.equals("12")){month = MZConstants.DECEMBER;}
			else if(month.equals("-98")){month = MZConstants.UNSPECIFIED;} 
			else if(month.equals("-97")){month = MZConstants.NOT_KNOWN;}
			else if(month.equals("-99")){month = "NichtÜberMofis";} else
				throw new RuntimeException("This should never happen!  Month: " + month+ " doesn't exist");
			vehiclesAttributes.putAttribute(hhnr.concat(fznr), MZConstants.MONTH_REGISTRATION, month);
			
						
			// creating matsim vehicle
			Vehicle vehicle = vehicles.getFactory().createVehicle(Id.create(hhnr.concat(fznr), Vehicle.class), new VehicleTypeImpl(Id.create(type, VehicleType.class))); 
			vehicles.addVehicle( vehicle);
			
			
			//filling vehicles data into matsim households
			Id<Household> hhid = Id.create(hhnr, Household.class);
			if(!this.households.getHouseholds().containsKey(hhid)){
				throw new RuntimeException("This should never happen!  Household hhnr: " + hhnr+ " doesn't exist");
			}		
			this.households.getHouseholds().get(hhid).getVehicleIds().add(Id.create(hhnr.concat(fznr), Vehicle.class));  // id = hhnr + fznr??
			
			
		}//end while
		
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # vehicles parsed = "  + vehicles.getVehicles().size());
		System.out.println();
		
	}

}
