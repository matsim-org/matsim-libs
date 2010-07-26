package playground.droeder.bvg09;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeCost;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;
import playground.droeder.gis.DaShapeWriter;

public class AddHafasLines2VisumNet {
	
	private final String PATH = DaPaths.OUTPUT + "bvg09/";
	
	private final String NETFILE = PATH + "intermediateNetwork.xml";
	private final String HAFASTRANSITFILE = PATH + "transitSchedule-HAFAS-Coord.xml";
	private final String VISUMTRANSITFILE = PATH + "intermediateTransitSchedule.xml";
	private final String FINALTRANSITFILE = PATH + "finalTransit.xml";
	private final String NEWLINESSHAPE = PATH + "newLines.shp";
	
	private final Id ERROR = new IdImpl("Error"); 
	
	private ScenarioImpl visumSc;
	private ScenarioImpl hafasSc;
	private ScenarioImpl newSc;
	
	private NetworkImpl visumNet;
	private TransitSchedule visumTransit;
	
	private TransitSchedule hafasTransit;
	
	private TransitSchedule finalTransitSchedule;
	private TransitScheduleFactory newTransitFactory;
	
	
	private TreeMap<Id, Id> vis2HafLines;
	
	private Map<Id, Id> haf2VisNearestStop;
	
	public static void main (String[] args){
		AddHafasLines2VisumNet add = new AddHafasLines2VisumNet();
		add.run();
	}

	
	public AddHafasLines2VisumNet(){
		this.visumSc = new ScenarioImpl();
		this.hafasSc = new ScenarioImpl();
		
		visumSc.getConfig().scenario().setUseTransit(true);
		readSchedule(VISUMTRANSITFILE, visumSc);
		visumTransit = visumSc.getTransitSchedule();
		new NetworkReaderMatsimV1(visumSc).readFile(NETFILE);
		visumNet = visumSc.getNetwork();
		
		hafasSc.getConfig().scenario().setUseTransit(true);
		readSchedule(HAFASTRANSITFILE, hafasSc);
		hafasTransit = hafasSc.getTransitSchedule();
		
		this.createHafasLineIdsFromVisum();
		
		newSc = new ScenarioImpl();
		newSc.getConfig().scenario().setUseTransit(true);
		newTransitFactory = newSc.getTransitSchedule().getFactory();
		finalTransitSchedule = newTransitFactory.createTransitSchedule();
	}
	
	public void run(){
		this.createHafasLineIdsFromVisum();
		this.findNearestStops();
		this.getNewRoutesFromMatchedStops();
		DaShapeWriter.writeDefaultLineString2Shape(NEWLINESSHAPE, "newRoutes", prepareNewRoutesForShape(), null);
	}
	
