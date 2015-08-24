//Nico de Koker, University of Pretoria, August 2014

package playground.southafrica.sandboxes.ndekoker.FCCCentroid;

class NDPolygon {

	// Data Members
	private GridPoint[] PolyFace;
	private int NCorners;

	//Constructor
	public NDPolygon(int nC, int nDim, double[] dCoordsList ) {
		
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
