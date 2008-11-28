package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.router.Dijkstra;
import org.matsim.utils.geometry.Coord;

import playground.mmoyo.PTRouter.PTNProximity;
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
	private PTNProximity ptnProximity;
	private Dijkstra dijkstra;
	private PTNetworkFactory2 ptNetworkFactory = new PTNetworkFactory2();
	private PTTravelCost ptTravelCost;
	private PTTravelTime ptTravelTime;
	
	/**
	 * @param network
	 */
	public PTRouter2(NetworkLayer ptNetworkLayer, PTTimeTable2 ptTimetable) {
		this.ptNetworkLayer = ptNetworkLayer;
		this.ptnProximity = new PTNProximity (ptNetworkLayer);
		this.ptTravelCost = new PTTravelCost(ptTimetable);
		this.ptTravelTime =new PTTravelTime(ptTimetable);
		this.dijkstra = new Dijkstra(ptNetworkLayer, ptTravelCost, ptTravelTime);	
	}
		
	public CarRoute findRoute(Coord coord1, Coord coord2, double time, int distToWalk){
		PTNode[] NearStops1= ptnProximity.getNearestBusStops(coord1, distToWalk);
		PTNode[] NearStops2= ptnProximity.getNearestBusStops(coord2, distToWalk);
		PTNode ptNode1=ptNetworkFactory.CreateWalkingNode(ptNetworkLayer, new IdImpl("W1"), coord1);
		PTNode ptNode2=ptNetworkFactory.CreateWalkingNode(ptNetworkLayer, new IdImpl("W2"), coord2);
		List <IdImpl> walkingLinkList1 = ptNetworkFactory.CreateWalkingLinks(ptNetworkLayer, ptNode1, NearStops1, true);
		List <IdImpl> walkingLinkList2 = ptNetworkFactory.CreateWalkingLinks(ptNetworkLayer, ptNode2, NearStops2, false);
		
		CarRoute route = dijkstra.calcLeastCostPath(ptNode1, ptNode2, time);
		
		ptNetworkFactory.removeWalkinkLinks(ptNetworkLayer, walkingLinkList1);
		ptNetworkFactory.removeWalkinkLinks(ptNetworkLayer, walkingLinkList2);
		this.ptNetworkLayer.removeNode(this.ptNetworkLayer.getNode("W1"));
		this.ptNetworkLayer.removeNode(this.ptNetworkLayer.getNode("W2"));
		this.ptNetworkLayer.removeNode(ptNode1);
		this.ptNetworkLayer.removeNode(ptNode2);

		if (route!=null){
			route.getNodes().remove(ptNode1);
			route.getNodes().remove(ptNode2);
		}
		return route;
	}

	public CarRoute findRoute(Coord coord1, Coord coord2, double time){
		PTNode node1= ptnProximity.getNearestNode(coord1.getX(), coord1.getY());
		PTNode node2= ptnProximity.getNearestNode(coord2.getX(), coord2.getY());
		return findRoute(node1, node2,time);
	}
	
	public CarRoute findRoute(Node ptNode1, Node ptNode2, double time){
		return dijkstra.calcLeastCostPath(ptNode1, ptNode2, time);
	}
	
	public CarRoute forceRoute(Coord coord1, Coord coord2, double time, int distToWalk){
		CarRoute route=null;
		while (route==null && distToWalk<1300){
			route= findRoute(coord1, coord2, time, distToWalk);
			distToWalk= distToWalk+50;
		}
		return route;
	}
	
	public List<Object> findLegActs(CarRoute route, double depTime){
		List<Object> actLegList = new ArrayList<Object>();
		if (route!=null){
			double legTravTime =0;
			double accumulatedTime=depTime;
			//double legArrTime=depTime;
			double routeTravelTime =0;
			int num=0;
		
			List<Link> linkList = new ArrayList<Link>();
			boolean first=true;
			List<Link> linkRoute = route.getLinks();
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
					CarRoute legRoute = new NodeCarRoute();
					legRoute.setTravelTime(routeTravelTime); //legRoute.setTravTime(routeTravelTime*3600);
					if (linkList.size()>0) {legRoute.setLinks(linkList);}
					
					//insert leg 
					Leg leg = new Leg(Leg.Mode.pt);
					//routeTravelTime =routeTravelTime; // routeTravelTime =routeTravelTime*3600;  //Seconds
					leg.setDepartureTime(accumulatedTime);
					leg.setTravelTime(routeTravelTime);
					leg.setArrivalTime((accumulatedTime + (routeTravelTime))); 
					leg.setNum(num); 		
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
		Act ptAct= new Act("Wait PT Vehicle", coord);
		ptAct.setStartTime(startTime);
		ptAct.setEndTime(endTime);
		ptAct.setDuration(dur);
		ptAct.calculateDuration();
		ptAct.setLink(link);
		//act.setDur(linkTravelTime*60);
		//act.setLinkId(link.getId());
		//act.setCoord(coord)
		return ptAct;
	}
		
	private Leg createLeg(int num, CarRoute legRoute, double depTime, double travTime, double arrTime){
		Leg leg = new Leg(Leg.Mode.pt);
		leg.setNum(num);
		leg.setRoute(legRoute);
		leg.setArrivalTime(arrTime);
		leg.setTravelTime(travTime);
		leg.setDepartureTime(depTime);
		return leg;
	}
	
	private Link findLink(Node node1, Node node2){
		Link link= null;
		for (Link l: (node1.getInLinks().values())) {
			if (l.getFromNode().equals(node2)){
				return l;
			}
		}
		return  link;
	}
	
	public void PrintRoute(CarRoute route){
		if (route!=null){
			System.out.print("\nLinks: ");
			//for (Link l L route.getLinks()) {
				//System.out.println("link: "l.getId() + " cost: " + link.);
			//}
		
			IdImpl idPTLine = new IdImpl("");
			for (Iterator<Node> iter = route.getNodes().iterator(); iter.hasNext();){
				PTNode ptNode= (PTNode)iter.next();
				if(ptNode.getIdPTLine()==idPTLine){
					System.out.print(ptNode.getId().toString() + " ");
				}else{
					System.out.println("\n" + ptNode.getIdPTLine().toString());
					System.out.print(ptNode.getId().toString() + " ");
				}
				idPTLine= ptNode.getIdPTLine();	
			}
			System.out.println("\nTravel cost of route=" + route.getTravelCost() + "  time of route:" + route.getTravelTime());
		}else{
			System.out.println("The route is null");
		}//if null

	}//printroute

}//class

