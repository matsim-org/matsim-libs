package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;

public class TransitRouteUtil {
	private final Network net;
	private final TransitRoute trRoute;
	private static final Logger log = Logger.getLogger(TransitRouteUtil.class);
	
	public TransitRouteUtil (final Network net, final TransitRoute trRoute){
		this.net = net;
		this.trRoute = trRoute;
	}
	
	/**
	 * returns the transit route link list including star and end Links
	 */
	public List<Link> getAllLinks (){
		List<Link> allLinkList  = new ArrayList<Link>();
		allLinkList.add(this.net.getLinks().get(this.trRoute.getRoute().getStartLinkId()));
		for (Id id : this.trRoute.getRoute().getLinkIds() ){
			allLinkList.add(this.net.getLinks().get(id));	
		}
		allLinkList.add(this.net.getLinks().get(this.trRoute.getRoute().getEndLinkId()));
		return allLinkList;
	}
	
	final String SEP = " ";
	protected void printStops (){
		for (TransitRouteStop stop : this.trRoute.getStops()){
			log.info(stop.getStopFacility().getId() + SEP + stop.getStopFacility().getLinkId());
		}
	}
	
	protected void printRouteLinkIds (){
		for (Id linkId : this.trRoute.getRoute().getLinkIds()){
			log.info(linkId);
		}
	}
	
	/**
	 * returns the links that leads to the next stop of the route
	 */
	public List<Link> getOutLinks (final TransitRouteStop stop, final TransitRoute trRoute){
		List<Link> outLinkList = new ArrayList<Link>();
		int stopIndex= trRoute.getStops().indexOf( stop);

		if(stopIndex >-1 && stopIndex < (trRoute.getStops().size()-1)){
			TransitRouteStop nextStop = trRoute.getStops().get( stopIndex + 1 );
			Link stopLink = this.net.getLinks().get(stop.getStopFacility().getLinkId());
			Link nextStopLink =  this.net.getLinks().get(nextStop.getStopFacility().getLinkId());
			
			List<Link> routeLinkList = this.getAllLinks();
			int startI = routeLinkList.indexOf(stopLink)+1;
			int endI = routeLinkList.indexOf(nextStopLink);
			
			//sublist is a view of the original list, so it is better to populate again a new list
			for (Link link : routeLinkList.subList(startI, endI+1)){
				outLinkList.add(link);
			}
		}else {
			System.out.println(" no outgoing links were found." );	
		}
		
		return outLinkList;
	}
	
	public static void main(String[] args) {
		String netFilePath; 
		String scheduleFilePath;
		
		if (args.length==2){
			netFilePath = args[0];
			scheduleFilePath = args[1];
		}else{
			netFilePath = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			scheduleFilePath = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		}
		
		String strTrRoute = "B-100.101.001.H";
			
		DataLoader dloader = new DataLoader();
		NetworkImpl net = dloader.readNetwork(netFilePath);
		TransitSchedule trSchedule = dloader.readTransitSchedule(net, scheduleFilePath);
		TransitRoute route = dloader.getTransitRoute(strTrRoute, trSchedule);
		dloader = null;
		
		TransitRouteUtil transitRouteUtil = new TransitRouteUtil(net, route );
		
		//get all links
		System.out.println("========================== \n expRoute links");
		List<Link> linkList = transitRouteUtil.getAllLinks();
		for (Link link : linkList){
			System.out.println(link.getId());
		}
		
		String str_stopId = "1003302.1";
		System.out.println("========================== \n outgoing links of stop: " + str_stopId);
		Id stopId = new IdImpl(str_stopId);
		TransitRouteStop stop = route.getStop(trSchedule.getFacilities().get(stopId));
		List<Link> list = transitRouteUtil.getOutLinks(stop, route);
		System.out.println("========================== \n outgoing links: " + list.size());
		for (Link link : list){
			System.out.println(link.getId());
		}
	}

}
