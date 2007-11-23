/* *********************************************************************** *
 * project: org.matsim.*
 * Test.java
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

package playground.kai;

import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.utils.identifiers.IdI;

public class Test implements BasicLinkI {

	public Test() {
		super();
		// TODO Auto-generated constructor stub
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

	public void build() {
		// TODO Auto-generated method stub

	}

	public void setLength_m(double length_m) {
		// TODO Auto-generated method stub

	}

	public void setMaxVel_m_s(double maxVel_m_s) {
		// TODO Auto-generated method stub

	}

	public void setMaxFlow_veh_s(double maxFlow_veh_s) {
		// TODO Auto-generated method stub

	}

	public void setLanes(int lanes) {
		// TODO Auto-generated method stub

	}

	public double getLength_m() {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getMaxVel_m_s() {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getMaxFlow_veh_s() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getLanes() {
		// TODO Auto-generated method stub
		return 0;
	}

}
