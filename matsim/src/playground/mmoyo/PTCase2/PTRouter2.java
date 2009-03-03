package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;
/*
<<<<<<< .mine
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.routes.LinkCarRoute;
=======
*/
import org.matsim.population.routes.LinkCarRoute;
//>>>>>>> .r5540
import org.matsim.router.Dijkstra;
import org.matsim.router.util.LeastCostPathCalculator.Path;


import playground.mmoyo.input.PTNetworkFactory2;


//import playground.mmoyo.PTRouter.PTNProximity;   //24 feb
import playground.mmoyo.PTRouter.PTNode;
/** 
 * Second version of Router using Matsims Class Dijkstra  
 * We avoid the relationship with the city network and use coordinate search instead
 *  *
 * @param nodeList  PTNodes in stored a a Node-List
 * @param linkList  Collection of org.matsim.network.Link
 * @param OriginNode Node where the trip begins
 * @param DestinationNode Node where the trip must finish
 * @param ptLinkCostCalculator Class that contains the weight information of links
 * @param time Milliseconds after the midnight in which the trip must begin
 */
public class PTRouter2 {
	private NetworkLayer ptNetworkLayer; 
	///private PTNProximity ptnProximity;  //24 feb
	private Dijkstra dijkstra;
	private PTNetworkFactory2 ptNetworkFactory = new PTNetworkFactory2();
	private PTTravelCost ptTravelCost;
	public PTTravelTime ptTravelTime; //TODO: make private
	
	/**
	 * @param network
	 */
	public PTRouter2(NetworkLayer ptNetworkLayer, PTTimeTable2 ptTimetable) {
		this.ptNetworkLayer = ptNetworkLayer;
		///this.ptnProximity = new PTNProximity (ptNetworkLayer);  //24 feb
		this.ptTravelCost = new PTTravelCost(ptTimetable);
		this.ptTravelTime =new PTTravelTime(ptTimetable);
		this.dijkstra = new Dijkstra(ptNetworkLayer, ptTravelCost, ptTravelTime);	
	}
		
	public Path findRoute(Coord coord1, Coord coord2, double time, double distToWalk){
		//original code //24 feb
		//PTNode[] NearStops1= ptnProximity.getNearestBusStops(coord1, distToWalk, false);
		//PTNode[] NearStops2= ptnProximity.getNearestBusStops(coord2, distToWalk, false);
		
		Collection <Node> NearStops1 = ptNetworkLayer.getNearestNodes(coord1, distToWalk);
		Collection <Node> NearStops2 = ptNetworkLayer.getNearestNodes(coord2, distToWalk);
		
		PTNode ptNode1=ptNetworkFactory.CreateWalkingNode(ptNetworkLayer, new IdImpl("W1"), coord1);
		PTNode ptNode2=ptNetworkFactory.CreateWalkingNode(ptNetworkLayer, new IdImpl("W2"), coord2);
		List <Id> walkingLinkList1 = ptNetworkFactory.CreateWalkingLinks(ptNetworkLayer, ptNode1, NearStops1, true);
		List <Id> walkingLinkList2 = ptNetworkFactory.CreateWalkingLinks(ptNetworkLayer, ptNode2, NearStops2, false);

		Path path = dijkstra.calcLeastCostPath(ptNode1, ptNode2, time);
		
		ptNetworkFactory.removeWalkingLinks(ptNetworkLayer, walkingLinkList1);
		ptNetworkFactory.removeWalkingLinks(ptNetworkLayer, walkingLinkList2);
		this.ptNetworkLayer.removeNode(this.ptNetworkLayer.getNode("W1"));
		this.ptNetworkLayer.removeNode(this.ptNetworkLayer.getNode("W2"));
		this.ptNetworkLayer.removeNode(ptNode1);
		this.ptNetworkLayer.removeNode(ptNode2);

		if (path!=null){
			path.nodes.remove(ptNode1);
			path.nodes.remove(ptNode2);
		}
		return path;
	}

	public Path findRoute(Coord coord1, Coord coord2, double time){
		//24 feb
		//PTNode node1= ptnProximity.getNearestNode(coord1.getX(), coord1.getY());
		//PTNode node2= ptnProximity.getNearestNode(coord2.getX(), coord2.getY());
		
		Node node1= ptNetworkLayer.getNearestNode(coord1);
		Node node2= ptNetworkLayer.getNearestNode(coord2);
		return findRoute(node1, node2,time);
	}
	
