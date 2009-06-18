package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.*;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.mmoyo.TransitSimulation.SimplifyPtLegs2;
import playground.mmoyo.TransitSimulation.TransitRouteFinder;
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.Pedestrian.Walk;
import org.matsim.core.population.PopulationReader;
import playground.marcel.pt.transitSchedule.TransitSchedule;

/**
 * Reads a plan file, finds a PT connection between two acts creating new PT legs and acts between them
 * and writes a output_plan file
 */
public class PTActWriter {
	private Walk walk = new Walk();
	private final Population population;
	private NetworkLayer net; 
	private PTRouter2 ptRouter;
	private String outputFile;
	private String plansFile;
	
	private Node originNode;
	private Node destinationNode;
	private Link walkLink1;
	private Link walkLink2;
	
	public PTActWriter(final PTOb ptOb){
		this.ptRouter = ptOb.getPtRouter2();
		this.net= ptOb.getPtNetworkLayer();
		String conf = ptOb.getConfig();
		this.outputFile = ptOb.getOutPutFile();
		this.plansFile =  ptOb.getPlansFile();
		
		Config config = new Config();
		config = Gbl.createConfig(new String[]{conf, "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		
		this.population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(this.population,net);
		plansReader.readFile(plansFile);
	}

	public void SimplifyPtLegs(){
		Population outPopulation = new PopulationImpl();
		PopulationReader plansReader = new MatsimPopulationReader(outPopulation,net);
		plansReader.readFile(outputFile);
		
		SimplifyPtLegs2 SimplifyPtLegs = new SimplifyPtLegs2();
		
		for (Person person: this.population.getPersons().values()) {
			//if (true){ Person person = population.getPersons().get(new IdImpl("3937204"));
			System.out.println(person.getId());
			SimplifyPtLegs.run(person.getPlans().get(0));
		}
		
		System.out.println("writing output plan file...");
		new PopulationWriter(this.population, outputFile, "v4").write();
		System.out.println("Done");	
	}

	/**
	 * Shows in console the legs that are created between the plan activities 
	 */
	public void printPTLegs(TransitSchedule transitSchedule){
		TransitRouteFinder transitRouteFinder= new TransitRouteFinder (transitSchedule);
		Person person = population.getPersons().get(new IdImpl("2180188"));

		Plan plan = person.getPlans().get(0);
 		Activity act1 = (Activity)plan.getPlanElements().get(0);
		Activity act2 = (Activity)plan.getPlanElements().get(2);
		List<Leg> legList = transitRouteFinder.calculateRoute (act1, act2, person );
		for (Leg leg : legList){
			System.out.println(leg);
		}
	}
	
	
	public void findRouteForActivities(){
		Population newPopulation = new PopulationImpl();
		int numPlans=0;

		//PathValidator ptPathValidator = new PathValidator ();
		int trips=0;
		int valid=0;
		int invalid=0;
		int inWalkRange=0;
		int lessThan2Node =0;
		int nulls =0;
		
		//List<Double> travelTimes = new ArrayList<Double>();  <-This is for the performance test
		//List<Path> invalidPaths = new ArrayList<Path>();  
		List<Double> durations = new ArrayList<Double>();  
		List<Path> validPaths = new ArrayList<Path>();
		
		for (Person person: this.population.getPersons().values()) {
		//if ( true ) {
		//Person person = population.getPersons().get(new IdImpl("812233"));
			System.out.println(numPlans + " id:" + person.getId());
			Plan plan = person.getPlans().get(0);

			boolean first =true;
			boolean addPerson= true;
			Activity lastAct = null;
			Activity thisAct= null;
			//double travelTime=0;
			
			double startTime=0;
			double duration=0;
			
			Plan newPlan = new PlanImpl(person);
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					thisAct= (Activity) pe;
				if (!first) {
					Coord lastActCoord = lastAct.getCoord();
		    		Coord actCoord = thisAct.getCoord();

					trips++;
		    		double distanceToDestination = CoordUtils.calcDistance(lastActCoord, actCoord);
		    		double distToWalk= walk.distToWalk(person.getAge());
		    		if (distanceToDestination<= distToWalk){
		    		//if (true){
		    			newPlan.addLeg(walkLeg(lastAct,thisAct));
		    			inWalkRange++;
		    		}else{
			    		startTime = System.currentTimeMillis();
			    		Path path = ptRouter.findPTPath(lastActCoord, actCoord, lastAct.getEndTime(), distToWalk);
			    		duration= System.currentTimeMillis()-startTime;

			    		if(path!=null){
				    		//travelTime=travelTime+ path.travelTime;
				    		if (path.nodes.size()>1){
				    			
				    			createWlinks(lastActCoord, path, actCoord);
				    			double dw1 = walkLink1.getLength();
								double dw2 = walkLink2.getLength();
			    				///if ((dw1+dw2)>=(distanceToDestination)){
			    				///	newPlan.addLeg(walkLeg(lastAct,thisAct));
			    				///	inWalkRange++;
			    				///}else{
			    					//if (ptPathValidator.isValid(path)){   /** all paths are valid now */
			    			    		durations.add(duration);
			    						insertLegActs(path, lastAct.getEndTime(), newPlan);
			    						valid++;
			    						validPaths.add(path);
			    					
			    				    //}else{
			    					//	newPlan.addLeg(walkLeg(lastAct,thisAct));
			    					//	invalid++;
			    				    //		//addPerson=false;    //18 may
			    					//	//invalidPaths.add(path);
			    					//}
			    					
			    					//legNum= insertLegActs(path, lastAct.getEndTime(), legNum, newPlan);
			    				///}
			    				removeWlinks();
			    			}else{
			    				newPlan.addLeg(walkLeg(lastAct, thisAct));
			    				//addPerson=false;   //18 may
			    				lessThan2Node++;
			    			}
			    		}else{
			    			newPlan.addLeg(walkLeg(lastAct,thisAct));
			    			//addPerson=false;   //18 may
			    			nulls++;
			    		}
					}
				}
				
		    	//-->Attention: this should be read from the city network not from pt network!!! 
		    	thisAct.setLink(net.getNearestLink(thisAct.getCoord()));
		    	
		    	newPlan.addActivity(newPTAct(thisAct.getType(), thisAct.getCoord(), thisAct.getLink(), thisAct.getStartTime(), thisAct.getEndTime()));
				lastAct = thisAct;
				first=false;
				}
			}

			if (addPerson){
				person.exchangeSelectedPlan(newPlan, true);
				person.removeUnselectedPlans();
				newPopulation.addPerson(person);
			}
			numPlans++;
			//travelTimes.add(travelTime);
		}//for person

		System.out.println("writing output plan file...");
		new PopulationWriter(newPopulation, outputFile, "v4").write();
		System.out.println("Done");
		System.out.println("plans:        " + numPlans + "\n--------------");
		System.out.println("\nTrips:      " + trips +  "\nvalid:        " + valid +  "\ninvalid:      " + invalid + "\ninWalkRange:  "+ inWalkRange + "\nnulls:        " + nulls + "\nlessThan2Node:" + lessThan2Node);
		
		
		System.out.println("===Printing routing durations");
		double total=0;
		double average100=0;
		int x=1;
		for (double d : durations ){
			total=total+d;
			average100= average100 + d;
			if(x==100){
				//System.out.println(average100/100);
				average100=0;
				x=0;
			}
			x++;
		}
				
		System.out.println("total " + total + " average: " + (total/durations.size()));
		
		
		//for(Path path: invalidPaths){
		//if (true){
		/*
		if (invalidPaths.size()>0){	
			System.out.println("invalid");
			for(Path path: invalidPaths){
				for(Link link: path.links){
					System.out.print(link.getType() + "-");
				}
			}
		}
		System.out.println("");
		//}
		*/
		/*
		if (validPaths.size()>0){
			System.out.println("valid:" );
			for(Path path: validPaths){
				for(Link link: path.links){
					System.out.print(link.getType() + "-");
				}
				System.out.println("");
			}
		}
		System.out.println("");
		*/	
		/*
		// start the control(l)er with the network and plans as defined above
		Controler controler = new Controler(Gbl.getConfig(),net,(Population) newPopulation);
		// this means existing files will be over-written.  Be careful!
		controler.setOverwriteFiles(true);
		// start the matsim iterations (configured by the config file)
		controler.run();
		*/
			
	}//createPTActs
	
	
	/**
	 * Cuts up the found path into acts and legs according to the type of links contained in the path
	 */
	public void insertLegActs(final Path path, double depTime, final Plan newPlan){
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
			// -> The travel time was already calculated. The result should be stored, not calculated again!
			linkTravelTime=this.ptRouter.ptTravelTime.getLinkTravelTime(link,accumulatedTime)*60;
			linkDistance = link.getLength();

			if (link.getType().equals("Standard")){
				if (first){ //first PTAct: getting on
					newPlan.addActivity(newPTAct("wait pt", link.getFromNode().getCoord(), link, accumulatedTime , accumulatedTime + linkTravelTime));
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
					newPlan.addLeg(newPTLeg(TransportMode.car, legRouteLinks, legDistance, arrTime-legTravelTime, legTravelTime, arrTime));
										
					//test: Check what method describes the location more exactly
					//newPlan.addAct(newPTAct("exit pt veh", link.getFromNode().getCoord(), link, arrTime, 0, arrTime));
					newPlan.addActivity(newPTAct("exit pt veh", link.getToNode().getCoord(), link, arrTime, arrTime));
				}

			}else if(link.getType().equals("Transfer") ){  //add the PTleg and a Transfer Act
				if (lastLinkType.equals("Standard")){
					arrTime= depTime+ legTravelTime;
					legDistance= legDistance+ linkDistance;
					//-->: The legMode car is temporal only for visualization purposes
					newPlan.addLeg(newPTLeg(TransportMode.car, legRouteLinks, legDistance, depTime, legTravelTime, arrTime));
					//newPlan.addAct(newPTAct("wait pt", link.getFromNode().getCoord(), link, accumulatedTime, linkTravelTime, accumulatedTime + linkTravelTime));
					double endTime = accumulatedTime + linkTravelTime;
					newPlan.addActivity(newPTAct("Wait pt veh", link.getFromNode().getCoord(), link, accumulatedTime, endTime));
					first=false;
				}
			}
			else if (link.getType().equals("DetTransfer")){
				/**standard links*/
				arrTime= depTime+ legTravelTime;
				legDistance= legDistance+ linkDistance;
				newPlan.addLeg(newPTLeg(TransportMode.car, legRouteLinks, legDistance, depTime, legTravelTime, arrTime));		
				
				/**act exit ptv*/
				newPlan.addActivity(newPTAct("transf out", link.getToNode().getCoord(), link, arrTime, arrTime));
				
				/**like a Walking leg*/
				double walkTime= walk.walkTravelTime(link.getLength());
				legRouteLinks.clear();
				legRouteLinks.add(link);
				depTime=arrTime;
				arrTime= depTime+ walkTime;
				newPlan.addLeg(newPTLeg(TransportMode.walk, legRouteLinks, linkDistance, depTime, walkTime, arrTime));

				/**wait pt*/
				double endActTime= depTime + linkTravelTime; // The ptTravelTime must be calculated it like this: travelTime = walk + transferTime;
				newPlan.addActivity(newPTAct("transf in", link.getFromNode().getCoord(), link, arrTime, endActTime));
				first=false;
			}

			else if (link.getType().equals("Walking")){
				legRouteLinks.clear();
				legRouteLinks.add(link);
				linkTravelTime= linkTravelTime/60;
				arrTime= accumulatedTime+ linkTravelTime;
				newPlan.addLeg(newPTLeg(TransportMode.walk, legRouteLinks, linkDistance, accumulatedTime, linkTravelTime, arrTime));
			}

			accumulatedTime =accumulatedTime+ linkTravelTime;
			lastLinkType = link.getType();
			linkCounter++;
		}//for Link
	}//insert

	
	private Activity newPTAct(final String type, final Coord coord, final Link link, final double startTime, final double endTime){
		Activity ptAct= new ActivityImpl(type, coord, link);
		ptAct.setStartTime(startTime);
		ptAct.setEndTime(endTime);
		return ptAct;
	}

	private Leg newPTLeg(TransportMode mode, final List<Link> routeLinks, final double distance, final double depTime, final double travTime, final double arrTime){
		NetworkRoute legRoute = new LinkNetworkRoute(null, null); 
		
		if (mode!=TransportMode.walk){
			legRoute.setLinks(null, routeLinks, null);
		}else{
			//mode= TransportMode.car;   //-> temporarly for Visualizer
		}
		
		legRoute.setTravelTime(travTime);
		legRoute.setDistance(distance);
		Leg leg = new LegImpl(mode);
		leg.setRoute(legRoute);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(arrTime);
		return leg;
	}

	private Leg walkLeg(final Activity act1, final Activity act2){
		double distance= CoordUtils.calcDistance(act1.getCoord(), act2.getCoord());
		double walkTravelTime = walk.walkTravelTime(distance);
		double depTime = act1.getEndTime();
		double arrTime = depTime + walkTravelTime;
		return newPTLeg(TransportMode.walk, new ArrayList<Link>(), distance, depTime, walkTravelTime, arrTime);
	}
	
	/*
	private void createWlinks(final Coord coord1, Path path, final Coord coord2){
		//-> move and use it in Link factory?
		//originNode= createNode(new IdImpl("w1"), coord1);
		destinationNode= ptRouter.createWalkingNode(new IdImpl("w1"), coord1);
		destinationNode= ptRouter.createWalkingNode(new IdImpl("w2"), coord2);
		path.nodes.add(0, originNode);
		path.nodes.add(destinationNode);
		Node firstStation= path.nodes.get(1);
		Node lastStation= path.nodes.get(path.nodes.size()-2);
		System.out.println(firstStation==null);
		System.out.println(lastStation==null);
		walkLink1 = net.createLink(new IdImpl("linkW1"), originNode, path.nodes.get(1), CoordUtils.calcDistance(originNode.getCoord(), firstStation.getCoord()) , 1, 1, 1, "0", "Walking");
		walkLink2 = net.createLink(new IdImpl("linkW2"), lastStation, destinationNode, CoordUtils.calcDistance(lastStation.getCoord(), destinationNode.getCoord()) , 1, 1, 1, "0", "Walking");
	}
	*/
	
	private void createWlinks(final Coord coord1, Path path, final Coord coord2){
		//-> move and use it in Link factory
		originNode= createWalkingNode(new IdImpl("w1"), coord1);
		destinationNode= createWalkingNode(new IdImpl("w2"), coord2);
		path.nodes.add(0, originNode);
		path.nodes.add(destinationNode);
		walkLink1 = createPTLink("linkW1", originNode , path.nodes.get(1), "Walking");
		walkLink2 = createPTLink("linkW2", path.nodes.get(path.nodes.size()-2) , destinationNode, "Walking");
	}
	
	/**
	 * Creates a temporary origin or destination node
	 * avoids the method net.createNode because it is not necessary to rebuild the Quadtree*/
	public Node createWalkingNode(Id id, Coord coord){
		Node node = new PTNode(id, coord, "Walking");
		net.getNodes().put(id, node);
		return node;
	}
	
	public Link createPTLink(String strIdLink, Node fromNode, Node toNode, String type){
		//->use link factory
		double length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
		return net.createLink( new IdImpl(strIdLink), fromNode, toNode, length, 1, 1, 1, "0", type); 
	}
	
	private void removeWlinks(){
		net.removeLink(walkLink1);
		net.removeLink(walkLink2);
		net.removeNode(originNode);
		net.removeNode(destinationNode);
	}
	
}