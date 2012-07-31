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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class MZWegeParser {

//////////////////////////////////////////////////////////////////////
//member variables
//////////////////////////////////////////////////////////////////////

	private Households households;
	private ObjectAttributes householdAttributes;
	private Population population;
	private ObjectAttributes populationAttributes;
	
	private static final String HOME = "home";

//////////////////////////////////////////////////////////////////////
//constructors
//////////////////////////////////////////////////////////////////////

	public MZWegeParser(Population population, ObjectAttributes populationAttributes,  Households households, ObjectAttributes householdAttributes) {
		super();
		this.households = households;
		this.householdAttributes = householdAttributes;
		this.population = population;
		this.populationAttributes = populationAttributes;
	}	


//////////////////////////////////////////////////////////////////////
//private methods
//////////////////////////////////////////////////////////////////////

	public ArrayList<Set<Id>> parse(String wegeFile) throws Exception{
		
		Set<Id> coord_err_pids = new HashSet<Id>();
		Set<Id> time_err_pids = new HashSet<Id>();
		Set<Id> neg_coord_pids = new HashSet<Id>();
		Set<Id> home_coord_pids = new HashSet<Id>();
		
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
			
			//store home coord for future consistency check
			CoordImpl homeCoord = (CoordImpl)householdAttributes.getAttribute(hhnr, "coord");
			
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
			
			//start coordinate (round to hectare) - WGS84 (22,23) & CH1903 (24,25)
			Coord start_coord = new CoordImpl(entries[24].trim(),entries[25].trim());
			//start_coord.setX(Math.round(start_coord.getX()/10.0)*10);
			//start_coord.setY(Math.round(start_coord.getY()/10.0)*10);
					
			//end coordinate (round to hectare) - WGS84 (42,43) & CH1903 (44,45)
			Coord end_coord = new CoordImpl(entries[44].trim(),entries[45].trim());
			//end_coord.setX(Math.round(end_coord.getX()/10.0)*10);
			//end_coord.setY(Math.round(end_coord.getY()/10.0)*10);
				
				// negative coord check
				if(start_coord.getX()<0 || start_coord.getY()<0 || end_coord.getX()<0 || end_coord.getY()<0){
					if(!neg_coord_pids.contains(pid)){neg_coord_pids.add(pid);}
				}
			
			
			// departure time (min => sec.)
			int departure = Integer.parseInt(entries[5].trim())*60;
			
			// arrival time (min => sec.)
			int arrival = Integer.parseInt(entries[6].trim())*60;
			
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
			
					
			if(wzweck2.equals("1") || ausnr.equals("-98") ){//hinweg or last wege of incomplete ausgaenge
			if(wzweck1.equals("1")){wzweck1 = "Change, change of transport, car park";}
			else if(wzweck1.equals("2")){purpose = "work";}else if(wzweck1.equals("3")){purpose = "education, school";}
			else if(wzweck1.equals("4")){purpose = "shopping";}else if(wzweck1.equals("5")){purpose = "errands and use of services";}
			else if(wzweck1.equals("6")){purpose = "business";}else if(wzweck1.equals("7")){purpose = "dienstfahrt";}
			else if(wzweck1.equals("8")){purpose = "leisure";}else if(wzweck1.equals("9")){purpose = "accompanying (children)";}
			else if(wzweck1.equals("10")){purpose = "accompanying (not children)";}else if(wzweck1.equals("11")){purpose= "foreign property";}
			else if(wzweck1.equals("12")){purpose = "other";}
			else if(wzweck1.equals("-99")){purpose = "Pseudoetappe";}
			else Gbl.errorMsg("This should never happen!  Purpose wzweck1: " +  wzweck1 + " doesn't exist");
			}else if(wzweck2.equals("2") || wzweck2.equals("3") ){// Nachhauseweg or Weg von zu Hause nach Hause
				purpose = HOME;	}
			else Gbl.errorMsg("This should never happen!  Purpose wzweck2: " +  wzweck2 + " doesn't exist");
			
				//home  coord consisteny check
				if(purpose.equals(HOME) & (end_coord.getX()!= homeCoord.getX() || end_coord.getY()!= homeCoord.getY())){
					home_coord_pids.add(pid);
				}
					
					
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
				//NetworkRoute route = new LinkNetworkRouteImpl(null, null);
				//leg.setRoute(route);
				//route.setDistance(distance);
				//route.setTravelTime(leg.getTravelTime());
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
			else {//first trip (from home, or from other place different than home if ausnr == -98!)
				
				ActivityImpl firstAct;
				if(!ausnr.equals("-98")){ firstAct = ((PlanImpl) plan).createAndAddActivity(HOME,start_coord);
				}else if(mode.equals("plane")){
					  firstAct = ((PlanImpl) plan).createAndAddActivity("airport",start_coord);
				}else if(mode.equals("train")){ firstAct = ((PlanImpl) plan).createAndAddActivity("train station",start_coord);
				}else if(mode.equals("ship")){ firstAct = ((PlanImpl) plan).createAndAddActivity("harbor",start_coord);
				}else if(wzweck1.equals("11")){ firstAct = ((PlanImpl) plan).createAndAddActivity("slept away from home/foreign property",start_coord);
				}else firstAct = ((PlanImpl) plan).createAndAddActivity("slept away from home/foreign property",start_coord);
								
				firstAct.setEndTime(departure);
				LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
				leg.setDepartureTime(departure);
				leg.setTravelTime(arrival-departure);
				leg.setArrivalTime(arrival);
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
		err_pids.add(home_coord_pids);
		
		return err_pids;
			
	}
		
		
}
