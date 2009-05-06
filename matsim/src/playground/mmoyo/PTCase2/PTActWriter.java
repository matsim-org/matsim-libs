package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
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
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.Validators.PathValidator;

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
		ptRouter = ptOb.getPtRouter2();
		net= ptOb.getPtNetworkLayer();
		String conf = ptOb.getConfig();
		outputFile = ptOb.getOutPutFile();
		plansFile =  ptOb.getPlansFile(); 
		
		Config config = new org.matsim.core.config.Config();
		config = Gbl.createConfig(new String[]{conf, "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		
		population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(this.population,net);
		plansReader.readFile(plansFile);
	}

	public void SimplifyPtLegs(){
		SimplifyPtLegs SimplifyPtLegs = new SimplifyPtLegs (net,ptRouter);
		
		for (Person person: this.population.getPersons().values()) {
		//if (true){ Person person = population.getPersons().get(new IdImpl("3937204"));
			SimplifyPtLegs.run(person.getPlans().get(0));
		}	
		SimplifyPtLegs.showPerformance();
	}
	
	
	public void writePTActsLegs(){
		
		Population newPopulation = new PopulationImpl();
		int x=0;

		PathValidator ptPathValidator = new PathValidator ();
		int trips=0;
		int valid=0;
		int invalid=0;
		int inWalkRange=0;
		int lessThan2Node =0;
		int nulls =0;
		
		//List<Double> travelTimes = new ArrayList<Double>();  <-This is for the performance test
		List<Double> durations = new ArrayList<Double>();  
		List<Path> paths = new ArrayList<Path>();  
		
		for (Person person: this.population.getPersons().values()) {
		//if ( true ) {
		//Person person = population.getPersons().get(new IdImpl("3937204"));
		
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
		    			newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
		    			inWalkRange++;
		    		
		    		}else{
			    		startTime = System.currentTimeMillis();
			    		Path path = ptRouter.findRoute(lastActCoord, actCoord, lastAct.getEndTime(), distToWalk);
			    		duration= System.currentTimeMillis()-startTime;
			    		durations.add(duration);
			    		if(path!=null){
				    		//travelTime=travelTime+ path.travelTime;
				    		if (path.nodes.size()>1){
				    			createWlinks(lastActCoord, path, actCoord);
			    				double dw1 = net.getLink("linkW1").getLength();
			    				double dw2 = net.getLink("linkW2").getLength();
			    				if ((dw1+dw2)>=(distanceToDestination)){
			    					newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    					inWalkRange++;
			    				}else{
			    					if (ptPathValidator.isValid(path)){
			    						legNum= insertLegActs(path, lastAct.getEndTime(), legNum, newPlan);
			    						valid++;
			    					}else{
			    						newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    						invalid++;
			    						addPerson=false;
			    						paths.add(path);
			    					}
			    					
			    					//legNum= insertLegActs(path, lastAct.getEndTime(), legNum, newPlan);
			    				}
			   				removeWlinks();
			    			}else{
			    				newPlan.addLeg(walkLeg(legNum++, lastAct, thisAct));
			    				addPerson=false;
			    				lessThan2Node++;
			    			}
			    		}else{
			    			newPlan.addLeg(walkLeg(legNum++, lastAct,thisAct));
			    			addPerson=false;
			    			nulls++;
			    		}
					}//distanceToDestination<= distToWalk
				}//if !First
				
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
			x++;
			//travelTimes.add(travelTime);
		}//for person


		System.out.println("writing output plan file...");
		new PopulationWriter(newPopulation, outputFile, "v4").write();
		System.out.println("Done");
		
		
		System.out.println("valid:        " + valid +  "\ninvalid:      " + invalid + "\ninWalkRange:  "+ inWalkRange + "\nnulls:        " + nulls + "\nlessThan2Node:" + lessThan2Node);
		System.out.println("--------------\nTrips:" + trips);
			
		
		System.out.println("===Printing routing durations");
		double total=0;
		for (double d : durations ){
			total=total+d;
		}
		System.out.println("total " + total + " average: " + (total/durations.size()));
		
		
		/*
		//for(Path path: paths){
		if (true){
			Path path = paths.get(0);
			for(Link link: path.links){
				System.out.print(link.getType() + "-");
			}
			System.out.println("");
		}
		*/
		
		
		// start the control(l)er with the network and plans as defined above
		//Controler controler = new Controler(Gbl.getConfig(),net,(Population) newPopulation);
		// this means existing files will be over-written.  Be careful!
		//controler.setOverwriteFiles(true);
		// start the matsim iterations (configured by the config file)
		//controler.run();
		
			
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
					newPlan.addLeg(newPTLeg(legNum++, TransportMode.car, legRouteLinks, legDistance, arrTime-legTravelTime, legTravelTime, arrTime));
										
					//test: Check what method describes the location more exactly
					//newPlan.addAct(newPTAct("exit pt veh", link.getFromNode().getCoord(), link, arrTime, 0, arrTime));
					newPlan.addActivity(newPTAct("exit pt veh", link.getToNode().getCoord(), link, arrTime, arrTime));
				}

			}else if(link.getType().equals("Transfer") || link.getType().equals("DetTransfer") ){  //add the PTleg and a Transfer Act
				if (lastLinkType.equals("Standard")){
					arrTime= depTime+ legTravelTime;
					legDistance= legDistance+ linkDistance;
					//-->: The legMode car is temporal only for visualization purposes
					newPlan.addLeg(newPTLeg(legNum++, TransportMode.car, legRouteLinks, legDistance, depTime, legTravelTime, arrTime));
					//newPlan.addAct(newPTAct("wait pt", link.getFromNode().getCoord(), link, accumulatedTime, linkTravelTime, accumulatedTime + linkTravelTime));
					newPlan.addActivity(newPTAct("Wait pt veh", link.getFromNode().getCoord(), link, accumulatedTime, accumulatedTime + linkTravelTime));
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
				double walkTime= walk.walkTravelTime(link.getLength());
				legRouteLinks.clear();
				legRouteLinks.add(link);
				arrTime= accumulatedTime+ walkTime;
				newPlan.addLeg(newPTLeg(legNum++, TransportMode.walk, legRouteLinks, linkDistance, accumulatedTime, walkTime, arrTime));

				//like a transfer link
				if (lastLinkType.equals("Standard")){  //-> how can be validated that the next link must be a standard link?
					double endActTime= arrTime + linkTravelTime -walkTime; // The ptTravelTime must be calculated it like this: travelTime = walk + transferTime;
					newPlan.addActivity(newPTAct("Change ptv", link.getFromNode().getCoord(), link, arrTime, endActTime));
					first=false;
				}
			}

			else if (link.getType().equals("Walking")){
				legRouteLinks.clear();
				legRouteLinks.add(link);
				linkTravelTime= linkTravelTime/60;
				arrTime= accumulatedTime+ linkTravelTime;
				newPlan.addLeg(newPTLeg(legNum++, TransportMode.walk, legRouteLinks, linkDistance, accumulatedTime, linkTravelTime, arrTime));
			}

			accumulatedTime =accumulatedTime+ linkTravelTime;
			lastLinkType = link.getType();
			linkCounter++;
		}//for Link
		return legNum;
	}//insert

	private Activity newPTAct(final String type, final Coord coord, final Link link, final double startTime, final double endTime){
		Activity ptAct= new ActivityImpl(type, coord);
		ptAct.setStartTime(startTime);
		ptAct.setEndTime(endTime);
		//ptAct.calculateDuration();
		ptAct.setLink(link);
		return ptAct;
	}

	private Leg newPTLeg(final int num, TransportMode mode, final List<Link> routeLinks, final double distance, final double depTime, final double travTime, final double arrTime){
		NetworkRoute legRoute = new LinkNetworkRoute(null, null); 
		
		if (mode!=TransportMode.walk){
			legRoute.setLinks(null, routeLinks, null);
		}else{
			mode= TransportMode.car;    //3 april 2009
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
		double walkTravelTime = walk.walkTravelTime(distance); 
		double depTime = act1.getEndTime();
		double arrTime = depTime + walkTravelTime;
		return newPTLeg(legNum, TransportMode.walk, new ArrayList<Link>(), distance, depTime, walkTravelTime, arrTime);
	}
	
}

	
