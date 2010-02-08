package playground.mzilske.bvg09;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.TransitScheduleWriterV1;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetworkReader;
import org.matsim.visum.VisumNetwork.StopPoint;
import org.xml.sax.SAXException;


public class VisumHafasScheduleMerger {

	private static Logger log = Logger.getLogger(VisumHafasScheduleMerger.class);

	private static String path = "../berlin-bvg09/pt/nullfall_M44_344_U8/";
	private static String InNetworkFile = path + "intermediateNetwork.xml";
	private static String InTransitScheduleFile = path + "intermediateTransitSchedule.xml";
	private static String HafasTransitScheduleFile = path + "transitSchedule-HAFAS.xml";
	private static String InVisumNetFile = "../berlin-bvg09/urdaten/nullfall2009-05-25.net";
	// private static String OutNetworkFile = path + "network.xml";
	private static String OutTransitScheduleFile = path + "mergedTransitSchedule.xml";

	private Map<Id, Id> hafas2visum;

	private VisumNetwork vNetwork;

	private static Id REMOVE = new IdImpl("REMOVE");

	private static Map<Id, Id> getHafas2visumM44() {
		Map<Id, Id> hafas2visum = new HashMap<Id, Id>();
		hafas2visum.put(new IdImpl("9081202"), new IdImpl("812020"));
		hafas2visum.put(new IdImpl("9081255"), new IdImpl("812550"));
		hafas2visum.put(new IdImpl("9081203"), new IdImpl("812030"));
		hafas2visum.put(new IdImpl("9081256"), new IdImpl("812560"));
		hafas2visum.put(new IdImpl("9081257"), new IdImpl("812570"));
		hafas2visum.put(new IdImpl("9081201"), new IdImpl("812013"));
		hafas2visum.put(new IdImpl("9080652"), new IdImpl("806520"));
		hafas2visum.put(new IdImpl("9080603"), new IdImpl("806030"));
		hafas2visum.put(new IdImpl("9080601"), new IdImpl("806010"));
		hafas2visum.put(new IdImpl("9080654"), new IdImpl("806540"));
		hafas2visum.put(new IdImpl("9080407"), new IdImpl("804070"));
		hafas2visum.put(new IdImpl("9080406"), new IdImpl("804060"));
		hafas2visum.put(new IdImpl("9080102"), new IdImpl("801020"));
		hafas2visum.put(new IdImpl("9080103"), new IdImpl("801030"));
		hafas2visum.put(new IdImpl("9080153"), new IdImpl("801530"));
		hafas2visum.put(new IdImpl("9080104"), new IdImpl("801040"));
		hafas2visum.put(new IdImpl("9079205"), new IdImpl("792050"));
		hafas2visum.put(new IdImpl("9079221"), new IdImpl("792200"));

		hafas2visum.put(new IdImpl("9080181"), REMOVE); // Betriebshof Britz
		return hafas2visum;
	}

	private static Map<Id, Id> getHafas2visum344() {
		Map<Id, Id> hafas2visum = new HashMap<Id, Id>();
		hafas2visum.put(new IdImpl("9079204"), new IdImpl("792040"));
		hafas2visum.put(new IdImpl("9079221"), new IdImpl("792200"));
		hafas2visum.put(new IdImpl("9079201"), new IdImpl("792013"));
		hafas2visum.put(new IdImpl("9079203"), new IdImpl("792030"));
		hafas2visum.put(new IdImpl("9079202"), new IdImpl("792023"));
		hafas2visum.put(new IdImpl("9079291"), new IdImpl("792910"));
		hafas2visum.put(new IdImpl("9078106"), new IdImpl("781060"));
		hafas2visum.put(new IdImpl("9078104"), new IdImpl("781040"));
		hafas2visum.put(new IdImpl("9078101"), new IdImpl("781015"));
		return hafas2visum;
	}

