/* *********************************************************************** *
 * project: org.matsim.*
 * LeftTurnIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.utils;

import java.util.TreeMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.signalsystems.CalculateAngle;

/**
 * based on {@code org.matsim.signalsystems.CalculateAngle}
 * 
 * @author yu
 * 
 */
public class LeftTurnIdentifier {
	public static boolean turnLeft(Link inLink, Link outLink) {
		if (outLink.getToNode().equals(inLink.getFromNode())) {
			/* U-Turn (size==0) */
			return true;
		}

		TreeMap<Double, Link> outLinksSortedByAngle = CalculateAngle
				.getOutLinksSortedByAngle(inLink);
		int realOutLinksSize = outLinksSortedByAngle.size();
		if (realOutLinksSize == 1) {
			/* NOT intersection */
			return false;
		} else if (realOutLinksSize > 1) {
			Double zeroAngle = 0d;
			if (!outLinksSortedByAngle.containsKey(zeroAngle)) {
				/* without straight link */
				Double lowerKey = outLinksSortedByAngle.lowerKey(zeroAngle);
				Double higherKey = outLinksSortedByAngle.higherKey(zeroAngle);
				if (lowerKey == null) {
					/* no left turns */
					return false;
				} else if (higherKey == null) {
					/* no right turns */
					return outLinksSortedByAngle.headMap(lowerKey)
							.containsValue(outLink);
				}
				return outLinksSortedByAngle.headMap(lowerKey,
						Math.abs(lowerKey) > Math.abs(higherKey))
						.containsValue(outLink);
				/* ">"- inclusive, "<=" - exclusive */
			} else {
				return outLinksSortedByAngle.headMap(zeroAngle).containsValue(
						outLink/* ,false strict exclusive 0 */);
			}
		}
		return false;
	}
}
