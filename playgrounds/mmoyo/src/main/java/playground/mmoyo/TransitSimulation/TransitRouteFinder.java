package playground.mmoyo.TransitSimulation;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.transitSchedule.api.TransitSchedule;

import playground.mmoyo.PTRouter.LogicFactory;
import playground.mmoyo.PTRouter.LogicIntoPlainTranslator;
import playground.mmoyo.PTRouter.PTRouter;
import playground.mmoyo.PTRouter.PTValues;

/**
 * Receives two acts and returns a list of PT legs connecting their coordinates
 */
public class TransitRouteFinder {
	private PTRouter ptRouter;
	private LogicIntoPlainTranslator logicToPlainTranslator;

	public TransitRouteFinder(final TransitSchedule transitSchedule){
		LogicFactory logicFactory = new LogicFactory(transitSchedule);
		this.ptRouter = new PTRouter(logicFactory.getLogicNet());
		this.logicToPlainTranslator = logicFactory.getLogicToPlainTranslator();
	}

	/**returns a list of legs that represent a PT connection between two activities locations*/
	public List<Leg> calculateRoute (final ActivityImpl fromAct, final ActivityImpl toAct, final Person person ){
		List<Leg> legList = new ArrayList<Leg>();
		Path path = ptRouter.findPTPath(fromAct.getCoord(), toAct.getCoord(), fromAct.getEndTime());

		if (path!= null){
			List <Link> linkList = new ArrayList<Link>();
			double depTime=fromAct.getEndTime();
			double travTime= depTime;

			int i=1;
			String linkType;
			String lastLinkType= null;
			for (Link link: path.links){
				linkType = ((LinkImpl)link).getType();

				if (i>1){
					if (!linkType.equals(lastLinkType)){
						if (!lastLinkType.equals(PTValues.TRANSFER_STR)){    //transfers will not be described in legs
							LegImpl newLeg = createLeg(selectMode(lastLinkType), linkList, depTime, travTime);
							legList.add(newLeg);
						}
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
			legList = logicToPlainTranslator.convertToPlainLeg(legList); //translates the listLeg into the plainNetwork
		}
		return legList;
	}

	private TransportMode selectMode(final String linkType){
		TransportMode mode = null;
		if (linkType.equals(PTValues.STANDARD_STR)){
			mode=  TransportMode.pt;
		}else{
			mode=  TransportMode.undefined;
		}
		return mode;
	}

	private LegImpl createLeg(TransportMode mode, final List<Link> routeLinks, final double depTime, final double arrivTime){
		double travTime= arrivTime - depTime;
		List<Node> routeNodeList = new ArrayList<Node>();

		double distance=0;
		if (routeLinks.size()>0) routeNodeList.add(routeLinks.get(0).getFromNode());
		for(Link link : routeLinks) {
			distance += link.getLength();
			routeNodeList.add(link.getToNode());
		}

		NetworkRoute legRoute = new LinkNetworkRouteImpl(null, null, null);
		legRoute.setDistance(distance);
		legRoute.setLinkIds(null, NetworkUtils.getLinkIds(routeLinks), null);
		legRoute.setTravelTime(travTime);
		legRoute.setLinkIds(null, NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(routeNodeList)), null);

		LegImpl leg = new LegImpl(mode);
		leg.setRoute(legRoute);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(arrivTime);

		return leg;
	}

}
