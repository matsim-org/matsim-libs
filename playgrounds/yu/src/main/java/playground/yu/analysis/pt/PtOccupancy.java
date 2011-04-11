/**
 * 
 */
package playground.yu.analysis.pt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.xml.sax.SAXException;

import playground.yu.utils.charts.TimeLineChart;
import playground.yu.utils.io.SimpleWriter;

/**
 * shows the ocuppancy situations of pt vehicles.
 * 
 * @author yu
 * 
 */
public class PtOccupancy implements PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler {
	/* Map<vehId,No.ofInstantaneousOccupancies> */
	private final Map<Id, Integer> occups = new HashMap<Id, Integer>();
	private Map<Id, Integer> routeOccups;
	/* Map<vehId,Map<time,NO.ofOccupancies */
	private final Map<Id, Map<Double, Integer>> timeOccups = new HashMap<Id, Map<Double, Integer>>();
	private Map<Id, Map<Double, Integer>> routeTimeOccups;
	private TransitSchedule schedule = null;
	/* Map<vehId,routeId>, Map<routeId,transitLineId> */
	private Map<Id, Id> vehRouteIds, routeLineIds;

	public PtOccupancy(TransitSchedule schedule) {
		this.schedule = schedule;
		if (schedule != null) {
			/* Map<routeId,No.ofInstantaneousOccupancies> */
			routeOccups = new HashMap<Id, Integer>();
			/* Map<routeId,Map<time,NO.ofOccupancies */
			routeTimeOccups = new HashMap<Id, Map<Double, Integer>>();

			vehRouteIds = new HashMap<Id, Id>();
			routeLineIds = new HashMap<Id, Id>();
			for (TransitLine tl : schedule.getTransitLines().values()) {
				Id tlId = tl.getId();
				// System.out.println("TransitLine:\t" + tlId);
				for (TransitRoute tr : tl.getRoutes().values()) {
					Id trId = tr.getId();
					// System.out.println("TransitRoute:\t" + trId);
					routeLineIds.put(trId, tlId);
					for (Departure dp : tr.getDepartures().values())
						vehRouteIds.put(dp.getVehicleId(), trId);
				}
				// System.out.println("------------------");
			}
		}
	}

	public void handleEvent(PersonEntersVehicleEvent event) {
		Id vehId = event.getVehicleId();
		double time = event.getTime();

		Integer occup = occups.get(vehId);
		if (occup == null)
			occup = 0;
		occups.put(vehId, ++occup);

		Map<Double, Integer> timeOccup = timeOccups.get(vehId);
		if (timeOccup == null)
			timeOccup = new TreeMap<Double, Integer>();
		timeOccup.put(time, occups.get(vehId));
		timeOccups.put(vehId, timeOccup);

		if (schedule != null) {
			Id routeId = vehRouteIds.get(vehId);

			Integer ro = routeOccups.get(routeId);
			if (ro == null)
				ro = 0;
			routeOccups.put(routeId, ++ro);

			Map<Double, Integer> rto = routeTimeOccups.get(routeId);
			if (rto == null)
				rto = new TreeMap<Double, Integer>();
			rto.put(time, routeOccups.get(routeId));
			routeTimeOccups.put(routeId, rto);
		}
	}

	public void reset(int iteration) {
	}

	public void handleEvent(PersonLeavesVehicleEvent event) {
		// System.out
		// .println(">>>>> i now am in handleEvent PersonLeavesVehicleEvent");
		Id vehId = event.getVehicleId();
		double time = event.getTime();

		Integer occup = occups.get(vehId);
		occups.put(vehId, --occup);
		// System.out.println("occups\t" + occups);

		Map<Double, Integer> timeOccup = timeOccups.get(vehId);
		timeOccup.put(time, occups.get(vehId));
		timeOccups.put(vehId, timeOccup);

		if (schedule != null) {
			Id routeId = vehRouteIds.get(vehId);

			Integer ro = routeOccups.get(routeId);
			routeOccups.put(routeId, --ro);
			// System.out.println("routeOccups\t" + routeOccups);

			Map<Double, Integer> rto = routeTimeOccups.get(routeId);
			rto.put(time, routeOccups.get(routeId));
			routeTimeOccups.put(routeId, rto);
		}
	}

