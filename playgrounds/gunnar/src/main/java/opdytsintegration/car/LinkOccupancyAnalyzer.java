package opdytsintegration.car;

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

import opdytsintegration.MATSimCountingStateAnalyzer;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkOccupancyAnalyzer extends MATSimCountingStateAnalyzer<Link>
		implements LinkLeaveEventHandler, LinkEnterEventHandler, VehicleEntersTrafficEventHandler,
		VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler {

	// -------------------- MEMBERS --------------------

	private final Set<Id<Link>> relevantLinks;

	// -------------------- CONSTRUCTION --------------------

	public LinkOccupancyAnalyzer(final TimeDiscretization timeDiscretization, final Set<Id<Link>> relevantLinks) {
		this(timeDiscretization.getStartTime_s(), timeDiscretization.getBinSize_s(), timeDiscretization.getBinCnt(),
				relevantLinks);
	}

	public LinkOccupancyAnalyzer(final int startTime_s, final int binSize_s, final int binCnt,
			final Set<Id<Link>> relevantLinks) {
		super(startTime_s, binSize_s, binCnt);
		this.relevantLinks = relevantLinks;
	}

	// -------------------- INTERNALS --------------------

	private boolean relevant(Id<Link> link) {
		return ((this.relevantLinks == null) || this.relevantLinks.contains(link));
	}

	// ---------- IMPLEMENTATION OF *EventHandler INTERFACES ----------

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		if (this.relevant(event.getLinkId())) {
			this.registerDecrease(event.getLinkId(), (int) event.getTime());
		}
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		if (this.relevant(event.getLinkId())) {
			this.registerIncrease(event.getLinkId(), (int) event.getTime());
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		if (this.relevant(event.getLinkId())) {
			this.registerIncrease(event.getLinkId(), (int) event.getTime());
		}
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		if (this.relevant(event.getLinkId())) {
			this.registerDecrease(event.getLinkId(), (int) event.getTime());
		}
	}

	@Override
	public void handleEvent(final VehicleAbortsEvent event) {
		if (this.relevant(event.getLinkId())) {
			this.registerDecrease(event.getLinkId(), (int) event.getTime());
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	// TODO make this a unit test
	public static void main(String[] args) {

		final int startTime_s = 0;
		final int binSize_s = 10;
		final int binCnt = 5;
		final LinkOccupancyAnalyzer analyzer = new LinkOccupancyAnalyzer(startTime_s, binSize_s, binCnt, null);

		final Id<Link> id1 = Id.createLinkId("1");
		final Id<Vehicle> veh1 = Id.createVehicleId("1");

		// [0,10): avg. occupancy = 0, last occupancy = 0

		analyzer.handleEvent(new LinkEnterEvent(10.0, veh1, id1));
		// time = 10, in = 1, out = 0
		analyzer.handleEvent(new VehicleEntersTrafficEvent(10.0, null, id1, veh1, null, 0.0));
		// time = 10, in = 2, out = 0
		analyzer.handleEvent(new LinkEnterEvent(14.0, veh1, id1));
		// time = 14, in = 3, out = 0
		analyzer.handleEvent(new LinkLeaveEvent(19.0, veh1, id1));
		// time = 19, in = 3, out = 1

		// [10,20): avg. occupancy = 2.5, last occupancy = 2

		analyzer.handleEvent(new VehicleEntersTrafficEvent(20.0, null, id1, veh1, "car", 0.0));
		// time = 20, in = 4, out = 1
		analyzer.handleEvent(new LinkEnterEvent(29.99, veh1, id1));
		// time = 29, in = 5, out = 1

		// [20,30): avg. occupancy = 3.1, last occupancy = 4

		analyzer.handleEvent(new VehicleLeavesTrafficEvent(30.0, null, id1, veh1, "car", 0.0));
		// time = 30, in = 5, out = 2
		analyzer.handleEvent(new LinkLeaveEvent(30.0, veh1, id1));
		// time = 30, in = 5, out = 3
		analyzer.handleEvent(new LinkEnterEvent(30.0, veh1, id1));
		// time = 30, in = 6, out = 3
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.99, null, id1, veh1, "car", 0.0));
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.999, null, id1, veh1, "car", 0.0));
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39.9999, null, id1, veh1, "car", 0.0));
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
			System.out.println("[" + (startTime_s + bin * binSize_s) + "," + (startTime_s + (bin + 1) * binSize_s)
					+ "): " + analyzer.getCount(id1, bin));
		}
	}
}
