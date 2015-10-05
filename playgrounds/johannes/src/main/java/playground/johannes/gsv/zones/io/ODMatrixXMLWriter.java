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

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import playground.johannes.gsv.zones.ODMatrix;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;

import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 *
 */
public class ODMatrixXMLWriter extends MatsimXmlWriter {

	static final String MATRIX_TAG = "matrix";
	
	static final String CELLS_TAG = "cells";
	
	static final String CELL_TAG = "cell";
	
	static final String ZONES_TAG = "zones";
	
	static final String ROW_KEY = "row";
	
	static final String COL_KEY = "col";
	
	static final String VALUE_KEY = "value";
	
	static final String ZONE_ID_KEY = "zoneID";
	
	public void write(ODMatrix m, String file) {
		openFile(file);
		writeXmlHead();
		writeStartTag(MATRIX_TAG, null);
		writeZones(m);
		writeEntries(m);
		writeEndTag(MATRIX_TAG);
		close();
	}
	
	
	
	private void writeEntries(ODMatrix m) {
		writeStartTag(CELLS_TAG, null);
		
		for(Zone i : m.keySet()) {
			for(Zone j : m.keySet()) {
				Double val = m.get(i, j);
				if(val != null) {
					List<Tuple<String, String>> atts = new ArrayList<>(3);
					atts.add(createTuple(ROW_KEY, i.getAttribute(ZONE_ID_KEY)));
					atts.add(createTuple(COL_KEY, j.getAttribute(ZONE_ID_KEY)));
					atts.add(createTuple(VALUE_KEY, val));
					writeStartTag(CELL_TAG, atts, true);
				}
			}
		}
		
		writeEndTag(CELLS_TAG);
	}
	
	private void writeZones(ODMatrix m) {
		int idx = 0;
		for(Zone zone : m.keySet()) {
			zone.setAttribute(ZONE_ID_KEY, String.valueOf(idx++));
		}
		
		writeStartTag(ZONES_TAG, null);
		writeContent(ZoneGeoJsonIO.toJson(m.keySet()), false);
		writeEndTag(ZONES_TAG);
	}
}