	public void write(String outputFilenameBase) {
		SimpleWriter writer = new SimpleWriter(outputFilenameBase + "txt");

		// -------------------------veh occupancies-----------------------------
		TimeLineChart chart = new TimeLineChart("vehicle(bus) occupancies",
				"time", "agents in bus [per.]");

		for (Entry<Id, Map<Double, Integer>> toEntry : timeOccups.entrySet()) {
			Map<Double, Integer> tos = toEntry.getValue();
			int size = tos.size();
			double[] xs = new double[size * 2], ys = new double[size * 2];
			int i = 0;
			for (Entry<Double, Integer> to : tos.entrySet()) {
				xs[i] = (to.getKey() - 1.0);
				ys[i] = (i > 0) ? ys[i - 1] : 0;
				xs[i + 1] = to.getKey();
				ys[i + 1] = to.getValue();
				i += 2;
			}
			Id vehId = toEntry.getKey();

			chart.addSeries("veh:\t" + vehId, xs, ys);

			writer.writeln("vehId:\t" + vehId
					+ "\ntime\tthe number of passengers");
			for (int j = 0; j < size * 2; j++)
				writer.writeln(Time.writeTime(xs[j] * 3600.0) + "\t" + ys[j]);
			writer.writeln("----------");
		}

		chart.saveAsPng(outputFilenameBase + "png", 1024, 768);
		// -----------------------route occupancies-----------------------------
		/* Map<lineId,chart> */
		Map<Id, TimeLineChart> charts = new HashMap<Id, TimeLineChart>();

		for (Entry<Id, Map<Double, Integer>> rtoEntry : routeTimeOccups
				.entrySet()) {
			Map<Double, Integer> tos = rtoEntry.getValue();
			int size = tos.size();
			double[] xs = new double[size * 2], ys = new double[size * 2];
			int i = 0;
			for (Entry<Double, Integer> to : tos.entrySet()) {
				xs[i] = (to.getKey()/* time */- 1.0);
				ys[i] = (i > 0) ? ys[i - 1] : 0;
				xs[i + 1] = to.getKey();
				ys[i + 1] = to.getValue()/* occupancies */;
				i += 2;
			}
			Id routeId = rtoEntry.getKey();
			Id lineId = routeLineIds.get(routeId);
			TimeLineChart chartR = charts.get(lineId);
			if (chartR == null)
				chartR = new TimeLineChart(
						"TransitRoute occupancies of TransitLine " + lineId,
						"time", "agents in bus(route) [per.]");

			chartR.addSeries("route:\t" + routeId, xs, ys);
			charts.put(lineId, chartR);
			writer.writeln("routeId:\t" + routeId
					+ "\ntime\tthe number of passengers");
			for (int j = 0; j < size * 2; j++)
				writer.writeln(Time.writeTime(xs[j] * 3600.0) + "\t" + ys[j]);
			writer.writeln("----------");
		}
		for (Entry<Id, TimeLineChart> chartEntry : charts.entrySet())
			chartEntry.getValue()
					.saveAsPng(
							outputFilenameBase + "line." + chartEntry.getKey()
									+ ".png", 1024, 768);

		writer.close();

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String eventsFilename = "../berlin-bvg09/pt/m2_schedule_delay/m2_out_100a/m2_out_100a/ITERS/it.1000/1000.events.xml.gz";
		String scheduleFilename = "../berlin-bvg09/pt/m2_schedule_delay/transitSchedule.xml";
		String netFilename = "../berlin-bvg09/pt/m2_schedule_delay/network.xml";

		ScenarioImpl s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		s.getConfig().scenario().setUseTransit(true);

		new MatsimNetworkReader(s).readFile(netFilename);

		TransitSchedule schedule = s.getTransitSchedule();
		try {
			new TransitScheduleReader(s).readFile(scheduleFilename);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		EventsManager em = (EventsManager) EventsUtils.createEventsManager();

		PtOccupancy po = new PtOccupancy(schedule);
		em.addHandler(po);

		new MatsimEventsReader(em).readFile(eventsFilename);

		po
				.write("../berlin-bvg09/pt/m2_schedule_delay/m2_out_100a/m2_out_100a/ITERS/it.1000/1000.occupancies.");
	}
}
