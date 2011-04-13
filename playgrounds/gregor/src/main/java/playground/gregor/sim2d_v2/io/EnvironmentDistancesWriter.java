/* *********************************************************************** *
 * project: org.matsim.*
 * EnvironmentDistancesWriter.java
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
package playground.gregor.sim2d_v2.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.gregor.sim2d_v2.simulation.floor.EnvironmentDistances;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class EnvironmentDistancesWriter extends MatsimXmlWriter {

	public void write(String file, StaticEnvironmentDistancesField fl) {
		
		QuadTree<EnvironmentDistances> tree = fl.getEnvironmentDistancesQuadTree();
		
		try {
			openFile(file);
			super.writeXmlHead();
			List<Tuple<String, String>> attrbs = new ArrayList<Tuple<String, String>>();
			Tuple<String, String> maxX = new Tuple<String, String>("maxX", Double.toString(tree.getMaxEasting()));
			Tuple<String, String> maxY = new Tuple<String, String>("maxY", Double.toString(tree.getMaxNorthing()));
			Tuple<String, String> minX = new Tuple<String, String>("minX", Double.toString(tree.getMinEasting()));
			Tuple<String, String> minY = new Tuple<String, String>("minY", Double.toString(tree.getMinNorthing()));
			
			Tuple<String, String> sens = new Tuple<String, String>("maxSensingRange", Double.toString(fl.getMaxSensingRange()));
			Tuple<String, String> res = new Tuple<String, String>("resolution", Double.toString(fl.getStaticEnvironmentDistancesFieldResolution()));
			
			attrbs.add(maxX);
			attrbs.add(maxY);
			attrbs.add(minX);
			attrbs.add(minY);
			attrbs.add(sens);
			attrbs.add(res);

			writeStartTag("staticEnvField", attrbs);
			for (EnvironmentDistances ed : tree.values()) {
				attrbs = new ArrayList<Tuple<String, String>>();
				Tuple<String, String> x = new Tuple<String, String>("x", Double.toString(ed.getLocation().x));
				Tuple<String, String> y = new Tuple<String, String>("y", Double.toString(ed.getLocation().y));
				attrbs.add(x);
				attrbs.add(y);
				writeStartTag("staticEnv", attrbs);
				for (Coordinate c : ed.getObjects()) {
					attrbs = new ArrayList<Tuple<String, String>>();
					x = new Tuple<String, String>("x", Double.toString(c.x));
					y = new Tuple<String, String>("y", Double.toString(c.y));
					attrbs.add(x);
					attrbs.add(y);
					writeStartTag("EnvCoord", attrbs, true);
				}
				writeEndTag("staticEnv");
			}

			writeEndTag("staticEnvField");
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
