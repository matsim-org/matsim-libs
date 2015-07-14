package org.matsim.contrib.wagonSim.schedule.mapping;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.xml.sax.SAXException;

/*
 * Version history:
 * 2010-??-??  balmermi   first version
 * 2012-09-28  mrieser    code cleanup, add public class methods, change adaptLinkCapacities to increase if necessary and not always add the pt-capacity
 */
/**
 * Adapts various transit related data structures so the schedule can be simulated for sure.
 * Essentially, it allows to:
 * <ul>
 * <li>make sure the capacities on the network are large enough that all transit vehicles can pass per hour,
 * even if the network capacity is scaled down to match a population sample.</li>
 * <li>make sure the freespeeds on the links are high enough that no transit vehicle gets delayed between
 * two stops just because of too slow speed</li>
 * </ul>
 *
 * @author balmermi
 */
public class NetworkAdaption {

	private final static Logger log = Logger.getLogger(NetworkAdaption.class);

	private final Scenario scenario;
	private final double popsample;

	public NetworkAdaption(final Scenario scenario) {
		this(scenario, 1.0);
	}

	public NetworkAdaption(final Scenario scenario, final double populationSampleSize) {
		this.scenario = scenario;
		this.popsample = populationSampleSize;
	}

	public void adaptLinkFreespeeds() {
		Map<Id,Tuple<Double,Double>> lttOffsets = getLinkTravelTimeOffsets(this.scenario);
		adaptLinkFreespeed(this.scenario, lttOffsets);
	}

	public void adaptLinkCapacities() {
		Map<Id,Map<Integer,Integer>> ptHCnts = getHourlyPtCounts(this.scenario);
		adaptLinkCapacities(this.scenario, ptHCnts, this.popsample);
	}
	
	// //////////////////////////////////////////////////////////////////////////
	// private methods
	// //////////////////////////////////////////////////////////////////////////

	private static final Map<Id,Tuple<Double,Double>> getLinkTravelTimeOffsets(final Scenario sc) {
		final TransitSchedule schedule = sc.getTransitSchedule();
		Map<Id,Tuple<Double,Double>> lttOffsets = new TreeMap<Id, Tuple<Double,Double>>();
		for (TransitLine tLine : schedule.getTransitLines().values()) {
			for (TransitRoute tRoute : tLine.getRoutes().values()) {
				for (int i=0; i<tRoute.getStops().size()-1; i++) {
					TransitRouteStop trsStart = tRoute.getStops().get(i);
					TransitRouteStop trsEnd = tRoute.getStops().get(i+1);
					double scheduledTTime = (trsEnd.getArrivalOffset() == Time.UNDEFINED_TIME ? trsEnd.getDepartureOffset() : trsEnd.getArrivalOffset()) -trsStart.getDepartureOffset();
					NetworkRoute r = tRoute.getRoute().getSubRoute(trsStart.getStopFacility().getLinkId(),trsEnd.getStopFacility().getLinkId());

					List<Id> rr = new ArrayList<Id>(r.getLinkIds().size()+1);
					rr.addAll(r.getLinkIds());
					rr.add(r.getEndLinkId());

					// calc route travel time
					double freespeedTTime = 0.0;
					for (Id lid : rr) {
						Link l = sc.getNetwork().getLinks().get(lid);
						freespeedTTime += l.getLength()/l.getFreespeed();
					}

					// if free speed travel time is higher than schedule travel time,
					// then offset is negative
					double offset = scheduledTTime - freespeedTTime;
					for (Id lid : rr) {
						Link l = sc.getNetwork().getLinks().get(lid);
						double tTime = l.getLength()/l.getFreespeed();
						double lttOffset = offset * (tTime / freespeedTTime);
						if (lttOffsets.get(lid) == null) {
							lttOffsets.put(lid,new Tuple<Double, Double>(lttOffset,lttOffset));
						}
						else {
							Tuple<Double,Double> t = lttOffsets.get(lid);
							double min = t.getFirst();
							double max = t.getSecond();
							if (lttOffset < min) { min = lttOffset; }
							if (lttOffset > max) { max = lttOffset; }
							lttOffsets.put(lid,new Tuple<Double, Double>(min,max));
						}
					}
				}
			}
		}
		return lttOffsets;
	}

