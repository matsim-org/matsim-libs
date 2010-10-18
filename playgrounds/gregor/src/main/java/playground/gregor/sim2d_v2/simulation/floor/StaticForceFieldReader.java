/* *********************************************************************** *
 * project: org.matsim.*
 * StaticForceFieldReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import playground.gregor.sim2d.simulation.Force;

public class StaticForceFieldReader extends MatsimXmlParser {
	
	private static final Logger log = Logger.getLogger(StaticForceFieldReader.class);
	
	private Stack<Force> forces;

	private double maxY = Double.NEGATIVE_INFINITY;

	private double maxX = Double.NEGATIVE_INFINITY;

	private double minY = Double.POSITIVE_INFINITY;

	private double minX = Double.POSITIVE_INFINITY;

	private QuadTree<Force> ret;

	private final String file;
	
	private Force currentForce = null;

	private StaticForceField sff;

	public StaticForceFieldReader(String file) {
		this.file = file;
	}
	
	private void readStaticForceField() {
		this.forces = new Stack<Force>();
		log.info("parsing static force file...");
		try {
			super.parse(this.file);
		} catch (SAXException e) {
			log.fatal("Error during parsing.", e);
		} catch (ParserConfigurationException e) {
			log.fatal("Error during parsing.", e);
		} catch (IOException e) {
			log.fatal("Error during parsing.", e);
		}
		log.info("done.");
		
		log.info("loading forces into quad tree...");
		this.ret = new QuadTree<Force>(this.minX,this.minY,this.maxX,this.maxY);
		while (this.forces.size() > 0) {
			Force f = this.forces.pop();
			this.ret.put(f.getXCoord(), f.getYCoord(), f);
		}
		log.info("done.");
	}

	public StaticForceField getStaticForceField() {
		if (this.ret == null) {
			readStaticForceField();
			this.sff = new StaticForceField(this.ret);
		}
		return this.sff;
	}
	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(this.currentForce != null) {
			this.forces.push(this.currentForce);
			this.currentForce = null;
		}
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals(StaticForceFieldWriter.STATIC_FORCE_TAG)) {
			String xs = atts.getValue(StaticForceFieldWriter.X_COORD_TAG);
			String ys = atts.getValue(StaticForceFieldWriter.Y_COORD_TAG);
			String xfs = atts.getValue(StaticForceFieldWriter.FORCE_X_TAG);
			String yfs = atts.getValue(StaticForceFieldWriter.FORCE_Y_TAG);
			createForce(xs,ys,xfs,yfs);
//			System.out.println(xs + "  " + ys + " " + xfs + " " + yfs);
		}
		
	}

	private void createForce(String xs, String ys, String xfs, String yfs) {
		Force f = new Force(Double.parseDouble(xfs),Double.parseDouble(yfs),Double.parseDouble(xs),Double.parseDouble(ys));
		if (f.getXCoord() > this.maxX) {
			this.maxX = f.getXCoord();
		} else if (f.getXCoord() < this.minX) {
			this.minX = f.getXCoord();
		}
		if (f.getYCoord() > this.maxY) {
			this.maxY = f.getYCoord();
		} else if (f.getYCoord() < this.minY) {
			this.minY = f.getYCoord();
		}
		this.currentForce = f;
	}

}
