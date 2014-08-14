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

class Polygon {

	// Data Members
	private GridPoint[] PolyFace;
	private int NCorners;

	//Constructor
	public Polygon(int nC, int nDim, double[] dCoordsList ) {
		
		NCorners = nC;
		PolyFace = new GridPoint[NCorners];

		for (int i=0; i<NCorners; i++) {
			if (nDim == 2) {
				PolyFace[i] = new GridPoint(dCoordsList[i*nDim], dCoordsList[i*nDim+1], 0d);
			}
			else if (nDim == 3) {
				PolyFace[i] = new GridPoint(dCoordsList[i*nDim], dCoordsList[i*nDim+1], dCoordsList[i*nDim+2]);
			}			
		}
		
	}

    //Returns PolyFace
	public GridPoint[] getPolyFace( ) {
		return PolyFace;
	}
	
    //Returns NCorners
	public int getNCorners( ) {
		return NCorners;
	}
		
}
