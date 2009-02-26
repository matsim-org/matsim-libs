package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.matsim.basic.v01.BasicActImpl;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.population.LegImpl;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationWriter;
import org.matsim.population.routes.LinkCarRoute;
import org.matsim.router.Dijkstra;
import org.matsim.router.util.LeastCostPathCalculator.Path;

import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.Validators.PathValidator;

public class PTActWriter {
	private final Population population;
	private final PTOb pt;
	//private PTNProximity ptnProximity;    //24 feb
	private PTTravelCost ptTravelCost;
	public PTTravelTime ptTravelTime;
	private final Dijkstra dijkstra;

	private PTNode ptNode1;
	private PTNode ptNode2;

	public PTActWriter(final PTOb ptOb){
		this.pt = ptOb;
		Config config = new org.matsim.config.Config();
		config = Gbl.createConfig(new String[]{this.pt.getConfig(), "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		Gbl.setConfig(config);
		//Gbl.getWorld().setNetworkLayer(pt.getPtNetworkLayer());
		//Gbl.getWorld().complete();

		this.ptTravelTime =new PTTravelTime(this.pt.getPtTimeTable());
		//this.ptnProximity= new PTNProximity(this.pt.getPtNetworkLayer());   //24 feb
		this.dijkstra = new Dijkstra(this.pt.getPtNetworkLayer(), this.ptTravelCost, this.ptTravelTime);
		this.population = new PopulationImpl(false);
		MatsimPopulationReader plansReader = new MatsimPopulationReader(this.population,this.pt.getPtNetworkLayer() );
		plansReader.readFile(this.pt.getPlansFile());
	}

	public void writePTActsLegs(){
		Population newPopulation = new PopulationImpl(false);
		int x=0;

		PathValidator ptPathValidator = new PathValidator ();
		int valid=0;
		int invalid=0;

		for (Person person: this.population.getPersons().values()) {
			//Person person = population.getPerson("3937204");
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
					Coord lastActCoord = lastAct.getCoord();
		    		Coord actCoord = thisAct.getCoord();

		    		double distanceToDestination = lastActCoord.calcDistance(actCoord);
		    		double distToWalk= distToWalk(person.getAge());
		    		if (distanceToDestination<= distToWalk){
		    			newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    	}else{
			    		Path path = this.pt.getPtRouter2().findRoute(lastActCoord, actCoord, lastAct.getEndTime(), distToWalk);
				    	if(path!=null){
			    			if (path.nodes.size()>1){
			    				createWlinks(lastActCoord, path, actCoord);
			    				double dw1 = this.pt.getPtNetworkLayer().getLink("linkW1").getLength();
			    				double dw2 = this.pt.getPtNetworkLayer().getLink("linkW2").getLength();
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
				
		    	//Attention: this should be read from the city network not from pt network!!! 
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
		}//for person

		System.out.println("writing output plan file...");
		Gbl.getConfig().plans().setOutputFile(this.pt.getOutPutFile());
		Gbl.getConfig().plans().setOutputVersion("v4");
		new PopulationWriter(newPopulation).write();
		System.out.println("Done");

		System.out.println("valid:" + valid +  " invalid:" + invalid);
	}//createPTActs

	private void createWlinks(final Coord coord1, final Path path, final Coord coord2){
		this.ptNode1= this.pt.getPtNetworkFactory().CreateWalkingNode(this.pt.getPtNetworkLayer(), new IdImpl("w1"), coord1);
		this.ptNode2= this.pt.getPtNetworkFactory().CreateWalkingNode(this.pt.getPtNetworkLayer(), new IdImpl("w2"), coord2);
		path.nodes.add(0, this.ptNode1);
		path.nodes.add(this.ptNode2);
		this.pt.getPtNetworkFactory().createPTLink(this.pt.getPtNetworkLayer(), "linkW1", this.ptNode1 , (PTNode)path.nodes.get(1), "Walking");
		this.pt.getPtNetworkFactory().createPTLink(this.pt.getPtNetworkLayer(), "linkW2", (PTNode)path.nodes.get(path.nodes.size()-2) , this.ptNode2, "Walking");
	}

	private void removeWlinks(){
		this.pt.getPtNetworkLayer().removeLink(this.pt.getPtNetworkLayer().getLink("linkW1"));
		this.pt.getPtNetworkLayer().removeLink(this.pt.getPtNetworkLayer().getLink("linkW2"));
		this.pt.getPtNetworkLayer().removeNode(this.ptNode1);
		this.pt.getPtNetworkLayer().removeNode(this.ptNode2);
	}

	private int distToWalk(final int personAge){
		int distance=0;
		if (personAge>=60)distance=300;
		if ((personAge>=40) && (personAge<60))distance=400;
		if ((personAge>=18) && (personAge<40))distance=600;
		if (personAge<18)distance=300;
		return distance;
	}

	public Path findRoute(final Coord coord1, final Coord coord2, final double time, final int distToWalk){
		//23 feb
		//PTNode[] NearStops1=  ptnProximity.getNearestBusStops(coord1, distToWalk, false);
		//PTNode[] NearStops2= ptnProximity.getNearestBusStops(coord2, distToWalk, false);

		Collection <Node> NearStops1 = this.pt.getPtNetworkLayer().getNearestNodes(coord1, distToWalk);
		Collection <Node> NearStops2 = this.pt.getPtNetworkLayer().getNearestNodes(coord2, distToWalk);

		this.ptNode1= this.pt.getPtNetworkFactory().CreateWalkingNode(this.pt.getPtNetworkLayer(), new IdImpl("W1"), coord1);
		this.ptNode2=this.pt.getPtNetworkFactory().CreateWalkingNode(this.pt.getPtNetworkLayer(), new IdImpl("W2"), coord2);
		List <Id> walkingLinkList1 = this.pt.getPtNetworkFactory().CreateWalkingLinks(this.pt.getPtNetworkLayer(), this.ptNode1, NearStops1, true);
		List <Id> walkingLinkList2 = this.pt.getPtNetworkFactory().CreateWalkingLinks(this.pt.getPtNetworkLayer(), this.ptNode2, NearStops2, false);

		Path path = this.dijkstra.calcLeastCostPath(this.ptNode1, this.ptNode2, time);

		this.pt.getPtNetworkFactory().removeWalkingLinks(this.pt.getPtNetworkLayer(), walkingLinkList1);
		this.pt.getPtNetworkFactory().removeWalkingLinks(this.pt.getPtNetworkLayer(), walkingLinkList2);
		this.pt.getPtNetworkLayer().removeNode(this.pt.getPtNetworkLayer().getNode("W1"));
		this.pt.getPtNetworkLayer().removeNode(this.pt.getPtNetworkLayer().getNode("W2"));
		this.pt.getPtNetworkLayer().removeNode(this.ptNode1);
		this.pt.getPtNetworkLayer().removeNode(this.ptNode2);

		return path;
	}



	private double linkDistance(final Link link){
		return coordDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
	}

	private double coordDistance(final Coord coord1, final Coord coord2){
		return coord1.calcDistance(coord2);
	}

	private double walkTravelTime(final double distance){
		final double WALKING_SPEED = 0.9; //      4 km/h  human speed
		return distance * WALKING_SPEED;
	}

	public int insertLegActs(final Path path, double depTime, int legNum, final Plan newPlan){
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
			linkTravelTime=this.ptTravelTime.getLinkTravelTime(link,accumulatedTime)*60;
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

					//Attention: The legMode car is temporal only for visualization purposes
					newPlan.addLeg(newPTLeg(legNum++, Leg.Mode.car, legRouteLinks, legDistance, arrTime-legTravelTime, legTravelTime, arrTime));
										
					//test: Check what method describes the location more exactly
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

	private Act newPTAct(final String type, final Coord coord, final Link link, final double startTime, final double dur, final double endTime){
		Act ptAct= new org.matsim.population.ActImpl(type, coord);
		ptAct.setStartTime(startTime);
		ptAct.setEndTime(endTime);
		//ptAct.setDuration(dur); Deprecated?
		ptAct.calculateDuration();
		ptAct.setLink(link);
		return ptAct;
	}

	private Leg newPTLeg(final int num, final Leg.Mode mode, final List<Link> routeLinks, final double distance, final double depTime, final double travTime, final double arrTime){
		CarRoute legRoute = new LinkCarRoute(null, null); 
		//CarRoute legRoute = new NodeCarRoute();  25 feb
		
		if (mode!=Leg.Mode.walk){
			legRoute.setLinks(null, routeLinks, null);
		}
		
		legRoute.setTravelTime(travTime);
		legRoute.setDist(distance);
		Leg leg = new LegImpl(mode);
		//leg.setNum(num); deprecated
		leg.setRoute(legRoute);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(arrTime);
		return leg;
	}

	private Leg walkLeg(final int legNum, final Act act1, final Act act2){
		double distance= coordDistance(act1.getCoord(), act2.getCoord());
		double walkTravelTime = walkTravelTime(distance); 
		double depTime = act1.getEndTime();
		double arrTime = depTime + walkTravelTime;
		return newPTLeg(legNum, Leg.Mode.walk, new ArrayList<Link>(), distance, depTime, walkTravelTime, arrTime);
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
