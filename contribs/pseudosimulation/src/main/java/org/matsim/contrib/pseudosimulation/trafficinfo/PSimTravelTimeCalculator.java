/**
 * 
 */
package org.matsim.contrib.pseudosimulation.trafficinfo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.pseudosimulation.RunPSim;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

/**
 * @author fouriep
 * 
 */
public class PSimTravelTimeCalculator extends TravelTimeCalculator {
	private final RunPSim.MobSimSwitcher switcher;

	public PSimTravelTimeCalculator(Network network,
			TravelTimeCalculatorConfigGroup ttconfigGroup, int numHrs, RunPSim.MobSimSwitcher switcher) {
		super(network, ttconfigGroup.getTraveltimeBinSize(), numHrs,
				ttconfigGroup);
		this.switcher = switcher;
	}

	@Override
	public void reset(int iteration) {
		if (switcher == null || switcher.isQSimIteration()) {
			Logger.getLogger(this.getClass()).error(
					"Calling reset on traveltimecalc");
			super.reset(iteration);
		}else{
			Logger.getLogger(this.getClass()).error(
					"Not resetting travel times as this is a PSim iteration");
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent e) {
		if (switcher.isQSimIteration())
			super.handleEvent(e);
	}

	@Override
	public void handleEvent(LinkLeaveEvent e) {
		if (switcher.isQSimIteration())
			super.handleEvent(e);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (switcher.isQSimIteration())
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (switcher.isQSimIteration())
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if (switcher.isQSimIteration())
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		if (switcher.isQSimIteration())
			super.handleEvent(event);
	}

}
