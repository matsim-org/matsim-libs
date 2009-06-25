package playground.mmoyo.PTRouter;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.mmoyo.TransitSimulation.LogicFactory;
import playground.mmoyo.TransitSimulation.SimplifyPtLegs;
import playground.mmoyo.TransitSimulation.TransitRouteFinder;
import playground.mmoyo.TransitSimulation.LogicToPlainConverter;
/**
 * Reads a plan file, finds a PT connection between two acts creating new PT legs and acts between them
 * and writes a output_plan file
 */
public class PTActWriter {
	private Walk walk = new Walk();
	private final Population population;
	private String outputFile;
	private String plansFile;
	private Node originNode;
	private Node destinationNode;
	private Link walkLink1;
	private Link walkLink2;
	
	private NetworkLayer logicNet;
	private PTRouter2 ptRouter;
	private LogicToPlainConverter logicToPlainConverter;
	private boolean withTransitSchedule = false;
	
	@Deprecated
	public PTActWriter(final PTOb ptOb){
		this.ptRouter = ptOb.getPtRouter2();
		this.logicNet= ptOb.getPtNetworkLayer();
		this.outputFile = ptOb.getOutPutFile();
		this.plansFile =  ptOb.getPlansFile();
		
		String strConf = ptOb.getConfig();
		Config config = new Config();
		config = Gbl.createConfig(new String[]{ strConf, "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		
		this.population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(this.population, logicNet);
		plansReader.readFile(plansFile);
	}
	
	/** Constructor with Transit Schedule*/
	public PTActWriter(TransitSchedule transitSchedule, final String configFile, final String plansFile, final String outputFile){
		withTransitSchedule= true;
		this.outputFile= outputFile;
		this.plansFile= plansFile;
		
		LogicFactory logicFactory = new LogicFactory(transitSchedule);
		this.logicNet= logicFactory.getLogicNet();
		this.ptRouter = logicFactory.getPTRouter();
		this.logicToPlainConverter = logicFactory.getLogicToPlainConverter();
		
		Config config = new Config();
		config = Gbl.createConfig(new String[]{ configFile, "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		
		this.population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(this.population, logicNet);
		plansReader.readFile(plansFile);	
	}
		
	public void SimplifyPtLegs(){
		Population outPopulation = new PopulationImpl();
		PopulationReader plansReader = new MatsimPopulationReader(outPopulation,logicNet);
		plansReader.readFile(outputFile);
		
		SimplifyPtLegs SimplifyPtLegs = new SimplifyPtLegs();
		
		for (Person person: this.population.getPersons().values()) {
			//if (true){ Person person = population.getPersons().get(new IdImpl("3937204"));
			System.out.println(person.getId());
			SimplifyPtLegs.run(person.getPlans().get(0));
		}
		
		System.out.println("writing output plan file...");
		new PopulationWriter(this.population, outputFile, "v4").write();
		System.out.println("done");	
	}

	/**
	 * Shows in console the legs that are created between the plan activities 
	 */
	public void printPTLegs(final TransitSchedule transitSchedule){
		TransitRouteFinder transitRouteFinder= new TransitRouteFinder (transitSchedule);
		Person person = population.getPersons().get(new IdImpl("2180188"));

		Plan plan = person.getPlans().get(0);
 		Activity act1 = (Activity)plan.getPlanElements().get(0);
		Activity act2 = (Activity)plan.getPlanElements().get(2);
		List<Leg> legList = transitRouteFinder.calculateRoute (act1, act2, person);
		for (Leg leg : legList){
			System.out.println(leg);
		}
	}
	
	public void findRouteForActivities(){
		Population newPopulation = new PopulationImpl();
		int numPlans=0;

		int trips=0;
		int inWalkRange=0;
		int lessThan2Node =0;
		int nulls =0;
		
		List<Double> durations = new ArrayList<Double>();  
		
		for (Person person: this.population.getPersons().values()) {
		//if ( true ) {
			//Person person = population.getPersons().get(new IdImpl("3246022")); // 5636428  2949483 
 			System.out.println(numPlans + " id:" + person.getId());
			Plan plan = person.getPlans().get(0);

			boolean first =true;
			boolean addPerson= true;
			Activity lastAct = null;       
			Activity thisAct= null;		 
			
			double startTime=0;
			double duration=0;
			
			Plan newPlan = new PlanImpl(person);
			
			//for (PlanElement pe : plan.getPlanElements()) {   		//temporarily commented in order to find only the first leg
			for	(int elemIndex=0; elemIndex<3; elemIndex++){            //jun09  finds only
				PlanElement pe= plan.getPlanElements().get(elemIndex);  //jun09  the first trip
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
				    			if (path.nodes.size()>1){
					    			createWlinks(lastActCoord, path, actCoord);
				    			    durations.add(duration);
				    				insertLegActs(path, lastAct.getEndTime(), newPlan);
				    				removeWlinks();
				    			}else{
				    				newPlan.addLeg(walkLeg(lastAct, thisAct));
				    				lessThan2Node++;
				    			}
				    		}else{
				    			newPlan.addLeg(walkLeg(lastAct,thisAct));
				    			nulls++;
				    		}
			    		}
					}
				
			    	//-->Attention: this should be read from the plain network not from logic network! 
			    	thisAct.setLink(logicNet.getNearestLink(thisAct.getCoord()));
					
			    	
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
		}//for person

		if (withTransitSchedule)logicToPlainConverter.convertToPlain(newPopulation);
		
		System.out.println("writing output plan file...");
		new PopulationWriter(newPopulation, outputFile, "v4").write();
		System.out.println("Done");
		System.out.println("plans:        " + numPlans + "\n--------------");
		System.out.println("\nTrips:      " + trips +  "\ninWalkRange:  "+ inWalkRange + "\nnulls:        " + nulls + "\nlessThan2Node:" + lessThan2Node);
		
		System.out.println("printing routing durations");
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
					newPlan.addActivity(newPTAct("transf", link.getFromNode().getCoord(), link, accumulatedTime, endTime));
					first=false;
				}
			}
			else if (link.getType().equals("DetTransfer")){
				/**standard links*/
				arrTime= depTime+ legTravelTime;
				legDistance= legDistance + linkDistance;
				newPlan.addLeg(newPTLeg(TransportMode.car, legRouteLinks, legDistance, depTime, legTravelTime, arrTime));		
				
				/**act exit ptv*/
				newPlan.addActivity(newPTAct("transf off", link.getFromNode().getCoord(), link, arrTime, arrTime));
				
				/**like a Walking leg*/
				double walkTime= walk.walkTravelTime(link.getLength());
				legRouteLinks.clear();
				legRouteLinks.add(link);
				depTime=arrTime;
				arrTime= depTime+ walkTime;
				newPlan.addLeg(newPTLeg(TransportMode.walk, legRouteLinks, linkDistance, depTime, walkTime, arrTime));

				/**wait pt*/
				double endActTime= depTime + linkTravelTime; // The ptTravelTime must be calculated it like this: travelTime = walk + transferTime;
				newPlan.addActivity(newPTAct("transf on", link.getToNode().getCoord(), link, arrTime, endActTime));
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
		logicNet.getNodes().put(id, node);
		return node;
	}
	
	public Link createPTLink(String strIdLink, Node fromNode, Node toNode, String type){
		//->use link factory
		double length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
		return logicNet.createLink( new IdImpl(strIdLink), fromNode, toNode, length, 1, 1, 1, "0", type); 
	}
	
	private void removeWlinks(){
		logicNet.removeLink(walkLink1);
		logicNet.removeLink(walkLink2);
		logicNet.removeNode(originNode);
		logicNet.removeNode(destinationNode);
	}

	
}