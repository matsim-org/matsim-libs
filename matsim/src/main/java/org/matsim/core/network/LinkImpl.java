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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.attributable.Attributes;

/*deliberately package*/ class LinkImpl implements Link {

	private final static Logger log = Logger.getLogger(Link.class);

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Id<Link> id;

	private Node from = null;
	private Node to = null;

	private double length = Double.NaN;
	private double freespeed = Double.NaN;
	private double capacity = Double.NaN;
	private double nofLanes = Double.NaN;

	private Set<String> allowedModes = HashSetCache.get(new HashSet<String>());

	private String type = null;

	private String origid = null;

	private final Network network;

	private static int fsWarnCnt = 0 ;
	private static int cpWarnCnt = 0 ;
	private static int plWarnCnt = 0 ;
	private static int lengthWarnCnt = 0;
	private static int loopWarnCnt = 0 ;
	private static final int maxFsWarnCnt = 1;
	private static final int maxCpWarnCnt = 1;
	private static final int maxPlWarnCnt = 1;
	private static final int maxLengthWarnCnt = 1;
	private static final int maxLoopWarnCnt = 1;

	private static final Set<String> DEFAULT_ALLOWED_MODES;
	static {
		Set<String> set = new HashSet<>();
		set.add(TransportMode.car);
		DEFAULT_ALLOWED_MODES = HashSetCache.get(set);
	}

	private final Attributes attributes = new Attributes();

	/*deliberately package*/ LinkImpl(final Id<Link> id, final Node from, final Node to, final Network network, final double length, final double freespeed, final double capacity, final double lanes) {
		this.id = id;
		this.network = network;
		this.from = from;
		this.to = to;
		this.allowedModes = DEFAULT_ALLOWED_MODES;
		this.setLength(length);
		//for the eventual time variant attributes don't call the setter as it must be overwritten in TimeVariantLinkImpl
		//and thus causes problems during object initialization, dg nov 2010
		this.freespeed = freespeed;
		this.checkFreespeedSemantics();
		this.capacity = capacity;
		this.checkCapacitiySemantics();
		this.checkCapacitiySemantics();
		this.nofLanes = lanes;
		this.checkNumberOfLanesSemantics();
		if (this.from.equals(this.to) && (loopWarnCnt < maxLoopWarnCnt)) {
			loopWarnCnt++ ;
			log.warn("[from=to=" + this.to + " link is a loop]");
			if ( loopWarnCnt == maxLoopWarnCnt )
				log.warn(Gbl.FUTURE_SUPPRESSED ) ;
		}
	}

	private void checkCapacitiySemantics() {
		/*
		 * I see no reason why a freespeed and a capacity of zero should not be
		 * allowed! joh 9may2008
		 * The warning says that it _may_ cause problems.  Not pretty if you want to get rid of warnings completely, but
		 * hopefully acceptable for the time being.  kai, oct'10
		 */
		if ((this.capacity <= 0.0) && (cpWarnCnt < maxCpWarnCnt) ) {
			cpWarnCnt++ ;
			log.warn("[capacity=" + this.capacity + " of link id " + this.getId() + " may cause problems]");
			log.warn( Gbl.FUTURE_SUPPRESSED ) ;
		}
	}

	private void checkFreespeedSemantics() {
		if ((this.freespeed <= 0.0) && (fsWarnCnt < maxFsWarnCnt) ) {
			fsWarnCnt++ ;
			log.warn("[freespeed=" + this.freespeed + " of link id " + this.getId() + " may cause problems]");
			if ( fsWarnCnt == maxFsWarnCnt )
				log.warn( Gbl.FUTURE_SUPPRESSED) ;
		}
	}

	private void checkNumberOfLanesSemantics(){
		if ((this.nofLanes < 1) && (plWarnCnt < maxPlWarnCnt) ) {
			plWarnCnt++ ;
			log.warn("[permlanes=" + this.nofLanes + " of link id " + this.getId() +" may cause problems]");
			if ( plWarnCnt == maxPlWarnCnt )
				log.warn( Gbl.FUTURE_SUPPRESSED ) ;
		}
	}

	private void checkLengthSemantics(){
		if ((this.getLength() <= 0.0) && (lengthWarnCnt < maxLengthWarnCnt)) {
			lengthWarnCnt++;
			log.warn("[length=" + this.length + " of link id " + this.getId() + " may cause problems]");
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

	/*deliberately package*/ final String getOrigId2() {
		// yyyyyy should now be possible to solve this with Attributable. kai, nov'16
		return this.origid ;
	}

	/*deliberately package*/ final String getType2() {
		// yyyyyy should now be possible to solve this with Attributable. kai, nov'16
		return this.type ;
	}
	
	// ---

	@Override
	public void setCapacity(double capacityPerNetworkCapcityPeriod){
		this.capacity = capacityPerNetworkCapcityPeriod;
		this.checkCapacitiySemantics();
	}

	@Override
	public double getCapacity() {
		return this.capacity;
	}

	@Override
	public double getCapacity(final double time) { // not final since needed in TimeVariantLinkImpl
		return this.capacity;
	}


	@Override
	public double getFlowCapacityPerSec() {
		return getFlowCapacityPerSec(Time.UNDEFINED_TIME);
	}
	@Override
	public double getFlowCapacityPerSec(@SuppressWarnings("unused") final double time) {
		return this.getCapacity(time) / network.getCapacityPeriod();
	}
	
	double getCapacityPeriod() {
		// since the link has a back pointer to network, we can as well provide this here (????)
		// TimeVariantLinkImpl needs this ... but why?
		return network.getCapacityPeriod() ;
	}

	// ---
	
	@Override
	public double getFreespeed() {
		return this.freespeed;
//		return getFreespeed(Time.UNDEFINED_TIME);
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
//		return this.getNumberOfLanes(Time.UNDEFINED_TIME);
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

	/*deliberately package*/ final void setOrigId2(final String id) {
		this.origid = id;
	}
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		this.from.addOutLink(this);
		this.to.addInLink(this);
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
//		"[origid=" + this.origid + "]" + // not in api
//		"[type=" + this.type + "]"; // not in api
		// yyyyyy add the "free text" attributes
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
		private final static Map<Integer, List<Set<String>>> cache = new ConcurrentHashMap<>();
		public static Set<String> get(final Set<String> set) {
			if (set == null) {
				return null;
			}
			int size = set.size();
			List<Set<String>> list = cache.get(size);
			if (list == null) {
				list = new ArrayList<>(4);
				cache.put(size, list);
				HashSet<String> set2 = new HashSet<>(set);
				Set<String> set3 = Collections.unmodifiableSet(set2);
				list.add(set3);
				return set3;
			}
			for (Set<String> s : list) {
				if (s.equals(set)) {
					return s;
				}
			}
			// not yet in cache
			HashSet<String> set2 = new HashSet<>(set);
			Set<String> set3 = Collections.unmodifiableSet(set2);
			list.add(set3);
			return set3;
		}

	}

	/*deliberately package*/ void setType2(String type2) {
		// yyyyyy should now be possible to solve this with Attributable. kai, nov'16
		this.type = type2 ;
	}
}
