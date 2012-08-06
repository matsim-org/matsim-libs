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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * 
 * Parses the wege.dat file from MZ2010 and  fills matsim population with activities' and legs' information.
 *
 * @author acmarmol
 * 
 */

public class MZWegeParser {

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private ObjectAttributes wegeAttributes;
	private Population population;
	
	
	
	private static final String HOME = "home";

//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZWegeParser(Population population, ObjectAttributes wegeAttributes) {
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
		Set<Id> border_crossing_pids = new HashSet<Id>();
		
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
			Id pid = new IdImpl(hhnr.concat(zielpnr));
			
			//wege number
			String wegnr = entries[3].trim();
			Id wid = new IdImpl(pid.toString().concat("-").concat(wegnr));
			wegeAttributes.putAttribute(wid.toString(), "number", Integer.parseInt(wegnr));
			
			// initialize number of etappen
			wegeAttributes.putAttribute(wid.toString(), "number of etappen", 0); //initialize
			
			//mode
			String mode = entries[80].trim();
			if(mode.equals("1")){mode = "plane";}
			else if(mode.equals("2")){mode = "train";}else if(mode.equals("3")){mode = "postauto";}
			else if(mode.equals("4")){mode = "ship";}else if(mode.equals("5")){mode = "tram";}
			else if(mode.equals("6")){mode = "bus";}else if(mode.equals("7")){mode = "sonstigerOeV";}
			else if(mode.equals("8")){mode = "reisecar";}else if(mode.equals("9")){mode = "car";}
			else if(mode.equals("10")){mode = "truck";}else if(mode.equals("11")){mode = "taxi";}
			else if(mode.equals("12")){mode = "motorcycle";}else if(mode.equals("13")){mode = "mofa";}
			else if(mode.equals("14")){mode = "bicycle";}else if(mode.equals("15")){mode = "walk";}
			else if(mode.equals("16")){mode = "skateboard/skates";}else if(mode.equals("17")){mode = "other";}
			else if(mode.equals("-99")){mode = "Pseudoetappe";}
			else Gbl.errorMsg("This should never happen!  Mode: " +  mode + " doesn't exist");
			wegeAttributes.putAttribute(wid.toString(), "principal mode", mode);
			
			//start coordinate - WGS84 (22,23) & CH1903 (24,25)
			Coord start_coord = new CoordImpl(entries[24].trim(),entries[25].trim());
			
							
			//end coordinate (round to hectare) - WGS84 (42,43) & CH1903 (44,45)
			Coord end_coord = new CoordImpl(entries[44].trim(),entries[45].trim());
			
			//starting and ending country ( == 8100 for switzerland)
			String sland = entries[36].trim();
			String zland = entries[56].trim();
			if(!sland.equals("8100") || !zland.equals("8100")){
				if((!sland.equals("8100") && !zland.equals("8100"))){
					wegeAttributes.putAttribute(wid.toString(), "Out of border type", "completely out");	
				} //completely out of CH
				else if((sland.equals("8100") && !zland.equals("8100"))){
					wegeAttributes.putAttribute(wid.toString(), "Out of border type", "out");	
				}  //going-out of CH
				else if((!sland.equals("8100") && zland.equals("8100"))){
					wegeAttributes.putAttribute(wid.toString(), "Out of border type", "in");					
				} //entering CH
			border_crossing_pids.add(wid);}
			
				
			// departure time (min => sec.)
			int departure = Integer.parseInt(entries[5].trim())*60;
			wegeAttributes.putAttribute(wid.toString(), "departure", departure);
			
			// arrival time (min => sec.)
			int arrival = Integer.parseInt(entries[6].trim())*60;
			wegeAttributes.putAttribute(wid.toString(), "arrival", arrival);
						
				// time consistency check N°1
				if(arrival<departure){
					if(!time_err_pids.contains(pid)){time_err_pids.add(pid);}
					//Gbl.errorMsg("This should never happen!  Arrival ("+arrival+") before departure ("+departure+")!- hhnr: " +hhnr+ " zielpnr: "+zielpnr+" wegnr: "+wegnr);
				}
			
			//bee-line distance (km => m)
			double distance = Double.parseDouble(entries[85].trim())*1000.0;
			entries[21] = Double.toString(distance);
			
			
			
			
			
			//ausgaenge number (=-98 if ausgaenge is imcomplete)
			String ausnr = entries[86].trim();
			
			//activity type
			String wzweck1 = entries[82].trim();
			String wzweck2 = entries[83].trim();
			String purpose ="";
			
					
			if(wzweck2.equals("1") || ausnr.equals("-98")){
			//hinweg or last wege of incomplete ausgaenge: for some reason with incomplete ausgaenges,
			// if wzweck = "2" doesnt necesarilly implies a Nachhauseweg  (maybe explained somewhere in documentation?)
				
			if(wzweck1.equals("1")){wzweck1 = "Change, change of transport, car park";}
			else if(wzweck1.equals("2")){purpose = "work";}else if(wzweck1.equals("3")){purpose = "education, school";}
			else if(wzweck1.equals("4")){purpose = "shopping";}else if(wzweck1.equals("5")){purpose = "errands and use of services";}
			else if(wzweck1.equals("6")){purpose = "business";}else if(wzweck1.equals("7")){purpose = "dienstfahrt";}
			else if(wzweck1.equals("8")){purpose = "leisure";}else if(wzweck1.equals("9")){purpose = "accompanying (children)";}
			else if(wzweck1.equals("10")){purpose = "accompanying (not children)";}else if(wzweck1.equals("11")){purpose= "foreign property";}
			else if(wzweck1.equals("12")){purpose = "other";}
			else if(wzweck1.equals("13")){purpose = "border crossing";}
			else if(wzweck1.equals("-99")){purpose = "Pseudoetappe";}
			else Gbl.errorMsg("This should never happen!  Purpose wzweck1: " +  wzweck1 + " doesn't exist");
			}else if(wzweck2.equals("2") || wzweck2.equals("3") ){// Nachhauseweg or Weg von zu Hause nach Hause
				purpose = HOME;	}
			else Gbl.errorMsg("This should never happen!  Purpose wzweck2: " +  wzweck2 + " doesn't exist");
			
		
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
					 //Gbl.errorMsg("This should never happen!   pid=" + person.getId() + ": previous destination not equal to the current origin (dist=" + ((CoordImpl) from_act.getCoord()).calcDistance(start_coord) + ")");
						coord_err_pids.add(pid);
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
				if(!ausnr.equals("-98")){ firstAct = ((PlanImpl) plan).createAndAddActivity(HOME,start_coord);
				}else firstAct = ((PlanImpl) plan).createAndAddActivity("overnight away",start_coord);
								
				firstAct.setEndTime(departure);
				LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);				
				leg.setDepartureTime(departure);
				leg.setTravelTime(arrival-departure);
				leg.setArrivalTime(arrival);
				GenericRouteImpl route = new GenericRouteImpl(null, null);
				leg.setRoute(route);
				route.setDistance(distance);
				route.setTravelTime(leg.getTravelTime());
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
		err_pids.add(border_crossing_pids);
		
		return err_pids;
			
	}
		
		
}
