/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.vsp.andreas.mzilske.osm;

import java.io.File;

import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.FastXmlReader;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;

public class ClipIncompleteEntitiesMain {
	
	public static void main(String[] args) {
		FastXmlReader reader = new FastXmlReader(new File("/Users/michaelzilske/osm/motorway_germany.osm"), true, CompressionMethod.None);
		ClipIncompleteEntities clipper = new ClipIncompleteEntities(IdTrackerType.BitSet, true, true, true);
		XmlWriter writer = new XmlWriter(new File("/Users/michaelzilske/osm/clipped.osm"), CompressionMethod.None);
		reader.setSink(clipper);
		clipper.setSink(writer);
		reader.run();
	}

}