	private void readSchedule(String fileName, ScenarioImpl sc){
		TransitScheduleReader reader = new TransitScheduleReader(sc);
		try {
			reader.readFile(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void findNearestStops(){
		this.haf2VisNearestStop = new HashMap<Id, Id>();
		
		for(TransitStopFacility h : hafasTransit.getFacilities().values()){
			Id vis = ERROR;
			Double dist = Double.MAX_VALUE;
			for(TransitStopFacility v : visumTransit.getFacilities().values()){
				double temp = getDist(v.getCoord(), h.getCoord());
				if(temp < dist){
					dist = temp;
					vis = v.getId();
				}
			}
			if(vis.equals(ERROR)){
				throw new RuntimeException("no Stop matched!");
			}else{
				
				haf2VisNearestStop.put(h.getId(), vis);
			}
		}
		
		//
		haf2VisNearestStop.put(new IdImpl("9160525"), new IdImpl("1605250"));
		haf2VisNearestStop.put(new IdImpl("9160524"), new IdImpl("1605240"));
		haf2VisNearestStop.put(new IdImpl("9160540"), new IdImpl("1605400"));
		haf2VisNearestStop.put(new IdImpl("9160539"), new IdImpl("1605390"));
		haf2VisNearestStop.put(new IdImpl("9160011"), new IdImpl("1600110"));
		haf2VisNearestStop.put(new IdImpl("9160523"), new IdImpl("1605230"));
		haf2VisNearestStop.put(new IdImpl("9160021"), new IdImpl("1600210"));
	}
	
	private Map<String, SortedMap<Integer, Coord>> prepareNewRoutesForShape(){
		Map<String, SortedMap<Integer, Coord>> preparedRoutes = new HashMap<String, SortedMap<Integer,Coord>>();
		
		for(TransitLine l : finalTransitSchedule.getTransitLines().values()){
			for(TransitRoute r : l.getRoutes().values()){
				int  i = 0;
				SortedMap<Integer, Coord> temp = new TreeMap<Integer, Coord>();
				for(Id id : r.getRoute().getLinkIds()){
					temp.put(i, visumNet.getLinks().get(id).getFromNode().getCoord());
					i++;
					temp.put(i, visumNet.getLinks().get(id).getToNode().getCoord());
					i++;
				}
				preparedRoutes.put(l.getId().toString() + "_" + r.getId().toString(), temp);
			}
			
		}
		return preparedRoutes;
	}
	
	private void getNewRoutesFromMatchedStops(){
		TransitLine newLine;
		TransitRoute newRoute;
		TransitRouteStop newStop;
		TransitStopFacility newFacility;
		List<TransitRouteStop> stops;
		
		
		Map<Id, TransitStopFacility> facilities = new HashMap<Id, TransitStopFacility>();
		for (Entry<Id, Id> e : vis2HafLines.entrySet()){
			newLine = newTransitFactory.createTransitLine(new IdImpl(e.getKey().toString()));
			for(TransitRoute hr : hafasTransit.getTransitLines().get(e.getValue()).getRoutes().values()){
				stops = new ArrayList<TransitRouteStop>();
				
				for(TransitRouteStop stop : hr.getStops()){
					TransitStopFacility hFacility = stop.getStopFacility();
					TransitStopFacility vFacility = visumTransit.getFacilities().get(haf2VisNearestStop.get(hFacility.getId()));
					
					if(facilities.containsKey(vFacility.getId())){
						newFacility = facilities.get(vFacility.getId());
					}else{
						newFacility = newTransitFactory.createTransitStopFacility(vFacility.getId(), vFacility.getCoord(),vFacility.getIsBlockingLane());
						facilities.put(vFacility.getId(), newFacility);
						newFacility.setLinkId(findNextLink(newFacility));
					}
					
					newStop = newTransitFactory.createTransitRouteStop(newFacility, stop.getArrivalOffset(), stop.getDepartureOffset());
					stops.add(newStop);
				}
				
				NetworkRoute networkRoute = getNetworkRoute(stops.get(0).getStopFacility().getLinkId(), stops.get(stops.size()-1).getStopFacility().getLinkId());
				
				newRoute = newTransitFactory.createTransitRoute(hr.getId(), networkRoute, stops, TransportMode.pt);
				newLine.addRoute(newRoute);
			}
			finalTransitSchedule.addTransitLine(newLine);
		}
	}
	
	private NetworkRoute getNetworkRoute(Id startLink, Id endLink){
		
		NetworkRoute route = new LinkNetworkRouteImpl(startLink, endLink);

//		not implement yet		
//		route.setLinkIds(startLink, links, endLink);
		
		return route;
	}
	
	
	
	private Id findNextLink(TransitStopFacility newFacility) {
		if(newFacility.getId().equals(new IdImpl("484014"))){
			return new IdImpl("1397");
		} else if(newFacility.getId().equals(new IdImpl("484013"))){
			return new IdImpl("2170");
		}else{
			Double dist = Double.MAX_VALUE;
			Node n;
			Id link = null;
			for(Link l : visumNet.getLinks().values()){
				n = l.getToNode();
				double temp = getDist(newFacility.getCoord(), n.getCoord());
				if (temp<dist){
					dist = temp;
					link = l.getId();
				}
			}
			return link;
		}
	}


	private void createHafasLineIdsFromVisum(){
		vis2HafLines = new TreeMap<Id, Id>();
		String[] idToChar;
		StringBuffer createdHafasId;
		String hafasId;
		for(TransitLine line : visumSc.getTransitSchedule().getTransitLines().values()){
			createdHafasId = new StringBuffer();
			idToChar = line.getId().toString().split("");
			
			if(idToChar[1].equals("B")){
				if(idToChar[4].equals(" ") ){
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("   ");
				}else{
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[4]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("  ");
				}
			}else if(idToChar[1].equals("U")){
				createdHafasId.append(idToChar[1]);
				createdHafasId.append(idToChar[3]);
				createdHafasId.append("   ");
			}else if(idToChar[1].equals("T") && idToChar[3].equals("M") ){
				if(idToChar[4].equals(" ") ){
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("   ");
				}else{
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[4]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("  ");
				}
			}else if(idToChar[1].equals("T") && !(idToChar.equals("M")) ){
				createdHafasId.append(idToChar[3]);
				createdHafasId.append(idToChar[4]);
				createdHafasId.append("   ");
			}
			
			hafasId = createdHafasId.toString();
			if(createdHafasId.length()>0 && hafasSc.getTransitSchedule().getTransitLines().containsKey(new IdImpl(hafasId)) ){
				vis2HafLines.put(line.getId() , new IdImpl(hafasId));
			}
		}
	}
	
	protected double getDist(Coord one, Coord two){
		
		double xDif = one.getX() - two.getX();
		double yDif = one.getY() - two.getY();
		
		return Math.sqrt(Math.pow(xDif, 2.0) + Math.pow(yDif, 2.0));
	}
}
