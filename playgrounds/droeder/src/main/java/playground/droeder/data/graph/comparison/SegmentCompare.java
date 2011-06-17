/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.data.graph.comparison;

import playground.droeder.data.graph.MatchingSegment;

/**
 * @author droeder
 *
 */
public class SegmentCompare extends AbstractCompare{

	public SegmentCompare(MatchingSegment refElement, MatchingSegment compareElement) {
		super(refElement, compareElement);
	}

	@Override
	public int compareTo(AbstractCompare o) {
		return super.compareTo(o);
	}

}
