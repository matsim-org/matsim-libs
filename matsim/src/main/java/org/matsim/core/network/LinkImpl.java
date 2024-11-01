/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractLink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.network;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import com.google.common.collect.ImmutableSortedSet;

/*deliberately package*/ class LinkImpl implements Link {

	private final static Logger log = LogManager.getLogger(Link.class);

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Id<Link> id;

	private Node from;
	private Node to;

	private double length = Double.NaN;
	private double freespeed;
	private double capacity;
	private double nofLanes;

	private Set<String> allowedModes = DEFAULT_ALLOWED_MODES;

	private final Network network;

	private static int fsWarnCnt = 0 ;
	private static int cpWarnCnt = 0 ;
	private static int plWarnCnt = 0 ;
	private static int lengthWarnCnt = 0;
	private static final int maxFsWarnCnt = 1;
	private static final int maxCpWarnCnt = 1;
	private static final int maxPlWarnCnt = 1;
	private static final int maxLengthWarnCnt = 1;

	private static final Set<String> DEFAULT_ALLOWED_MODES = HashSetCache.get(Set.of(TransportMode.car));

	private final Attributes attributes = new AttributesImpl();

	/*deliberately package*/ LinkImpl(final Id<Link> id, final Node from, final Node to, final Network network, final double length, final double freespeed, final double capacity, final double lanes) {
		this.id = id;
		this.network = network;
		this.from = from;
		this.to = to;
		this.setLength(length);
		//for the eventual time variant attributes don't call the setter as it must be overwritten in TimeVariantLinkImpl
		//and thus causes problems during object initialization, dg nov 2010
		this.freespeed = freespeed;
		this.checkFreespeedSemantics();
		this.capacity = capacity;
		this.checkCapacitySemantics();
		this.nofLanes = lanes;
		this.checkNumberOfLanesSemantics();
		// loop links have become an acceptable thing for matsim.  kai, sep'19. --> warnings turned off
	}

	private void checkCapacitySemantics() {
		/*
		 * I see no reason why a freespeed and a capacity of zero should not be
		 * allowed! joh 9may2008
		 * The warning says that it _may_ cause problems.  Not pretty if you want to get rid of warnings completely, but
		 * hopefully acceptable for the time being.  kai, oct'10
		 */
		if ((this.capacity <= 0.0) && (cpWarnCnt < maxCpWarnCnt) ) {
			cpWarnCnt++ ;
			log.warn("capacity=" + this.capacity + " of link id " + this.getId() + " may cause problems");
			if ( cpWarnCnt==maxCpWarnCnt ){
				log.warn( Gbl.FUTURE_SUPPRESSED );
			}
		}
	}

	private void checkFreespeedSemantics() {
		if ((this.freespeed <= 0.0) && (fsWarnCnt < maxFsWarnCnt) ) {
			fsWarnCnt++ ;
			log.warn("freespeed=" + this.freespeed + " of link id " + this.getId() +" may cause problems");
			if ( fsWarnCnt == maxFsWarnCnt )
				log.warn( Gbl.FUTURE_SUPPRESSED) ;
		}
	}

	private void checkNumberOfLanesSemantics(){
		if ((this.nofLanes < 1) && (plWarnCnt < maxPlWarnCnt) ) {
			plWarnCnt++ ;
			log.warn("permlanes=" + this.nofLanes + " of link id " + this.getId() +" may cause problems");
			if ( plWarnCnt == maxPlWarnCnt )
				log.warn( Gbl.FUTURE_SUPPRESSED ) ;
		}
	}

	private void checkLengthSemantics(){
		if ((this.getLength() <= 0.0) && (lengthWarnCnt < maxLengthWarnCnt)) {
			lengthWarnCnt++;
			log.warn("length=" + this.length + " of link id " + this.getId() + " may cause problems");
			if ( lengthWarnCnt == maxLengthWarnCnt )
				log.warn(Gbl.FUTURE_SUPPRESSED) ;
		}
	}



	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public Node getFromNode() {
		return this.from;
	}

	@Override
	public final boolean setFromNode(final Node node) {
		this.from = node;
		return true;
	}

	@Override
	public Node getToNode() {
		return this.to;
	}

	@Override
	public final boolean setToNode(final Node node) {
		this.to = node;
		return true;
	}

	// ---

	@Override
	public void setCapacity(double capacityPerNetworkCapcityPeriod){
		this.capacity = capacityPerNetworkCapcityPeriod;
		this.checkCapacitySemantics();
	}

	@Override
	public double getCapacity() {
		return this.capacity;
	}

	@Override
	public double getCapacity(final double time) { // not final since needed in TimeVariantLinkImpl
		return this.capacity;
	}

	public double getCapacityPeriod() {
		// since the link has a back pointer to network, we can as well provide this here (????)
		// TimeVariantLinkImpl needs this ... but why?
		return network.getCapacityPeriod() ;
	}

	// ---

	@Override
	public double getFreespeed() {
		return this.freespeed;
	}

	/**
	 * This method returns the freespeed velocity in meter per seconds.
	 *
	 * @param time - the current time
	 * @return freespeed
	 */
	@Override
	public double getFreespeed(final double time) { // not final since needed in TimeVariantLinkImpl
		return this.freespeed;
	}

	@Override
	public void setFreespeed(double freespeed) {
		this.freespeed = freespeed;
		this.checkFreespeedSemantics();
	}

	@Override
	public double getLength() {
		return this.length;
	}

	@Override
	public final void setLength(double length) {
		this.length = length;
		this.checkLengthSemantics();
	}

	@Override
	public double getNumberOfLanes() {
		return this.nofLanes;
	}

	@Override
	public double getNumberOfLanes(final double time) { // not final since needed in TimeVariantLinkImpl
		return this.nofLanes;
	}

	@Override
	public void setNumberOfLanes(double lanes) {
		this.nofLanes = lanes;
		this.checkNumberOfLanesSemantics();
	}

	@Override
	public final Set<String> getAllowedModes() {
		return this.allowedModes;
	}

	@Override
	public final void setAllowedModes(final Set<String> modes) {
		this.allowedModes = HashSetCache.get(modes);
	}

	@Override
	public String toString() {
		return super.toString() +
		"[id=" + this.getId() + "]" +
		"[from_id=" + this.from.getId() + "]" +
		"[to_id=" + this.to.getId() + "]" +
		"[length=" + this.length + "]" +
		"[freespeed=" + this.freespeed + "]" +
		"[capacity=" + this.capacity + "]" +
		"[permlanes=" + this.nofLanes + "]" +
		"[modes=" + this.allowedModes ;
	}

	@Override
	public Id<Link> getId() {
		return id;
	}

	@Override
	public Coord getCoord() {
		Coord fromXY = getFromNode().getCoord();
		Coord toXY = getToNode().getCoord();
		return new Coord((fromXY.getX() + toXY.getX()) / 2.0, (fromXY.getY() + toXY.getY()) / 2.0);
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	/*package*/ abstract static class HashSetCache {
		private final static ConcurrentMap<Integer, Set<String>> cache = new ConcurrentHashMap<>();

		public static Set<String> get(final Set<String> set) {
			if (set == null) {
				return null;
			}
			return cache.computeIfAbsent(set.hashCode(), key -> ImmutableSortedSet.copyOf(set));
		}
	}
}
