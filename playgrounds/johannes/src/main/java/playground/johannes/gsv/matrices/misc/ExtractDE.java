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

package playground.johannes.gsv.matrices.misc;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;

import java.io.IOException;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class ExtractDE {

	public static KeyMatrix extract(KeyMatrix m, String zonefile, String key) throws IOException {
		ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zonefile, key);

		KeyMatrix newM = new KeyMatrix();
		Set<String> keys = m.keys();

		for (String i : keys) {
			Zone zi = zones.get(i);
			if (zi != null) {
				for (String j : keys) {
					Zone zj = zones.get(j);
					if (zj != null) {
						newM.set(i, j, m.get(i, j));
					}
				}
			}
		}
		
		return newM;
	}

}
