package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
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

import playground.mmoyo.PTRouter.PTNode;

public class PTActWrter {
	private final Population population;
	private PTOb pt;
	
	public PTActWrter(PTOb ptOb){
		this.pt = ptOb;
		Config config = new org.matsim.config.Config();
		config = Gbl.createConfig(new String[]{pt.getConfig(), "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		Gbl.setConfig(config);
		Gbl.getWorld().setNetworkLayer(pt.getPtNetworkLayer());

		this.population = new org.matsim.population.Population(false);
		PopulationReaderMatsimV4 plansReader = new org.matsim.population.PopulationReaderMatsimV4(this.population);
		plansReader.readFile(pt.getPlansFile());
	}
	/*
	public Route findRoute(Coord coord1, Coord coord2, double time, int distToWalk){
		PTNode[] NearStops1=  ptnProximity.getNearestBusStops(coord1, distToWalk);
		PTNode[] NearStops2= ptnProximity.getNearestBusStops(coord2, distToWalk);
		PTNode ptNode1=ptNetworkFactory.CreateWalkingNode(ptNetworkLayer, new IdImpl("W1"), coord1);
		PTNode ptNode2=ptNetworkFactory.CreateWalkingNode(ptNetworkLayer, new IdImpl("W2"), coord2);
		List <IdImpl> walkingLinkList1 = ptNetworkFactory.CreateWalkingLinks(ptNetworkLayer, ptNode1, NearStops1, true);
		List <IdImpl> walkingLinkList2 = ptNetworkFactory.CreateWalkingLinks(ptNetworkLayer, ptNode2, NearStops2, false);
		
		Route route = dijkstra.calcLeastCostPath(ptNode1, ptNode2, time);
		
		ptNetworkFactory.removeWalkinkLinks(ptNetworkLayer, walkingLinkList1);
		ptNetworkFactory.removeWalkinkLinks(ptNetworkLayer, walkingLinkList2);
		this.ptNetworkLayer.removeNode(this.ptNetworkLayer.getNode("W1"));
		this.ptNetworkLayer.removeNode(this.ptNetworkLayer.getNode("W2"));
		this.ptNetworkLayer.removeNode(ptNode1);
		this.ptNetworkLayer.removeNode(ptNode2);

		if (route!=null){
			route.getRoute().remove(ptNode1);
			route.getRoute().remove(ptNode2);
		}
		return route;
	}
	
	public Route forceRoute(Coord coord1, Coord coord2, double time, int distToWalk){
		Route route=null;
		while (route==null && distToWalk<1300){
			route= findRoute(coord1, coord2, time, distToWalk);
			distToWalk= distToWalk+50;
		}
		return route;
	}
	
	
	public void createPTActs(final String outputFile, PTTimeTable2 ptTimetable){
		Population newPopulation = new org.matsim.population.Population(false);
		
		int x=0;
		for (Person person: this.population.getPersons().values()) {
			//Person person = population.getPerson("1005733");
			System.out.println(x + " id:" + person.getId());
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

		    		int distToWalk= distToWalk(person.getAge());
		    		//Route legRoute = this.ptRouter2.findRoute(lastActCoord, actCoord, lastAct.getEndTime(), distToWalk);
		    		Route legRoute = this.ptRouter2.forceRoute(lastActCoord, actCoord, lastAct.getEndTime(), distToWalk);
		    		if(legRoute!=null){
		    			if (legRoute.getRoute().size()>2){
			    			List<Object> listLegAct = new ArrayList<Object>();
			    	    	
			    			listLegAct=this.ptRouter2.findLegActs(legRoute, lastAct.getEndTime());

		    				boolean first= true;
			    	    	for (Iterator<Object> iter2 = listLegAct.iterator(); iter2.hasNext();){
			    	    		Object legAct = iter2.next();
			    	    		if(Leg.class.isInstance(legAct)){
			    	    			Leg ptLeg= (Leg)legAct;
			    	    			ptLeg.setNum(legNum++);
			    	    			//System.out.println("adding ptleg");
			    	    			newPlan.addLeg(ptLeg);
			    	    		}else{
			    	    			ptAct =(Act) legAct;
					    	    	if (first){
				    	    			//insert the walking leg from act location to bus stop and "waiting bus activity"
					    				walkLeg = walkLeg(legNum++, lastAct, ptAct);
					    				//System.out.println("adding walkleg1");
					    				newPlan.addLeg(walkLeg);

					    				/*
					    				double distanice=lastAct.getCoord().calcDistance(toPTNode.getCoord()
					    					lastAct.getEndTime()+ lastAct
					    				double duration = 
					    				
					    				
					    				//set values of first ptAct
					    				double arrTime=walkLeg.getArrTime();
					    				Id idPTNode = ptAct.getLink().getFromNode().getId();
					    				double nextDeparture= ptTimetable.nextDeparture(idPTNode, arrTime);
					    				double duration = nextDeparture- arrTime;
					    				ptAct.setStartTime(walkLeg.getArrTime());
					    				ptAct.setDur(duration);
					    				ptAct.setEndTime(nextDeparture);
					    				//Route route  =null;
					    					
					    				first=false;
					    	    	}
			    	    			
			    	    			//this should not be necesary anymore
			    	    			//ptAct.setLink(this.network.getNearestLink(ptAct.getCoord()));
			    	    			//System.out.println("adding ptAct");
			    	    			newPlan.addAct(ptAct);
			    	    			lastAct= ptAct;
			    	    		}//if act.classisinstance
			    	    	}//for iterator
			    	    	//System.out.println("adding Walkleg2");
			    	    	newPlan.addLeg(walkLeg(legNum++,lastAct,thisAct));
			    	    	
		    			}else{     // if router didn't find a PT connection then walk
		    				walkLeg = walkLeg(legNum++, lastAct,thisAct);
		    				//System.out.println("adding Walkleg3");
		    				newPlan.addLeg(walkLeg);
		    			}//legRoute.getRoute().size()>2
		    		}else{
	    				walkLeg = walkLeg(legNum++, lastAct,thisAct);
	    				//System.out.println("adding walkleg4");
	    				newPlan.addLeg(walkLeg);
		    		}//if(legRoute!=null)

				}//if val
				thisAct.setLink(this.network.getNearestLink(thisAct.getCoord()));
				//System.out.println("adding thisAct");
				newPlan.addAct(thisAct);
				
				lastAct = thisAct;
				val=true;
			}
			
			person.exchangeSelectedPlan(newPlan, true);
			person.removeUnselectedPlans();
			newPopulation.addPerson(person);
			x++;
		}//for person
	
		//Write outplan XML
		System.out.println("writing output plan file...");
		Gbl.getConfig().plans().setOutputFile(outputFile);
		Gbl.getConfig().plans().setOutputVersion("v4");
		new PopulationWriter(newPopulation).write();
		System.out.println("Done");
		
		
		System.out.println("writing pt network...");
		new NetworkWriter(this.network).write();
		System.out.println("done.");
		
		
	}//createPTActs

	
	private Leg walkLeg(int legNum, Act act1, Act act2){
		//set Walkleg
		double walkDistance = walkDistance(act1.getCoord(), act2.getCoord());
		double walkTravelTime = walkTravelTime(walkDistance); 
		double depTime = act1.getEndTime();
		double arrTime = depTime + walkTravelTime;

		//Set walkRoute
		Route walkRoute= new Route();
		walkRoute.setDist(walkDistance);
		walkRoute.setTravTime(walkTravelTime);

		Leg walkLeg1= new Leg(Leg.Mode.walk);
		walkLeg1.setNum(legNum);
		walkLeg1.setDepTime(depTime);
		walkLeg1.setTravTime(walkTravelTime); //walkLeg1.setTravTime(walkTravelTime);    
		walkLeg1.setArrTime(arrTime);
		walkLeg1.setRoute(walkRoute);
		
		return walkLeg1;
	}
	
	private double walkDistance(Coord coord1, Coord coord2){
		CoordImpl coordImpl = new CoordImpl(coord1);
		return coordImpl.calcDistance(coord2); //the swiss coordinate system with 6 digit means meters
	}
	
	private double walkTravelTime(double distance){
		final double WALKING_SPEED = 0.9; //      4 km/h  human speed 
		return distance * WALKING_SPEED;
	}
	

	//TODO: Check this
	private int distToWalk(int personAge){
		int distance=0;
		if (personAge>=60)distance=300; 
		if (personAge>=40 || personAge<60)distance=400;
		if (personAge>=18 || personAge<40)distance=800;
		if (personAge<18)distance=300;
		return distance;
	}

	
	
	
	
	*/
	
}
