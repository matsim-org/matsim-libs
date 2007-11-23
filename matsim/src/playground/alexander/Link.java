/* *********************************************************************** *
 * project: org.matsim.*
 * Link.java
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

package playground.alexander;

import org.matsim.basic.v01.BasicNode;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.utils.identifiers.IdI;

public class Link implements BasicLinkI{

	protected BasicNode from = null;
	protected BasicNode to = null;

	protected double length = Double.NaN;
	protected double width = Double.NaN;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public boolean setFromNode(BasicNodeI node) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean setToNode(BasicNodeI node) {
		// TODO Auto-generated method stub
		return false;
	}

	public BasicNodeI getFromNode() {
		// TODO Auto-generated method stub
		return null;
	}

	public BasicNodeI getToNode() {
		// TODO Auto-generated method stub
		return null;
	}

	public IdI getId() {
		// TODO Auto-generated method stub
		return null;
	}

}
