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

public class MZ2000WegeParser {

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private ObjectAttributes wegeAttributes;
	private Population population;

	//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZ2000WegeParser(Population population, ObjectAttributes wegeAttributes) {
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
			String zielpnr = entries[1].trim();
			String intnr = entries[0].trim();
			//Id pid = new IdImpl(hhnr.concat(zielpnr));
			Id<Person> pid = Id.create(intnr, Person.class);
			
			//wege number
			String wegnr = entries[2].trim();
			String wid = pid.toString().concat("-").concat(wegnr);
			wegeAttributes.putAttribute(wid, "number", Integer.parseInt(wegnr));
			
			// initialize number of etappen
			wegeAttributes.putAttribute(wid, MZConstants.NUMBER_STAGES, 0); //initialize
			
			//mode
			String mode = "undefined";
//			if(mode.equals("1")){mode =  MZConstants.PLANE;}
//			else if(mode.equals("2")){mode =  MZConstants.TRAIN;}
//			else if(mode.equals("3")){mode =  MZConstants.POSTAUTO;}
//			else if(mode.equals("4")){mode =  MZConstants.SHIP;}
//			else if(mode.equals("5")){mode =  MZConstants.TRAM;}
//			else if(mode.equals("6")){mode =  MZConstants.BUS;}
//			else if(mode.equals("7")){mode =  MZConstants.SONSTINGER_OEV;}
//			else if(mode.equals("8")){mode =  MZConstants.REISECAR;}
//			else if(mode.equals("9")){mode =  MZConstants.CAR;}
//			else if(mode.equals("10")){mode =  MZConstants.TRUCK;}
//			else if(mode.equals("11")){mode =  MZConstants.TAXI;}
//			else if(mode.equals("12")){mode =  MZConstants.MOTORCYCLE;}
//			else if(mode.equals("13")){mode =  MZConstants.MOFA;}
//			else if(mode.equals("14")){mode =  MZConstants.BYCICLE;}
//			else if(mode.equals("15")){mode =  MZConstants.WALK;}
//			else if(mode.equals("16")){mode =  MZConstants.SKATEBOARD;}
//			else if(mode.equals("17")){mode =  MZConstants.OTHER;}
//			else if(mode.equals("-99")){mode =  MZConstants.PSEUDOETAPPE;}
//			else Gbl.errorMsg("This should never happen!  Mode: " +  mode + " doesn't exist");
//			wegeAttributes.putAttribute(wid, MZConstants.PRINCIPAL_MODE, mode);
			
			//start coordinate - CH1903 (18,19)
			Coord start_coord = new CoordImpl(0,0);
			
							
			//end coordinate - CH1903 (30,31)
			Coord end_coord = new CoordImpl(0,0);
			
			//starting and ending country ( == "" for switzerland) - Startort im Ausland NUTS
			String sland = entries[17].trim();
			String zland = entries[24].trim();
			wegeAttributes.putAttribute(wid, MZConstants.START_COUNTRY, sland);
			wegeAttributes.putAttribute(wid, MZConstants.END_COUNTRY, zland);
			
			//starting point address
			String street =  entries[15].trim();
			String number =  entries[16].trim();
			wegeAttributes.putAttribute(wid, MZConstants.ADDRESS_START, street+number);
			
			//destination adress
			street =  entries[22].trim();
			number =  entries[23].trim();
			wegeAttributes.putAttribute(wid, MZConstants.ADDRESS_END, street+number);
			
			// 9999 = Ausland / undefiniert; 9999 = Grenze
			String sort = entries[17].trim();
			String zort = entries[24].trim();
			
				
			// departure time (min => sec.)
			int departure = Integer.parseInt(entries[8].trim())*60;
			wegeAttributes.putAttribute(wid, MZConstants.DEPARTURE, departure);
			
			// arrival time (min => sec.)
			int arrival = Integer.parseInt(entries[9].trim())*60;
			wegeAttributes.putAttribute(wid, MZConstants.ARRIVAL, arrival);		
			
			
				// time consistency check N°1
				if(arrival<departure){
					if(!time_err_pids.contains(pid)){time_err_pids.add(pid);}
					throw new RuntimeException("This should never happen!  Arrival ("+arrival+") before departure ("+departure+")!- hhnr: " +hhnr+ " zielpnr: "+zielpnr+" wegnr: "+wegnr);
				}
			
			//bee-line distance (km => m)
//			double distance = Double.parseDouble(entries[52].trim())*1000.0;
//			entries[21] = Double.toString(distance);
			
						
			
			//ausgaenge number (=-97 if ausgaenge is imcomplete)
//			String ausnr = entries[58].trim();
			
			//activity type
			String wzweck1 = entries[11].trim();
			String purpose ="";
			
				
			if(wzweck1.equals("0")){purpose = MZConstants.CHANGE ;}
			else if(wzweck1.equals("1")){purpose =  MZConstants.WORK;}
			else if(wzweck1.equals("2")){purpose =  MZConstants.EDUCATION;}
			else if(wzweck1.equals("3")){purpose =  MZConstants.SHOPPING;}
			else if(wzweck1.equals("4")){purpose =  MZConstants.BUSINESS;}
			else if(wzweck1.equals("5")){purpose =  MZConstants.DIENSTFAHRT;}
			else if(wzweck1.equals("6")){purpose =  MZConstants.LEISURE;}
			else if(wzweck1.equals("7")){purpose =  MZConstants.ERRANDS;}
			else if(wzweck1.equals("8")){purpose = MZConstants.ACCOMPANYING;}
			else if(wzweck1.equals("9")){purpose=  MZConstants.NO_ANSWER;} else
				throw new RuntimeException("This should never happen!  Purpose wzweck1: " +  wzweck1 + " doesn't exist");

					

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
			else {//first trip: usually from home unless its part of an incomplete ausgaenge (ausnr=-98)
				  //here a mistake could be done if the person is at home and the ausgaenge is incomplete because the ausgaenge
				  //finishes away. These cases are corrected lately by MZPopulationUtils.setHomeLocations
				
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
