package playground.mmoyo.TransitSimulation;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.pt.integration.ExperimentalTransitRoute;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.mmoyo.PTCase2.PTRouter2;
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.Pedestrian.Walk;
import playground.mmoyo.Validators.PathValidator;

public class SimplifyPtLegs implements PlanAlgorithm{
	private Walk walk = new Walk();
	private PTRouter2 ptRouter;
	private NetworkLayer net; 
	
	PathValidator pathValidator;
	
	private Node originNode;
	private Node destinationNode;
	private Link walkLink1;
	private Link walkLink2;
	
	///////////Performance variables//////////////////
	private int numPlans=0;
	private double iniTime;
	private double totalTimePTLeg=0;
	private double pathSearchDuration;
	private List<Path> invalidPaths = new ArrayList<Path>();  
	private int inWalkRange;
	private int valid;
	private int invalid;
	private int nulls;
	private int lessThan2Node;
	private List<Boolean> completePaths = new ArrayList<Boolean>();
	//////////////////////////////////////////////////
	
	////Indexes of walking legs
	private int nodeIndex=0;
	private int linkIndex=0;
	///

	//////////Simulation variables////////
	private TransitSchedule transitSchedule;
	public List<ExperimentalTransitRoute> PlanExpTransRoute;
	
	public SimplifyPtLegs(NetworkLayer net, PTRouter2 ptRouter){
		this.ptRouter= ptRouter;
		this.net = net; 
	}
	
