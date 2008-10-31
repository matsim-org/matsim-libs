package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReaderMatsimV4;
import org.matsim.population.PopulationWriter;
import org.matsim.population.Route;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

public class PTActivityCreator {
	private final String CONFIG;
	private final Population plans;
	private final PTRouter2 ptRouter2;
	private final NetworkLayer network;

	//constructor
	public PTActivityCreator(final NetworkLayer ptNetworkLayer, final String Config, final String plansFile, final PTRouter2 ptRouter2) {
		this.CONFIG= Config;
		this.ptRouter2 = ptRouter2;
		this.network = ptNetworkLayer;

		Config config = new org.matsim.config.Config();
		config = Gbl.createConfig(new String[]{this.CONFIG, "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		Gbl.setConfig(config);
		Gbl.getWorld().setNetworkLayer(ptNetworkLayer);

		this.plans = new org.matsim.population.Population(false);
		PopulationReaderMatsimV4 plansReader = new org.matsim.population.PopulationReaderMatsimV4(this.plans);
		plansReader.readFile(plansFile);
	}

	public void createPTActs(final String outputFile){
		int x=0;
		for (Person person: this.plans.getPersons().values()) {
			System.out.println(x);
			//Person person = plans.getPerson("1008146");
			Plan plan = person.getPlans().get(0);
			
			boolean val =false;
			Act lastAct = null;
			Act thisAct= null;
			int legNum=0;
			
		
			Plan newPlan = new Plan(person);
			for (Iterator iter= plan.getIteratorAct(); iter.hasNext();) {
				
		    	Act ptAct=null;
		    	Leg walkLeg=null;

		    	thisAct= (Act)iter.next();
				
		    	if (val) {
					Coord lastActCoord = lastAct.getCoord();
		    		Coord actCoord = thisAct.getCoord();

		    		Route legRoute = this.ptRouter2.findRoute(lastActCoord, actCoord, lastAct.getEndTime());
		    		if(legRoute!=null){
		    			if (legRoute.getRoute().size()>2){// if router didn't find a PT connection then walk
			    			List<Object> listLegAct = new ArrayList<Object>();
			    	    	listLegAct=this.ptRouter2.findLegActs(legRoute, lastAct.getEndTime());
			    	    	
			    	    	for (Iterator<Object> iter2 = listLegAct.iterator(); iter2.hasNext();) {
			    	    		Object legAct = iter2.next();
			    	    		if(Leg.class.isInstance(legAct)){
			    	    			Leg ptLeg= (Leg)legAct;
			    	    			ptLeg.setNum(legNum++);
			    	    			newPlan.addLeg(ptLeg);
			    	    		}else{
			    	    			ptAct =(Act) legAct;
			    	    			ptAct.setLink(this.network.getNearestLink(ptAct.getCoord()));
			    	    			newPlan.addAct(ptAct);
			    	    		
			    	    			lastAct= ptAct;
			    	    		}//if act.classisinstance
			    	    	}//for iterator
			    	    	newPlan.addLeg(walkLeg(legNum++,lastAct,thisAct));
			    	    	
		    			}else{
		    				walkLeg = walkLeg(legNum++, lastAct,thisAct);
		    				newPlan.addLeg(walkLeg);
		    			}//legRoute.getRoute().size()>2
		    		}else{
	    				walkLeg = walkLeg(legNum++, lastAct,thisAct);
	    				newPlan.addLeg(walkLeg);
		    		}//if(legRoute!=null)

				}//if val
				thisAct.setLink(this.network.getNearestLink(thisAct.getCoord()));
				newPlan.addAct(thisAct);
				
				lastAct = thisAct;
				val=true;
			}
			
			person.exchangeSelectedPlan(newPlan, true);
			person.removeUnselectedPlans();
			
			x++;
		}//for person
	
		//Write outplan XML
		System.out.println("writing output plan file...");
		Gbl.getConfig().plans().setOutputFile(outputFile);
		Gbl.getConfig().plans().setOutputVersion("v4");
		new PopulationWriter(this.plans).write();
		System.out.println("Done");
		
		/*
		System.out.println("writing pt network...");
		new NetworkWriter(this.network).write();
		System.out.println("done.");
		*/
	}//createPTActs

	
	private Leg walkLeg(int legNum, Act act1, Act act2){
		//Set walkRoute
		CoordImpl coordImpl = new CoordImpl(act1.getCoord());
		Route walkRoute= new Route();
		double walkDistance= coordImpl.calcDistance(act2.getCoord()); //the swiss coordinate system with 6 digit means meters
		double walkingSpeed= 0.9;   //3600/4000;   must be adecuated to agent's age. By the being time it is used the average 4 km/h 
		double walkTravelTime = walkDistance * walkingSpeed;
		//walkDistance = java.text.DecimalFormat df = new java.text.DecimalFormat("0.0");
		walkRoute.setDist(walkDistance);
		walkRoute.setTravTime(walkTravelTime);

		//set Walkleg
		double depTime = act1.getEndTime();
		double arrTime = depTime + walkTravelTime;
		
		Leg walkLeg1= new Leg(Leg.Mode.walk);
		walkLeg1.setNum(legNum);
		walkLeg1.setDepTime(depTime);
		walkLeg1.setTravTime(walkTravelTime); //walkLeg1.setTravTime(walkTravelTime);    
		walkLeg1.setArrTime(arrTime);
		walkLeg1.setRoute(walkRoute);
		return walkLeg1;
	}
	
	/*
	person = plans.getPerson("1001350");
	org.matsim.population.Plan plan = person.getPlans().get(0);
	plan.setSelected(true);
	org.matsim.population.Act firstAct = plan.getFirstActivity();
	org.matsim.population.Leg firstLeg = plan.getNextLeg(firstAct);
	org.matsim.population.Act secondAct = plan.getNextActivity(firstLeg);
	 */
	
	
	/* CODIGO ORIGINAL
	public void createPTActs(final String outputFile){
		//Version with one act, one plan, one person
	

		int x=0;
		for (Person person: this.plans.getPersons().values()) {
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

			//	Leg originalLeg= plan.getNextLeg(act);
				
				
				if (val) {
					Act secondAct = act;
					Coord c1 = firstAct.getCoord();
		    		Coord c2 = secondAct.getCoord();

		    		Route legRoute = this.ptRouter2.findRoute(c1, c2, firstAct.getEndTime());

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

		    				secondAct.setLink(this.network.getNearestLink(secondAct.getCoord()));
		    				newPlan.addAct(secondAct);
		    				val=true;

							//////////////////////////////////////////////////
		    			}else{
	    	    			List<Object> listLegAct = new ArrayList<Object>();
		    	    		listLegAct=this.ptRouter2.findLegActs(legRoute, firstAct.getEndTime());
		    	    		for (Iterator<Object> iter2 = listLegAct.iterator(); iter2.hasNext();) {
		    	    			Object legAct = iter2.next();
		    	    			if(Act.class.isInstance(legAct)){
		    	    				((Act) legAct).setLink(this.network.getNearestLink(((Act) legAct).getCoord()));
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
		    			//newPlan.addLeg(originalLeg);
		    			//TODO: make something if the router did not find a pt route
		    			// - search near stations to transfer
		    			// - use bycicle`
		    			//
		    		}//if legRoute
				}else{
					

					val=true;
				}//if val
				act.setLink(this.network.getNearestLink(act.getCoord()));
				newPlan.addAct(act);
				
				firstAct = act;
			}//for iterator iter

			person.exchangeSelectedPlan(newPlan, true);
			person.removeUnselectedPlans();

		}//for Person person

		//Write outplan XML
		System.out.println("writing output plan file...");
		Gbl.getConfig().plans().setOutputFile(outputFile);
		Gbl.getConfig().plans().setOutputVersion("v4");
		new PopulationWriter(this.plans).write();
		System.out.println("Done");
		
		System.out.println("writing pt network...");
		new NetworkWriter(this.network).write();
		System.out.println("done.");
	}//createPTActs
	
   */

}