	//////////////////////////////////////////////////////////////////////

	private static final Map<Id,Map<Integer,Integer>> getHourlyPtCounts(final Scenario sc) {
		final TransitSchedule schedule = sc.getTransitSchedule();
		Map<Id,Map<Integer,Integer>> ptHCnts = new TreeMap<Id, Map<Integer,Integer>>();
		for (TransitLine tLine : schedule.getTransitLines().values()) {
			for (TransitRoute tRoute : tLine.getRoutes().values()) {
				for (Departure d : tRoute.getDepartures().values()) {
					for (int i=0; i<tRoute.getStops().size()-1; i++) {
						TransitRouteStop trsStart = tRoute.getStops().get(i);
						TransitRouteStop trsEnd = tRoute.getStops().get(i+1);
						NetworkRoute r = tRoute.getRoute().getSubRoute(trsStart.getStopFacility().getLinkId(),trsEnd.getStopFacility().getLinkId());
						List<Id> rr = new ArrayList<Id>(r.getLinkIds().size()+1);
						for (Id lid : r.getLinkIds()) { rr.add(lid); }
						rr.add(r.getEndLinkId());

						double currTime = d.getDepartureTime()+trsStart.getDepartureOffset();
						for (Id lid : rr) {
							Link l = sc.getNetwork().getLinks().get(lid);
							currTime += l.getLength()/l.getFreespeed();
							Integer hour = (int)(currTime/3600.0);
							Map<Integer,Integer> ptHLCnts = ptHCnts.get(lid);
							if (ptHLCnts == null) { ptHLCnts = new TreeMap<Integer, Integer>(); ptHCnts.put(lid,ptHLCnts); }
							Integer cnt = ptHLCnts.get(hour);
							if (cnt == null) { ptHLCnts.put(hour,1); }
							else { ptHLCnts.put(hour,cnt+1); }
						}
					}
				}
			}
		}
		return ptHCnts;
	}

	//////////////////////////////////////////////////////////////////////

