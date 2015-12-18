package opdytsintegration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;

import floetteroed.utilities.DynamicData;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class OccupancyAnalyzer implements LinkLeaveEventHandler,
		LinkEnterEventHandler, Wait2LinkEventHandler,
		VehicleLeavesTrafficEventHandler {

	// -------------------- MEMBERS --------------------

	private final DynamicData<Id<Link>> occupancies_veh;

	private final Map<Id<Link>, RecursiveCountAverage> link2avg = new LinkedHashMap<>();

	private int lastCompletedBin = -1;

	// -------------------- CONSTRUCTION --------------------

	public OccupancyAnalyzer(final int startTime_s, final int binSize_s,
			final int binCnt) {
		this.occupancies_veh = new DynamicData<>(startTime_s, binSize_s, binCnt);
	}

	// -------------------- INTERNALS --------------------

	private void completeBins(final int lastBinToComplete) {
		while (this.lastCompletedBin < lastBinToComplete) {
			this.lastCompletedBin++; // is now zero or larger
			final int lastCompletedBinEndTime_s = this.occupancies_veh
					.getStartTime_s()
					+ (this.lastCompletedBin + 1)
					* this.occupancies_veh.getBinSize_s();
			for (Map.Entry<Id<Link>, RecursiveCountAverage> link2avgEntry : this.link2avg
					.entrySet()) {
				link2avgEntry.getValue().advanceTo(lastCompletedBinEndTime_s);
				this.occupancies_veh.put(link2avgEntry.getKey(),
						this.lastCompletedBin, link2avgEntry.getValue()
								.getAverage());
				link2avgEntry.getValue().reset(lastCompletedBinEndTime_s);
			}
		}
	}

	private void advanceToTime(final int time_s) {
		if (time_s >= this.occupancies_veh.getStartTime_s()) {
			final int currentBin = this.occupancies_veh.bin(time_s);
			if (this.lastCompletedBin < currentBin - 1) {
				this.completeBins(Math.min(currentBin - 1,
						this.occupancies_veh.getBinCnt() - 1));
			}
		}
	}

	private RecursiveCountAverage getRecursiveAverage(final Id<Link> link) {
		RecursiveCountAverage result = this.link2avg.get(link);
		if (result == null) {
			result = new RecursiveCountAverage(
					this.occupancies_veh.getStartTime_s()
							+ this.lastCompletedBin
							* this.occupancies_veh.getBinSize_s());
		}
		return result;
	}

	private void registerEntry(final Id<Link> link, final int time_s) {
		this.advanceToTime(time_s);
		this.getRecursiveAverage(link).inc(time_s);
	}

	private void registerExit(final Id<Link> link, final int time_s) {
		this.advanceToTime(time_s);
		this.getRecursiveAverage(link).dec(time_s);
	}

	private void advanceToEnd() {
		this.completeBins(this.occupancies_veh.getBinCnt() - 1);
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
	public void handleEvent(final Wait2LinkEvent event) {
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

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	// TODO make this a unit test
	public static void main(String[] args) {

		final int startTime_s = 0;
		final int binSize_s = 10;
		final int binCnt = 4;
		final OccupancyAnalyzer analyzer = new OccupancyAnalyzer(startTime_s,
				binSize_s, binCnt);

		final Id<Link> id1 = Id.createLinkId("1");

		// before time = 10: occupancy = 0

		analyzer.handleEvent(new LinkEnterEvent(10.0, null, id1, null));
		// time = 10, in = 1, out = 0
		analyzer.handleEvent(new Wait2LinkEvent(10.0, null, id1, null, null,
				0.0));
		// time = 10, in = 2, out = 0
		analyzer.handleEvent(new LinkEnterEvent(14.0, null, id1, null));
		// time = 14, in = 3, out = 0
		analyzer.handleEvent(new LinkLeaveEvent(19.0, null, id1, null));
		// time = 19, in = 3, out = 1

		// before time = 20: occupancy = 2

		analyzer.handleEvent(new Wait2LinkEvent(20.0, null, id1, null, "car",
				0.0));
		// time = 20, in = 4, out = 1
		analyzer.handleEvent(new LinkEnterEvent(29.99, null, id1, null));
		// time = 29, in = 5, out = 1

		// before time = 30: occupancy = 4

		analyzer.handleEvent(new VehicleLeavesTrafficEvent(30.0, null, id1,
				null, "car", 0.0));
		// time = 30, in = 5, out = 2
		analyzer.handleEvent(new LinkLeaveEvent(30.0, null, id1, null));
		// time = 30, in = 5, out = 3
		analyzer.handleEvent(new LinkEnterEvent(30.0, null, id1, null));
		// time = 30, in = 6, out = 3
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.99, null, id1,
				null, "car", 0.0));
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.999, null, id1,
				null, "car", 0.0));
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.9999, null, id1,
				null, "car", 0.0));
		// time = 39, in = 6, out = 6

		// before time = 40: occupancy = 4

		analyzer.handleEvent(new LinkEnterEvent(40.0, null, id1, null));
		analyzer.handleEvent(new LinkEnterEvent(40.0, null, id1, null));
		analyzer.handleEvent(new LinkLeaveEvent(40.0, null, id1, null));
		analyzer.handleEvent(new LinkLeaveEvent(100.0, null, id1, null));

		analyzer.advanceToEnd();

		for (int bin = 0; bin < binCnt; bin++) {
			System.out.println(analyzer.getOccupancy_veh(id1, bin));
		}
	}
}
