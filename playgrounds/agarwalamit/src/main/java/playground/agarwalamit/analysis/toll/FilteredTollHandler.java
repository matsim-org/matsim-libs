/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.toll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.utils.GeometryUtils;
import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;

/**
 * @author amit
 */

public class FilteredTollHandler implements PersonMoneyEventHandler, PersonDepartureEventHandler, LinkLeaveEventHandler, CongestionEventHandler,
VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private static final Logger LOGGER = Logger.getLogger(FilteredTollHandler.class.getName());

	private final TollInfoHandler delegate;
	private final Vehicle2DriverEventHandler veh2DriverDelegate = new Vehicle2DriverEventHandler();
	private final MunichPersonFilter pf = new MunichPersonFilter();
	private final Collection<Geometry> zonalGeoms;
	private final Network network;
	private final String ug ;

	private final Map<Id<Person>,Id<Link>> person2DepartureLeaveLink = new HashMap<>();

	/**
	 * No filtering will be used, result will include all links, persons from all user groups.
	 */
	public FilteredTollHandler(final double simulationEndTime, final int numberOfTimeBins){
		this(simulationEndTime,numberOfTimeBins,null,null);
		LOGGER.info("No filtering is used, result will include all links, persons from all user groups.");
	}

	/**
	 * User group filtering will be used, result will include all links but persons from given user group only.
	 */
	public FilteredTollHandler(final double simulationEndTime, final int numberOfTimeBins, final String userGroup){
		this(simulationEndTime,numberOfTimeBins,null,null,userGroup);
		LOGGER.info("Usergroup filtering is used, result will include all links but persons from given user group only.");
		LOGGER.warn("User group will be identified for Munich scenario only, i.e. Urban, (Rev)Commuter and Freight.");
	}

	/**
	 * Area filtering will be used, result will include links falls inside the given shape and persons from all user groups.
	 */
	public FilteredTollHandler(final double simulationEndTime, final int numberOfTimeBins, final String shapeFile, final Network network){
		this(simulationEndTime,numberOfTimeBins,shapeFile,network,null);
		LOGGER.info("Area filtering is used, result will include links falls inside the given shape and persons from all user groups.");
	}

	/**
	 * Area and user group filtering will be used, links fall inside the given shape and persons belongs to the given user group will be considered.
	 */
	public FilteredTollHandler(final double simulationEndTime, final int numberOfTimeBins, final String shapeFile, 
			final Network network, final String userGroup){
		this.delegate = new TollInfoHandler(simulationEndTime, numberOfTimeBins);
		if(shapeFile!=null) {
			Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(shapeFile);
			this.zonalGeoms = GeometryUtils.getSimplifiedGeometries(features);
		}
		else this.zonalGeoms = new ArrayList<>();

		this.network = network;
		this.ug=userGroup;
		LOGGER.info("Area and user group filtering is used, links fall inside the given shape and belongs to the given user group will be considered.");
		LOGGER.warn("User group will be identified for Munich scenario only, i.e. Urban, (Rev)Commuter and Freight.");
	}

	@Override
	public void reset(int iteration) {
		this.zonalGeoms.clear();
		this.person2DepartureLeaveLink.clear();
		this.delegate.reset(iteration);
		this.veh2DriverDelegate.reset(iteration);
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		if(this.ug!=null){ 
			Id<Person> driverId = Id.createPersonId(event.getPersonId());
			if ( ! this.zonalGeoms.isEmpty() ) { // filtering for both
//				removal will not work, because for a link levae event two (corresponding to warm and cold emission) personMoney events are possible.
//				Id<Link> linkId = this.person2DepartureLeaveLink.remove(event.getPersonId());
				Id<Link> linkId = this.person2DepartureLeaveLink.get(event.getPersonId());
				Link link = network.getLinks().get(linkId);
				if ( this.pf.getUserGroupAsStringFromPersonId(driverId).equals(ug)  && GeometryUtils.isLinkInsideGeometries(zonalGeoms, link)   ) {
					delegate.handleEvent(event);
				}
			} else { // filtering for user group only
				if ( this.pf.getUserGroupAsStringFromPersonId(driverId).equals(ug)  ) {
					delegate.handleEvent(event);
				}
			}
		} else {
			if( ! this.zonalGeoms.isEmpty()  ) { // filtering for area only
				Id<Link> linkId = this.person2DepartureLeaveLink.get(event.getPersonId());
				Link link = network.getLinks().get(linkId);
				if( GeometryUtils.isLinkInsideGeometries(zonalGeoms, link) ) {
					delegate.handleEvent(event);
				}
			} else { // no filtering at all
				delegate.handleEvent(event);
			}
		}
	}

	@Override
	public void handleEvent(CongestionEvent event) {
		if(! this.zonalGeoms.isEmpty() ) {
			throw new RuntimeException("The methodology should work for congestion events as well, "
					+ "by storing the link id from affected agent for causing agent."
					+ "however, it is not implemented yet.");
		} else {
			// this should be fine, because areal filtering is not used.
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = this.veh2DriverDelegate.getDriverOfVehicle(event.getVehicleId());
		// money events from warm emission events are thrown after link leave events.
		// money events from cold emission events are thrown after departure and link leave events until 2km length
		this.person2DepartureLeaveLink.put(personId, event.getLinkId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// money events from cold emission events are thrown after departure and link leave events until 2km length 
		this.person2DepartureLeaveLink.put(event.getPersonId(), event.getLinkId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.veh2DriverDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.veh2DriverDelegate.handleEvent(event);		
	}

	/**
	 * @return time bin to person id to toll value after filtering if any
	 */
	public SortedMap<Double,Map<Id<Person>,Double>> getTimeBin2Person2Toll() {
		return this.delegate.getTimeBin2Person2Toll();
	}

	/**
	 * @return timeBin to toll values after filtering if any
	 */
	public SortedMap<Double,Double> getTimeBin2Toll(){
		return this.delegate.getTimeBin2Toll();
	}
}