	private static final void adaptLinkFreespeed(final Scenario sc, final Map<Id,Tuple<Double,Double>> lttOffsets) {
		for (Map.Entry<Id, Tuple<Double, Double>> e : lttOffsets.entrySet()) {
			Id linkId = e.getKey();
			Link l = sc.getNetwork().getLinks().get(linkId);
			if (!l.getAllowedModes().contains(TransportMode.car) && l.getAllowedModes().contains(TransportMode.pt)) {
				double lTTimeNew = l.getLength()/l.getFreespeed() + e.getValue().getFirst();
				double fSpeedNew = l.getLength()/lTTimeNew;
				if ((fSpeedNew == Double.NEGATIVE_INFINITY) || (fSpeedNew <= 0.0)) {
					// keep original speed definition
				}
				else if ((fSpeedNew == Double.POSITIVE_INFINITY) || (fSpeedNew >= 200.0/3.6)) {
					fSpeedNew = 200.0/3.6; // max network speed
					log.info("change freespeed: linkId="+linkId+": fsOld="+l.getFreespeed()+" ==> fsNew="+fSpeedNew);
					l.setFreespeed(fSpeedNew);
				}
				else {
					fSpeedNew = fSpeedNew*3.6; // km/h
					fSpeedNew = Math.ceil(fSpeedNew/10.0)*10.0; // round up to next 10 km/h
					fSpeedNew = fSpeedNew/3.6; // m/s
					if (Math.abs(l.getFreespeed() - fSpeedNew) > 0.001) {
						log.info("change freespeed: linkId="+linkId+": fsOld="+l.getFreespeed()+" ==> fsNew="+fSpeedNew);
						l.setFreespeed(fSpeedNew);
					}
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	private static final void adaptLinkCapacities(final Scenario sc, final Map<Id,Map<Integer,Integer>> ptHCnts, final double popsample) {
		double ptVehCapacityFactor = 5;
		for (Map.Entry<Id, Map<Integer, Integer>> e : ptHCnts.entrySet()) {
			Id linkId = e.getKey();
			Link l = sc.getNetwork().getLinks().get(linkId);
			double max = 0;
			for (Integer cnt : e.getValue().values()) {
				if (max < cnt) { max = cnt; }
			}
			double minCap = ptVehCapacityFactor*max/popsample;
			minCap = Math.ceil(minCap/100.0)*100.0; // round up to the next 100 veh/hour
			if (l.getCapacity() < minCap) {
				log.info("increase capacity: linkId="+linkId+": capOld="+l.getCapacity()+" ==> newCap="+(minCap));
				l.setCapacity(minCap);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	private static final void adaptVehicleCapacities(final Vehicles vehicles, final double popsample) {
		for (VehicleType vt : vehicles.getVehicleTypes().values()) {
			vt.getCapacity().setSeats((int)Math.ceil(vt.getCapacity().getSeats()*popsample));
			vt.getCapacity().setStandingRoom((int)Math.ceil(vt.getCapacity().getStandingRoom()*popsample));
			log.info("vtid("+vt.getDescription()+")="+vt.getId()+": seats="+vt.getCapacity().getSeats()+"; stand="+vt.getCapacity().getStandingRoom());
		}
	}

	//////////////////////////////////////////////////////////////////////

	private static final void adaptVehicleAETimes(final Vehicles vehicles, final double popsample) {
		for (VehicleType vt : vehicles.getVehicleTypes().values()) {
			vt.setAccessTime(vt.getAccessTime()/popsample);
			vt.setEgressTime(vt.getEgressTime()/popsample);
			log.info("vtid("+vt.getDescription()+")="+vt.getId()+": aTime="+vt.getAccessTime()+"; eTime="+vt.getEgressTime());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {

		if (args.length != 5) {
			log.error("NetworkAdaption networkFile transitScheduleFile transitVehiclesFile popsample outputBase");
			System.exit(-1);
		}
		String networkFile = args[0];
		String transitScheduleFile = args[1];
		String transitVehiclesFile = args[2];
		Double popsample = Double.parseDouble(args[3]);
		String outputBase = args[4];

		log.info("networkFile: "+networkFile);
		log.info("transitScheduleFile: "+transitScheduleFile);
		log.info("transitVehiclesFile: "+transitVehiclesFile);
		log.info("popsample: "+popsample);
		log.info("outputBase: "+outputBase);

		log.info("parsing network, transitSchedule and transitVehicles...");
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
		new MatsimNetworkReader(sc).readFile(networkFile);
		new TransitScheduleReaderV1(sc).readFile(transitScheduleFile);
		new VehicleReaderV1(sc.getTransitVehicles()).readFile(transitVehiclesFile);
		log.info("done. (parsing)");

		log.info("getting link travel time offsets...");
		Map<Id,Tuple<Double,Double>> lttOffsets = getLinkTravelTimeOffsets(sc);
		log.info("done. (getting link travel time offsets)");

		log.info("adapting link free speed...");
		adaptLinkFreespeed(sc,lttOffsets);
		log.info("done. (adapting)");

		log.info("getting hourly pt counts...");
		Map<Id,Map<Integer,Integer>> ptHCnts = getHourlyPtCounts(sc);
		log.info("done. (getting hourly pt counts)");

		log.info("adapting link capacities...");
		adaptLinkCapacities(sc,ptHCnts, popsample);
		log.info("done. (adapting link capacities)");

		log.info("adapting vehicle capacities...");
		adaptVehicleCapacities(sc.getTransitVehicles(), popsample);
		log.info("done. (adapting vehicle capacities)");

		log.info("adapting vehicle access and egress times...");
		adaptVehicleAETimes(sc.getTransitVehicles(), popsample);
		log.info("done. (adapting vehicle access and egress times)");

		if (!(new File(outputBase).mkdir())) { log.warn("Could not create "+outputBase); }

		log.info("write network...");
		new NetworkWriter(sc.getNetwork()).write(outputBase+"/network.final.xml.gz");
		if (!(new File(outputBase+"/shp_network.final").mkdir())) { throw new RuntimeException("Could not create "+outputBase+"/shp_network.final"); }
		new NetworkWriteAsTable(outputBase+"/shp_network.final",5.0).run(sc.getNetwork());
		log.info("done. (write network)");

		log.info("writing transit vehicles...");
		new VehicleWriterV1(sc.getTransitVehicles()).writeFile(outputBase+"/transitVehicles.final.xml.gz");
		log.info("done. (writing)");

	}
}
