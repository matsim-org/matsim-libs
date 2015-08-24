/* *********************************************************************** *
 *	 Nico de Koker                                                         *
 *	 Aug 2014                                                              *
 *   University of Pretoria                                                *
 *                                                                         *	   
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.southafrica.sandboxes.ndekoker.FCCCentroid;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class RunFCCCentroid {

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

		// Given z level, return polyhedral mapping to 2D
		FCCPlanePolygon[] myPPoly;
		myPPoly = new FCCPlanePolygon[6];

		myPPoly[0] = new FCCPlanePolygon(0d, 0d, 0d, 3, 0.00d, 1d);	// cx, cy, cz, normal direction, level of z, scale 
		myPPoly[1] = new FCCPlanePolygon(0d, 0d, 0d, 3, 0.25d, 1d);
		myPPoly[2] = new FCCPlanePolygon(0d, 0d, 0d, 3, 0.50d, 1d);
		myPPoly[3] = new FCCPlanePolygon(0d, 0d, 0d, 3, 0.75d, 1d);
		myPPoly[4] = new FCCPlanePolygon(0d, 0d, 0d, 3, 1.00d, 1d);
		myPPoly[5] = new FCCPlanePolygon(0d, 0d, 0d, 3, 1.50d, 1d); // plane outside centroid influence region

		// Save polhedral map
		File myFile3 = new File("/Users/dekoker/Work/Research/Transport/Codes/fccplane.dat");
		myFile3.getParentFile().mkdirs();
		PrintWriter myPFile3 = new PrintWriter(myFile3);

		for (int i=0; i < 6; i++) {
			for (int j=0; j < 8; j++) {
			myPFile3.printf("%12.4f %12.4f %12.4f\n", myPPoly[i].getFcPlanePolygon().getPolyFace()[j].getX()
													, myPPoly[i].getFcPlanePolygon().getPolyFace()[j].getY()						
													, myPPoly[i].getFcPlanePolygon().getPolyFace()[j].getZ());
			}
		myPFile3.println();
		}


		myPFile3.close();
	}

}





