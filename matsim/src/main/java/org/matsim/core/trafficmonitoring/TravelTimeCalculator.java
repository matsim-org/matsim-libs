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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates actual travel times on link from events and optionally also the link-to-link 
 * travel times, e.g. if signaled nodes are used and thus turns in different directions
 * at a node may take a different amount of time.
 * <br>
 * Travel times on links are collected and averaged in bins/slots with a specified size
 * (<code>binSize</code>, in seconds, default 900 seconds = 15 minutes). The data for the travel times per link
 * is stored in {@link TravelTimeData}-objects. If a short binSize is used, it is useful to
 * use {@link TravelTimeDataHashMap} (see {@link #setTravelTimeDataFactory(TravelTimeDataFactory)}
 * as that one does not use any memory to time bins where no traffic occurred. By default,
 * {@link TravelTimeDataArray} is used.
 * 
 * @author dgrether
 * @author mrieser
 */
public class TravelTimeCalculator implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, VehicleArrivesAtFacilityEventHandler, 
	VehicleAbortsEventHandler {

	private static final String ERROR_STUCK_AND_LINKTOLINK = "Using the stuck feature with turning move travel times is not available. As the next link of a stucked" +
			"agent is not known the turning move travel time cannot be calculated!";

	/*package*/ final int timeSlice;
	/*package*/ final int numSlots;
	private AbstractTravelTimeAggregator aggregator;

	private static final Logger log = Logger.getLogger(TravelTimeCalculator.class);

	private Map<Id<Link>, DataContainer> linkData;

	private Map<Tuple<Id<Link>, Id<Link>>, DataContainer> linkToLinkData;

	private final DataContainerProvider dataContainerProvider;
	
	private final Map<Id<Vehicle>, LinkEnterEvent> linkEnterEvents;

	private final Set<Id<Vehicle>> vehiclesToIgnore;
	private final Set<String> analyzedModes;

	private final boolean filterAnalyzedModes;

	private final boolean calculateLinkTravelTimes;

	private final boolean calculateLinkToLinkTravelTimes;

	private TravelTimeDataFactory ttDataFactory = null;

	public static TravelTimeCalculator create(Network network, TravelTimeCalculatorConfigGroup group) {
		TravelTimeCalculator calculator = new TravelTimeCalculator(network, group);
		configure(calculator, group, network);
		return calculator;
	}

	static TravelTimeCalculator configure(TravelTimeCalculator calculator, TravelTimeCalculatorConfigGroup config, Network network) {
		// Customize micro-behavior of the TravelTimeCalculator based on config. Should not be necessary for most use cases.
		switch ( config.getTravelTimeCalculatorType() ) {
			case TravelTimeCalculatorArray:
				calculator.setTravelTimeDataFactory(new TravelTimeDataArrayFactory(network, calculator.numSlots));
				break;
			case TravelTimeCalculatorHashMap:
				calculator.setTravelTimeDataFactory(new TravelTimeDataHashMapFactory(network));
				break;
			default:
				throw new RuntimeException(config.getTravelTimeCalculatorType() + " is unknown!");
		}

		AbstractTravelTimeAggregator travelTimeAggregator;
		switch( config.getTravelTimeAggregatorType() ) {
			case "optimistic":
				travelTimeAggregator = new OptimisticTravelTimeAggregator(calculator.numSlots, calculator.timeSlice);
				break ;
			case "experimental_LastMile":
				travelTimeAggregator = new PessimisticTravelTimeAggregator(calculator.numSlots, calculator.timeSlice);
				break ;
			default:
				throw new RuntimeException(config.getTravelTimeAggregatorType() + " is unknown!");
		}
		calculator.setTravelTimeAggregator(travelTimeAggregator);

		TravelTimeGetter travelTimeGetter;
		switch( config.getTravelTimeGetterType() ) {
			case "average":
				travelTimeGetter = new AveragingTravelTimeGetter();
				break ;
			case "linearinterpolation":
				travelTimeGetter = new LinearInterpolatingTravelTimeGetter(calculator.numSlots, calculator.timeSlice);
				break ;
			default:
				throw new RuntimeException(config.getTravelTimeGetterType() + " is unknown!");
		}
		travelTimeAggregator.connectTravelTimeGetter(travelTimeGetter);
		return calculator;
	}

	public TravelTimeCalculator(final Network network, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		this(network, ttconfigGroup.getTraveltimeBinSize(), 30*3600, ttconfigGroup); // default: 30 hours at most
		// yyyy the hard-coded 30 hours seems a bit dangerous ... assume someone wants to run matsim for a week?? kai, jan'16
	}

	public TravelTimeCalculator(final Network network, final int timeslice, final int maxTime, TravelTimeCalculatorConfigGroup ttconfigGroup) {
		this(network, timeslice, maxTime, ttconfigGroup.isCalculateLinkTravelTimes(), ttconfigGroup.isCalculateLinkToLinkTravelTimes(), ttconfigGroup.isFilterModes(), CollectionUtils.stringToSet(ttconfigGroup.getAnalyzedModes()));
	}

	TravelTimeCalculator(final Network network, final int timeslice, final int maxTime,
								boolean calculateLinkTravelTimes, boolean calculateLinkToLinkTravelTimes, boolean filterModes, Set<String> strings) {
		this.calculateLinkTravelTimes = calculateLinkTravelTimes;
		this.calculateLinkToLinkTravelTimes = calculateLinkToLinkTravelTimes;
		this.filterAnalyzedModes = filterModes;
		this.analyzedModes = strings;
		this.timeSlice = timeslice;
		this.numSlots = (maxTime / this.timeSlice) + 1;
		this.aggregator = new OptimisticTravelTimeAggregator(this.numSlots, this.timeSlice);
		this.ttDataFactory = new TravelTimeDataArrayFactory(network, this.numSlots);
		if (this.calculateLinkTravelTimes){
			this.linkData = new ConcurrentHashMap<>((int) (network.getLinks().size() * 1.4));

			/*
			 * So far, link data objects were stored in a HashMap. This lookup strategy is used
			 * by a MapBasedDataContainerProvider.
			 * When ArrayRoutingNetworks are used (as the FastRouter implementations do), the
			 * getArrayIndex() methods from the RoutingLinks can be used to lookup the link
			 * data objects in an array. This approach is implemented by the ArrayBasedDataContainerProvider.
			 * Using a ArrayBasedDataContainerProvider instead of a MapBasedDataContainerProvider
			 * increases the routing performance by 20-30%.
			 * cdobler, oct'13
			 */
			//		this.dataContainerProvider = new MapBasedDataContainerProvider(linkData, ttDataFactory);
			this.dataContainerProvider = new ArrayBasedDataContainerProvider(linkData, ttDataFactory, network);
		} else this.dataContainerProvider = null;
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

		LinkEnterEvent oldEvent = this.linkEnterEvents.remove(e.getVehicleId());
		if ((oldEvent != null) && this.calculateLinkToLinkTravelTimes) {
			Tuple<Id<Link>, Id<Link>> fromToLink = new Tuple<>(oldEvent.getLinkId(), e.getLinkId());
			DataContainer data = getLinkToLinkTravelTimeData(fromToLink, true);
			this.aggregator.addTravelTime(data.ttData, oldEvent.getTime(), e.getTime());
			data.needsConsolidation = true;
		}
		this.linkEnterEvents.put(e.getVehicleId(), e);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent e) {
		if (this.calculateLinkTravelTimes) {
			LinkEnterEvent oldEvent = this.linkEnterEvents.get(e.getVehicleId());
			if (oldEvent != null) {
				DataContainer data = this.dataContainerProvider.getTravelTimeData(e.getLinkId(), true);
				this.aggregator.addTravelTime(data.ttData, oldEvent.getTime(), e.getTime());
				data.needsConsolidation = true;
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
		 * otherwise, the activity duration would counted as travel time, when the
		 * vehicle departs again and leaves the link! */
		this.linkEnterEvents.remove(event.getVehicleId());

		// try to remove vehicles from set with filtered vehicles
		if (filterAnalyzedModes) this.vehiclesToIgnore.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		/* remove EnterEvents from list when a bus stops on a link.
		 * otherwise, the stop time would counted as travel time, when the
		 * bus departs again and leaves the link! */
		this.linkEnterEvents.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		LinkEnterEvent e = this.linkEnterEvents.remove(event.getVehicleId());
		if (e != null) {
			DataContainer data = this.dataContainerProvider.getTravelTimeData(e.getLinkId(), true);
			data.needsConsolidation = true;
			this.aggregator.addStuckEventTravelTime(data.ttData, e.getTime(), event.getTime());
			if (this.calculateLinkToLinkTravelTimes){
				log.error(ERROR_STUCK_AND_LINKTOLINK);
				throw new IllegalStateException(ERROR_STUCK_AND_LINKTOLINK);
			}
		}

		// try to remove vehicle from set with filtered vehicles
		if (filterAnalyzedModes) this.vehiclesToIgnore.remove(event.getVehicleId());
	}

	private DataContainer getLinkToLinkTravelTimeData(Tuple<Id<Link>, Id<Link>> fromLinkToLink, final boolean createIfMissing) {
		DataContainer data = this.linkToLinkData.get(fromLinkToLink);
		if ((null == data) && createIfMissing) {
			data = new DataContainer(this.ttDataFactory.createTravelTimeData(fromLinkToLink.getFirst()));
			this.linkToLinkData.put(fromLinkToLink, data);
		}
		return data;
	}
	
	public double getLinkTravelTime(final Id<Link> linkId, final double time) {
		if (this.calculateLinkTravelTimes) {
			DataContainer data = this.dataContainerProvider.getTravelTimeData(linkId, true);
			if (data.needsConsolidation) {
				consolidateData(data);
			}
			return this.aggregator.getTravelTime(data.ttData, time);
		}
		throw new IllegalStateException("No link travel time is available " +
				"if calculation is switched off by config option!");
	}

	public double getLinkToLinkTravelTime(final Id<Link> fromLinkId, final Id<Link> toLinkId, double time) {
		if (!this.calculateLinkToLinkTravelTimes) {
			throw new IllegalStateException("No link to link travel time is available " +
					"if calculation is switched off by config option!");
		}
		DataContainer data = this.getLinkToLinkTravelTimeData(new Tuple<>(fromLinkId, toLinkId), true);
		if (data.needsConsolidation) {
			consolidateData(data);
		}
		return this.aggregator.getTravelTime(data.ttData, time);
	}

	@Override
	public void reset(int iteration) {
		if (this.calculateLinkTravelTimes) {
			for (DataContainer data : this.linkData.values()){
				data.ttData.resetTravelTimes();
				data.needsConsolidation = false;
			}
		}
		if (this.calculateLinkToLinkTravelTimes){
			for (DataContainer data : this.linkToLinkData.values()){
				data.ttData.resetTravelTimes();
				data.needsConsolidation = false;
			}
		}
		this.linkEnterEvents.clear();
		this.vehiclesToIgnore.clear();
	}

	public void setTravelTimeDataFactory(final TravelTimeDataFactory factory) {
		this.ttDataFactory = factory;
	}

	public void setTravelTimeAggregator(final AbstractTravelTimeAggregator aggregator) {
		this.aggregator = aggregator;
	}

	/**
	 * Makes sure that the travel times "make sense".
	 * <p/>
	 * Imagine short bin sizes (e.g. 5min), small links (e.g. 300 veh/hour)
	 * and small sample sizes (e.g. 2%). This would mean that effectively
	 * in the simulation only 6 vehicles can pass the link in one hour,
	 * one every 10min. So, the travel time in one time slot could be 
	 * >= 10min if two cars enter the link at the same time. If no car
	 * enters in the next time bin, the travel time in that time bin should
	 * still be >=5 minutes (10min - binSize), and not freespeedTraveltime,
	 * because actually every car entering the link in this bin will be behind
	 * the car entered before, which still needs >=5min until it can leave.
	 * <p/>
	 * This method ensures that the travel time in a time bin
	 * cannot be smaller than the travel time in the bin before minus the
	 * bin size.
	 * 
	 */
	private void consolidateData(final DataContainer data) {
		synchronized(data) {
			if (data.needsConsolidation) {
				TravelTimeData r = data.ttData;

				// initialize prevTravelTime with ttime from time bin 0 and time 0.  (The interface comment already states that
				// having both as argument does not make sense.)
				double prevTravelTime = r.getTravelTime(0, 0.0);
				// changed (1, 0.0) to (0, 0.0) since Michal has convinced me (by a test) that using "1" is wrong
				// because you get the wrong result for time slot number 1.  This change does not affect the existing
				// unit tests.  kai, oct'11

				// go from time slot 1 forward in time:
				for (int i = 1; i < this.numSlots; i++) {

					// once more the getter is weird since it needs both the time slot and the time:
					double travelTime = r.getTravelTime(i, i * this.timeSlice);

					// if the travel time in the previous time slice was X, then now it is X-S, where S is the time slice:
					double minTravelTime = prevTravelTime - this.timeSlice;

					// if the travel time that has been measured so far is less than that minimum travel time, then do something:
					if (travelTime < minTravelTime) {

						r.setTravelTime(i, minTravelTime);
						// (set the travel time to the smallest possible travel time that makes sense according to the argument above)

					} 
					prevTravelTime = r.getTravelTime(i, i * this.timeSlice ) ;
				}
				data.needsConsolidation = false;
			}
		}
	}

	public int getNumSlots() {
		return this.numSlots;
	}

	/**
	 * @return the size of a time bin in seconds.
	 */
	public int getTimeSlice() {
		return this.timeSlice;
	}

	/*package*/ static class DataContainer {
		/*package*/ final TravelTimeData ttData;
		/*package*/ volatile boolean needsConsolidation = false;

		/*package*/ DataContainer(final TravelTimeData data) {
			this.ttData = data;
		}
	}

	public TravelTime getLinkTravelTimes() {
		return new TravelTime() {

			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return TravelTimeCalculator.this.getLinkTravelTime(link.getId(), time);
			}

		};

	}

	public LinkToLinkTravelTime getLinkToLinkTravelTimes() {
		return new LinkToLinkTravelTime() {

			@Override
			public double getLinkToLinkTravelTime(Link fromLink, Link toLink, double time) {
				return TravelTimeCalculator.this.getLinkToLinkTravelTime(fromLink.getId(), toLink.getId(), time);
			}
		};
	}

}
