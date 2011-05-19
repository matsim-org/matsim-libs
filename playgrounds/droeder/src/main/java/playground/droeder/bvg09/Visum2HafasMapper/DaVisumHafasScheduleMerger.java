package playground.droeder.bvg09.Visum2HafasMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetwork.StopPoint;
import org.matsim.visum.VisumNetworkReader;

import playground.droeder.DaPaths;
/**
 * doesn't work yet. need to be checked
 *
 * @author droeder
 *
 */

@Deprecated
public class DaVisumHafasScheduleMerger {

	private static Logger log = Logger.getLogger(DaVisumHafasScheduleMerger.class);

	private static String path = DaPaths.OUTPUT + "bvg09/";
	private static String InNetworkFile = path + "intermediateNetwork.xml";
	private static String InTransitScheduleFile = path + "intermediateTransitSchedule.xml";
	private static String HafasTransitScheduleFile = path + "transitSchedule-HAFAS.xml";
	private static String InVisumNetFile = DaPaths.BVG09 + "nullfall2009-05-25.net";
	// private static String OutNetworkFile = path + "network.xml";
	private static String OutTransitScheduleFile = path + "mergedTransitSchedule.xml";

	private VisumNetwork vNetwork;

	static Id REMOVE = new IdImpl("REMOVE");

	ScenarioImpl intermediateScenario;
	Config intermediateConfig;
	ScenarioImpl hafasScenario;
	Config hafasConfig;
	ScenarioImpl outScenario;
	Config outConfig;

	private TransitSchedule outSchedule;

	public DaVisumHafasScheduleMerger() {
		this.intermediateScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.intermediateConfig = this.intermediateScenario.getConfig();
		this.hafasScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.hafasConfig = this.hafasScenario.getConfig();
		this.outScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
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
		new TransitScheduleReaderV1(intermediateScenario.getTransitSchedule(), intermediateScenario.getNetwork(), intermediateScenario).readFile(InTransitScheduleFile);
		new TransitScheduleReaderV1(hafasScenario.getTransitSchedule(), hafasScenario.getNetwork(), hafasScenario).readFile(HafasTransitScheduleFile);
		ScenarioLoaderImpl outLoader = new ScenarioLoaderImpl(outScenario);
		outLoader.loadScenario();
		readVisumNetwork();
	}

	private void treatAllRoutes(){

		Visum2HafasMapper mapper = new Visum2HafasMapper();
		Map<Id, Map<Id, Id>> visum2HafasMap = mapper.getVisum2HafasMap();
		Map<Id, Id> visum2hafasLineIds = mapper.getMatchedLines();

		for(Entry<Id, Id> e : visum2hafasLineIds.entrySet()){
			System.out.println(e.getKey() + " " + e.getValue());
		}

		int i = 0;
		for (Entry<Id, TransitLine> entry : this.intermediateScenario.getTransitSchedule().getTransitLines().entrySet()) {
			if (visum2hafasLineIds.get(entry.getKey()) == null) {
				log.warn("Could not find hafas line for visum line " + entry.getKey() + " Adding anyway.");
				outSchedule.addTransitLine(this.intermediateScenario.getTransitSchedule().getTransitLines().get(entry.getKey()));
			} else {
				TransitLine outLine = treatRoutes(visum2hafasLineIds.get(entry.getKey()), entry.getKey(), visum2HafasMap.get(entry.getKey()));
				if(outLine == null){
					outSchedule.addTransitLine(this.intermediateScenario.getTransitSchedule().getTransitLines().get(entry.getKey()));
					log.error("there was a matching for line " + entry.getKey().toString() + " but there was no matching found! Adding anyway!");
				}else{
					outSchedule.addTransitLine(outLine);
					log.info("add hafas line for visum line " + entry.getKey().toString());
					i++;
				}

			}
		}
		log.info(i + " of " + visum2hafasLineIds.size() + " prematched visum lines are matched!");
	}

