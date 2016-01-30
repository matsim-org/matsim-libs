/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.emission.filtering;

import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.agarwalamit.utils.GeometryUtils;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler;

/**
 * @author amit
 */

public class FilteredWarmEmissionHandler implements WarmEmissionEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private static final Logger LOGGER = Logger.getLogger(FilteredWarmEmissionHandler.class.getName());
	
	private final EmissionsPerLinkWarmEventHandler delegate;
	private final Vehicle2DriverEventHandler veh2DriverDelegate = new Vehicle2DriverEventHandler();
	private final ExtendedPersonFilter pf = new ExtendedPersonFilter();
	private final Collection<SimpleFeature> features ;
	private Network network;
	private final UserGroup ug ;

	/**
	 * Area and user group filtering will be used, links fall inside the given shape and persons belongs to the given user group will be considered.
	 */
	public FilteredWarmEmissionHandler (final double simulationEndTime, final int noOfTimeBins, final String shapeFile, 
			final Network network, final UserGroup userGroup){
		this.delegate = new EmissionsPerLinkWarmEventHandler(simulationEndTime,noOfTimeBins);
		
		if(shapeFile!=null) this.features = new ShapeFileReader().readFileAndInitialize(shapeFile);
		else this.features = null;
		
		this.network = network;
		this.ug=userGroup;
		LOGGER.info("Area and user group filtering is used, links fall inside the given shape and belongs to the given user group will be considered.");
	}

	/**
	 * User group filtering will be used, result will include all links but persons from given user group only. Another class 
	 * {@link EmissionsPerPersonPerUserGroup} could give results for all user groups in one run only.
	 */
	public FilteredWarmEmissionHandler (final double simulationEndTime, final int noOfTimeBins, final UserGroup userGroup){
		this(simulationEndTime,noOfTimeBins,null,null,userGroup);
		LOGGER.info("Usergroup filtering is used, result will include all links but persons from given user group only.");
		LOGGER.warn( "This could be achieved from the other class \"EmissionsPerPersonPerUserGroup\", alternatively verify your results with the other class.");
	}
	
	/**
	 * Area filtering will be used, result will include links falls inside the given shape and persons from all user groups.
	 */
	public FilteredWarmEmissionHandler (final double simulationEndTime, final int noOfTimeBins, final String shapeFile, final Network network){
		this(simulationEndTime,noOfTimeBins,shapeFile,network,null);
		LOGGER.info("Area filtering is used, result will include links falls inside the given shape and persons from all user groups.");
	}
	
	/**
	 * No filtering will be used, result will include all links, all persons.
	 */
	public FilteredWarmEmissionHandler (final double simulationEndTime, final int noOfTimeBins){
		this(simulationEndTime,noOfTimeBins,null,null);
		LOGGER.info("No filtering is used, result will include all links, all persons.");
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		
		if( this.ug != null ) {
			if( this.features!=null ) { // filtering for area only
				Link link = network.getLinks().get(event.getLinkId());
				if(GeometryUtils.isLinkInsideCity(features, link) ) {
					delegate.handleEvent(event);
				}
			} else { // no filtering at all
				delegate.handleEvent(event);
			}
		} else { 
			if ( this.features!=null ) { // filtering for both
				Link link = network.getLinks().get(event.getLinkId());
				Id<Person> driverId = this.veh2DriverDelegate.getDriverOfVehicle(event.getVehicleId());
				 if ( this.pf.isPersonIdFromUserGroup(driverId, ug)  && GeometryUtils.isLinkInsideCity(features, link) ) {
					 delegate.handleEvent(event);
				 }
			} else { // filtering for user group only
				Id<Person> driverId = this.veh2DriverDelegate.getDriverOfVehicle(event.getVehicleId());
				 if ( this.pf.isPersonIdFromUserGroup(driverId, ug)  ) {
					 delegate.handleEvent(event);
				 }
			}
		}
	}

	public Map<Double, Map<Id<Link>, Double>> getTime2linkIdLeaveCount() {
		return delegate.getTime2linkIdLeaveCount();
	}

	public Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> getWarmEmissionsPerLinkAndTimeInterval() {
		return delegate.getWarmEmissionsPerLinkAndTimeInterval();
	}
	
	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
		veh2DriverDelegate.reset(iteration);
	}
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		veh2DriverDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		veh2DriverDelegate.handleEvent(event);
	}
}