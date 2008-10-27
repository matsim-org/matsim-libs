package playground.mmoyo.PTCase2;

import org.matsim.network.NetworkLayer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.PopulationWriter;
import org.matsim.population.Route;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.config.Config;
import org.matsim.population.Population;
import org.matsim.population.PopulationReaderMatsimV4;

public class PTActivityCreator {
	private final String CONFIG;
	private final Population plans;
	private final PTRouter2 ptRouter2;
	
	//constructor
	public PTActivityCreator(NetworkLayer ptNetworkLayer, String Config, String plansFile, PTRouter2 ptRouter2) {
		CONFIG= Config;
		this.ptRouter2 = ptRouter2;
		
		Config config = new org.matsim.config.Config();
		config = Gbl.createConfig(new String[]{CONFIG, "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		Gbl.setConfig(config);
		Gbl.getWorld().setNetworkLayer(ptNetworkLayer);

		plans = new org.matsim.population.Population(false);
		PopulationReaderMatsimV4 plansReader = new org.matsim.population.PopulationReaderMatsimV4(plans);
		plansReader.readFile(plansFile);
	}
	
	public void createPTActs(String outputFile){
		//Version wth one act, one plan, one person
		/*
		person = plans.getPerson("1001350");
		org.matsim.population.Plan plan = person.getPlans().get(0);
		plan.setSelected(true);
		org.matsim.population.Act firstAct = plan.getFirstActivity();
		org.matsim.population.Leg firstLeg = plan.getNextLeg(firstAct);
		org.matsim.population.Act secondAct = plan.getNextActivity(firstLeg);
		 */

		int x=0;
		for (Person person: plans.getPersons().values()) {
			System.out.println(++x + " person: " + person.getId());
			
			Plan plan = person.getPlans().get(0);
			plan.setSelected(false);
			Plan newPlan = new Plan(person);
			
			//Iterate in plans  of the person and insert the new ptActs - ptlegs in the new ptPlan
			boolean val =false;
			Act firstAct = null;
			int legNum=0;
			for (Iterator iter= plan.getIteratorAct(); iter.hasNext();) {
				Act act= (Act)iter.next();
				
				if (val) {
					Act secondAct = act;	
					Coord c1 = firstAct.getCoord();
		    		Coord c2 = secondAct.getCoord();
		    		
		    		Route legRoute = ptRouter2.findRoute(c1, c2, firstAct.getEndTime());
		    		
		    		if(legRoute!=null){  
		    			
		    			//INSERT WALKING LEG
		    			if (legRoute.getRoute().size()<2){// if router didn't find a PT connection then walk
	
		    				//calculate walking route 
		    				Route walkRoute= new Route();
		    				CoordImpl coordImpl = new CoordImpl(c1);
		    				double walkDistance= coordImpl.calcDistance(c2); //the swiss coordinate system with 6 digit means meters
		    				double walkingSpeed= 3600/4000;  // must be adecuated to agent's age. By the being time it is used the average 4 km/h http://en.wikipedia.org/wiki/Walking#cite_note-4
		    				double walkTravelTime = walkDistance * walkingSpeed; 
		    				//walkDistance = java.text.DecimalFormat df = new java.text.DecimalFormat("0.0");
		    				walkRoute.setDist(walkDistance);
		    				walkRoute.setTravTime(walkTravelTime);
	
		    				//setting Walk leg 
		    				Leg walkLeg= new Leg(Leg.Mode.walk);
		    				walkLeg.setNum(legNum);
		    				walkLeg.setDepTime(firstAct.getEndTime());
		    				walkLeg.setTravTime(walkRoute.getTravTime());
		    				walkLeg.setArrTime(firstAct.getEndTime()+ walkLeg.getTravTime());
		    				walkLeg.setRoute(walkRoute);
		    				newPlan.addLeg(walkLeg);
		    				legNum++;
		    				
		    				newPlan.addAct(secondAct);
		    				val=true;
		    				
							//////////////////////////////////////////////////
		    			}else{
	    	    			List<Object> listLegAct = new ArrayList<Object>();
		    	    		listLegAct=ptRouter2.findLegActs(legRoute, firstAct.getEndTime());
		    	    		for (Iterator<Object> iter2 = listLegAct.iterator(); iter2.hasNext();) {
		    	    			Object legAct = iter2.next();	
		    	    			if(Act.class.isInstance(legAct)){
		    	    				newPlan.addAct((Act)legAct);
		    	    			}else{
		    	    				Leg ptLeg = (Leg)legAct;
		    	    				ptLeg.setNum(legNum);
		    	    				newPlan.addLeg(ptLeg);
		    	    				legNum++;
		    	    			}
		    	    		}//for iterator iter2
		    			}//legRoute.getRoute().size()<2
		    		
		    		}else{
		    			
		    			//TODO: make something if the router did not find a  pt route
		    			// - search near stations to transfer
		    			// - use bycicle`
		    			//
		    		}//if legRoute
				}else{
					newPlan.addAct(act);
	
					val=true;
				}//if val
				firstAct = act;
			}//for iterator iter 
		
			person.exchangeSelectedPlan(newPlan, true);
			person.removeUnselectedPlans();
	
		}//for Person person
		
		//Write outplan XML
		Gbl.getConfig().plans().setOutputFile(outputFile); 
		Gbl.getConfig().plans().setOutputVersion("v4");
		final PopulationWriter plansWriter = new PopulationWriter(plans);
		plans.addAlgorithm(plansWriter);
		plansWriter.write();
		
	}//constructor
}