	private static Map<Id, Id> getHafas2visumU8() {
		Map<Id, Id> hafas2visum = new HashMap<Id, Id>();
		hafas2visum.put(new IdImpl("9096101"), new IdImpl("964072"));
		hafas2visum.put(new IdImpl("9096410"), new IdImpl("964100"));
		hafas2visum.put(new IdImpl("9096458"), new IdImpl("964580"));
		hafas2visum.put(new IdImpl("9086160"), new IdImpl("861600"));
		hafas2visum.put(new IdImpl("9085104"), new IdImpl("851040"));
		hafas2visum.put(new IdImpl("9085203"), new IdImpl("852030"));
		hafas2visum.put(new IdImpl("9085202"), new IdImpl("852020"));
		hafas2visum.put(new IdImpl("9009202"), new IdImpl("92025"));
		hafas2visum.put(new IdImpl("9009203"), new IdImpl("92030"));
		hafas2visum.put(new IdImpl("9007102"), new IdImpl("71023"));
		hafas2visum.put(new IdImpl("9007103"), new IdImpl("71030"));
		hafas2visum.put(new IdImpl("9007110"), new IdImpl("71100"));
		hafas2visum.put(new IdImpl("9100023"), new IdImpl("1000230"));
		hafas2visum.put(new IdImpl("9100051"), new IdImpl("1000510"));
		hafas2visum.put(new IdImpl("9100003"), new IdImpl("1000034"));
		hafas2visum.put(new IdImpl("9100004"), new IdImpl("1000043"));
		hafas2visum.put(new IdImpl("9100008"), new IdImpl("1000080"));
		hafas2visum.put(new IdImpl("9013101"), new IdImpl("131010"));
		hafas2visum.put(new IdImpl("9013102"), new IdImpl("131020"));
		hafas2visum.put(new IdImpl("9016201"), new IdImpl("162010"));
		hafas2visum.put(new IdImpl("9078101"), new IdImpl("781013"));
		hafas2visum.put(new IdImpl("9079202"), new IdImpl("792020"));
		hafas2visum.put(new IdImpl("9079201"), new IdImpl("792010"));
		hafas2visum.put(new IdImpl("9079221"), new IdImpl("792215"));
		return hafas2visum;
	}

	private TransitLine hafasLine;
	private TransitLine visumLine;

	ScenarioImpl intermediateScenario;
	Config intermediateConfig;
	ScenarioImpl hafasScenario;
	Config hafasConfig;
	ScenarioImpl outScenario;
	Config outConfig;

	private TransitSchedule outSchedule;

	public VisumHafasScheduleMerger() {
		this.intermediateScenario = new ScenarioImpl();
		this.intermediateConfig = this.intermediateScenario.getConfig();
		this.hafasScenario = new ScenarioImpl();
		this.hafasConfig = this.hafasScenario.getConfig();
		this.outScenario = new ScenarioImpl();
		this.outConfig = this.outScenario.getConfig();
	}

	private void prepareConfig() {
		this.hafasConfig.scenario().setUseTransit(true);
		this.hafasConfig.scenario().setUseVehicles(true);
		this.hafasConfig.network().setInputFile(InNetworkFile);
		this.intermediateConfig.scenario().setUseTransit(true);
		this.intermediateConfig.scenario().setUseVehicles(true);
		this.intermediateConfig.network().setInputFile(InNetworkFile);
		this.outConfig.scenario().setUseTransit(true);
		this.outConfig.scenario().setUseVehicles(true);
		this.outConfig.network().setInputFile(InNetworkFile);
		ScenarioLoaderImpl intermediateLoader = new ScenarioLoaderImpl(intermediateScenario);
		intermediateLoader.loadScenario();
		ScenarioLoaderImpl hafasLoader = new ScenarioLoaderImpl(hafasScenario);
		hafasLoader.loadScenario();
		try {
			new TransitScheduleReaderV1(intermediateScenario.getTransitSchedule(), intermediateScenario.getNetwork()).readFile(InTransitScheduleFile);
		} catch (SAXException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		} catch (IOException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		}
		try {
			new TransitScheduleReaderV1(hafasScenario.getTransitSchedule(), hafasScenario.getNetwork()).readFile(HafasTransitScheduleFile);
		} catch (SAXException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		} catch (IOException e) {
			throw new RuntimeException("could not read transit schedule.", e);
		}
		ScenarioLoaderImpl outLoader = new ScenarioLoaderImpl(outScenario);
		outLoader.loadScenario();
		readVisumNetwork();
	}

	private void treatRoutes(Id hafasLineId, Id visumLineId, Map<Id, Id> hafas2visum) {
		this.hafas2visum = hafas2visum;
		this.hafasLine = hafasScenario.getTransitSchedule().getTransitLines().get(hafasLineId);
		this.visumLine = intermediateScenario.getTransitSchedule().getTransitLines().get(visumLineId);
		TransitLine outLine = outSchedule.getFactory().createTransitLine(hafasLineId);
		for (TransitRoute route : hafasLine.getRoutes().values()) {
			TransitRoute outRoute = treatRoute(route);
			outLine.addRoute(outRoute);
		}
		outSchedule.addTransitLine(outLine);
	}

	private void copyFacilities() {
		TransitSchedule outSchedule = outScenario.getTransitSchedule();
		for (TransitStopFacility facility : intermediateScenario.getTransitSchedule().getFacilities().values()) {
			if (!outSchedule.getFacilities().containsValue(facility)) {
				outSchedule.addStopFacility(facility);
			}
		}
		this.outSchedule = outSchedule;
	}

