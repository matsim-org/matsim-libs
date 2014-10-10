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

package playground.acmarmol.matsim2030.microcensus2000;

import java.io.BufferedReader;
import java.io.FileReader;

import org.matsim.api.core.v01.Id;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.households.IncomeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.acmarmol.matsim2030.microcensus2010.MZConstants;

/**
* 
* Parses the haushalte.dat file from MZ2005, creates matsim households, and
* fills the households attributes with the microcensus information.
* 
* @see org.matsim.utils.objectattributes 
*
* @author acmarmol
* 
*/

public class MZ2000HouseholdParser {

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////	
	
	private Households households;
	private ObjectAttributes householdAttributes;
	


//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZ2000HouseholdParser(Households households, ObjectAttributes householdAttributes) {
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
		String size = entries[14].trim();
		householdAttributes.putAttribute(hhnr, MZConstants.HOUSEHOLD_SIZE, size);
		
		//household income
		String income = entries[17].trim();
		householdAttributes.putAttribute(hhnr, MZConstants.HOUSEHOLD_INCOME, income);
		householdAttributes.putAttribute(hhnr, MZConstants.HOUSEHOLD_INCOME_MIDDLE_OF_INTERVAL, getIncome(income));

		
		
		// location coordinate (round to 1/10 of hectare) - WGS84 (5,6) & CH1903 (7,8)
		//Coord location = new CoordImpl(entries[7].trim(),entries[8].trim());
		//location.setX(Math.round(location.getX()/10.0)*10);
		//location.setY(Math.round(location.getY()/10.0)*10);
		//householdAttributes.putAttribute(hhnr, "coord", location);
		
		//Kanton
		String kanton =  entries[10].trim();
		if(kanton.equals("1")){kanton = MZConstants.ZURICH;}						else if(kanton.equals("2")){kanton = MZConstants.BERN;}
		else if(kanton.equals("3")){kanton = MZConstants.LUZERN;}					else if(kanton.equals("4")){kanton = MZConstants.URI;}
		else if(kanton.equals("5")){kanton = MZConstants.SCHWYZ;}					else if(kanton.equals("6")){kanton = MZConstants.OBWALDEN;}
		else if(kanton.equals("7")){kanton = MZConstants.NIDWALDEN;}				else if(kanton.equals("8")){kanton = MZConstants.GLARUS;}
		else if(kanton.equals("9")){kanton = MZConstants.ZUG;}					else if(kanton.equals("10")){kanton = MZConstants.FRIBOURG;}
		else if(kanton.equals("11")){kanton = MZConstants.SOLOTHURN;}				else if(kanton.equals("12")){kanton = MZConstants.BASEL_STADT;}
		else if(kanton.equals("13")){kanton = MZConstants.BASEL_LAND;}			else if(kanton.equals("14")){kanton = MZConstants.SCHAFFHAUSEN;}
		else if(kanton.equals("15")){kanton = MZConstants.APPENZELL_AUSSERHODEN;}else if(kanton.equals("16")){kanton = MZConstants.APPENZELL_INNERHODEN;}
		else if(kanton.equals("17")){kanton = MZConstants.ST_GALLEN;}			else if(kanton.equals("18")){kanton = MZConstants.GRAUBUNDEN;}
		else if(kanton.equals("19")){kanton = MZConstants.AARGAU;}				else if(kanton.equals("20")){kanton = MZConstants.THURGAU;}
		else if(kanton.equals("21")){kanton = MZConstants.TICINO;}				else if(kanton.equals("22")){kanton = MZConstants.VAUD;}
		else if(kanton.equals("23")){kanton = MZConstants.VALAIS;}				else if(kanton.equals("24")){kanton = MZConstants.NEUCHATEL;}
		else if(kanton.equals("25")){kanton = MZConstants.GENEVE;}				else if(kanton.equals("26")){kanton = MZConstants.JURA;}
		else if(kanton.equals("-97")){kanton = MZConstants.UNSPECIFIED;}		
		householdAttributes.putAttribute(hhnr, MZConstants.CANTON, kanton);
		