	public Path findRoute(Node ptNode1, Node ptNode2, double time){
		return dijkstra.calcLeastCostPath(ptNode1, ptNode2, time);
	}
	
	public List<Object> findLegActs(Path path, double depTime){
		List<Object> actLegList = new ArrayList<Object>();
		if (path!=null){
			double legTravTime =0;
			double accumulatedTime=depTime;
			//double legArrTime=depTime;
			double routeTravelTime =0;
			int num=0;
		
			List<Link> linkList = new ArrayList<Link>();
			boolean first=true;
			List<Link> linkRoute = path.links;
			for(int x=0; x< linkRoute.size();x++){
				Link link = linkRoute.get(x);
				double linkTravelTime=ptTravelTime.getLinkTravelTime(link,accumulatedTime);
				accumulatedTime =accumulatedTime + linkTravelTime;
				routeTravelTime =routeTravelTime+linkTravelTime;
				

				//insert first ptActivity: boarding first PTVehicle
				if (first){ 
					Coord coord = link.getFromNode().getCoord();
					double startTime = 0; //this must be inmediately set when we know the passenger gets to the station
					double dur= 0;        //this must be inmediately set when we know the passenger gets to the station
					double endTime = depTime;
					actLegList.add(newPTAct(coord, link, startTime, dur, endTime));
					first=false;
				}
				
				
				if (link.getType().equals("Standard")){
					legTravTime = legTravTime+ linkTravelTime; 
					linkList.add(link);
				}else{
					//CarRoute legRoute = new NodeCarRoute();    25 feb
					LinkCarRoute legRoute = new LinkCarRoute(null, null); 
					
					legRoute.setTravelTime(routeTravelTime); //legRoute.setTravTime(routeTravelTime*3600);
					if (linkList.size()>0) {legRoute.setLinks(null, linkList, null);}
					
					//insert leg 
					Leg leg = new org.matsim.population.LegImpl(Leg.Mode.pt);
					//routeTravelTime =routeTravelTime; // routeTravelTime =routeTravelTime*3600;  //Seconds
					leg.setDepartureTime(accumulatedTime);
					leg.setTravelTime(routeTravelTime);
					leg.setArrivalTime((accumulatedTime + (routeTravelTime))); 
					//leg.setNum(num);   deprecated 		
					leg.setRoute(legRoute);
					actLegList.add(leg);		
					
					//clean variables
					linkList = new ArrayList<Link>();
					legTravTime=0;
					num++;
					
					//insert transfer activity  TODO: what about walking and other posibble "pt modal choices"
					Coord coord = link.getToNode().getCoord();
					double startTime=depTime + routeTravelTime;
					double dur= linkTravelTime; //double dur= linkTravelTime*60;  //Seconds
					double endTime = startTime + dur;
					actLegList.add(newPTAct(coord,link, startTime, dur, endTime));
					
				}//if link = standard
				//set arrTime for the next loop:
				//legArrTime =  accumulatedTime;
				routeTravelTime=0;
					
			}// for x=0
		}//if route!null
		return actLegList;
		
	}
	
	private Act newPTAct(Coord coord, Link link, double startTime, double dur, double endTime){
		Act ptAct= new org.matsim.population.ActImpl("Wait PT Vehicle", coord);
		ptAct.setStartTime(startTime);
		ptAct.setEndTime(endTime);
		//ptAct.setDuration(dur);   deprecated
		ptAct.calculateDuration();
		ptAct.setLink(link);
		//act.setDur(linkTravelTime*60);
		//act.setLinkId(link.getId());
		//act.setCoord(coord)
		return ptAct;
	}
		
	public void PrintRoute(Path path){
		if (path!=null){
			System.out.print("\nLinks: ");
			//for (Link l L route.getLinks()) {
				//System.out.println("link: "l.getId() + " cost: " + link.);
			//}
		
			Id idPTLine = new IdImpl("");
			for (Iterator<Node> iter = path.nodes.iterator(); iter.hasNext();){
				PTNode ptNode= (PTNode)iter.next();
				if(ptNode.getIdPTLine()==idPTLine){
					System.out.print(ptNode.getId().toString() + " ");
				}else{
					System.out.println("\n" + ptNode.getIdPTLine().toString());
					System.out.print(ptNode.getId().toString() + " ");
				}
				idPTLine= ptNode.getIdPTLine();	
			}
			System.out.println("\nTravel cost of route=" + path.travelCost + "  time of route:" + path.travelTime);
		}else{
			System.out.println("The route is null");
		}//if null

	}//printroute

}//class
