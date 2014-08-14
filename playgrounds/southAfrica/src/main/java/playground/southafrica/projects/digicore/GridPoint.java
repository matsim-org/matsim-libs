/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.digicore;

class GridPoint {

	// Data Members
	private double x, y, z;
    
	//Constructor
	public GridPoint(double lx, double ly, double lz ) {
		x = lx;
		y = ly;
		z = lz;
	}

    //Returns x
	public double getX( ) {
		return x;
	}
	
    //Assigns x
	public void setX(double lx) {
		x = lx;
	}
	
    //Returns y
	public double getY( ) {
		return y;
	}
	
    //Assigns y
	public void setY(double ly) {
		y = ly;
	}
	
    //Returns z
	public double getZ( ) {
		return z;
	}
	
    //Assigns z
	public void setZ(double lz) {
		z = lz;
	}
}