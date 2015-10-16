//Nico de Koker, University of Pretoria, August 2014


package playground.southafrica.sandboxes.ndekoker.FCCCentroid;

class FCCPolyhedron {

	// Data Members
	private NDPolygon[] FcPolyhedron;
	private final int NFaces;

	//Constructors
	public FCCPolyhedron(double dCx, double dCy, double dCz, double dScale) {
		
		NFaces = 12;
		
		FcPolyhedron = new NDPolygon[NFaces];
		
		double[] dBasicPoly = new double[12];
		double[] dCoordsList = new double[12];
		
		// Basic +x+y unit polygon face
		dBasicPoly[0]  = 1;
		dBasicPoly[1]  = 0;
		dBasicPoly[2]  = 0;
		
		dBasicPoly[3]  = 1/2d;
		dBasicPoly[4]  = 1/2d;
		dBasicPoly[5]  = 1/2d;
		
		dBasicPoly[6]  = 0;
		dBasicPoly[7]  = 1;
		dBasicPoly[8]  = 0;
		
		dBasicPoly[9]  = 1/2d;
		dBasicPoly[10] = 1/2d;
		dBasicPoly[11] = -1/2d;
		
		// Face +x+y		
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] = dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[0] = new NDPolygon(4, 3, dCoordsList );

		// Face +x-y		
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] = dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = -1*dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[1] = new NDPolygon(4, 3, dCoordsList );

		// Face -x+y
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] = -1*dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[2] = new NDPolygon(4, 3, dCoordsList );

		// Face -x-y
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] = -1*dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = -1*dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[3] = new NDPolygon(4, 3, dCoordsList );

		// Face +x+z unit polygon face
		dBasicPoly[0]  = 1;
		dBasicPoly[1]  = 0;
		dBasicPoly[2]  = 0;
		
		dBasicPoly[3]  = 1/2d;
		dBasicPoly[4]  = -1/2d;
		dBasicPoly[5]  = 1/2d;
		
		dBasicPoly[6]  = 0;
		dBasicPoly[7]  = 0;
		dBasicPoly[8]  = 1;
		
		dBasicPoly[9]  = 1/2d;
		dBasicPoly[10] = 1/2d;
		dBasicPoly[11] = 1/2d;
		
		// Face +x+z
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] =   dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[4] = new NDPolygon(4, 3, dCoordsList );

		// Face +x-z
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] =   dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = -1*dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[5] = new NDPolygon(4, 3, dCoordsList );

		// Face -x+z
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] = -1*dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[6] = new NDPolygon(4, 3, dCoordsList );

		// Face -x-z
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] = -1*dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = -1*dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[7] = new NDPolygon(4, 3, dCoordsList );

		// Face +y+z unit polygon face
		dBasicPoly[0]  = 0;
		dBasicPoly[1]  = 1;
		dBasicPoly[2]  = 0;
		
		dBasicPoly[3]  = 1/2d;
		dBasicPoly[4]  = 1/2d;
		dBasicPoly[5]  = 1/2d;
		
		dBasicPoly[6]  = 0;
		dBasicPoly[7]  = 0;
		dBasicPoly[8]  = 1;
		
		dBasicPoly[9]  = -1/2d;
		dBasicPoly[10] = 1/2d;
		dBasicPoly[11] = 1/2d;
		
		// Face +y+z
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] = dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[8] = new NDPolygon(4, 3, dCoordsList );

		// Face +y-z
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] = dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = -1*dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[9] = new NDPolygon(4, 3, dCoordsList );

		// Face -y+z
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] = dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = -1*dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[10] = new NDPolygon(4, 3, dCoordsList );

		// Face -y-z
		for (int i=0; i<12; i+=3) {
			dCoordsList[i] = dBasicPoly[i]*dScale + dCx;
			dCoordsList[i+1] = -1*dBasicPoly[i+1]*dScale + dCy;
			dCoordsList[i+2] = -1*dBasicPoly[i+2]*dScale + dCz;
		}
		FcPolyhedron[11] = new NDPolygon(4, 3, dCoordsList );

	}
	
	public FCCPolyhedron(double dScale) {
		this(0d, 0d, 0d, dScale);
	}

	public FCCPolyhedron() {
		this(0d, 0d, 0d, 1d);
	}
	
 //Returns FcPolyhedron
	public NDPolygon[] getFcPolyhedron( ) {
		return FcPolyhedron;
	}
	
 //Returns NFaces
	public int getNFaces( ) {
		return NFaces;
	}

}