/*
for (Iterator<Node> iter = route.getRoute().iterator(); iter.hasNext();){
	PTNode ptNode= (PTNode)iter.next();
	if(ptNode.getIdPTLine()==idPTLine && iter.hasNext()){
		//legList.get(x);
		//System.out.print(ptNode.getId().toString() + " ");
		legRoute.getRoute().add(ptNode);
		if (check){
			Link link= findLink(fromNode, ptNode);
			if (link== null){
				System.out.println (fromNode.getId() + " "+  ptNode.getId());	
			}
			travTime = travTime+ ptTravelTime.getLinkTravelTime(link, travTime); //TODO: corregir este travTime
			//System.out.println("link:" + link.getId() + " traveltime:" + this.ptTravelTime.getLinkTravelTime(link, 8000));
			//System.out.println(ptNode.getId() + " " + ptNode.getIdPTLine() + " " + legRoute.getRoute().size());
		}
	}else{
		//System.out.println("\n" + ptNode.getIdPTLine().toString());
		//System.out.println(subRoute.getRoute().toString());
		//System.out.println(legRoute.toString());
		//System.out.println("\nCambio" );
		
		Leg leg = new Leg(Leg.Mode.pt);
		leg.setNum(num);
		leg.setRoute(legRoute);
		leg.setDepTime(depTime);
		leg.setTravTime(travTime);
		leg.setArrTime(depTime+travTime);
		legList.add(leg);
		
		//clean variables
		depTime= leg.getArrTime();
		legRoute = new Route();
		fromNode= ptNode;
		travTime=0;
		check=true;
		num++;
		//x++;
	}
	idPTLine= ptNode.getIdPTLine();	
}
 */
//System.out.println(legList.toString());


/*codigo original
if (x>0){
	legRoute.getRoute().add(route.getLinkRoute()[x-1].getToNode()); //if Transfer or end of route we add the toNode of last Link to complete the route;
}
	//Add the standard leg 
	arrTime=depTime+travTime;
	Leg leg = createLeg(num, legRoute, depTime, travTime, arrTime);
	legList.add(leg);
	
	depTime= leg.getArrTime();
	num++;

	//Add the transfer leg
	if (link.getType().equals("Transfer")){
		legRoute = new Route();
		legRoute.getRoute().add(link.getFromNode());
		legRoute.getRoute().add(link.getToNode());
	
		///////////////////////////////////////////////////////
		//System.out.println(subRoute.getRoute().toString());
		//System.out.println(legRoute.toString());
		//System.out.println("\nCambio" );
		//leg.setNum(num);
		//System.out.println(leg.setRoute(legRoute);
		//leg.setDepTime(depTime);
		//leg.setTravTime(travTime);
		//leg.setArrTime(depTime+travTime);
		//legList.add(leg);
		///////////////////////////////////////////////////////
		
		arrTime= depTime;
		travTime= ptTravelTime.getLinkTravelTime(link, travTime);
		depTime= depTime +  travTime;
		
		num++;
	}
	//clean variables
	legRoute = new Route();
	leg=null;
	travTime=0;
*/