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
import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.households.IncomeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import playground.acmarmol.utils.CoordConverter;



public class HouseholdsFromMZ{

//////////////////////////////////////////////////////////////////////
// member variables
//////////////////////////////////////////////////////////////////////
	
	private final String householdsInputfile;
	private final String householdsPeopleInputfile;
	private final String householdsVehiclesInputfile;
	private ObjectAttributes household_attributes;
	private ObjectAttributes vehicles_attributes;
	private Households households;
	private Vehicles vehicles;
	
	private static final String NO_ANSWER = "no answer";
	private static final String UNSPECIFIED = "unspecified";
	private static final String NOT_KNOWN = "not known";
		
	
//////////////////////////////////////////////////////////////////////
// constructors
//////////////////////////////////////////////////////////////////////

	public HouseholdsFromMZ(final String householdsInputfile, final String householdsPeopleInputfile, final String householdsVehiclesInputfile) {
		super();
		this.householdsInputfile = householdsInputfile;
		this.householdsPeopleInputfile = householdsPeopleInputfile;		
		this.householdsVehiclesInputfile = householdsVehiclesInputfile;		
		this.household_attributes  = new ObjectAttributes();
		this.vehicles_attributes  = new ObjectAttributes();
		this.households  = new HouseholdsImpl();
		this.vehicles = VehicleUtils.createVehiclesContainer();
	
	}	
	
//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////	
	
	
	public void createHouseholds() throws Exception{
	
		System.out.println("      parsing households from " + this.householdsInputfile);	
		
		FileReader fr = new FileReader(this.householdsInputfile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		
		while ((curr_line = br.readLine()) != null) {
			
		String[] entries = curr_line.split("\t", -1);
		
		//household number
		String hhnr = entries[0].trim();
		
		//household weight 
		String hh_weight = entries[1];
		household_attributes.putAttribute(hhnr, "weight", hh_weight);
		
		//household size
		String size = entries[76].trim();
		household_attributes.putAttribute(hhnr, "size", size);
		
		//household income
		String income = entries[98].trim();
		household_attributes.putAttribute(hhnr, "income", income);
		
		
		// location coordinate (round to hectare) - WGS84 (5,6) & CH1903 (7,8)
		Coord location = new CoordImpl(entries[7].trim(),entries[8].trim());
		location.setX(Math.round(location.getX()/100.0)*100);
		location.setY(Math.round(location.getY()/100.0)*100);
		household_attributes.putAttribute(hhnr, "location coord", location);
		
		//municipality BFS number
		String municipality =  entries[10].trim();
		household_attributes.putAttribute(hhnr, "municipality", municipality);
		
		
		//number of cars
		String nr_cars = entries[77];
		if(nr_cars.equals("-98")){nr_cars = NO_ANSWER;}
		else if(nr_cars.equals("-97")){nr_cars = NOT_KNOWN;}
		household_attributes.putAttribute(hhnr, "total cars ", nr_cars);
		
		//number of motorcycles
		String nr_mcycles = entries[79];
		if(nr_mcycles.equals("-98")){nr_mcycles = NO_ANSWER;}
		else if(nr_mcycles.equals("-97")){nr_mcycles = NOT_KNOWN;}
		household_attributes.putAttribute(hhnr, "total motorcycles ", nr_mcycles);
		
		//number of small motorcycles
		String nr_smcycles = entries[79];
		if(nr_smcycles.equals("-98")){nr_smcycles = NO_ANSWER;}
		else if(nr_smcycles.equals("-97")){nr_smcycles = NOT_KNOWN;}
		household_attributes.putAttribute(hhnr, "total small motorcycles ", nr_smcycles);
		
		//number of mofa
		String nr_mofas = entries[79];
		if(nr_mofas.equals("-98")){nr_mofas = NO_ANSWER;}
		else if(nr_mofas.equals("-97")){nr_mofas = NOT_KNOWN;}
		household_attributes.putAttribute(hhnr, "total mofas ", nr_mofas);
		
		//number of bicycles
		String nr_bikes = entries[79];
		if(nr_bikes.equals("-98")){nr_bikes = NO_ANSWER;}
		else if(nr_bikes.equals("-97")){nr_bikes = NOT_KNOWN;}
		household_attributes.putAttribute(hhnr, "total bicycles", nr_bikes);
		
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
	
	public void createHouseholdsPeople() throws Exception{
		
		System.out.println("      parsing households people from " + this.householdsPeopleInputfile);	
		
		FileReader fr = new FileReader(this.householdsPeopleInputfile);
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
			
			
			//filling person data into matsim households
			IdImpl hhid = new IdImpl(hhnr);
			if(!this.households.getHouseholds().containsKey(hhid)){
				Gbl.errorMsg("This should never happen!  Household hhnr: " + hhnr+ " doesn't exist");
			}		
			this.households.getHouseholds().get(hhid).getMemberIds().add(new IdImpl(hhnr.concat(hpnr)));  // id = hhnr + hpnr??
		}
		
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # households people parsed = "  + nr_persons);
		System.out.println();
		
	}
		
		
	public void createHouseholdsVehicles() throws Exception{	
		
		System.out.println("      parsing households vehicles from " + this.householdsVehiclesInputfile );	
		
		FileReader fr = new FileReader(this.householdsVehiclesInputfile);
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
	
			vehicles_attributes.putAttribute(hhnr.concat(fznr), "type", type);
			
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
			vehicles_attributes.putAttribute(hhnr.concat(fznr), "fuel type", fuel);
			
			//year of registration
			String year = entries[6].trim();
			if(year.equals("-98")){year = UNSPECIFIED;}
			else if(year.equals("-97")){year = NOT_KNOWN;}
			vehicles_attributes.putAttribute(hhnr.concat(fznr), "year of registration", year);
			
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
			vehicles_attributes.putAttribute(hhnr.concat(fznr), "month of registration", month);
			
						
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
		

	
//////////////////////////////////////////////////////////////////////
// run method
//////////////////////////////////////////////////////////////////////
		
	public void run() throws Exception{
		
		
		createHouseholds();
		createHouseholdsPeople();
		createHouseholdsVehicles();
		
		System.out.println("################################################################################# \n " +
						   "Writing households  xml file \n" +
						   "#################################################################################");		
		new HouseholdsWriterV10(this.households).writeFile("./output/MicroCensus2010/households.xml");
		System.out.println("  done.");
		
		System.out.println("################################################################################# \n " +
						   "Writing households' attributes xml file \n" +
							"#################################################################################");			
		ObjectAttributesXmlWriter households_axmlw = new ObjectAttributesXmlWriter(household_attributes);
		households_axmlw.putAttributeConverter(CoordImpl.class, new CoordConverter());
		households_axmlw.writeFile("./output/MicroCensus2010/householdsAttributes.xml");
		System.out.println("  done.");
		
		System.out.println("################################################################################# \n " +
				   		   "Writing vehicles xml file \n" +
					       "#################################################################################");
		new VehicleWriterV1(this.vehicles).writeFile("./output/MicroCensus2010/vehicles.xml");
		System.out.println("  done.");
		
		System.out.println("################################################################################# \n " +
						   "Writing vehicles' attributes xml file \n" +
						   "#################################################################################");	
		new ObjectAttributesXmlWriter(vehicles_attributes).writeFile("./output/MicroCensus2010/vehiclesAttributes.xml");
		System.out.println("  done.");
		
			
	}
	
	
	
	
	
	
	
}