/**
 *
 */
package org.matsim.contrib.pseudosimulation.trafficinfo;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

/**
 * @author fouriep
 */
public class PSimTravelTimeCalculator implements Provider<TravelTime>, LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
									   VehicleArrivesAtFacilityEventHandler, VehicleAbortsEventHandler{
	// I have replaced inheritance by delegation to make TravelTimeCalculator final, but I can't say if that preserves functionality.  Hopefully, it is covered by a regression
	// test.  kai, feb'19

	private final MobSimSwitcher switcher;
	private final TravelTimeCalculator travelTimeCalculator;

	@Inject
	PSimTravelTimeCalculator(TravelTimeCalculatorConfigGroup ttconfigGroup, EventsManager eventsManager, Network network, MobSimSwitcher switcher) {
		travelTimeCalculator = new TravelTimeCalculator(network, ttconfigGroup);
		this.switcher = switcher;
		eventsManager.addHandler( travelTimeCalculator );
	}

	@Override
	public TravelTime get() {
		return travelTimeCalculator.getLinkTravelTimes();
	}

	public void handleEvent( final LinkEnterEvent e ){
		travelTimeCalculator.handleEvent( e );
	}

	public void handleEvent( final LinkLeaveEvent e ){
		travelTimeCalculator.handleEvent( e );
	}

	public void handleEvent( VehicleEntersTrafficEvent event ){
		travelTimeCalculator.handleEvent( event );
	}

	public void handleEvent( final VehicleLeavesTrafficEvent event ){
		travelTimeCalculator.handleEvent( event );
	}

	public void handleEvent( VehicleArrivesAtFacilityEvent event ){
		travelTimeCalculator.handleEvent( event );
	}

	public void handleEvent( VehicleAbortsEvent event ){
		travelTimeCalculator.handleEvent( event );
	}

	public void reset( int iteration ){
		if (switcher == null || switcher.isQSimIteration()) {
			Logger.getLogger( travelTimeCalculator.getClass() ).error(
					"Calling reset on traveltimecalc" );
			travelTimeCalculator.reset( iteration );
		} else {
			Logger.getLogger( travelTimeCalculator.getClass() ).error(
					"Not resetting travel times as this is a PSim iteration" );
		}
	}

}