		//municipality BFS number
		String municipality =  entries[5].trim();
		householdAttributes.putAttribute(hhnr, MZConstants.MUNICIPALITY, municipality);
		
		//adress
		String street =  entries[6].trim();
		String number =  entries[7].trim();
		householdAttributes.putAttribute(hhnr, MZConstants.ADDRESS, street+number);
		
		//number of cars
		String nr_cars = entries[27];
		if(nr_cars.equals("-2")){nr_cars = MZConstants.NO_ANSWER;}
		else if(nr_cars.equals("-1")){nr_cars = MZConstants.NOT_KNOWN;}
		else if(nr_cars.equals("7")){nr_cars = "more than 6";}
		householdAttributes.putAttribute(hhnr, MZConstants.TOTAL_CARS, nr_cars);
		
		//number of motorcycles
		String nr_mcycles = entries[28];
		if(nr_mcycles.equals("-2")){nr_mcycles = MZConstants.NO_ANSWER;}
		else if(nr_mcycles.equals("-1")){nr_mcycles = MZConstants.NOT_KNOWN;}
		else if(nr_mcycles.equals("7")){nr_mcycles = "more than 6";}
		householdAttributes.putAttribute(hhnr, MZConstants.TOTAL_MOTORCYCLES, nr_mcycles);
		
		//number of small motorcycles
		String nr_smcycles = entries[29];
		if(nr_smcycles.equals("-2")){nr_smcycles = MZConstants.NO_ANSWER;}
		else if(nr_smcycles.equals("-1")){nr_smcycles = MZConstants.NOT_KNOWN;}
		else if(nr_smcycles.equals("-1")){nr_smcycles = "more than 6";}
		householdAttributes.putAttribute(hhnr, MZConstants.TOTAL_SMALL_MOTORCYCLES, nr_smcycles);
		
		//number of mofa
		String nr_mofas = entries[30];
		if(nr_mofas.equals("-2")){nr_mofas = MZConstants.NO_ANSWER;}
		else if(nr_mofas.equals("-1")){nr_mofas = MZConstants.NOT_KNOWN;}
		else if(nr_mofas.equals("7")){nr_mofas = "more than 6";}
		householdAttributes.putAttribute(hhnr, MZConstants.TOTAL_MOFAS, nr_mofas);
		
		//number of bicycles
		String nr_bikes = entries[31];
		if(nr_bikes.equals("-2")){nr_bikes = MZConstants.NO_ANSWER;}
		else if(nr_bikes.equals("-1")){nr_bikes = MZConstants.NOT_KNOWN;}
		else if(nr_bikes.equals("7")){nr_bikes = "more than 6";}
		householdAttributes.putAttribute(hhnr, MZConstants.TOTAL_BICYCLES, nr_bikes);
		
		// creating matsim household
		Household hh = households.getFactory().createHousehold(Id.create(hhnr, Household.class));
		hh.setIncome(new IncomeImpl(Double.parseDouble(income), IncomePeriod.month));
		households.getHouseholds().put(hh.getId(), hh);
		}
		
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # households parsed = "  + households.getHouseholds().size());
		System.out.println();
		
		
	}
	
	private String getIncome(String hh_income) {
			
			if(hh_income.equals("1")){
				return "1000";
			}else if(hh_income.equals("2")){
				return "3000";
			}else if(hh_income.equals("3")){
				return "5000";
			}else if(hh_income.equals("4")){
				return "7000";
			}else if(hh_income.equals("5")){
				return "9000";
			}else if(hh_income.equals("6")){
				return "11000";
			}else if(hh_income.equals("7")){
				return "13000";
			}else if(hh_income.equals("8")){
				return "18000";
			}else{
				return MZConstants.UNSPECIFIED;
			}
		}
	
}
