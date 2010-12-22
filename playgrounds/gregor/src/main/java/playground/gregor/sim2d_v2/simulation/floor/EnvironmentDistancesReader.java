/* *********************************************************************** *
 * project: org.matsim.*
 * EnvironmentDistancesReader.java
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
package playground.gregor.sim2d_v2.simulation.floor;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class EnvironmentDistancesReader extends MatsimXmlParser {

	private QuadTree<ForceLocation> tree = null;
	private EnvironmentDistances currentEnvDist;

	public QuadTree<ForceLocation> getEnvDistQuadTree() {
		if (this.tree == null) {
			throw new RuntimeException("Environment file needs to parsed before this method is allowed to be called!");
		}
		return this.tree;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.utils.io.MatsimXmlParser#startTag(java.lang.String,
	 * org.xml.sax.Attributes, java.util.Stack)
	 */
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals("staticEnvField")) {
			initQuadTree(atts);
		} else if (name.equals("staticEnv")) {
			initCurrentEnvDist(atts);
		} else if (name.equals("EnvCoord")) {
			initAndAddEnvCoord(atts);
		} else {
			throw new RuntimeException("Unknown tag:" + name);
		}
	}

	/**
	 * @param atts
	 */
	private void initAndAddEnvCoord(Attributes atts) {
		String x = atts.getValue("x");
		String y = atts.getValue("y");
		this.currentEnvDist.addEnvironmentDistanceLocation(new Coordinate(Double.parseDouble(x), Double.parseDouble(y)));
	}

	/**
	 * @param atts
	 */
	private void initCurrentEnvDist(Attributes atts) {
		String x = atts.getValue("x");
		String y = atts.getValue("y");
		this.currentEnvDist = new EnvironmentDistances(new Coordinate(Double.parseDouble(x), Double.parseDouble(y)));
		ForceLocation fl = new ForceLocation(this.currentEnvDist);
		this.tree.put(this.currentEnvDist.getLocation().x, this.currentEnvDist.getLocation().y, fl);

	}

	/**
	 * @param atts
	 */
	private void initQuadTree(Attributes atts) {
		String mx = atts.getValue("maxX");
		String my = atts.getValue("maxY");
		String mix = atts.getValue("minX");
		String miy = atts.getValue("minY");
		this.tree = new QuadTree<ForceLocation>(Double.parseDouble(mix), Double.parseDouble(miy), Double.parseDouble(mx), Double.parseDouble(my));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.utils.io.MatsimXmlParser#endTag(java.lang.String,
	 * java.lang.String, java.util.Stack)
	 */
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		// TODO Auto-generated method stub

	}

	public static void main(String[] args) {
		try {
			EnvironmentDistancesReader x = new EnvironmentDistancesReader();
			x.setValidating(false);
			x.parse("/home/laemmel/devel/dfg/data/envDistances.xml.gz");
			System.out.println(x.getEnvDistQuadTree().size());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
