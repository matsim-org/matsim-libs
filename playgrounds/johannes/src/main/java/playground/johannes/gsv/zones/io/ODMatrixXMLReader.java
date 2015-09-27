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

package playground.johannes.gsv.zones.io;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import playground.johannes.gsv.zones.ODMatrix;
import playground.johannes.synpop.gis.Zone;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * @author johannes
 *
 */
public class ODMatrixXMLReader extends MatsimXmlParser {

	private ODMatrix m;
	
	private Map<String, Zone> zoneIndex;
	
	public ODMatrix getMatrix() {
		return m;
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equalsIgnoreCase(ODMatrixXMLWriter.MATRIX_TAG)) {
			m = new ODMatrix();
		} else if(name.equalsIgnoreCase(ODMatrixXMLWriter.ZONES_TAG)) {
			zoneIndex = null;
		} else if(name.equalsIgnoreCase(ODMatrixXMLWriter.CELL_TAG)) {
			Zone orig = zoneIndex.get(atts.getValue(ODMatrixXMLWriter.ROW_KEY));
			Zone dest = zoneIndex.get(atts.getValue(ODMatrixXMLWriter.COL_KEY));
			String val = atts.getValue(ODMatrixXMLWriter.VALUE_KEY);
			
			m.set(orig, dest, new Double(val));
		}

	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equalsIgnoreCase(ODMatrixXMLWriter.MATRIX_TAG)) {
			// do nothing
		} else if (name.equalsIgnoreCase(ODMatrixXMLWriter.ZONES_TAG)) {
			zoneIndex = new HashMap<String, Zone>();
			Set<Zone> zones = Zone2GeoJSON.parseFeatureCollection(content);
			for(Zone zone : zones) {
				String id = zone.getAttribute(ODMatrixXMLWriter.ZONE_ID_KEY);
				zoneIndex.put(id, zone);
			}
			
		}

	}
	
	public static void main(String args[]) {
		ODMatrixXMLReader reader = new ODMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/gsv/synpop/output/matrix.xml");
		ODMatrix m = reader.getMatrix();
		m.toString();
	}

}
