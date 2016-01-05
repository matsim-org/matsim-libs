package opdytsintegration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class OccupancyAnalyzerTest {

	private static void smallTest() {

		System.out.println("STARTED SMALL TEST");
		
		final int startTime_s = 0;
		final int binSize_s = 10;
		final int binCnt = 5;
		final OccupancyAnalyzer analyzer = new OccupancyAnalyzer(startTime_s,
				binSize_s, binCnt);

		final Id<Link> id1 = Id.createLinkId("1");

		// [0,10): avg. occupancy = 0, last occupancy = 0

		analyzer.handleEvent(new LinkEnterEvent(10.0, null, id1));
		// time = 10, in = 1, out = 0
		analyzer.handleEvent(new VehicleEntersTrafficEvent(10.0, null, id1,
				null, null, 0.0));
		// time = 10, in = 2, out = 0
		analyzer.handleEvent(new LinkEnterEvent(14.0, null, id1));
		// time = 14, in = 3, out = 0
		analyzer.handleEvent(new LinkLeaveEvent(19.0, null, id1));
		// time = 19, in = 3, out = 1

		// [10,20): avg. occupancy = 2.5, last occupancy = 2

		analyzer.handleEvent(new VehicleEntersTrafficEvent(20.0, null, id1,
				null, "car", 0.0));
		// time = 20, in = 4, out = 1
		analyzer.handleEvent(new LinkEnterEvent(29.99, null, id1));
		// time = 29, in = 5, out = 1

		// [20,30): avg. occupancy = 3.1, last occupancy = 4

		analyzer.handleEvent(new VehicleLeavesTrafficEvent(30.0, null, id1,
				null, "car", 0.0));
		// time = 30, in = 5, out = 2
		analyzer.handleEvent(new LinkLeaveEvent(30.0, null, id1));
		// time = 30, in = 5, out = 3
		analyzer.handleEvent(new LinkEnterEvent(30.0, null, id1));
		// time = 30, in = 6, out = 3
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.99, null, id1,
				null, "car", 0.0));
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.999, null, id1,
				null, "car", 0.0));
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.9999, null, id1,
				null, "car", 0.0));
		// time = 39, in = 6, out = 6

		// [30,39): avg. occupancy: 0.9*3+0.1*0=2.7, last occupancy = 0

		analyzer.handleEvent(new LinkEnterEvent(40.0, null, id1));
		analyzer.handleEvent(new LinkEnterEvent(40.0, null, id1));
		analyzer.handleEvent(new LinkLeaveEvent(40.0, null, id1));
		// time = 40, in = 8, out = 7

		// [40,49): avg. occupancy: 1.0, last occupancy = 0

		analyzer.handleEvent(new LinkLeaveEvent(100.0, null, id1));
		// time = 100, in = 8, out = 8

		analyzer.advanceToEnd();

		for (int bin = 0; bin < binCnt; bin++) {
			System.out.println("[" + (startTime_s + bin * binSize_s) + ","
					+ (startTime_s + (bin + 1) * binSize_s) + "): "
					+ analyzer.getOccupancy_veh(id1, bin));
		}
		
		System.out.println("DONE");
	}

	private static void largeTest() {

		System.out.println("STARTED LARGE TEST");

		final int startTime_s = 0;
		final int binSize_s = 3600;
		final int binCnt = 24;
		final OccupancyAnalyzer analyzer = new OccupancyAnalyzer(startTime_s,
				binSize_s, binCnt);

		final EventsManager events = EventsUtils
				.createEventsManager(ConfigUtils.createConfig());
		events.addHandler(analyzer);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile("/Nobackup/Profilen/Documents/proposals/2015/IHOP2/showcase/"
				+ "2015-11-23ab_LARGE_RegentMATSim/2015-11-23a_No_Toll_large/summary/"
				+ "iteration-3/it.400/400.events.xml.gz");

		for (int bin = 0; bin < binCnt; bin++) {
			double sum = 0.0;
			for (Id<Link> link : analyzer.linkSet()) {
				sum += analyzer.getOccupancy_veh(link, bin);
			}
			System.out.println(bin + "\t" + sum);
		}

		System.out.println("DONE");

	}

	public static void main(String[] args) {

		// smallTest();
		largeTest();

	}

}
