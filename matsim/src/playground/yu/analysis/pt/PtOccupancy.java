/**
 * 
 */
package playground.yu.analysis.pt;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.misc.Time;

import playground.yu.utils.io.SimpleWriter;

/**
 * shows the ocuppancy situations of pt vehicles.
 * 
 * @author yu
 * 
 */
public class PtOccupancy implements PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler {
	private Map<Id, Integer> occups = new HashMap<Id, Integer>();
	private Map<Id, Map<Double, Integer>> timeOccups = new HashMap<Id, Map<Double, Integer>>();

	public void handleEvent(PersonEntersVehicleEvent event) {
		Id vehId = event.getVehicleId();
		double time = event.getTime() / 3600.0;

		Integer occup = occups.get(vehId);
		if (occup == null)
			occup = 0;
		occups.put(vehId, ++occup);

		// System.out.println(">>>>> veh:\t" + vehId + "\toccup:\t"
		// + occups.get(vehId));

		Map<Double, Integer> timeOccup = timeOccups.get(vehId);
		if (timeOccup == null)
			timeOccup = new TreeMap<Double, Integer>();

		timeOccup.put(time, occups.get(vehId));
		timeOccups.put(vehId, timeOccup);
	}

	public void reset(int iteration) {
	}

	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id vehId = event.getVehicleId();
		double time = event.getTime() / 3600.0;

		Integer occup = occups.get(vehId);
		occups.put(vehId, --occup);

		// System.out.println(">>>>> veh:\t" + vehId + "\toccup:\t"
		// + occups.get(vehId));

		Map<Double, Integer> timeOccup = timeOccups.get(vehId);

		timeOccup.put(time, occups.get(vehId));
		timeOccups.put(vehId, timeOccup);

		// if (occups.get(vehId) == 0)
		// occups.remove(vehId);
	}

	public void write(String outputFilenameBase) {

		XYLineChart avgSpeedChart = new XYLineChart("vehicle(bus) occupancies",
				"time", "agents in bus [per.]");
		SimpleWriter writer = new SimpleWriter(outputFilenameBase + "txt");

		for (Entry<Id, Map<Double, Integer>> vto : timeOccups.entrySet()) {
			Map<Double, Integer> tos = vto.getValue();
			int size = tos.size();
			double[] xs = new double[size * 2], ys = new double[size * 2];
			int i = 0;
			for (Entry<Double, Integer> to : tos.entrySet()) {
				xs[i] = to.getKey() - 1.0 / 3600.0;
				ys[i] = (i > 0) ? ys[i - 1] : 0;
				xs[i + 1] = to.getKey();
				ys[i + 1] = to.getValue();
				i += 2;
			}
			Id vehId = vto.getKey();

			avgSpeedChart.addSeries("vehId\t" + vehId, xs, ys);

			writer.writeln("vehId:\t" + vehId
					+ "\ntime\tthe number of passengers");
			for (int j = 0; j < size * 2; j++)
				writer.writeln(Time.writeTime(xs[j] * 3600.0) + "\t" + ys[j]);
			writer.writeln("----------");
		}

		writer.close();
		avgSpeedChart.saveAsPng(outputFilenameBase + "png", 1024, 768);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String eventsFilename = "../berlin-bvg09/pt/nullfall_M44_344/test/output200/ITERS/it.100/100.events.xml.gz";

		EventsManager em = new EventsManagerImpl();

		PtOccupancy po = new PtOccupancy();
		em.addHandler(po);

		new MatsimEventsReader(em).readFile(eventsFilename);

		po
				.write("../berlin-bvg09/pt/nullfall_M44_344/test/output200/ITERS/it.100/100.occupancies.");
	}
}
