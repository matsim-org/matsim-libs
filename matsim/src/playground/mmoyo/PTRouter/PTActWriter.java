package playground.mmoyo.PTRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.api.TransitSchedule;

import playground.mmoyo.TransitSimulation.LogicFactory;
import playground.mmoyo.TransitSimulation.LogicIntoPlainTranslator;
import playground.mmoyo.TransitSimulation.SimplifyPtLegs;
import playground.mmoyo.TransitSimulation.TransitRouteFinder;
import playground.mmoyo.deprecVersion.PTOb;
/**
 * Reads a plan file, finds a PT connection between two acts creating new PT legs and acts between them
 * and writes a output_plan file
 */
public class PTActWriter {
	private Walk walk = new Walk();
	private final PopulationImpl population;
	private String outputFile;
	private String plansFile;
	private NodeImpl originNode;
	private NodeImpl destinationNode;
	private LinkImpl walkLink1;
	private LinkImpl walkLink2;
	
	private NetworkLayer logicNet;
	private NetworkLayer plainNet;
	private PTRouter ptRouter;
	private LogicIntoPlainTranslator logicToPlainConverter;
	private boolean withTransitSchedule = false;
	
	private final String ACCESS = "Access";
	private final String STANDARD = "Standard";
	private final String WALKING = "Walking";
	private final String TRANSFER = "Transfer";
	private final String DETTRANSFER = "DetTransfer";
	
	Map <Id, Double> costMap = new TreeMap <Id, Double>();
	
	//public Population detouredPopulation = new PopulationImpl();  //temporal to find plans with detoured trips
	
