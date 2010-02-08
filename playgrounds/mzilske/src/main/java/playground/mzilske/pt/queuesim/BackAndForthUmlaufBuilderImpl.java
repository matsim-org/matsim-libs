package playground.mzilske.pt.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.pt.UmlaufImpl;
import org.matsim.pt.UmlaufStueck;
import org.matsim.pt.UmlaufStueckI;
import org.matsim.pt.Wenden;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;

public class BackAndForthUmlaufBuilderImpl implements UmlaufBuilder {

	private static final Comparator<Departure> departureTimeComparator = new Comparator<Departure>() {

		public int compare(Departure o1, Departure o2) {
			return Double.compare(o1.getDepartureTime(), o2.getDepartureTime());
		}

	};

	private final NetworkLayer network;
	private final Collection<TransitLine> transitLines;
	private ArrayList<Umlauf> umlaeufe;
	private final CharyparNagelScoringConfigGroup config;

	public BackAndForthUmlaufBuilderImpl(NetworkLayer network, Collection<TransitLine> transitLines, CharyparNagelScoringConfigGroup config) {
		this.network = network;
		this.transitLines = transitLines;
		this.config = config;
	}

	public boolean canBuild() {
		for (TransitLine line : transitLines) {
			if (!canBuildThisLine(line)) {
				return false;
			}
		}
		return true;
	}

	public ArrayList<Umlauf> build() {
		if (!canBuild()) {
			throw new IllegalArgumentException();
		}
		umlaeufe = new ArrayList<Umlauf>();
		int id = 0;
		for (TransitLine line : transitLines) {
			Iterator<TransitRoute> i = line.getRoutes().values().iterator();
			TransitRoute routeH = i.next();
			TransitRoute routeR = i.next();
			Umlauf umlauf = new UmlaufImpl(new IdImpl(id++));
			List<Departure> departuresH = new ArrayList<Departure>(routeH.getDepartures().values());
			Collections.sort(departuresH, departureTimeComparator);
			Iterator<Departure> iH = departuresH.iterator();
			List<Departure> departuresR = new ArrayList<Departure>(routeR.getDepartures().values());
			Collections.sort(departuresR, departureTimeComparator);
			Iterator<Departure> iR = departuresR.iterator();
			while (iH.hasNext() && iR.hasNext()) {
				Departure departureH = iH.next();
				Departure departureR = iR.next();
				addDepartureToUmlauf(line, routeH, departureH, umlauf);
				addDepartureToUmlauf(line, routeR, departureR, umlauf);
			}
			umlaeufe.add(umlauf);
		}
		return umlaeufe;
	}

	private void addDepartureToUmlauf(TransitLine line, TransitRoute route,
			Departure departure, Umlauf umlauf) {
		UmlaufStueck umlaufStueck = new UmlaufStueck(line, route, departure);
		List<UmlaufStueckI> umlaufStuecke = umlauf.getUmlaufStuecke();
		if (!umlaufStuecke.isEmpty()) {
			UmlaufStueckI previousUmlaufStueck = umlaufStuecke.get(umlaufStuecke.size() - 1);
			Id fromLinkId = previousUmlaufStueck.getCarRoute().getEndLinkId();
			Id toLinkId = route.getRoute().getStartLinkId();
			if (!fromLinkId.equals(toLinkId)) {
				insertWenden(this.network.getLinks().get(fromLinkId), this.network.getLinks().get(toLinkId), umlauf);
			}
		}
		umlaufStuecke.add(umlaufStueck);
	}

	private boolean canBuildThisLine(TransitLine line) {
		if (line.getRoutes().size() != 2) {
			return false;
		}
		Iterator<TransitRoute> i = line.getRoutes().values().iterator();
		TransitRoute routeH = i.next();
		TransitRoute routeR = i.next();
		if (routeH.getDepartures().size() != routeR.getDepartures().size()) {
			return false;
		}
		return true;
	}

	private void insertWenden(Link fromLink, Link toLink, Umlauf umlauf) {
		FreespeedTravelTimeCost calculator = new FreespeedTravelTimeCost(this.config);
		LeastCostPathCalculator routingAlgo = new Dijkstra(network, calculator, calculator);

		Node startNode = fromLink.getToNode();
		Node endNode = toLink.getFromNode();

		double depTime = 0.0;

		Path wendenPath = routingAlgo.calcLeastCostPath(startNode, endNode, depTime);
		wendenPath = routingAlgo.calcLeastCostPath(startNode, endNode, depTime);
		if (wendenPath == null) {
			throw new RuntimeException("No route found from node "
					+ startNode.getId() + " to node " + endNode.getId() + ".");
		}
		NetworkRouteWRefs route = (NetworkRouteWRefs) this.network.getFactory()
				.createRoute(TransportMode.car, fromLink.getId(), toLink.getId());
		route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(wendenPath.links), toLink.getId());
		umlauf.getUmlaufStuecke().add(new Wenden(route));
	}


}
