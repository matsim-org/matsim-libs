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

package playground.johannes.gsv.zones;

import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class Matrix2ZoneMatrix {

	public static KeyMatrix convert(Matrix mIn) {
		KeyMatrix mOut = new KeyMatrix();

		Set<String> keys = new HashSet<>(mIn.getFromLocations().keySet());
		keys.addAll(mIn.getToLocations().keySet());

		for (String from : keys) {
			for (String to : keys) {
				if (from.equals(to)) {
					if (mOut.get(from, to) == null) {
						Entry e = mIn.getEntry(from, to);
						if (e != null) {
							mOut.set(from, to, e.getValue());
						}
					}
				} else {
					Entry e = mIn.getEntry(from, to);
					if (e != null) {
						mOut.set(from, to, e.getValue());
					}
				}
			}
		}

		return mOut;
	}
}