	public void run (Plan plan){
		iniTime = System.currentTimeMillis();
		Person person =  plan.getPerson();
		System.out.println( " id:" + person.getId());
	
		boolean first =true;
		boolean addPerson= true;
		Activity lastAct = null;
		Activity thisAct= null;
		double travelTime=0;
		
		double startTime=0;
		double duration=0;
		
		Plan newPlan = new PlanImpl(person);
		PlanExpTransRoute = new ArrayList<ExperimentalTransitRoute>();
		
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				thisAct= (Activity) pe;
				
				if (!first) {
					Coord lastActCoord = lastAct.getCoord();
					Coord actCoord = thisAct.getCoord();
					
					double distanceToDestination = CoordUtils.calcDistance(lastActCoord, actCoord);
					double distToWalk= walk.distToWalk(person.getAge());
					if (distanceToDestination<= distToWalk){
						newPlan.addLeg(walkLeg(lastAct,thisAct));
						inWalkRange++;
					}else{
						startTime = System.currentTimeMillis();
						Path path = ptRouter.findRoute(lastActCoord, actCoord, lastAct.getEndTime(), distToWalk);
						duration= System.currentTimeMillis()-startTime;
						pathSearchDuration += duration;
						if(path!=null){
							//travelTime=travelTime+ path.travelTime;
							if (path.nodes.size()>1){
								createWlinks(lastActCoord, path, actCoord);
								double dw1 = walkLink1.getLength();
								double dw2 = walkLink2.getLength();
								if ((dw1+dw2)>=(distanceToDestination)){
									newPlan.addLeg(walkLeg(lastAct,thisAct));
									inWalkRange++;
								}else{
									if (pathValidator.isValid(path)){
										insertLegActs(path, lastAct.getEndTime(), newPlan);
										valid++;
									}else{
										newPlan.addLeg(walkLeg(lastAct,thisAct));
										invalid++;
										addPerson=false;
										invalidPaths.add(path);
									}
									//legNum= insertLegActs(path, lastAct.getEndTime(), legNum, newPlan);
								}//if dw1+dw2
								removeWlinks();   //-> 16 april
							}else{
								newPlan.addLeg(walkLeg(lastAct, thisAct));
								addPerson=false;
								lessThan2Node++;
							}
						}else{
							newPlan.addLeg(walkLeg(lastAct,thisAct));
							addPerson=false;
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
			person.exchangeSelectedPlan(newPlan, addPerson);
			person.removeUnselectedPlans();
		}
		
		totalTimePTLeg += (System.currentTimeMillis()- iniTime);
		numPlans++;
	}

	private void insertLegActs(final Path path, double depTime, final Plan newPlan){
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
					newPlan.addLeg(newPTLeg(TransportMode.car, legRouteLinks, legDistance, arrTime-legTravelTime, legTravelTime, arrTime));
										
					PlanExpTransRoute.add(InsertExperimentalTransitRoute(legRouteLinks));   // 8 May
					
					//test: Check what method describes the location more exactly
					//newPlan.addAct(newPTAct("exit pt veh", link.getFromNode().getCoord(), link, arrTime, arrTime));
					newPlan.addActivity(newPTAct("exit pt veh", link.getToNode().getCoord(), link, arrTime, arrTime));
				}

			}else if(link.getType().equals("Transfer") || link.getType().equals("DetTransfer") ){  //add the PTleg and a Transfer Act
				if (lastLinkType.equals("Standard")){
					arrTime= depTime+ legTravelTime;
					legDistance= legDistance+ linkDistance;
					//-->: The legMode car is temporal only for visualization purposes
					newPlan.addLeg(newPTLeg(TransportMode.car, legRouteLinks, legDistance, depTime, legTravelTime, arrTime));
					//newPlan.addAct(newPTAct("wait pt", link.getFromNode().getCoord(), link, accumulatedTime, linkTravelTime, accumulatedTime + linkTravelTime));
					newPlan.addActivity(newPTAct("Wait pt veh", link.getFromNode().getCoord(), link, accumulatedTime, accumulatedTime + linkTravelTime));
					first=false;
				}

			}
			else if (link.getType().equals("DetTransfer")){
				//it is divided into an walk leg (Walking to the near station) and a activity (Waiting for a PTV)

				//like a Walking leg
				double walkTime= walk.walkTravelTime(link.getLength());
				legRouteLinks.clear();
				legRouteLinks.add(link);
				arrTime= accumulatedTime+ walkTime;
				newPlan.addLeg(newPTLeg( TransportMode.walk, legRouteLinks, linkDistance, accumulatedTime, walkTime, arrTime));

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
				newPlan.addLeg(newPTLeg(TransportMode.walk, legRouteLinks, linkDistance, accumulatedTime, linkTravelTime, arrTime));
			}

			accumulatedTime =accumulatedTime+ linkTravelTime;
			lastLinkType = link.getType();
			linkCounter++;
		}//for Link

	}//insert
		
	private Activity newPTAct(final String type, final Coord coord, final Link link, final double startTime, final double endTime){
		Activity ptAct= new ActivityImpl(type, coord);
		ptAct.setStartTime(startTime);
		ptAct.setEndTime(endTime);
		//ptAct.calculateDuration();
		ptAct.setLink(link);
		return ptAct;
	}
	
	private Leg newPTLeg(TransportMode mode, final List<Link> routeLinks, final double distance, final double depTime, final double travTime, final double arrTime){
		NetworkRoute legRoute = new LinkNetworkRoute(null, null); 
		
		if (mode!=TransportMode.walk){
			legRoute.setLinks(null, routeLinks, null);
		}else{
			mode= TransportMode.car;  //-> temporarly for Visualizer
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
	
	public void showPerformance(){
		System.out.print(
				"\nPlans: " + numPlans +
				"\nTotal time of ptLegs modifications: " + totalTimePTLeg +
				"\nSum of route search: " + pathSearchDuration +
				"\nvalid paths: " + valid +  
				"\ninvalid paths: " + invalid +
				"\nnull paths: " + nulls +
				"\nless than 2 node paths: " + lessThan2Node);
	}
	
	public void setFacilities(ActivityFacilities facilities, TransitSchedule transitSchedule){
		this.transitSchedule = transitSchedule;
	}
	
	private ExperimentalTransitRoute InsertExperimentalTransitRoute(List<Link> legRouteLinks ){
		Node node = legRouteLinks.get(0).getFromNode();
		Id idEgressFacility = legRouteLinks.get(legRouteLinks.size()-1).getToNode().getId();
		
		TransitStopFacility accessFacility = this.transitSchedule.getFacilities().get(node.getId());
		TransitLine line = this.transitSchedule.getTransitLines().get(((PTNode)node).getIdPTLine()); 
		TransitStopFacility egressFacility = this.transitSchedule.getFacilities().get(idEgressFacility);
		return new ExperimentalTransitRoute(accessFacility, line, egressFacility);
	}  
	
}
