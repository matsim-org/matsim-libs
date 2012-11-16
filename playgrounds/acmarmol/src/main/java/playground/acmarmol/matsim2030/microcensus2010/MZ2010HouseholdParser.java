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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.IncomeImpl;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
* 
* Parses the haushalte.dat file from MZ2010, creates matsim households, and
* fills the households attributes with the microcensus information.
* 
* @see org.matsim.utils.objectattributes 
*
* @author acmarmol
* 
*/

public class MZ2010HouseholdParser {

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////	
	
	private Households households;
	private ObjectAttributes householdAttributes;
	


//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZ2010HouseholdParser(Households households, ObjectAttributes householdAttributes) {
	super();
	this.households = households;
	this.householdAttributes = householdAttributes;
	
	}	
	
	
//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////


	public void parse(String haushalteFile) throws Exception{
		
		FileReader fr = new FileReader(haushalteFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		
		while ((curr_line = br.readLine()) != null) {
			
		String[] entries = curr_line.split("\t", -1);
		
		//household number
		String hhnr = entries[0].trim();
		
		//household weight 
		String hh_weight = entries[1];
		householdAttributes.putAttribute(hhnr, MZConstants.HOUSEHOLD_WEIGHT, hh_weight);
		
		//household size
		String size = entries[76].trim();
		householdAttributes.putAttribute(hhnr, MZConstants.HOUSEHOLD_SIZE, size);
		
		//household income
		String income = entries[100].trim();
		householdAttributes.putAttribute(hhnr, MZConstants.HOUSEHOLD_INCOME, income);
		
		
		// location coordinate (round to 1/10 of hectare) - WGS84 (5,6) & CH1903 (7,8)
		Coord location = new CoordImpl(entries[7].trim(),entries[8].trim());
		//location.setX(Math.round(location.getX()/10.0)*10);
		//location.setY(Math.round(location.getY()/10.0)*10);
		householdAttributes.putAttribute(hhnr, MZConstants.COORD, location);
		
		//Kanton
		String kanton =  entries[17].trim();
		if(kanton.equals("1")){kanton = "zürich";}						else if(kanton.equals("2")){kanton = "bern";}
		else if(kanton.equals("3")){kanton = "luzern";}					else if(kanton.equals("4")){kanton = "uri";}
		else if(kanton.equals("5")){kanton = "schwyz";}					else if(kanton.equals("6")){kanton = "obwalden";}
		else if(kanton.equals("7")){kanton = "nidwalden";}				else if(kanton.equals("8")){kanton = "glarus";}
		else if(kanton.equals("9")){kanton = "zug";}					else if(kanton.equals("10")){kanton = "fribourg";}
		else if(kanton.equals("11")){kanton = "solothurn";}				else if(kanton.equals("12")){kanton = "basel stadt";}
		else if(kanton.equals("13")){kanton = "basel land";}			else if(kanton.equals("14")){kanton = "schaffhausen";}
		else if(kanton.equals("15")){kanton = "appenzell ausserrhoden";}else if(kanton.equals("16")){kanton = "appenzell innerrhoden";}
		else if(kanton.equals("17")){kanton = "st. gallen";}			else if(kanton.equals("18")){kanton = "graubünden";}
		else if(kanton.equals("19")){kanton = "aargau";}				else if(kanton.equals("20")){kanton = "thurgau";}
		else if(kanton.equals("21")){kanton = "ticino";}				else if(kanton.equals("22")){kanton = "vaud";}
		else if(kanton.equals("23")){kanton = "valais";}				else if(kanton.equals("24")){kanton = "neuchâtel";}
		else if(kanton.equals("25")){kanton = "genève";}				else if(kanton.equals("26")){kanton = "jura";}
		else if(kanton.equals("-97")){kanton = MZConstants.UNSPECIFIED;}		
		householdAttributes.putAttribute(hhnr, MZConstants.CANTON, kanton);
		
		//municipality BFS number
		String municipality =  entries[10].trim();
		householdAttributes.putAttribute(hhnr, MZConstants.MUNICIPALITY, municipality);
		
		
		//municipality BFS number
		String region =  entries[16].trim();
		householdAttributes.putAttribute(hhnr, MZConstants.REGION, region);
		
		//number of cars
		String nr_cars = entries[77];
		if(nr_cars.equals("-98")){nr_cars = MZConstants.NO_ANSWER;}
		else if(nr_cars.equals("-97")){nr_cars = MZConstants.NOT_KNOWN;}
		householdAttributes.putAttribute(hhnr, MZConstants.TOTAL_CARS, nr_cars);
		
		//number of motorcycles
		String nr_mcycles = entries[79];
		if(nr_mcycles.equals("-98")){nr_mcycles = MZConstants.NO_ANSWER;}
		else if(nr_mcycles.equals("-97")){nr_mcycles = MZConstants.NOT_KNOWN;}
		householdAttributes.putAttribute(hhnr, MZConstants.TOTAL_MOTORCYCLES, nr_mcycles);
		
		//number of small motorcycles
		String nr_smcycles = entries[80];
		if(nr_smcycles.equals("-98")){nr_smcycles = MZConstants.NO_ANSWER;}
		else if(nr_smcycles.equals("-97")){nr_smcycles = MZConstants.NOT_KNOWN;}
		householdAttributes.putAttribute(hhnr, MZConstants.TOTAL_SMALL_MOTORCYCLES, nr_smcycles);
		
		//number of mofa
		String nr_mofas = entries[82];
		if(nr_mofas.equals("-98")){nr_mofas = MZConstants.NO_ANSWER;}
		else if(nr_mofas.equals("-97")){nr_mofas = MZConstants.NOT_KNOWN;}
		householdAttributes.putAttribute(hhnr, MZConstants.TOTAL_MOFAS, nr_mofas);
		
		//number of bicycles
		String nr_bikes = entries[83];
		if(nr_bikes.equals("-98")){nr_bikes = MZConstants.NO_ANSWER;}
		else if(nr_bikes.equals("-97")){nr_bikes = MZConstants.NOT_KNOWN;}
		householdAttributes.putAttribute(hhnr, MZConstants.TOTAL_BYCICLES, nr_bikes);
		
		// creating matsim household
		Household hh = households.getFactory().createHousehold(new IdImpl(hhnr));
		hh.setIncome(new IncomeImpl(Double.parseDouble(income), IncomePeriod.month));
		households.getHouseholds().put(hh.getId(), hh);
		}
		
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # households parsed = "  + households.getHouseholds().size());
		System.out.println();
		
		
	}
	
	
}
