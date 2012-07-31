package playground.acmarmol.microcensus2010;

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

public class MZHouseholdParser {

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////	
	
	private Households households;
	private ObjectAttributes householdAttributes;
	
	private static final String NO_ANSWER = "no answer";
	private static final String NOT_KNOWN = "not known";

//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZHouseholdParser(Households households, ObjectAttributes householdAttributes) {
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
		householdAttributes.putAttribute(hhnr, "weight", hh_weight);
		
		//household size
		String size = entries[76].trim();
		householdAttributes.putAttribute(hhnr, "size", size);
		
		//household income
		String income = entries[98].trim();
		householdAttributes.putAttribute(hhnr, "income", income);
		
		
		// location coordinate (round to 1/10 of hectare) - WGS84 (5,6) & CH1903 (7,8)
		Coord location = new CoordImpl(entries[7].trim(),entries[8].trim());
		//location.setX(Math.round(location.getX()/10.0)*10);
		//location.setY(Math.round(location.getY()/10.0)*10);
		householdAttributes.putAttribute(hhnr, "coord", location);
		
		//municipality BFS number
		String municipality =  entries[10].trim();
		householdAttributes.putAttribute(hhnr, "municipality", municipality);
		
		
		//number of cars
		String nr_cars = entries[77];
		if(nr_cars.equals("-98")){nr_cars = NO_ANSWER;}
		else if(nr_cars.equals("-97")){nr_cars = NOT_KNOWN;}
		householdAttributes.putAttribute(hhnr, "total cars ", nr_cars);
		
		//number of motorcycles
		String nr_mcycles = entries[79];
		if(nr_mcycles.equals("-98")){nr_mcycles = NO_ANSWER;}
		else if(nr_mcycles.equals("-97")){nr_mcycles = NOT_KNOWN;}
		householdAttributes.putAttribute(hhnr, "total motorcycles ", nr_mcycles);
		
		//number of small motorcycles
		String nr_smcycles = entries[79];
		if(nr_smcycles.equals("-98")){nr_smcycles = NO_ANSWER;}
		else if(nr_smcycles.equals("-97")){nr_smcycles = NOT_KNOWN;}
		householdAttributes.putAttribute(hhnr, "total small motorcycles ", nr_smcycles);
		
		//number of mofa
		String nr_mofas = entries[79];
		if(nr_mofas.equals("-98")){nr_mofas = NO_ANSWER;}
		else if(nr_mofas.equals("-97")){nr_mofas = NOT_KNOWN;}
		householdAttributes.putAttribute(hhnr, "total mofas ", nr_mofas);
		
		//number of bicycles
		String nr_bikes = entries[79];
		if(nr_bikes.equals("-98")){nr_bikes = NO_ANSWER;}
		else if(nr_bikes.equals("-97")){nr_bikes = NOT_KNOWN;}
		householdAttributes.putAttribute(hhnr, "total bicycles", nr_bikes);
		
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
