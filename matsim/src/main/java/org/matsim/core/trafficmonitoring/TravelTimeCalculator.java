/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.core.trafficmonitoring;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculates actual travel times on link from events and optionally also the link-to-link
 * travel times, e.g. if signaled nodes are used and thus turns in different directions
 * at a node may take a different amount of time.
 * <br>
 * Travel times on links are collected and averaged in bins/slots with a specified size
 * (<code>binSize</code>, in seconds, default 900 seconds = 15 minutes).
 *
 * @author dgrether
 * @author mrieser
 */
public final class TravelTimeCalculator implements LinkEnterEventHandler, LinkLeaveEventHandler,
									     VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, VehicleArrivesAtFacilityEventHandler,
									     VehicleAbortsEventHandler {
	private static final Logger log = LogManager.getLogger(TravelTimeCalculator.class);

	private static final String ERROR_STUCK_AND_LINKTOLINK = "Using the stuck feature with turning move travel times is not available. As the next link of a stucked" +
											     "agent is not known the turning move travel time cannot be calculated!";

	private final double timeSlice;
	private final int numSlots;
	TimeSlotComputation aggregator;

	private final Network network;
	private IdMap<Link, TravelTimeDataArray> linkData;

	private Map<Tuple<Id<Link>, Id<Link>>, TravelTimeDataArray> linkToLinkData;

	private final Map<Id<Vehicle>, LinkEnterEvent> linkEnterEvents;

	private final Set<Id<Vehicle>> vehiclesToIgnore;
	private final Set<String> analyzedModes;

	private final boolean filterAnalyzedModes;

	private final boolean calculateLinkTravelTimes;

	private final boolean calculateLinkToLinkTravelTimes;

	@Inject private QSimConfigGroup qsimConfig ;
	TravelTimeGetter travelTimeGetter ;

	@Deprecated // use builder instead.  kai, feb'19
	public static TravelTimeCalculator create(Network network, TravelTimeCalculatorConfigGroup group) {
		TravelTimeCalculator calculator = new TravelTimeCalculator(network, group);
		configure(calculator, group, network);
		return calculator;
	}

	private static TravelTimeCalculator configure(TravelTimeCalculator calculator, TravelTimeCalculatorConfigGroup config, Network network) {
		// This should be replaced by a builder if we need the functionality.  kai/mads, feb'19

		switch( config.getTravelTimeGetterType() ){
			case "average":
				calculator.travelTimeGetter = new AveragingTravelTimeGetter( calculator.aggregator );
				break;
			case "linearinterpolation":
				calculator.travelTimeGetter = new LinearInterpolatingTravelTimeGetter( calculator.numSlots, calculator.timeSlice, calculator.aggregator );
				break;
			default:
				throw new RuntimeException( config.getTravelTimeGetterType() + " is unknown!" );
		}
		return calculator;
	}

	@Deprecated // user builder instead.  kai, feb'19
	@Inject // yyyy why is this needed?  In general, this class is NOT injected, but explicitly constructed in TravelTimeCalculatorModule.  kai, feb'19
	TravelTimeCalculator(TravelTimeCalculatorConfigGroup ttconfigGroup, EventsManager eventsManager, Network network) {
		// this injected constructor is not used when getSeparateModes is true
		this(network, ttconfigGroup.getTraveltimeBinSize(), ttconfigGroup.getMaxTime(), ttconfigGroup.isCalculateLinkTravelTimes(),
			  ttconfigGroup.isCalculateLinkToLinkTravelTimes(), ttconfigGroup.isFilterModes(), CollectionUtils.stringToSet(ttconfigGroup.getAnalyzedModesAsString() ) );
		eventsManager.addHandler(this);
		configure(this, ttconfigGroup, network);
	}

	@Deprecated // user builder instead.  kai, feb'19
	public TravelTimeCalculator( final Network network, TravelTimeCalculatorConfigGroup ttconfigGroup ) {
		// one tests needs this public
		// some tests currently use this. they are also quite happy without an events manager.  kai, feb'19
		this(network, ttconfigGroup.getTraveltimeBinSize(), ttconfigGroup.getMaxTime(), ttconfigGroup);
	}

	@Deprecated // user builder instead.  kai, feb'19
	public TravelTimeCalculator(final Network network, final double timeslice, final int maxTime, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		this(network, timeslice, maxTime, ttconfigGroup.isCalculateLinkTravelTimes(), ttconfigGroup.isCalculateLinkToLinkTravelTimes(), ttconfigGroup.isFilterModes(),
			  CollectionUtils.stringToSet(ttconfigGroup.getAnalyzedModesAsString() ) );
	}

	public final static class Builder {
		// The idea here is that the config group will NOT be passed into this object any more. kai, feb'19

		private final Network network ;
		private double timeslice = 900 ;
		private int maxTime = 36*3600 ; // yy replace by long or double!
		private boolean calculateLinkTravelTimes = true ;
		private boolean calculateLinkToLinkTravelTimes = false ;
		private boolean filterModes = false ;
		private Set<String> analyzedModes = null ;
		private TravelTimeCalculatorConfigGroup ttcConfig;
		private boolean toBeConfigured = false ;

		public Builder( Network network ) {
			this.network = network ;
		}

		public void setTimeslice( double timeslice ){
			this.timeslice = timeslice;
		}

		public void setMaxTime( int maxTime ){
			this.maxTime = maxTime;
		}

		public void setCalculateLinkTravelTimes( boolean calculateLinkTravelTimes ){
			this.calculateLinkTravelTimes = calculateLinkTravelTimes;
		}

		public void setCalculateLinkToLinkTravelTimes( boolean calculateLinkToLinkTravelTimes ){
			this.calculateLinkToLinkTravelTimes = calculateLinkToLinkTravelTimes;
		}

		public void setFilterModes( boolean filterModes ){
			this.filterModes = filterModes;
		}

		public void setAnalyzedModes( Set<String> analyzedModes ){
			this.analyzedModes = analyzedModes;
		}

		public void configure ( TravelTimeCalculatorConfigGroup ttcConfig ) {
			// yyyyyy this is a fix to get the outward API sorted out somewhat better.  kai, feb'19
			// yyyyyy presumably would like to replace this with setters for {@link TravelTimeDataFactory} and {@link TravelTimeGetter}.  But it ain't that easy because
			// they again depend on material that (currently) is only available _after_ construction of {@link TravelTimeCalculator}.  kai, feb'19

			this.ttcConfig = ttcConfig ;
			this.toBeConfigured = true ;
		}

		public TravelTimeCalculator build() {
			TravelTimeCalculator abc = new TravelTimeCalculator( network, timeslice, maxTime, calculateLinkTravelTimes, calculateLinkToLinkTravelTimes, filterModes,
				  analyzedModes );
			if( toBeConfigured ){
				TravelTimeCalculator.configure( abc, this.ttcConfig, this.network );
			}
			return abc ;
		}

	}

	private TravelTimeCalculator(final Network network, final double timeslice, final int maxTime,
								 boolean calculateLinkTravelTimes, boolean calculateLinkToLinkTravelTimes, boolean filterModes, Set<String> analyzedModes) {
		this.calculateLinkTravelTimes = calculateLinkTravelTimes;
		this.calculateLinkToLinkTravelTimes = calculateLinkToLinkTravelTimes;
		this.filterAnalyzedModes = filterModes;
		this.analyzedModes = analyzedModes;
		this.network = network;
		this.timeSlice = timeslice;
		this.numSlots = TimeBinUtils.getTimeBinCount(maxTime, timeslice);
		this.aggregator = new TimeSlotComputation(this.numSlots, this.timeSlice);
		this.travelTimeGetter = new AveragingTravelTimeGetter( this.aggregator ) ;
		if (this.calculateLinkTravelTimes) {
			this.linkData = new IdMap<>(Link.class);
		}
		if (this.calculateLinkToLinkTravelTimes){
			// assume that every link has 2 outgoing links as default
			this.linkToLinkData = new ConcurrentHashMap<>((int) (network.getLinks().size() * 1.4 * 2));
		}
		this.linkEnterEvents = new ConcurrentHashMap<>();

		// if we just look at one mode, we need to ignore all vehicles with a different mode. However, the info re the mode is only in
		// the vehicleEntersTraffic event.  So we need to memorize the ignored vehicles from there ...
		this.vehiclesToIgnore = new HashSet<>();

		this.reset(0);
	}

	@Override
	public void handleEvent(final LinkEnterEvent e) {
		/* if only some modes are analyzed, we check whether the vehicles
		 * performs a trip with one of those modes. if not, we skip the event. */
		if (filterAnalyzedModes && vehiclesToIgnore.contains(e.getVehicleId())) return;

		LinkEnterEvent oldEvent = this.linkEnterEvents.put(e.getVehicleId(), e);
		if ((oldEvent != null) && this.calculateLinkToLinkTravelTimes) {
			Tuple<Id<Link>, Id<Link>> fromToLink = new Tuple<>(oldEvent.getLinkId(), e.getLinkId());
			TravelTimeData data = getLinkToLinkTravelTimeData(fromToLink );
			double enterTime = oldEvent.getTime();

			final int timeSlot = this.aggregator.getTimeSlotIndex(enterTime );
			data.addTravelTime(timeSlot, e.getTime() - enterTime );
			data.setNeedsConsolidation( true );
		}
	}

	@Override
	public void handleEvent(final LinkLeaveEvent e) {
		if (this.calculateLinkTravelTimes) {
			LinkEnterEvent oldEvent = this.linkEnterEvents.get(e.getVehicleId());
			if (oldEvent != null) {
				TravelTimeData data = this.getTravelTimeData(e.getLinkId(), true);
				double enterTime = oldEvent.getTime();

				final int timeSlot = this.aggregator.getTimeSlotIndex(enterTime );
				data.addTravelTime(timeSlot, e.getTime() - enterTime );
				data.setNeedsConsolidation( true );
			}
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		/* if filtering transport modes is enabled and the vehicles
		 * starts a leg on a non analyzed transport mode, add the vehicle
		 * to the filtered vehicles set. */
		if (filterAnalyzedModes && !analyzedModes.contains(event.getNetworkMode())) {
			this.vehiclesToIgnore.add(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		/* remove EnterEvents from list when a vehicle arrives.
		 * otherwise, the activity duration would be counted as travel time, when the
		 * vehicle departs again and leaves the link! */
		this.linkEnterEvents.remove(event.getVehicleId());

		// try to remove vehicles from set with filtered vehicles
		if (filterAnalyzedModes) this.vehiclesToIgnore.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		/* remove EnterEvents from list when a bus stops on a link.
		 * otherwise, the stop time would be counted as travel time, when the
		 * bus departs again and leaves the link! */
		this.linkEnterEvents.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		LinkEnterEvent e = this.linkEnterEvents.remove(event.getVehicleId());
		if (e != null) {
			TravelTimeData data = this.getTravelTimeData(e.getLinkId(), true);
			data.setNeedsConsolidation( true );

			//			this.aggregator.addStuckEventTravelTime(data, e.getTime(), event.getTime());
			// this functionality is no longer there.

			if (this.calculateLinkToLinkTravelTimes
					&& event.getTime() < qsimConfig.getEndTime().seconds()
				// (we think that this only makes problems when the abort is not just because of mobsim end time. kai & theresa, jan'17)
			){
				log.error(ERROR_STUCK_AND_LINKTOLINK);
				throw new IllegalStateException(ERROR_STUCK_AND_LINKTOLINK);
			}
		}

		// try to remove vehicle from set with filtered vehicles
		if (filterAnalyzedModes) this.vehiclesToIgnore.remove(event.getVehicleId());
	}

	private TravelTimeDataArray getTravelTimeData(final Id<Link> linkId, final boolean createIfMissing) {
		TravelTimeDataArray data = this.linkData.get(linkId);
		if ((null == data) && createIfMissing) {
			data = this.createTravelTimeData(linkId);
			this.linkData.put(linkId, data);
		}
		return data;
	}

	private TravelTimeDataArray getLinkToLinkTravelTimeData( Tuple<Id<Link>, Id<Link>> fromLinkToLink ) {
		TravelTimeDataArray data = this.linkToLinkData.get(fromLinkToLink);
		if ( null == data ) {
			data = this.createTravelTimeData(fromLinkToLink.getFirst()) ;
			this.linkToLinkData.put(fromLinkToLink, data);
		}
		return data;
	}

	private TravelTimeDataArray createTravelTimeData(Id<Link> linkId) {
		return new TravelTimeDataArray(this.network.getLinks().get(linkId), this.numSlots);
	}

	private double getLinkTravelTime(final Id<Link> linkId, final double time) {
		if (this.calculateLinkTravelTimes) {

			TravelTimeData data = this.getTravelTimeData(linkId, true);
			if ( data.isNeedingConsolidation() ) {
				consolidateData(data);
			}
			return this.travelTimeGetter.getTravelTime( data, time );
		}
		throw new IllegalStateException("No link travel time is available " +
								    "if calculation is switched off by config option!");
	}

	private double getLinkToLinkTravelTime(final Id<Link> fromLinkId, final Id<Link> toLinkId, double time) {
		if (!this.calculateLinkToLinkTravelTimes) {
			throw new IllegalStateException("No link to link travel time is available " +
									    "if calculation is switched off by config option!");
		}
		TravelTimeData data = this.getLinkToLinkTravelTimeData(new Tuple<>(fromLinkId, toLinkId) );
		if ( data.isNeedingConsolidation() ) {
			consolidateData(data);
		}
		return this.travelTimeGetter.getTravelTime( data, time );
	}

	@Override
	public void reset(int iteration) {
		if (this.calculateLinkTravelTimes) {
			for (TravelTimeData data : this.linkData.values()){
				data.resetTravelTimes();
				data.setNeedsConsolidation( false );
			}
		}
		if (this.calculateLinkToLinkTravelTimes){
			for (TravelTimeData data : this.linkToLinkData.values()){
				data.resetTravelTimes();
				data.setNeedsConsolidation( false );
			}
		}
		this.linkEnterEvents.clear();
		this.vehiclesToIgnore.clear();
	}

	/**
	 * Makes sure that the travel times "make sense".
	 * <p></p>
	 * Imagine short bin sizes (e.g. 5min), small links (e.g. 300 veh/hour)
	 * and small sample sizes (e.g. 2%). This would mean that effectively
	 * in the simulation only 6 vehicles can pass the link in one hour,
	 * one every 10min. So, the travel time in one time slot could be
	 * >= 10min if two cars enter the link at the same time. If no car
	 * enters in the next time bin, the travel time in that time bin should
	 * still be >=5 minutes (10min - binSize), and not freespeedTraveltime,
	 * because actually every car entering the link in this bin will be behind
	 * the car entered before, which still needs >=5min until it can leave.
	 * <p></p>
	 * This method ensures that the travel time in a time bin
	 * cannot be smaller than the travel time in the bin before minus the
	 * bin size.
	 *
	 */
	private void consolidateData(final TravelTimeData data) {
		synchronized(data) {
			if ( data.isNeedingConsolidation() ) {

				// initialize prevTravelTime with ttime from time bin 0 and time 0.  (The interface comment already states that
				// having both as argument does not make sense.)
				double prevTravelTime = data.getTravelTime(0, 0.0 );
				// changed (1, 0.0) to (0, 0.0) since Michal has convinced me (by a test) that using "1" is wrong
				// because you get the wrong result for time slot number 1.  This change does not affect the existing
				// unit tests.  kai, oct'11

				// go from time slot 1 forward in time:
				for (int i = 1; i < this.numSlots; i++) {

					// once more the getter is weird since it needs both the time slot and the time:
					double travelTime = data.getTravelTime(i, i * this.timeSlice );

					// if the travel time in the previous time slice was X, then now it is X-S, where S is the time slice:
					double minTravelTime = prevTravelTime - this.timeSlice;

					// if the travel time that has been measured so far is less than that minimum travel time, then do something:
					if (travelTime < minTravelTime) {
						// (set the travel time to the smallest possible travel time that makes sense according to the argument above)
						travelTime = minTravelTime;
						data.setTravelTime(i, travelTime);
					}
					prevTravelTime = travelTime;
				}
				data.setNeedsConsolidation( false );
			}
		}
	}

	private static int cnt = 0 ;

	public TravelTime getLinkTravelTimes() {
		return new TravelTime() {

			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				// right now, the link speed limit comes from the travel time calculator, and this here just overrides it.  One might consider doing all of this here;
				// possibly would make the code easier to read.  kai/mads, feb'19

				double linkTtimeFromVehicle = 0. ;
				if ( vehicle!=null ){
					final VehicleType vehicleType = vehicle.getType();
					if ( vehicleType==null ){
						if( cnt < 1 ){
							cnt++;
							log.warn( "encountered vehicle where vehicle.getType() returns null.  That should be repaired (whereever it comes from)." );
							log.warn( Gbl.ONLYONCE );
						}
					} else{
						linkTtimeFromVehicle = link.getLength() / vehicleType.getMaximumVelocity();
					}
				}
				double linkTTimeFromObservation = TravelTimeCalculator.this.getLinkTravelTime(link.getId(), time);
				return Math.max( linkTtimeFromVehicle, linkTTimeFromObservation) ;
				// yyyyyy should this not be min?  kai/janek, may'19
				// No, it is correct. It is preventing the router to route with an empirical speed from
				// the previous iteration that exceeds the maximum vehicle speed.
				// Thus, the lowest speed (highest travel time) of the two should be used.    Mads, Nov'19
			}

		};

	}

	public LinkToLinkTravelTime getLinkToLinkTravelTimes() {
		return new LinkToLinkTravelTime() {

			@Override
			public double getLinkToLinkTravelTime(Link fromLink, Link toLink, double time, Person person, Vehicle vehicle) {
				double linkTtimeFromVehicle = 0. ;
				if ( vehicle!=null ){
					final VehicleType vehicleType = vehicle.getType();
					if ( vehicleType==null ){
						if( cnt < 1 ){
							cnt++;
							log.warn( "encountered vehicle where vehicle.getType() returns null.  That should be repaired (whereever it comes from)." );
							log.warn( Gbl.ONLYONCE );
						}
					} else{
						linkTtimeFromVehicle = fromLink.getLength() / vehicleType.getMaximumVelocity();
					}
				}
				double linkTTimeFromObservation = TravelTimeCalculator.this.getLinkToLinkTravelTime(fromLink.getId(), toLink.getId(), time);

				return Math.max(linkTTimeFromObservation, linkTtimeFromVehicle);
			}
		};
	}

}
