package playground.mmoyo.TransitSimulation;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.marcel.pt.transitSchedule.TransitScheduleImpl;
import playground.mmoyo.PTRouter.PTRouter2;
import playground.mmoyo.PTRouter.Walk;
/**
 * Receives two acts and returns a list of PT legs connecting their coordinates 
 */
public class TransitRouteFinder {
	private PTRouter2 ptRouter;
	private LogicIntoPlainTranslator logicToPlainTranslator;
	private static Walk walk = new Walk();
	
	public TransitRouteFinder(final TransitScheduleImpl transitSchedule){
		LogicFactory logicFactory = new LogicFactory(transitSchedule);
		this.ptRouter = logicFactory.getPTRouter();
		this.logicToPlainTranslator = logicFactory.getLogicToPlainConverter();
	}
	
	@Deprecated
	public TransitRouteFinder(PTRouter2 ptRouter ){
		this.ptRouter = ptRouter;
	}
	
	public List<LegImpl> calculateRoute (final ActivityImpl fromAct, final ActivityImpl toAct, final Person person ){
		List<LegImpl> legList = new ArrayList<LegImpl>();
		
		double distToWalk = walk.distToWalk(person.getAge());
		Path path = ptRouter.findPTPath(fromAct.getCoord(), toAct.getCoord(), fromAct.getEndTime(), distToWalk);

		if (path!= null){
			List <Link> linkList = new ArrayList<Link>();
			double depTime=fromAct.getEndTime();
			double travTime= depTime;
			
			int i=1;
			String linkType;
			String lastLinkType= null;
			for (Link link: path.links){
				linkType = link.getType();
				
				if (i>1){
					if (!linkType.equals(lastLinkType)){
						LegImpl newLeg = createLeg(selectMode(lastLinkType), linkList, depTime, travTime);  
						legList.add(newLeg);
						
						depTime=travTime;
						linkList = new ArrayList<Link>();
					}
					if (i == path.links.size()){						
						travTime = travTime + ptRouter.ptTravelTime.getLinkTravelTime(link, travTime);
						LegImpl newLeg = createLeg(selectMode(linkType), linkList, depTime, travTime);  
						legList.add(newLeg);
					}
				}
				travTime = travTime + ptRouter.ptTravelTime.getLinkTravelTime(link, travTime);
				linkList.add(link);
				lastLinkType = linkType;
				i++;
			}
			legList = logicToPlainTranslator.convertToPlainLeg(legList);
		}
		return legList;
	}
	
	private TransportMode selectMode(final String linkType){
		TransportMode mode = null;
		if (linkType.equals("Standard")){ 
			mode=  TransportMode.pt;
		}else{
			mode=  TransportMode.walk;
		}
		return mode;
	}
	
	private LegImpl createLeg(TransportMode mode, final List<Link> routeLinks, final double depTime, final double arrivTime){
		NetworkRoute legRoute = new LinkNetworkRoute(null, null);  
		double travTime= arrivTime - depTime;
		double distance=0;
		for(Link link : routeLinks) {
			distance= distance + link.getLength();
		} 
		legRoute.setDistance(distance);
		legRoute.setLinks(null, routeLinks, null);
		legRoute.setTravelTime(travTime);

		LegImpl leg = new LegImpl(mode);
		leg.setRoute(legRoute);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(arrivTime);

		return leg;
	}
	
}