	private TransitLine treatRoutes(Id hafasLineId, Id visumLineId, Map<Id, Id> visum2Hafas) {
		TransitLine visumLine = intermediateScenario.getTransitSchedule().getTransitLines().get(visumLineId);
		TransitLine outLine = outSchedule.getFactory().createTransitLine(visumLineId);
		for (TransitRoute route : hafasScenario.getTransitSchedule().getTransitLines().get(hafasLineId).getRoutes().values()) {
			TransitRoute outRoute = treatRoute(visumLine, route, visum2Hafas);
			if(outRoute == null){
				return null;
			}
			outLine.addRoute(outRoute);
		}
		return outLine;

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

	private TransitRoute treatRoute(TransitLine visumLine, TransitRoute hafasRoute, Map<Id, Id> visum2Hafas) {
		List<Id> idRouteHafas = getIdRoute(hafasRoute.getStops());
		List<Id> idRouteVisum = listHafas2Visum(idRouteHafas, visum2Hafas);
		for (TransitRoute candidateRoute : visumLine.getRoutes().values()) {
			NetworkRoute networkRoute = tryToMatch(candidateRoute, idRouteVisum);
			if (networkRoute != null) {
				List<TransitRouteStop> outStops = new ArrayList<TransitRouteStop>();
				for (TransitRouteStop stop : hafasRoute.getStops()) {
					Id visumId = REMOVE;
					for(Entry<Id, Id> e : visum2Hafas.entrySet()){
						if (stop.getStopFacility().getId().equals(e.getValue())){
							visumId = e.getKey();
						}
					}
					TransitRouteStop outStop = outScenario.getTransitSchedule().getFactory().createTransitRouteStop(outScenario.getTransitSchedule().getFacilities().get(visumId), stop.getArrivalOffset(), stop.getDepartureOffset());
					outStops.add(outStop);
				}
				TransitRoute outRoute = outScenario.getTransitSchedule().getFactory().createTransitRoute(hafasRoute.getId(), networkRoute, outStops, hafasRoute.getTransportMode());
				for (Departure departure : hafasRoute.getDepartures().values()) {
					outRoute.addDeparture(departure);
				}
				return outRoute;
			}
		}
		return null;
	}

	private List<Id> getIdRoute(List<TransitRouteStop> stops) {
		List<Id> idRoute = new ArrayList<Id>();
		for (TransitRouteStop stop : stops) {
			idRoute.add(stop.getStopFacility().getId());
		}
		return idRoute;
	}

	private NetworkRoute tryToMatch(TransitRoute candidateRoute, List<Id> idRouteToMatch) {
		NetworkRoute linkNetworkRoute = candidateRoute.getRoute();
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
		NetworkRoute usedSegment = RouteUtils.createNetworkRoute(NetworkUtils.getLinkIds(usedSegmentLinks), this.outScenario.getNetwork());
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

	private List<Link> getAllLink(NetworkRoute linkNetworkRoute) {
		ArrayList<Link> links = new ArrayList<Link>();
		links.add(this.outScenario.getNetwork().getLinks().get(linkNetworkRoute.getStartLinkId()));
		links.addAll(NetworkUtils.getLinks(this.outScenario.getNetwork(), linkNetworkRoute.getLinkIds()));
		links.add(this.outScenario.getNetwork().getLinks().get(linkNetworkRoute.getEndLinkId()));
		return links;
	}

	private List<Id> listHafas2Visum(List<Id> idRouteHafas, Map<Id, Id> visum2Hafas) {
		List<Id> idRouteVisum = new ArrayList<Id>();
		for (Id hafasId : idRouteHafas) {
			for(Entry<Id, Id> e : visum2Hafas.entrySet()){
				if (hafasId.equals(e.getValue())) {
					idRouteVisum.add(e.getKey());
				}
			}
		}
		return idRouteVisum;
	}

	private void readVisumNetwork()  {
		vNetwork = new VisumNetwork();
		log.info("reading visum network.");
		new VisumNetworkReader(vNetwork).read(InVisumNetFile);
	}

	private void writeNetworkAndScheduleAndVehicles() {
		new TransitScheduleWriterV1(outScenario.getTransitSchedule()).write(OutTransitScheduleFile);
	}

	public static void main(final String[] args) {
		DaVisumHafasScheduleMerger visumHafasScheduleMerger = new DaVisumHafasScheduleMerger();
		visumHafasScheduleMerger.prepareConfig();
		visumHafasScheduleMerger.copyFacilities();
		visumHafasScheduleMerger.treatAllRoutes();
		visumHafasScheduleMerger.writeNetworkAndScheduleAndVehicles();
	}

}
