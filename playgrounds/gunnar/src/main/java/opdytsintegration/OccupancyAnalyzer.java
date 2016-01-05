package opdytsintegration;

import static java.lang.Math.min;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.DynamicData;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class OccupancyAnalyzer implements LinkLeaveEventHandler,
		LinkEnterEventHandler, VehicleEntersTrafficEventHandler,
		VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler {

	// -------------------- MEMBERS --------------------

	private final DynamicData<Id<Link>> occupancies_veh;

	private final Map<Id<Link>, RecursiveCountAverage> link2avg = new LinkedHashMap<>();

	private int lastCompletedBin = -1;

	// -------------------- CONSTRUCTION --------------------

	public OccupancyAnalyzer(final TimeDiscretization timeDiscretization) {
		this(timeDiscretization.getStartTime_s(), timeDiscretization
				.getBinSize_s(), timeDiscretization.getBinCnt());
	}

	public OccupancyAnalyzer(final int startTime_s, final int binSize_s,
			final int binCnt) {
		this.occupancies_veh = new DynamicData<>(startTime_s, binSize_s, binCnt);
		this.reset(-1);
	}

	// -------------------- INTERNALS --------------------

	private int lastCompletedBinEndTime() {
		return this.occupancies_veh.getStartTime_s()
				+ (this.lastCompletedBin + 1)
				* this.occupancies_veh.getBinSize_s();
	}

	private void completeBins(final int lastBinToComplete) {
		while (this.lastCompletedBin < lastBinToComplete) {
			this.lastCompletedBin++; // is now zero or larger
			final int lastCompletedBinEndTime = this.lastCompletedBinEndTime();
			for (Map.Entry<Id<Link>, RecursiveCountAverage> link2avgEntry : this.link2avg
					.entrySet()) {
				link2avgEntry.getValue().advanceTo(lastCompletedBinEndTime);
				this.occupancies_veh.put(link2avgEntry.getKey(),
						this.lastCompletedBin, link2avgEntry.getValue()
								.getAverage());
				link2avgEntry.getValue().resetTime(lastCompletedBinEndTime);
			}
		}
	}

	private void advanceToTime(final int time_s) {
		final int lastBinToComplete = this.occupancies_veh.bin(time_s) - 1;
		this.completeBins(min(lastBinToComplete,
				this.occupancies_veh.getBinCnt() - 1));
	}

	private RecursiveCountAverage avg(final Id<Link> link) {
		RecursiveCountAverage avg = this.link2avg.get(link);
		if (avg == null) {
			avg = new RecursiveCountAverage(this.lastCompletedBinEndTime());
			this.link2avg.put(link, avg);
		}
		return avg;
	}

	private void registerEntry(final Id<Link> link, final int time_s) {
		this.advanceToTime(time_s);
		this.avg(link).inc(time_s);
	}

	private void registerExit(final Id<Link> link, final int time_s) {
		this.advanceToTime(time_s);
		this.avg(link).dec(time_s);
	}

	public void advanceToEnd() {
		this.completeBins(this.occupancies_veh.getBinCnt() - 1);
	}

	public Set<Id<Link>> linkSet() {
		return this.occupancies_veh.keySet();
	}

	// -------------------- CONTENT ACCESS --------------------

	public double getOccupancy_veh(final Id<Link> link, final int bin) {
		return this.occupancies_veh.getBinValue(link, bin);
	}

	// ---------- IMPLEMENTATION OF *EventHandler INTERFACES ----------

	@Override
	public void reset(final int iteration) {
		this.occupancies_veh.clear();
		this.link2avg.clear();
		this.lastCompletedBin = -1;
	}

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		this.registerExit(event.getLinkId(), (int) event.getTime());
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		this.registerEntry(event.getLinkId(), (int) event.getTime());
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		this.registerEntry(event.getLinkId(), (int) event.getTime());
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		this.registerExit(event.getLinkId(), (int) event.getTime());
	}

	@Override
	public void handleEvent(final VehicleAbortsEvent event) {
		this.registerExit(event.getLinkId(), (int) event.getTime());
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	// TODO make this a unit test
	public static void main(String[] args) {

		final int startTime_s = 0;
		final int binSize_s = 10;
		final int binCnt = 5;
		final OccupancyAnalyzer analyzer = new OccupancyAnalyzer(startTime_s,
				binSize_s, binCnt);

		final Id<Link> id1 = Id.createLinkId("1");
		final Id<Vehicle> veh1 = Id.createVehicleId("1");

		// [0,10): avg. occupancy = 0, last occupancy = 0

		analyzer.handleEvent(new LinkEnterEvent(10.0, veh1, id1));
		// time = 10, in = 1, out = 0
		analyzer.handleEvent(new VehicleEntersTrafficEvent(10.0, null, id1,
				veh1, null, 0.0));
		// time = 10, in = 2, out = 0
		analyzer.handleEvent(new LinkEnterEvent(14.0, veh1, id1));
		// time = 14, in = 3, out = 0
		analyzer.handleEvent(new LinkLeaveEvent(19.0, veh1, id1));
		// time = 19, in = 3, out = 1

		// [10,20): avg. occupancy = 2.5, last occupancy = 2

		analyzer.handleEvent(new VehicleEntersTrafficEvent(20.0, null, id1,
				veh1, "car", 0.0));
		// time = 20, in = 4, out = 1
		analyzer.handleEvent(new LinkEnterEvent(29.99, veh1, id1));
		// time = 29, in = 5, out = 1

		// [20,30): avg. occupancy = 3.1, last occupancy = 4

		analyzer.handleEvent(new VehicleLeavesTrafficEvent(30.0, null, id1,
				veh1, "car", 0.0));
		// time = 30, in = 5, out = 2
		analyzer.handleEvent(new LinkLeaveEvent(30.0, veh1, id1));
		// time = 30, in = 5, out = 3
		analyzer.handleEvent(new LinkEnterEvent(30.0, veh1, id1));
		// time = 30, in = 6, out = 3
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.99, null, id1,
				veh1, "car", 0.0));
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.999, null, id1,
				veh1, "car", 0.0));
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.9999, null, id1,
				veh1, "car", 0.0));
		// time = 39, in = 6, out = 6

		// [30,39): avg. occupancy: 0.9*3+0.1*0=2.7, last occupancy = 0

		analyzer.handleEvent(new LinkEnterEvent(40.0, veh1, id1));
		analyzer.handleEvent(new LinkEnterEvent(40.0, veh1, id1));
		analyzer.handleEvent(new LinkLeaveEvent(40.0, veh1, id1));

		// time = 40, in = 8, out = 7

		// [40,49): avg. occupancy: 1.0, last occupancy = 0

		analyzer.handleEvent(new LinkLeaveEvent(100.0, veh1, id1));

		// time = 100, in = 8, out = 8

		analyzer.advanceToEnd();

		for (int bin = 0; bin < binCnt; bin++) {
			System.out.println("[" + (startTime_s + bin * binSize_s) + ","
					+ (startTime_s + (bin + 1) * binSize_s) + "): "
					+ analyzer.getOccupancy_veh(id1, bin));
		}
	}
}
