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


package playground.acmarmol.matsim2030.microcensus1989;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.acmarmol.matsim2030.microcensus2010.MZConstants;

/**
 * 
 * Parses the wege.dat file from MZ2000 and  fills matsim population with activities' and legs' information.
 *
 * @author acmarmol
 * 
 */

public class MZ1989WegeParser {

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private ObjectAttributes wegeAttributes;
	private Population population;

	//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZ1989WegeParser(Population population, ObjectAttributes wegeAttributes) {
		super();
		this.wegeAttributes = wegeAttributes;
		this.population = population;
	}	


//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////

	public ArrayList<Set<Id>> parse(String wegeFile) throws Exception{
		
		Set<Id> coord_err_pids = new HashSet<Id>();
		Set<Id> time_err_pids = new HashSet<Id>();
		Set<Id> neg_coord_pids = new HashSet<Id>();
		
		
		FileReader fr = new FileReader(wegeFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		int weg_counter = 0;
			
		while ((curr_line = br.readLine()) != null) {
			
			weg_counter++;
	
			String[] entries = curr_line.split("\t", -1);
				
			//household number
			String hhnr = entries[1].trim();
						
			//person number (zielpnr)
			String zielpnr = entries[2].trim();
			Id<Person> pid = Id.create(hhnr.concat(zielpnr), Person.class);
			//Id pid = new IdImpl(intnr);
			
			//wege number
			String wegnr = entries[3].trim();
			String wid = pid.toString().concat("-").concat(wegnr);
			wegeAttributes.putAttribute(wid.toString(), "number", Integer.parseInt(wegnr));
			
			// initialize number of etappen
			wegeAttributes.putAttribute(wid.toString(), MZConstants.NUMBER_STAGES, 0); //initialize
			
			//mode
			String mode = null;
			String mode1 = entries[12].trim();
			String mode2 = entries[13].trim();
			String mode3 = entries[14].trim();
			String mode4 = entries[15].trim();
			String mode5 = entries[16].trim();
			String mode6 = entries[17].trim();
			String mode7 = entries[18].trim();
			String mode8 = entries[19].trim();
			String mode9 = entries[20].trim();
			
			if(mode8.equals("1")){mode =  MZConstants.TRAIN;}
			else if(mode7.equals("1")){mode =  MZConstants.POSTAUTO;}
			else if(mode6.equals("1")){mode =  MZConstants.BUS_TRAM;}
			else if(mode5.equals("1")){mode =  MZConstants.CAR;}
			else if(mode4.equals("1")){mode =  MZConstants.MOTORCYCLE;}
			else if(mode3.equals("1")){mode =  MZConstants.MOFA;}
			else if(mode2.equals("1")){mode =  MZConstants.BICYCLE;}
			else if(mode1.equals("1")){mode =  MZConstants.WALK;}
			else if(mode9.equals("1")){mode =  MZConstants.OTHER;}
			else mode = MZConstants.NO_ANSWER;
			wegeAttributes.putAttribute(wid.toString(), MZConstants.PRINCIPAL_MODE, mode);
			
			//start coordinate - CH1903 (18,19)
			Coord start_coord = new CoordImpl(0,0);
			
							
			//end coordinate - CH1903 (30,31)
			Coord end_coord = new CoordImpl(0,0);
			
//			//starting and ending country ( == "" for switzerland) - Startort im Ausland NUTS
//			String sland = entries[17].trim();
//			String zland = entries[24].trim();
//			wegeAttributes.putAttribute(wid.toString(), MZConstants.START_COUNTRY, sland);
//			wegeAttributes.putAttribute(wid.toString(), MZConstants.END_COUNTRY, zland);
//			
//			//starting point address
//			String street =  entries[15].trim();
//			String number =  entries[16].trim();
//			wegeAttributes.putAttribute(wid.toString(), MZConstants.ADDRESS_START, street+number);
//			
//			//destination adress
//			street =  entries[22].trim();
//			number =  entries[23].trim();
//			wegeAttributes.putAttribute(wid.toString(), MZConstants.ADDRESS_END, street+number);
//			
//			// 9999 = Ausland / undefiniert; 9999 = Grenze
//			String sort = entries[17].trim();
//			String zort = entries[24].trim();
			
				
			// departure time (min => sec.)
			String dep_t = entries[10].trim();
			int length = dep_t.length();
			for(int i=0; i<4-length;i++){dep_t = "0".concat(dep_t);}	
			int departure_hours = Integer.parseInt(dep_t.substring(0,2));
			int departure_minutes = Integer.parseInt(dep_t.trim().substring(2,4));
			int departure = departure_hours*3600 + departure_minutes*60;
			wegeAttributes.putAttribute(wid.toString(), MZConstants.DEPARTURE, departure);
			
			// arrival time (min => sec.)
			String arr_t = entries[22].trim();
			length= arr_t.length();
			for(int i=0; i<4-length;i++){arr_t = "0".concat(arr_t);}
			int arrival_hours = Integer.parseInt(arr_t.substring(0,2));
			int arrival_minutes = Integer.parseInt(arr_t.trim().substring(2,4));
			int arrival = arrival_hours*3600 + arrival_minutes*60;
			wegeAttributes.putAttribute(wid.toString(), MZConstants.ARRIVAL, arrival);			
			
			
			
				// time consistency check N°1
				if(arrival<departure){
					if(!time_err_pids.contains(pid)){time_err_pids.add(pid);}
					//Gbl.errorMsg("This should never happen!  Arrival ("+arrival+") before departure ("+departure+")!- hhnr: " +hhnr+ " zielpnr: "+zielpnr+" wegnr: "+wegnr);
				}
			
			//bee-line distance (km => m)
//			double distance = Double.parseDouble(entries[52].trim())*1000.0;
//			entries[21] = Double.toString(distance);
			
						
			
			//ausgaenge number (=-97 if ausgaenge is imcomplete)
//			String ausnr = entries[58].trim();
			
			//activity type
			String wzweck1 = entries[11].trim();
			String purpose ="";
			
				if(wzweck1.equals("1")){purpose =  MZConstants.WORK;}
				else if(wzweck1.equals("2")){purpose =  MZConstants.SHOPPING;}
				else if(wzweck1.equals("3")){purpose =  MZConstants.LEISURE;}
				else if(wzweck1.equals("4")){purpose =  MZConstants.BUSINESS;}
				else if(wzweck1.equals("5")){purpose =  MZConstants.HOME;}
				else if(wzweck1.equals("6")){purpose=  MZConstants.OTHER;}
				else {purpose=  MZConstants.NOT_KNOWN;}

				

			// creating/getting plan
			PersonImpl person = (PersonImpl) population.getPersons().get(pid);
			Plan plan = person.getSelectedPlan();
			if (plan == null) {
				person.createAndAddPlan(true);
				plan = person.getSelectedPlan();
				
			}
			
			// adding acts/legs
			if (plan.getPlanElements().size() != 0) { // already lines parsed and added (not first wege)
				ActivityImpl from_act = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
				
				LegImpl previous_leg = (LegImpl)plan.getPlanElements().get(plan.getPlanElements().size()-2);
				from_act.setEndTime(departure);
				LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
				leg.setDepartureTime(departure);
				leg.setTravelTime(arrival-departure);
				leg.setArrivalTime(arrival);
				ActivityImpl act = ((PlanImpl) plan).createAndAddActivity(purpose,end_coord);
				act.setStartTime(arrival);
			
				// coordinate consistency check
//				if ((from_act.getCoord().getX() != start_coord.getX()) || (from_act.getCoord().getY() != start_coord.getY())) {
//					if((from_act.getCoord().getX() != -97) &  (from_act.getCoord().getY() != -97) &
//					    (start_coord.getX() != -97) &  (start_coord.getY() != -97) 	){
//						
//						start_coord = from_act.getCoord();
//						
//					}else{
//					// Gbl.errorMsg("This should never happen!   pid=" + person.getId() + ": previous destination not equal to the current origin (from_act coord: " + from_act.getCoord() +", start coord: "+ start_coord +")");
//						coord_err_pids.add(pid);
//					}
//				}
				
				// time consistency check N°2
				if (previous_leg.getArrivalTime() > leg.getDepartureTime()) {
					if(!time_err_pids.contains(pid)){time_err_pids.add(pid);}
					//Gbl.errorMsg("This should never happen!   pid=" + person.getId() + ": activity end time "+ leg.getDepartureTime() + " greater than start time " + previous_leg.getArrivalTime());
						
				}
			
				
			}
			else {//first trip: usually from home unless...?
				
				ActivityImpl firstAct;
				firstAct = ((PlanImpl) plan).createAndAddActivity(MZConstants.HOME,start_coord);
			
				firstAct.setEndTime(departure);
				LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);				
				leg.setDepartureTime(departure);
				leg.setTravelTime(arrival-departure);
				leg.setArrivalTime(arrival);
				//GenericRouteImpl route = new GenericRouteImpl(null, null);
				//leg.setRoute(route);
				//route.setDistance(distance);
				//route.setTravelTime(leg.getTravelTime());
				ActivityImpl act = ((PlanImpl) plan).createAndAddActivity(purpose,end_coord);
				act.setStartTime(arrival);
			}
						
		}//end while
		
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # weges parsed = " + weg_counter  );
		
			
		ArrayList<Set<Id>> err_pids = new ArrayList<Set<Id>>();
		err_pids.add(coord_err_pids);
		err_pids.add(time_err_pids);
		err_pids.add(neg_coord_pids);
			
		return err_pids;
			
	}
		
		
}
