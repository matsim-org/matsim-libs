/**
 * 
 */
package playground.pieter.pseudosimulation.trafficinfo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

import playground.pieter.pseudosimulation.controler.listeners.MobSimSwitcher;

/**
 * @author fouriep
 * 
 */
public class PSimTravelTimeCalculator extends TravelTimeCalculator {
	public PSimTravelTimeCalculator(Network network,
			TravelTimeCalculatorConfigGroup ttconfigGroup) {
		super(network, ttconfigGroup.getTraveltimeBinSize(), 50 * 3600,
				ttconfigGroup);
	}

	@Override
	public void reset(int iteration) {
		if (MobSimSwitcher.isQSimIteration) {
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
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(e);
	}

	@Override
	public void handleEvent(LinkLeaveEvent e) {
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(e);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(event);
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		if (MobSimSwitcher.isQSimIteration)
			super.handleEvent(event);
	}

}