	@Deprecated
	public PTActWriter(final PTOb ptOb){
		this.ptRouter = ptOb.getPtRouter();
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
		this.plainNet= logicFactory.getPlainNet();
		this.ptRouter = logicFactory.getPTRouter();
		this.logicToPlainConverter = logicFactory.getLogicToPlainTranslator();
		
		Config config = new Config();
		config = Gbl.createConfig(new String[]{ configFile, "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		
		this.population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(this.population, this.plainNet);
		plansReader.readFile(plansFile);
	}
	
	public void SimplifyPtLegs(){
		
		SimplifyPtLegs SimplifyPtLegs = new SimplifyPtLegs();
		
		for (PersonImpl person: population.getPersons().values()) {
			//if (true){ Person person = population.getPersons().get(new IdImpl("3937204"));
			System.out.println(person.getId());
			SimplifyPtLegs.run(person.getPlans().get(0));
		}
		
		System.out.println("writing output plan file..." + outputFile );
		new PopulationWriter(this.population, outputFile, "v4").write();
		System.out.println("done");	
	}

	/**
	 * Shows in console the legs that are created between the plan activities 
	 */
	public void printPTLegs(final TransitSchedule transitSchedule){
		TransitRouteFinder transitRouteFinder= new TransitRouteFinder (transitSchedule);
		
		//for (PersonImpl person: this.population.getPersons().values()) {
		if (true){	
			PersonImpl person = population.getPersons().get(new IdImpl("2180188"));   //2180188
	
			PlanImpl plan = person.getPlans().get(0);
	 		ActivityImpl act1 = (ActivityImpl)plan.getPlanElements().get(0);
			ActivityImpl act2 = (ActivityImpl)plan.getPlanElements().get(2);
			List<LegImpl> legList = transitRouteFinder.calculateRoute (act1, act2, person);
			
			for (LegImpl leg : legList){
				NetworkRoute networkRoute = (NetworkRoute)leg.getRoute(); 
				System.out.println(" ");
				System.out.println(leg.toString());
				
				for (Node node : networkRoute.getNodes()){
					if (node != null) System.out.print(" " + node.getId() + " " );
				}
			}
		}
	}
	
	public void findRouteForActivities(){
		PopulationImpl newPopulation = new PopulationImpl();
		
		int numPlans=0;

		int trips=0;
		int inWalkRange=0;
		int lessThan2Node =0;
		int nulls =0;
		
		List<Double> durations = new ArrayList<Double>();  
		
		for (PersonImpl person: this.population.getPersons().values()) {
			//if ( true ) {
			//PersonImpl person = population.getPersons().get(new IdImpl("35420")); // 5636428  2949483 
 			System.out.println(numPlans + " id:" + person.getId());
			PlanImpl plan = person.getPlans().get(0);

			boolean first =true;
			boolean addPerson= true;
			ActivityImpl lastAct = null;       
			ActivityImpl thisAct= null;		 
			
			double startTime=0;
			double duration=0;
			
			PlanImpl newPlan = new PlanImpl(person);
			
			//for (PlanElement pe : plan.getPlanElements()) {   		//temporarily commented in order to find only the first leg
			for	(int elemIndex=0; elemIndex<3; elemIndex++){            //jun09  finds only
				PlanElement pe= plan.getPlanElements().get(elemIndex);  //jun09  the first trip
				if (pe instanceof ActivityImpl) {  				
					thisAct= (ActivityImpl) pe;					
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

				    			this.costMap.put(person.getId(), path.travelTime);
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
		
		// This is to compare the travelTime of path with different cost calculations
		/*
		System.out.println ("---------------------------------------------");
		for(Map.Entry <Id,Double> entry: costMap.entrySet() ){
			Id idAgent = entry.getKey(); 
			Double travelTime = entry.getValue();
			System.out.println (idAgent + "-"  + Double.toString(travelTime));
		}
		*/
		
		
	}//createPTActs
	
	/**
	 * Cuts up the found path into acts and legs according to the type of links contained in the path
	 */
	public void insertLegActs(final Path path, double depTime, final PlanImpl newPlan){
		List<Link> routeLinks = path.links;
		List<Link> legRouteLinks = new ArrayList<Link>();
		double accumulatedTime=depTime;
		double arrTime;
		double legTravelTime=0;
		double legDistance=0;
		double linkTravelTime=0;
		double linkDistance=0;
		double walkTime=0;
		int linkIndex=1;
		boolean first=true;
		LinkImpl lastLink = null;
		
		for(Link link2: routeLinks){
			LinkImpl link = (LinkImpl) link2;
			linkTravelTime=this.ptRouter.ptTravelTime.getLinkTravelTime(link,accumulatedTime);
			linkDistance = link.getLength();
			
			if (link.getType().equals(STANDARD)){
				if (!lastLink.getType().equals(STANDARD)){  //reset to start a new ptLeg
					legRouteLinks.clear();
					depTime=accumulatedTime;
					legTravelTime=0;
					legDistance=0;
				}
				legTravelTime=legTravelTime+linkTravelTime;
				legRouteLinks.add(link);
				if(linkIndex == (routeLinks.size()-1)){//Last PTAct: getting off
					arrTime= depTime+ legTravelTime;
					legDistance=legDistance + linkDistance;
					newPlan.addLeg(newPTLeg(TransportMode.car, legRouteLinks, legDistance, depTime, legTravelTime, arrTime)); //Attention: The legMode car is temporal only for visualization purposes
					newPlan.addActivity(newPTAct("exit pt veh", link.getToNode().getCoord(), link, arrTime, arrTime)); //describes the location
				}

			}else if(link.getType().equals(TRANSFER) ){  //add the PTleg and a Transfer Act
				//if (lastLink.getType().equals(STANDARD)){
					arrTime= depTime+ legTravelTime;
					legDistance= legDistance+ linkDistance;
					newPlan.addLeg(newPTLeg(TransportMode.car, legRouteLinks, legDistance, depTime, legTravelTime, arrTime)); //-->: The legMode car is temporal only for visualization purposes
					//newPlan.addAct(newPTAct("wait pt", link.getFromNode().getCoord(), link, accumulatedTime, linkTravelTime, accumulatedTime + linkTravelTime));
					double endTime = accumulatedTime + linkTravelTime;
					newPlan.addActivity(newPTAct("transf", link.getFromNode().getCoord(), link, accumulatedTime, endTime));
					first=false;
					
					/*
					////////////////////////////// find roundabout connections
					NodeImpl nodeA = path.nodes.get(0);
					NodeImpl nodeB = path.nodes.get(path.nodes.size()-1);
					double a_bDistance = CoordUtils.calcDistance(nodeA.getCoord() , nodeB.getCoord());
					double a_TransterDistance = CoordUtils.calcDistance(nodeA.getCoord() , link.getFromNode().getCoord());
					double b_TransterDistance = CoordUtils.calcDistance(nodeB.getCoord() , link.getFromNode().getCoord());
					if(a_TransterDistance > a_bDistance || b_TransterDistance > a_bDistance){
						PersonImpl detouredPerson = newPlan.getPerson();
						if (!detouredPopulation.getPersons().containsValue(deoturedPerson))
							detouredPopulation.addPerson(newPlan.getPerson());
					}
					///////////////////////////*/
					
				//}
			}else if (link.getType().equals(DETTRANSFER)){
				/**standard links*/
				arrTime= depTime+ legTravelTime;
				legDistance= legDistance + linkDistance;
				newPlan.addLeg(newPTLeg(TransportMode.car, legRouteLinks, legDistance, depTime, legTravelTime, arrTime));		
				
				/**act exit ptv*/
				newPlan.addActivity(newPTAct("transf off", link.getFromNode().getCoord(), link, arrTime, arrTime));
				
				/**like a Walking leg*/
				walkTime= walk.walkTravelTime(link.getLength());
				legRouteLinks.clear();
				legRouteLinks.add(link);
				depTime=arrTime;
				arrTime= arrTime + walkTime;
				newPlan.addLeg(newPTLeg(TransportMode.walk, legRouteLinks, linkDistance, depTime, walkTime, arrTime));

				/**wait pt*/
				double endTime= depTime + linkTravelTime; // The ptTravelTime must be calculated like this: travelTime = walk + transferTime;
				newPlan.addActivity(newPTAct("transf on", link.getToNode().getCoord(), link, arrTime, endTime));
				first=false;
			
			}else if (link.getType().equals(WALKING)){
				legRouteLinks.clear();
				legRouteLinks.add(link);
				arrTime= accumulatedTime+ linkTravelTime;
				newPlan.addLeg(newPTLeg(TransportMode.walk, legRouteLinks, linkDistance, accumulatedTime, linkTravelTime, arrTime));
			
			}else if (link.getType().equals(ACCESS)){
					
				/**like a Walking leg*/
				walkTime= walk.walkTravelTime(link.getLength());
				legRouteLinks.clear();
				legRouteLinks.add(link);
				depTime=accumulatedTime;
				arrTime = depTime + walkTime;
				newPlan.addLeg(newPTLeg(TransportMode.walk, legRouteLinks, linkDistance, depTime, walkTime, arrTime));

				/**wait pt*/
				double endTime= depTime + linkTravelTime; 
				newPlan.addActivity(newPTAct("wait pt", link.getToNode().getCoord(), link, arrTime, endTime));
				first=false;
			}		
					/*
					double waitTime  = ((PTTravelTime)ptRouter.ptTravelTime).transferTime(lastLink, accumulatedTime);					
					newPlan.addActivity(newPTAct("wait pt", link.getFromNode().getCoord(), link, accumulatedTime , accumulatedTime + waitTime));
					accumulatedTime = accumulatedTime + waitTime; 
					first=false;
					*/
			accumulatedTime =accumulatedTime+ linkTravelTime;
			lastLink = link;
			linkIndex++;
		}//for Link
	}//insert

	private ActivityImpl newPTAct(final String type, final Coord coord, final LinkImpl link, final double startTime, final double endTime){
		ActivityImpl ptAct= new ActivityImpl(type, coord, link);
		ptAct.setStartTime(startTime);
		ptAct.setEndTime(endTime);
		return ptAct;
	}

	private LegImpl newPTLeg(TransportMode mode, final List<Link> routeLinks, final double distance, final double depTime, final double travTime, final double arrTime){
		NetworkRoute legRoute = new LinkNetworkRoute(null, null); 
		
		if (mode!=TransportMode.walk){
			legRoute.setLinks(null, routeLinks, null);
		}else{
			//mode= TransportMode.car;   //-> temporarly for Visualizer
		}
		
		legRoute.setTravelTime(travTime);
		legRoute.setDistance(distance);
		LegImpl leg = new LegImpl(mode);
		leg.setRoute(legRoute);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(arrTime);
		return leg;
	}

	private LegImpl walkLeg(final ActivityImpl act1, final ActivityImpl act2){
		double distance= CoordUtils.calcDistance(act1.getCoord(), act2.getCoord());
		double walkTravelTime = walk.walkTravelTime(distance);
		double depTime = act1.getEndTime();
		double arrTime = depTime + walkTravelTime;
		return newPTLeg(TransportMode.walk, new ArrayList<Link>(), distance, depTime, walkTravelTime, arrTime);
	}
	
	private void createWlinks(final Coord coord1, Path path, final Coord coord2){
		//-> move and use it in Link factory
		originNode= createWalkingNode(new IdImpl("W1"), coord1);
		destinationNode= createWalkingNode(new IdImpl("W2"), coord2);
		path.nodes.add(0, originNode);
		path.nodes.add(destinationNode);
		walkLink1 = createPTLink("linkW1", originNode , path.nodes.get(1), "Access");
		walkLink2 = createPTLink("linkW2", path.nodes.get(path.nodes.size()-2) , destinationNode, "Walking");
	}
	
	/**
	 * Creates a temporary origin or destination node
	 * avoids the method net.createNode because it is not necessary to rebuild the Quadtree*/
	public NodeImpl createWalkingNode(Id id, Coord coord){
		NodeImpl node = new PTNode(id, coord, "Walking");
		logicNet.getNodes().put(id, node);
		return node;
	}
	
	public LinkImpl createPTLink(String strIdLink, Node fromNode, Node toNode, String type){
		//->use link factory
		double length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
		return logicNet.createLink( new IdImpl(strIdLink), (NodeImpl) fromNode, (NodeImpl) toNode, length, 1, 1, 1, "0", type); 
	}
	
	private void removeWlinks(){
		logicNet.removeLink(walkLink1);
		logicNet.removeLink(walkLink2);
		logicNet.removeNode(originNode);
		logicNet.removeNode(destinationNode);
	}

}