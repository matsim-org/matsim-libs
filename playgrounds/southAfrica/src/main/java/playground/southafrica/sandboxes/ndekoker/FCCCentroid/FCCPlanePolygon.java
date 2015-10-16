/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.sandboxes.ndekoker.FCCCentroid;


class FCCPlanePolygon {
	
	// Data Members
	private NDPolygon FcPlanePolygon;

	//Constructors
	public FCCPlanePolygon(double dCx, double dCy, double dCz, int iDim, double dLevel, double dScale) {

		double dSLevel = dLevel/dScale;
		double dCLevel;
		
		double dBaseCornerX, dBaseCornerY, dBaseCornerZ;
		
		double[] dBasicPoly = new double[24];
		double[] dCoordsList = new double[24];

		switch (iDim) {
		case 1: dCLevel = dCx/dScale;
				break;
		case 2: dCLevel = dCy/dScale;
				break;		
		case 3: 
		default: dCLevel = dCz/dScale;
				break;
		}
		
		double dSRLevel = Math.abs(dSLevel - dCLevel);

		dBaseCornerX = 1.0d-dSRLevel;
		dBaseCornerZ = dSRLevel;
		
		if (dSRLevel <= 0.5d) {
			dBaseCornerY = dSRLevel;
		}
		else if (dSRLevel <= 1.0d) {
			dBaseCornerY = 1.0d-dSRLevel;
		}
		else {
			for (int i=0; i<24; i++) {
				dCoordsList[i] = 0d;
			}
			FcPlanePolygon = new NDPolygon(8, 3, dCoordsList);
			return;
		}
		
		dBasicPoly[0] = dBaseCornerX;
		dBasicPoly[1] = dBaseCornerY;
		dBasicPoly[2] = dBaseCornerZ;
		
		dBasicPoly[3] = dBaseCornerY;
		dBasicPoly[4] = dBaseCornerX;
		dBasicPoly[5] = dBaseCornerZ;
		
		dBasicPoly[6] = -1d*dBaseCornerY;
		dBasicPoly[7] = dBaseCornerX;
		dBasicPoly[8] = dBaseCornerZ;
		
		dBasicPoly[9] = -1d*dBaseCornerX;
		dBasicPoly[10] = dBaseCornerY;
		dBasicPoly[11] = dBaseCornerZ;
		
		dBasicPoly[12] = -1d*dBaseCornerX;
		dBasicPoly[13] = -1d*dBaseCornerY;
		dBasicPoly[14] = dBaseCornerZ;
		
		dBasicPoly[15] = -1d*dBaseCornerY;
		dBasicPoly[16] = -1d*dBaseCornerX;
		dBasicPoly[17] = dBaseCornerZ;
		
		dBasicPoly[18] = dBaseCornerY;
		dBasicPoly[19] = -1d*dBaseCornerX;
		dBasicPoly[20] = dBaseCornerZ;
		
		dBasicPoly[21] = dBaseCornerX;
		dBasicPoly[22] = -1d*dBaseCornerY;
		dBasicPoly[23] = dBaseCornerZ;
		
		switch (iDim) {
		case 1: for (int i=0; i<24; i+=3) {
					dCoordsList[i] = dBasicPoly[i+2]*dScale + dCz;
					dCoordsList[i+1] = dBasicPoly[i]*dScale + dCx;
					dCoordsList[i+2] = dBasicPoly[i+1]*dScale + dCy;
				}
				break;
		case 2: for (int i=0; i<24; i+=3) {
					dCoordsList[i] = dBasicPoly[i+1]*dScale + dCy;
					dCoordsList[i+1] = dBasicPoly[i+2]*dScale + dCz;
					dCoordsList[i+2] = dBasicPoly[i]*dScale + dCx;
				}
				break;
		case 3:
		default: for (int i=0; i<24; i+=3) {
					dCoordsList[i] = dBasicPoly[i]*dScale + dCx;
					dCoordsList[i+1] = dBasicPoly[i+1]*dScale + dCy;
					dCoordsList[i+2] = dBasicPoly[i+2]*dScale + dCz;
				}
				break;
		}
				
		FcPlanePolygon = new NDPolygon(8, 3, dCoordsList);
		
	}

	public FCCPlanePolygon(double dCx, double dCy, double dCz, double dLevel, double dScale) {
		this( dCx,  dCy,  dCz,  3, dLevel,  dScale);		
	}

	public FCCPlanePolygon(int iDim, double dLevel) {
		this( 0d,  0d,  0d,  iDim, dLevel,  1d);		
	}

	public FCCPlanePolygon(double dLevel) {
		this( 0d,  0d,  0d,  3, dLevel,  1d);		
	}

	 //Returns FcPlanePolyhedron
		public NDPolygon getFcPlanePolygon( ) {
			return FcPlanePolygon;
		}
		
}
