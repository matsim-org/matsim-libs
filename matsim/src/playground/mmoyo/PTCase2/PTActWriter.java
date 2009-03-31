package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.BasicActivityImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.network.NetworkLayer;

import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.Validators.PathValidator;

public class PTActWriter {
	private final Population population;
	private PTTravelCost ptTravelCost;
	public PTTravelTime ptTravelTime;
	private final Dijkstra dijkstra;
	private NetworkLayer net; 
	private PTRouter2 ptRouter;
	private String outputFile;
	
	private Node originNode;
	private Node destinationNode;
	private Link walkLink1;
	private Link walkLink2;
	
	public PTActWriter(final PTOb ptOb){
		ptRouter = ptOb.getPtRouter2();
		net= ptOb.getPtNetworkLayer();
		String conf = ptOb.getConfig();
		PTTimeTable2 timeTable = ptOb.getPtTimeTable();
		outputFile = ptOb.getOutPutFile();
		
		Config config = new org.matsim.core.config.Config();
		config = Gbl.createConfig(new String[]{conf, "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		Gbl.setConfig(config);

		ptTravelTime =new PTTravelTime(timeTable);
		dijkstra = new Dijkstra(net, ptTravelCost, ptTravelTime);
		population = new PopulationImpl(false);
		MatsimPopulationReader plansReader = new MatsimPopulationReader(this.population,net);
		String plansFile = ptOb.getPlansFile();
		plansReader.readFile(plansFile);
	}

	public void writePTActsLegs(){
		Population newPopulation = new PopulationImpl(false);
		int x=0;

		PathValidator ptPathValidator = new PathValidator ();
		int valid=0;
		int invalid=0;
		int trips=0;
		//List<Double> travelTimes = new ArrayList<Double>();  <-This is for the performance test
		List<Double> durations = new ArrayList<Double>();  
		
		//for (Person person: this.population.getPersons().values()) {
		if ( true ) {
		Person person = population.getPersons().get(new IdImpl("3937204"));
		
			System.out.println(x + " id:" + person.getId());
			Plan plan = person.getPlans().get(0);

			boolean first =true;
			boolean addPerson= true;
			Activity lastAct = null;
			Activity thisAct= null;
			int legNum=0;
			double travelTime=0;
			
			double startTime=0;
			double duration=0;
			
			Plan newPlan = new PlanImpl(person);
			
			for (Iterator<BasicActivityImpl> iter= plan.getIteratorAct(); iter.hasNext();){
				thisAct= (Activity)iter.next();
				
				if (!first) {
					Coord lastActCoord = lastAct.getCoord();
		    		Coord actCoord = thisAct.getCoord();

					trips++;
		    		double distanceToDestination = CoordUtils.calcDistance(lastActCoord, actCoord);
		    		double distToWalk= distToWalk(person.getAge());
		    		if (distanceToDestination<= distToWalk){
		    			newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    	}else{
			    		startTime = System.currentTimeMillis();
			    		Path path = ptRouter.findRoute(lastActCoord, actCoord, lastAct.getEndTime(), distToWalk);
			    		duration= System.currentTimeMillis()-startTime;
			    		System.out.println("duration of patch search:" + duration);
			    		durations.add(duration);
		    		
			    		if(path!=null){
				    					    			
				    		//travelTime=travelTime+ path.travelTime;
				    		
				    		if (path.nodes.size()>1){
				    			createWlinks(lastActCoord, path, actCoord);
			    				double dw1 = net.getLink("linkW1").getLength();
			    				double dw2 = net.getLink("linkW2").getLength();
			    				if ((dw1+dw2)>=(distanceToDestination)){
			    					newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    				}else{

			    					if (ptPathValidator.isValid(path)){
			    						legNum= insertLegActs(path, lastAct.getEndTime(), legNum, newPlan);
			    						valid++;
			    					}else{
			    						invalid++;
			    						newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    					}
			    					
			    					//legNum= insertLegActs(path, lastAct.getEndTime(), legNum, newPlan);
			    				}//if dw1+dw2
			   				removeWlinks();
			    			}else{
			    				newPlan.addLeg(walkLeg(legNum++, lastAct, thisAct));
			    				addPerson=false;
			    			}//if path.nodes
			    		}else{
			    			newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    			addPerson=false;
			    		}//path=null
					}//distanceToDestination<= distToWalk
				}//if !First
				
		    	//-->Attention: this should be read from the city network not from pt network!!! 
		    	thisAct.setLink(net.getNearestLink(thisAct.getCoord()));

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
			//travelTimes.add(travelTime);
		}//for person

		System.out.println("writing output plan file...");
		Gbl.getConfig().plans().setOutputFile(outputFile);
		Gbl.getConfig().plans().setOutputVersion("v4");
		new PopulationWriter(newPopulation).write();
		System.out.println("Done");
		System.out.println("Trips:" + trips);
		System.out.println("valid:" + valid +  " invalid:" + invalid);

		
		System.out.println("===Imprimiendo duracion de los ruteos");
		double total=0;
		for (double d : durations ){
			if (d > 0.0){
				System.out.println(d);
				total=total+d;
			}
		}
		System.out.println("average: " + (total/durations.size()));


		// start the control(l)er with the network and plans as defined above
		Controler controler = new Controler(Gbl.getConfig(),net,(Population) newPopulation);
		// this means existing files will be over-written.  Be careful!
		controler.setOverwriteFiles(true);
		// start the matsim iterations (configured by the config file)
		controler.run();

	}//createPTActs

	
	private void createWlinks(final Coord coord1, final Path path, final Coord coord2){
		originNode= ptRouter.CreateWalkingNode(new IdImpl("w1"), coord1);
		destinationNode= ptRouter.CreateWalkingNode(new IdImpl("w2"), coord2);
		path.nodes.add(0, originNode);
		path.nodes.add(destinationNode);
		walkLink1 = ptRouter.createPTLink("linkW1", originNode , path.nodes.get(1), "Walking");
		walkLink2 = ptRouter.createPTLink("linkW2", path.nodes.get(path.nodes.size()-2) , destinationNode, "Walking");
	}
	
	private void removeWlinks(){
		net.removeLink(walkLink1);
		net.removeLink(walkLink2);
		net.removeNode(originNode);
		net.removeNode(destinationNode);
	}

	private int distToWalk(final int personAge){
		// TODO [kn] Sehe das gerade. Ich bin wenig begeistert über solche ad-hoc Festlegungen von Verhaltensparametern. kai, mar09 
		int distance=0;
		if (personAge>=60)distance=300;
		if ((personAge>=40) && (personAge<60))distance=400;
		if ((personAge>=18) && (personAge<40))distance=600;
		if (personAge<18)distance=300;
		return distance;
	}

	/*
	public Path findRoute(final Coord coord1, final Coord coord2, final double time, final int distToWalk){
		Collection <Node> NearStops1 = net.getNearestNodes(coord1, distToWalk);
		Collection <Node> NearStops2 = net.getNearestNodes(coord2, distToWalk);

		this.ptNode1= this.pt.getPtNetworkFactory().CreateWalkingNode(net, new IdImpl("W1"), coord1);
		this.ptNode2=this.pt.getPtNetworkFactory().CreateWalkingNode(net, new IdImpl("W2"), coord2);
		List <Id> walkingLinkList1 = this.pt.getPtNetworkFactory().CreateWalkingLinks(net, this.ptNode1, NearStops1, true);
		List <Id> walkingLinkList2 = this.pt.getPtNetworkFactory().CreateWalkingLinks(net, this.ptNode2, NearStops2, false);

		Path path = this.dijkstra.calcLeastCostPath(this.ptNode1, this.ptNode2, time);

		this.pt.getPtNetworkFactory().removeWalkingLinks(net, walkingLinkList1);
		this.pt.getPtNetworkFactory().removeWalkingLinks(net, walkingLinkList2);
		net.removeNode(net.getNode("W1"));
		net.removeNode(net.getNode("W2"));
		net.removeNode(this.ptNode1);
		net.removeNode(this.ptNode2);

		return path;
	}
	*/

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
			linkDistance = link.getLength();

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
					//-->: The legMode car is temporal only for visualization purposes
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

	private Activity newPTAct(final String type, final Coord coord, final Link link, final double startTime, final double dur, final double endTime){
		Activity ptAct= new org.matsim.core.population.ActivityImpl(type, coord);
		ptAct.setStartTime(startTime);
		ptAct.setEndTime(endTime);
		//ptAct.setDuration(dur); Deprecated?
		ptAct.calculateDuration();
		ptAct.setLink(link);
		return ptAct;
	}

	private Leg newPTLeg(final int num, final Leg.Mode mode, final List<Link> routeLinks, final double distance, final double depTime, final double travTime, final double arrTime){
		NetworkRoute legRoute = new LinkNetworkRoute(null, null); 
		//CarRoute legRoute = new NodeCarRoute();  25 feb
		
		if (mode!=Leg.Mode.walk){
			legRoute.setLinks(null, routeLinks, null);
		}
		
		legRoute.setTravelTime(travTime);
		legRoute.setDistance(distance);
		Leg leg = new LegImpl(mode);
		//leg.setNum(num); deprecated
		leg.setRoute(legRoute);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(arrTime);
		return leg;
	}

	private Leg walkLeg(final int legNum, final Activity act1, final Activity act2){
		double distance= CoordUtils.calcDistance(act1.getCoord(), act2.getCoord());
		double walkTravelTime = walkTravelTime(distance); 
		double depTime = act1.getEndTime();
		double arrTime = depTime + walkTravelTime;
		return newPTLeg(legNum, Leg.Mode.walk, new ArrayList<Link>(), distance, depTime, walkTravelTime, arrTime);
	}
}

	
