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
 * Parses the wege.dat file from MZ2005 and  fills matsim population with activities' and legs' information.
 *
 * @author acmarmol
 * 
 */

public class MZ2005WegeParser {

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private ObjectAttributes wegeAttributes;
	private Population population;

	//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZ2005WegeParser(Population population, ObjectAttributes wegeAttributes) {
		super();
		this.wegeAttributes = wegeAttributes;
		this.population = population;
	}	


//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////

	public ArrayList<Set<Id<Person>>> parse(String wegeFile) throws Exception{
		
		Set<Id<Person>> coord_err_pids = new HashSet<>();
		Set<Id<Person>> time_err_pids = new HashSet<>();
		Set<Id<Person>> neg_coord_pids = new HashSet<>();
		
		
		FileReader fr = new FileReader(wegeFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		int weg_counter = 0;
			
		while ((curr_line = br.readLine()) != null) {
			
			weg_counter++;
	
			String[] entries = curr_line.split("\t", -1);
				
			//household number
			String hhnr = entries[0].trim();
						
			//person number (zielpnr)
			String zielpnr = entries[1].trim();
			Id<Person> pid = Id.create(hhnr.concat(zielpnr), Person.class);
			
			//wege number
			String wegnr = entries[3].trim();
			String wid = pid.toString().concat("-").concat(wegnr);
			wegeAttributes.putAttribute(wid.toString(), "number", Integer.parseInt(wegnr));
			
			// initialize number of etappen
			wegeAttributes.putAttribute(wid.toString(), MZConstants.NUMBER_STAGES, 0); //initialize
			
			//mode
			String mode = entries[53].trim();
			if(mode.equals("1")){mode =  MZConstants.PLANE;}
			else if(mode.equals("2")){mode =  MZConstants.TRAIN;}
			else if(mode.equals("3")){mode =  MZConstants.POSTAUTO;}
			else if(mode.equals("4")){mode =  MZConstants.SHIP;}
			else if(mode.equals("5")){mode =  MZConstants.TRAM;}
			else if(mode.equals("6")){mode =  MZConstants.BUS;}
			else if(mode.equals("7")){mode =  MZConstants.SONSTINGER_OEV;}
			else if(mode.equals("8")){mode =  MZConstants.REISECAR;}
			else if(mode.equals("9")){mode =  MZConstants.CAR;}
			else if(mode.equals("10")){mode =  MZConstants.TRUCK;}
			else if(mode.equals("11")){mode =  MZConstants.TAXI;}
			else if(mode.equals("12")){mode =  MZConstants.MOTORCYCLE;}
			else if(mode.equals("13")){mode =  MZConstants.MOFA;}
			else if(mode.equals("14")){mode =  MZConstants.BICYCLE;}
			else if(mode.equals("15")){mode =  MZConstants.WALK;}
			else if(mode.equals("16")){mode =  MZConstants.SKATEBOARD;}
			else if(mode.equals("17")){mode =  MZConstants.OTHER;}
			else if(mode.equals("-99")){mode =  MZConstants.PSEUDOETAPPE;} else
				throw new RuntimeException("This should never happen!  Mode: " +  mode + " doesn't exist");
			wegeAttributes.putAttribute(wid.toString(), MZConstants.PRINCIPAL_MODE, mode);
			
			//start coordinate - CH1903 (18,19)
			Coord start_coord = new CoordImpl(entries[18].trim(),entries[19].trim());
			
							
			//end coordinate - CH1903 (30,31)
			Coord end_coord = new CoordImpl(entries[30].trim(),entries[31].trim());
			
			//starting and ending country ( == 8100 for switzerland)
			String sland = entries[25].trim();
			String zland = entries[37].trim();
			wegeAttributes.putAttribute(wid.toString(), MZConstants.START_COUNTRY, sland);
			wegeAttributes.putAttribute(wid.toString(), MZConstants.END_COUNTRY, zland);
			
				
			// departure time (min => sec.)
			int departure = Integer.parseInt(entries[5].trim())*60;
			wegeAttributes.putAttribute(wid.toString(), MZConstants.DEPARTURE, departure);
			
			// arrival time (min => sec.)
			int arrival;
			if(entries[48].trim().equals("")){
				arrival = departure;
			} else{
			   arrival = departure + Integer.parseInt(entries[48].trim())*60;
			wegeAttributes.putAttribute(wid.toString(), MZConstants.ARRIVAL, arrival);
			}			
				// time consistency check N°1
				if(arrival<departure){
					if(!time_err_pids.contains(pid)){time_err_pids.add(pid);}
					//Gbl.errorMsg("This should never happen!  Arrival ("+arrival+") before departure ("+departure+")!- hhnr: " +hhnr+ " zielpnr: "+zielpnr+" wegnr: "+wegnr);
				}
			
			//bee-line distance (km => m)
			double distance = Double.parseDouble(entries[52].trim())*1000.0;
			entries[21] = Double.toString(distance);
			
			
			
			
			
			
			//ausgaenge number (=-97 if ausgaenge is imcomplete)
			String ausnr = entries[58].trim();
			
			//activity type
			String wzweck1 = entries[56].trim();
			String wzweck2 = entries[55].trim();
			String purpose ="";
			
					
			if(wzweck2.equals("1") || ausnr.equals("-97")){
			//hinweg or last wege of incomplete ausgaenge: for some reason with incomplete ausgaenges,
			// if wzweck = "2" doesnt necesarilly implies a Nachhauseweg  (maybe explained somewhere in documentation?)
				
			if(wzweck1.equals("1")){wzweck1 = MZConstants.CHANGE ;}
			else if(wzweck1.equals("2")){purpose =  MZConstants.WORK;}
			else if(wzweck1.equals("3")){purpose =  MZConstants.EDUCATION;}
			else if(wzweck1.equals("4")){purpose =  MZConstants.SHOPPING;}
			else if(wzweck1.equals("5")){purpose =  MZConstants.ERRANDS;}
			else if(wzweck1.equals("6")){purpose =  MZConstants.BUSINESS;}
			else if(wzweck1.equals("7")){purpose =  MZConstants.DIENSTFAHRT;}
			else if(wzweck1.equals("8")){purpose =  MZConstants.LEISURE;}
			else if(wzweck1.equals("9")){purpose =  MZConstants.ACCOMPANYING_CHILDREN;}
			else if(wzweck1.equals("10")){purpose = MZConstants.ACCOMPANYING_NOT_CHILDREN;}
			else if(wzweck1.equals("11")){purpose=  MZConstants.FOREIGN_PROPERTY;}
			else if(wzweck1.equals("12")){purpose =  MZConstants.OTHER;}
			//else if(wzweck1.equals("13")){purpose = "border crossing";}
			else if(wzweck1.equals("-99")){purpose = MZConstants.PSEUDOETAPPE;} else
				throw new RuntimeException("This should never happen!  Purpose wzweck1: " +  wzweck1 + " doesn't exist");
			}else if(wzweck2.equals("2") || wzweck2.equals("3") ){// Nachhauseweg or Weg von zu Hause nach Hause
				purpose =  MZConstants.HOME;	} else
				throw new RuntimeException("This should never happen!  Purpose wzweck2: " +  wzweck2 + " doesn't exist");
			
		
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
				if ((from_act.getCoord().getX() != start_coord.getX()) || (from_act.getCoord().getY() != start_coord.getY())) {
					if((from_act.getCoord().getX() != -97) &  (from_act.getCoord().getY() != -97) &
					    (start_coord.getX() != -97) &  (start_coord.getY() != -97) 	){
						
						start_coord = from_act.getCoord();
						
					}else{
					// Gbl.errorMsg("This should never happen!   pid=" + person.getId() + ": previous destination not equal to the current origin (from_act coord: " + from_act.getCoord() +", start coord: "+ start_coord +")");
						coord_err_pids.add(pid);
					}
				}
				
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
				if(!ausnr.equals("-97")){ firstAct = ((PlanImpl) plan).createAndAddActivity(MZConstants.HOME,start_coord);
				}else firstAct = ((PlanImpl) plan).createAndAddActivity(MZConstants.OVERNIGHT,start_coord);
								
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
		
			
		ArrayList<Set<Id<Person>>> err_pids = new ArrayList<Set<Id<Person>>>();
		err_pids.add(coord_err_pids);
		err_pids.add(time_err_pids);
		err_pids.add(neg_coord_pids);
			
		return err_pids;
			
	}
		
		
}