	private TransitRoute treatRoute(TransitRoute route) {
		List<Id> idRouteHafas = getIdRoute(route.getStops());
		List<Id> idRouteVisum = listHafas2Visum(idRouteHafas);
		for (TransitRoute candidateRoute : visumLine.getRoutes().values()) {
			NetworkRouteWRefs networkRoute = tryToMatch(candidateRoute, idRouteVisum);
			if (networkRoute != null) {
				List<TransitRouteStop> outStops = new ArrayList<TransitRouteStop>();
				for (TransitRouteStop stop : route.getStops()) {
					Id visumId = hafas2visum.get(stop.getStopFacility().getId());
					if (!visumId.equals(REMOVE)) {
						TransitRouteStop outStop = outScenario.getTransitSchedule().getFactory().createTransitRouteStop(outScenario.getTransitSchedule().getFacilities().get(visumId), stop.getArrivalOffset(), stop.getDepartureOffset());
						outStops.add(outStop);
					}
				}
				TransitRoute outRoute = outScenario.getTransitSchedule().getFactory().createTransitRoute(route.getId(), networkRoute, outStops, route.getTransportMode());
				for (Departure departure : route.getDepartures().values()) {
					outRoute.addDeparture(departure);
				}
				return outRoute;
			}
		}
		throw new RuntimeException("Nothing found: " + route.getId());
	}

	private List<Id> getIdRoute(List<TransitRouteStop> stops) {
		List<Id> idRoute = new ArrayList<Id>();
		for (TransitRouteStop stop : stops) {
			idRoute.add(stop.getStopFacility().getId());
		}
		return idRoute;
	}

	private NetworkRouteWRefs tryToMatch(TransitRoute candidateRoute, List<Id> idRouteToMatch) {
		NetworkRouteWRefs linkNetworkRoute = candidateRoute.getRoute();
		List<Link> links = getAllLink(linkNetworkRoute);
		ListIterator<Link> linkIterator = links.listIterator();
		boolean firstStop = true;
		int firstLinkIndex = -1;
		Link link;
		if (linkIterator.hasNext()) {
			link = linkIterator.next();
		} else {
			return null;
		}
		for (Id stopPointNo : idRouteToMatch) {
			Id stopPointNodeNo = findNodeId(stopPointNo);
			if (stopPointNodeNo == null) {
				throw new RuntimeException("Stop point " + stopPointNo + " doesn't appear to be in the visum network.");
			}
			while (!stopPointNodeNo.equals(link.getToNode().getId())) {
				if (linkIterator.hasNext()) {
					link = linkIterator.next();
				} else {
					return null;
				}
			}
			if (firstStop) {
				firstLinkIndex = linkIterator.previousIndex();
			}
			firstStop = false;
		}
		int lastLinkIndex = linkIterator.previousIndex();
		List<Link> usedSegmentLinks = links.subList(firstLinkIndex, lastLinkIndex + 1);
		NetworkRouteWRefs usedSegment = NetworkUtils.createLinkNetworkRoute(NetworkUtils.getLinkIds(usedSegmentLinks), this.outScenario.getNetwork());
		return usedSegment;
	}

	private Id findNodeId(Id stopPointNo) {
		for (StopPoint stopPoint : vNetwork.stopPoints.values()) {
			if (stopPointNo.toString().equals(stopPoint.id.toString())) {
				return stopPoint.nodeId;
			}
		}
		return null;
	}

	private List<Link> getAllLink(NetworkRouteWRefs linkNetworkRoute) {
		ArrayList<Link> links = new ArrayList<Link>();
		links.add(this.outScenario.getNetwork().getLinks().get(linkNetworkRoute.getStartLinkId()));
		links.addAll(NetworkUtils.getLinks(this.outScenario.getNetwork(), linkNetworkRoute.getLinkIds()));
		links.add(this.outScenario.getNetwork().getLinks().get(linkNetworkRoute.getEndLinkId()));
		return links;
	}

	private List<Id> listHafas2Visum(List<Id> idRouteHafas) {
		List<Id> idRouteVisum = new ArrayList<Id>();
		for (Id hafasId : idRouteHafas) {
			Id visumId = hafas2visum.get(hafasId);
			if (!visumId.equals(REMOVE)) {
				idRouteVisum.add(visumId);
			}
		}
		return idRouteVisum;
	}

	private void readVisumNetwork()  {
		vNetwork = new VisumNetwork();
		log.info("reading visum network.");
		try {
			new VisumNetworkReader(vNetwork).read(InVisumNetFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeNetworkAndScheduleAndVehicles() {
		try {
			new TransitScheduleWriterV1(outScenario.getTransitSchedule())
					.write(OutTransitScheduleFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		VisumHafasScheduleMerger visumHafasScheduleMerger = new VisumHafasScheduleMerger();
		visumHafasScheduleMerger.prepareConfig();
		visumHafasScheduleMerger.copyFacilities();
		visumHafasScheduleMerger.treatRoutes(new IdImpl("M44  "), new IdImpl("B-M44"), getHafas2visumM44());
		visumHafasScheduleMerger.treatRoutes(new IdImpl("344  "), new IdImpl("B-344"), getHafas2visum344());
		visumHafasScheduleMerger.treatRoutes(new IdImpl("U8   "), new IdImpl("U-8"), getHafas2visumU8());
		visumHafasScheduleMerger.writeNetworkAndScheduleAndVehicles();
	}

}
