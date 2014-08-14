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

// Nico de Koker, University of Pretoria, August 2014

import java.lang.*;
import java.util.*;
import java.io.*;
import javax.media.opengl.*;
import java.awt.*;

public class RunTDC {

	public static void main(String[] args) throws IOException {

		// Generate grid
		FCCGrid myGrid = new FCCGrid(-0d,3d,-0d,3d,-0d,3d,1d);

		// Save grid to file
		File myFile = new File("/Users/dekoker/Work/Research/Transport/Codes/fccgrid.dat");
		myFile.getParentFile().mkdirs();
		PrintWriter myPFile = new PrintWriter(myFile);

		for (int i=0; i < myGrid.getNGrid(); i++) {
			myPFile.printf("%12.4f %12.4f %12.4f\n", myGrid.getFcGrid()[i].getX(), myGrid.getFcGrid()[i].getY(), myGrid.getFcGrid()[i].getZ());
		}
			
		myPFile.close();
		
		// Make one polyhedron
		FCCPolyhedron myPoly = new FCCPolyhedron(0d, 0d, 0d, 1d);
	
		// Save polyhedron to file
		File myFile2 = new File("/Users/dekoker/Work/Research/Transport/Codes/fccpoly.dat");
		myFile2.getParentFile().mkdirs();
		PrintWriter myPFile2 = new PrintWriter(myFile2);

		for (int i=0; i < 12; i++) {
			for (int j=0; j < 4; j++) {
				myPFile2.printf("%12.4f %12.4f %12.4f\n", myPoly.getFcPolyhedron()[i].getPolyFace()[j].getX()
													    , myPoly.getFcPolyhedron()[i].getPolyFace()[j].getY()						
								 					    , myPoly.getFcPolyhedron()[i].getPolyFace()[j].getZ());
			}
			myPFile2.println();
		}
			
		myPFile2.close();
		
	}

}

