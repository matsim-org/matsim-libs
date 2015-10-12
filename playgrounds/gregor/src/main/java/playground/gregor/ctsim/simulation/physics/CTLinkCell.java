package playground.gregor.ctsim.simulation.physics;
/* *********************************************************************** *
 * project: org.matsim.*
 *
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

/**
 * Created by laemmel on 12/10/15.
 */
public class CTLinkCell extends CTCell {

	public CTLinkCell(double x, double y, CTNetwork net, CTNetworkEntity parent) {
		super(x, y, net, parent);
	}

	@Override
	double getFHHi(CTPed ped, CTCellFace face) {
		return 1 + Math.cos(ped.getDesiredDir() - face.h_i);
	}

}
