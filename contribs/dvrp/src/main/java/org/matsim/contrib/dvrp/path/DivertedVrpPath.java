/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.path;

import java.util.Iterator;

import org.matsim.api.core.v01.network.Link;

import com.google.common.collect.Iterators;

/**
 * A DivertedVrpPath is a VrpPath which additionally keeps information about the original path, where the new path
 * deviates from the original one, and at which link index they diverted.
 * 
 * @author (of documentation) nagel
 */
public class DivertedVrpPath implements VrpPath {
	private final VrpPath originalPath;
	private final VrpPath newSubPath;
	private final int diversionLinkIdx;// originalPath.getLink(diversionLinkIdx) == newSubPath.getLink(0)

	public DivertedVrpPath(VrpPath originalPath, VrpPath newSubPath, int diversionLinkIdx) {
		if (originalPath.getLink(diversionLinkIdx) != newSubPath.getLink(0)) {
			throw new IllegalArgumentException();
		}

		this.originalPath = originalPath;
		this.newSubPath = newSubPath;
		this.diversionLinkIdx = diversionLinkIdx;
	}

	@Override
	public int getLinkCount() {
		return diversionLinkIdx + newSubPath.getLinkCount();
	}

	@Override
	public Link getLink(int idx) {
		if (isInOriginalPath(idx)) {
			return originalPath.getLink(idx);
		} else {
			return newSubPath.getLink(idx - diversionLinkIdx);
		}
	}

	@Override
	public double getLinkTravelTime(int idx) {
		if (isInOriginalPath(idx)) {
			return originalPath.getLinkTravelTime(idx);
		} else {
			return newSubPath.getLinkTravelTime(idx - diversionLinkIdx);
		}
	}

	private boolean isInOriginalPath(int idx) {
		// for getLink() both idx < diversionLinkIdx and idx <= diversionLinkIdx are OK
		// for getLinkTT() diversionLinkIdx must be taken from originalPath since TT for the first link
		// in newSubPath is 1 second (a vehicle enters the link at its end)
		return idx <= diversionLinkIdx;
	}

	@Override
	public void setLinkTravelTime(int idx, double linkTT) {
		if (isInOriginalPath(idx)) {
			originalPath.setLinkTravelTime(idx, linkTT);
		} else {
			newSubPath.setLinkTravelTime(idx - diversionLinkIdx, linkTT);
		}
	}

	@Override
	public Link getFromLink() {
		return originalPath.getFromLink();
	}

	@Override
	public Link getToLink() {
		return newSubPath.getToLink();
	}

	@Override
	public Iterator<Link> iterator() {
		return Iterators.concat(Iterators.limit(originalPath.iterator(), diversionLinkIdx), newSubPath.iterator());
	}

	public VrpPath getOriginalPath() {
		return originalPath;
	}

	public VrpPath getNewSubPath() {
		return newSubPath;
	}

	public int getDiversionLinkIdx() {
		return diversionLinkIdx;
	}
}
