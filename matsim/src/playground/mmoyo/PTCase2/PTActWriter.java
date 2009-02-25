package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;

import org.matsim.basic.v01.BasicActImpl;
import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.Link;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.router.Dijkstra;
import org.matsim.router.util.LeastCostPathCalculator.Path;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.network.Node;

//import playground.mmoyo.PTRouter.PTNProximity;  //24 feb
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.Validators.PathValidator;

public class PTActWriter {
	private final Population population;
	private PTOb pt;
	//private PTNProximity ptnProximity;    //24 feb
	private PTTravelCost ptTravelCost;
	public PTTravelTime ptTravelTime;
	private Dijkstra dijkstra;

	private PTNode ptNode1;
	private PTNode ptNode2;

	public PTActWriter(PTOb ptOb){
		this.pt = ptOb;
		Config config = new org.matsim.config.Config();
		config = Gbl.createConfig(new String[]{pt.getConfig(), "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		Gbl.setConfig(config);
		//Gbl.getWorld().setNetworkLayer(pt.getPtNetworkLayer());
		//Gbl.getWorld().complete();

		this.ptTravelTime =new PTTravelTime(pt.getPtTimeTable());
		//this.ptnProximity= new PTNProximity(this.pt.getPtNetworkLayer());   //24 feb 
		this.dijkstra = new Dijkstra(this.pt.getPtNetworkLayer(), ptTravelCost, ptTravelTime);
		this.population = new org.matsim.population.Population(false);
		MatsimPopulationReader plansReader = new MatsimPopulationReader(this.population,this.pt.getPtNetworkLayer() );
		plansReader.readFile(pt.getPlansFile());
	}
	
	public void writePTActsLegs(){
		Population newPopulation = new org.matsim.population.Population(false);
		int x=0;
		
		PathValidator ptPathValidator = new PathValidator ();
		int valid=0;
		int invalid=0;
		
		//for (Person person: this.population.getPersons().values()) {
			Person person = population.getPerson("3937204");
			System.out.println(x + " id:" + person.getId());
			Plan plan = person.getPlans().get(0);
			
			boolean first =true;
			boolean addPerson= true;
			Act lastAct = null;
			Act thisAct= null;
			int legNum=0;

			Plan newPlan = new org.matsim.population.PlanImpl(person);
			for (Iterator<BasicActImpl> iter= plan.getIteratorAct(); iter.hasNext();){
				thisAct= (Act)iter.next();
			
				if (!first) {
					//System.out.println("====new plan");
					Coord lastActCoord = lastAct.getCoord();
		    		Coord actCoord = thisAct.getCoord();

		    		int distanceToDestination = (int)lastActCoord.calcDistance(actCoord);
		    		int distToWalk= distToWalk(person.getAge());
		    		if (distanceToDestination<= distToWalk){
		    			newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    	}else{
			    		Path path = this.pt.getPtRouter2().findRoute(lastActCoord, actCoord, lastAct.getEndTime(), distToWalk);
				    	if(path!=null){
			    			if (path.nodes.size()>1){
			    				createWlinks(lastActCoord, path, actCoord);
			    				double dw1 = pt.getPtNetworkLayer().getLink("linkW1").getLength();
			    				double dw2 = pt.getPtNetworkLayer().getLink("linkW2").getLength();
			    				if ((dw1+dw2)>=(distanceToDestination)){
			    					newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    				}else{
			    					//temporary  =(
			    					if( ptPathValidator.isValid(path)){valid++;}else{invalid++;}
			    					/*TODO: Ideally it should validate paths
			    					if (ptPathValidator.isValid(path)){
			    						 
			    						legNum= insertLegActs(path, lastAct.getEndTime(), legNum, newPlan);			    	
			    						valid++;
			    					}else{
			    						invalid++;
			    						newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    					}
			    					*/
			    					legNum= insertLegActs(path, lastAct.getEndTime(), legNum, newPlan);
			    				}//if dw1+dw2
			   				removeWlinks();
			    			}else{
			    				newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    				addPerson=false;				    				
			    			}//if path.nodes
			    		}else{
			    			newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    			addPerson=false;
			    		}//path=null
					}//distanceToDestination<= distToWalk
				}//if !First
				
		    	//TODO: this must be read from the city network not from pt network!!! 
		    	thisAct.setLink(this.pt.getPtNetworkLayer().getNearestLink(thisAct.getCoord()));
				
		    	newPlan.addAct(thisAct);
				lastAct = thisAct;
				first=false;
			}//Iterator<BasicActImpl>
			
			if (addPerson){
				person.exchangeSelectedPlan(newPlan, true);
				person.removeUnselectedPlans();
				newPopulation.addPerson(person);
			}
			x++;
		//}//for person
	
		//Write outplan XML
		System.out.println("writing output plan file...");
		Gbl.getConfig().plans().setOutputFile(this.pt.getOutPutFile());
		Gbl.getConfig().plans().setOutputVersion("v4");
		new PopulationWriter(newPopulation).write();
		System.out.println("Done");
		
		System.out.println("valid:" + valid +  " invalid:" + invalid);
	}//createPTActs

	private void createWlinks(Coord coord1, Path path, Coord coord2){
		ptNode1= this.pt.getPtNetworkFactory().CreateWalkingNode(pt.getPtNetworkLayer(), new IdImpl("w1"), coord1);
		ptNode2= this.pt.getPtNetworkFactory().CreateWalkingNode(pt.getPtNetworkLayer(), new IdImpl("w2"), coord2);
		path.nodes.add(0, ptNode1);
		path.nodes.add(ptNode2);
		this.pt.getPtNetworkFactory().createPTLink(this.pt.getPtNetworkLayer(), "linkW1", ptNode1 , (PTNode)path.nodes.get(1), "Walking");
		this.pt.getPtNetworkFactory().createPTLink(this.pt.getPtNetworkLayer(), "linkW2", (PTNode)path.nodes.get(path.nodes.size()-2) , ptNode2, "Walking");
	}
	
	private void removeWlinks(){
		pt.getPtNetworkLayer().removeLink(pt.getPtNetworkLayer().getLink("linkW1"));
		pt.getPtNetworkLayer().removeLink(pt.getPtNetworkLayer().getLink("linkW2"));
		pt.getPtNetworkLayer().removeNode(ptNode1);
		pt.getPtNetworkLayer().removeNode(ptNode2);
	}	
	
	private int distToWalk(int personAge){
		int distance=0;
		if (personAge>=60)distance=300; 
		if ((personAge>=40) && (personAge<60))distance=400;
		if ((personAge>=18) && (personAge<40))distance=600;
		if (personAge<18)distance=300;
		return distance;
	}

	public Path findRoute(Coord coord1, Coord coord2, double time, int distToWalk){
		//origincal code
		//PTNode[] NearStops1=  ptnProximity.getNearestBusStops(coord1, distToWalk, false);
		//PTNode[] NearStops2= ptnProximity.getNearestBusStops(coord2, distToWalk, false);

		Collection <Node> NearStops1 = pt.getPtNetworkLayer().getNearestNodes(coord1, distToWalk);
		Collection <Node> NearStops2 = pt.getPtNetworkLayer().getNearestNodes(coord2, distToWalk);
		
		ptNode1= this.pt.getPtNetworkFactory().CreateWalkingNode(this.pt.getPtNetworkLayer(), new IdImpl("W1"), coord1);
		ptNode2=this.pt.getPtNetworkFactory().CreateWalkingNode(this.pt.getPtNetworkLayer(), new IdImpl("W2"), coord2);
		List <Id> walkingLinkList1 = this.pt.getPtNetworkFactory().CreateWalkingLinks(this.pt.getPtNetworkLayer(), ptNode1, NearStops1, true);
		List <Id> walkingLinkList2 = this.pt.getPtNetworkFactory().CreateWalkingLinks(this.pt.getPtNetworkLayer(), ptNode2, NearStops2, false);
		
		Path path = dijkstra.calcLeastCostPath(ptNode1, ptNode2, time);

		this.pt.getPtNetworkFactory().removeWalkingLinks(this.pt.getPtNetworkLayer(), walkingLinkList1);
		this.pt.getPtNetworkFactory().removeWalkingLinks(this.pt.getPtNetworkLayer(), walkingLinkList2);
		this.pt.getPtNetworkLayer().removeNode(this.pt.getPtNetworkLayer().getNode("W1"));
		this.pt.getPtNetworkLayer().removeNode(this.pt.getPtNetworkLayer().getNode("W2"));
		this.pt.getPtNetworkLayer().removeNode(ptNode1);
		this.pt.getPtNetworkLayer().removeNode(ptNode2);
	
		return path;
	}
	
	private Leg walkLeg(int legNum, Act act1, Act act2){
		double walkDistance = coordDistance(act1.getCoord(), act2.getCoord());
		double walkTravelTime = walkTravelTime(walkDistance); 
		double depTime = act1.getEndTime();
		double arrTime = depTime + walkTravelTime;
		double distance= coordDistance(act1.getCoord(), act2.getCoord());
		return newPTLeg(legNum, Leg.Mode.walk, new ArrayList<Link>(), distance, depTime, walkTravelTime, arrTime);
	}
	
	private double linkDistance(Link link){
		return coordDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
	}
	
	private double coordDistance(Coord coord1, Coord coord2){
		CoordImpl coordImpl = new CoordImpl(coord1);
		return coordImpl.calcDistance(coord2); //the swiss coordinate system with 6 digit means meters
	}
	
	private double walkTravelTime(double distance){
		final double WALKING_SPEED = 0.9; //      4 km/h  human speed 
		return distance * WALKING_SPEED;
	}
	
	public int insertLegActs(Path path, double depTime, int legNum, Plan newPlan){
		List<Link> routeLinks = path.links;
		List<Link> legRouteLinks = new ArrayList<Link>();
		double accumulatedTime=depTime;
		double arrTime;
		double legTravelTime=0;
		double legDistance=0;
		double linkTravelTime=0;
		double linkDistance=0;
		int linkCounter=1;
		boolean first=true;
		String lastLinkType="";
		
		for(Link link: routeLinks){
			linkTravelTime=ptTravelTime.getLinkTravelTime(link,accumulatedTime)*60;
			linkDistance = linkDistance(link);
			
			if (link.getType().equals("Standard")){
				if (first){ //first PTAct: getting on
					newPlan.addAct(newPTAct("wait pt", link.getFromNode().getCoord(), link, accumulatedTime , linkTravelTime, accumulatedTime + linkTravelTime));	
					accumulatedTime =accumulatedTime+ linkTravelTime;
					first=false;
				}
				if (!lastLinkType.equals("Standard")){  //reset to start a new ptLeg
					legRouteLinks.clear();
					depTime=accumulatedTime;
					legTravelTime=0;
					legDistance=0;
				}
				legTravelTime=legTravelTime+(linkTravelTime); 
				legRouteLinks.add(link);
				if(linkCounter == (routeLinks.size()-1)){//Last PTAct: getting off
					arrTime= depTime+ legTravelTime;
					legDistance=legDistance + linkDistance;  
					//TODO: The legMode car is temporal only for visualization purposes
					newPlan.addLeg(newPTLeg(legNum++, Leg.Mode.car, legRouteLinks, legDistance, arrTime-legTravelTime, legTravelTime, arrTime));	
					//test
					//newPlan.addAct(newPTAct("exit pt veh", link.getFromNode().getCoord(), link, arrTime, 0, arrTime));
					newPlan.addAct(newPTAct("exit pt veh", link.getToNode().getCoord(), link, arrTime, 0, arrTime));
				}
				
			
			}else if(link.getType().equals("Transfer") || link.getType().equals("DetTransfer") ){  //add the PTleg and a Transfer Act
				if (lastLinkType.equals("Standard")){ 
					arrTime= depTime+ legTravelTime;
					legDistance= legDistance+ linkDistance;
					//TODO: The legMode car is temporal only for visualization purposes
					newPlan.addLeg(newPTLeg(legNum++, Leg.Mode.car, legRouteLinks, legDistance, depTime, legTravelTime, arrTime));
					//newPlan.addAct(newPTAct("wait pt", link.getFromNode().getCoord(), link, accumulatedTime, linkTravelTime, accumulatedTime + linkTravelTime));
					newPlan.addAct(newPTAct("Wait pt veh", link.getFromNode().getCoord(), link, accumulatedTime, linkTravelTime, accumulatedTime + linkTravelTime));
					first=false;
				}
				
				/*
				if (lastLinkType.equals("Transfer")){
					// 2 transfer links togheter???
					Act act = (Act)newPlan.getActsLegs().get(newPlan.getActsLegs().size()-1);
					act.setCoord(link.getFromNode().getCoord());
					act.setStartTime(act.getStartTime()+linkTravelTime);
					act.setDur( act.getDur()+linkTravelTime);
					act.setEndTime(act.getStartTime()+ act.getDur());
					act.setLink(link);
				}
				*/
			
			}
			else if (link.getType().equals("DetTransfer")){
				//it is divided into an walk leg (Walking to the near station) and a activity (Waiting for a PTV)

				//like a Walking leg				
				double walkTime= link.getLength()* this.ptTravelTime.WALKING_SPEED ;
				legRouteLinks.clear();
				legRouteLinks.add(link);
				arrTime= accumulatedTime+ walkTime;
				newPlan.addLeg(newPTLeg(legNum++, Leg.Mode.walk, legRouteLinks, linkDistance, accumulatedTime, walkTime, arrTime));
				
				//like a transfer link
				if (lastLinkType.equals("Standard")){  //-> how can be validated that the next link must be a standard link?
					double startWaitingTime = arrTime;
					double waitingTime = linkTravelTime -walkTime;  // The ptTravelTime must calculated it like this: travelTime = walk + transferTime;
					double endActTime= startWaitingTime + waitingTime;
					newPlan.addAct(newPTAct("Change ptv", link.getFromNode().getCoord(), link, startWaitingTime, waitingTime, endActTime));
					first=false;
				}
			}
			
			else if (link.getType().equals("Walking")){
				legRouteLinks.clear();
				legRouteLinks.add(link);
				linkTravelTime= linkTravelTime/60;
				arrTime= accumulatedTime+ linkTravelTime;
				newPlan.addLeg(newPTLeg(legNum++, Leg.Mode.walk, legRouteLinks, linkDistance, accumulatedTime, linkTravelTime, arrTime));
			}

			accumulatedTime =accumulatedTime+ linkTravelTime;
			lastLinkType = link.getType();
			linkCounter++;
		}//for Link
		return legNum;
	}//insert
	
	private Act newPTAct(String type, Coord coord, Link link, double startTime, double dur, double endTime){
		Act ptAct= new org.matsim.population.ActImpl(type, coord);
		ptAct.setStartTime(startTime);
		ptAct.setEndTime(endTime);
		//ptAct.setDuration(dur); Deprecated?
		ptAct.calculateDuration();
		ptAct.setLink(link);
		return ptAct;
	}
		
	private Leg newPTLeg(int num, Leg.Mode mode, List<Link> routeLinks, double distance, double depTime, double travTime, double arrTime){	
		CarRoute legRoute = new NodeCarRoute();
		if (mode!=Leg.Mode.walk){
			legRoute.setLinks(null, routeLinks, null);
		}
		legRoute.setTravelTime(travTime);
		legRoute.setDist(distance);  
	
		Leg leg = new org.matsim.population.LegImpl(mode);
		//leg.setNum(num); deprecated
		leg.setRoute(legRoute);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(arrTime);
		return leg;
	}
}

/*
 * public Path forceRoute(Coord coord1, Coord coord2, double time, int distToWalk){
		Path path=null;
		while ((path==null) && (distToWalk<1300)){
			path= findRoute(coord1, coord2, time, distToWalk);
			distToWalk= distToWalk+50;
		}
		return path;
	}
	*/
