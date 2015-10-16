//Nico de Koker, University of Pretoria, August 2014

package playground.southafrica.sandboxes.ndekoker.FCCCentroid;


class FCCGrid {

	// Data Members
	private GridPoint[] FcGrid;
	private int NX,  NY,  NZ, NGrid;

	//Constructor
	public FCCGrid(double dXmin, double dXmax, double dYmin, double dYmax, double dZmin, double dZmax, double dScale ) {

		double dX0, dX1, dY0, dY1, dZ0, dZ1;
		
		// Determine grid consistent boundaries
		double dsXmin = dXmin / (2*dScale);
		double dsXmax = dXmax / (2*dScale);
		
		double dsYmin = dYmin / (2*dScale);
		double dsYmax = dYmax / (2*dScale);
		
		double dsZmin = dZmin / (2*dScale);
		double dsZmax = dZmax / (2*dScale);

		if (dsXmin < 0d)
			dX0 = Math.rint(dsXmin+0.5d);
		else if (dsXmin > 0d)
			dX0 = Math.rint(dsXmin-0.5d);
		else 
			dX0 = 0;
		
		if (dsYmin < 0d)
			dY0 = Math.rint(dsYmin+0.5d);
		else if (dsYmin > 0d)
			dY0 = Math.rint(dsYmin-0.5d);
		else 
			dY0 = 0;
		
		if (dsZmin < 0d)
			dZ0 = Math.rint(dsZmin+0.5d);
		else if (dsZmin > 0d)
			dZ0 = Math.rint(dsZmin-0.5d);
		else 
			dZ0 = 0;
		
		if (dsXmax < 0d)
			dX1 = Math.rint(dsXmax+0.5d);
		else if (dsXmax > 0d)
			dX1 = Math.rint(dsXmax-0.5d);
		else 
			dX1 = 0;
		
		if (dsYmax < 0d)
			dY1 = Math.rint(dsYmax+0.5d);
		else if (dsYmax > 0d)
			dY1 = Math.rint(dsYmax-0.5d);
		else 
			dY1 = 0;
		
		if (dsZmax < 0d)
			dZ1 = Math.rint(dsZmax+0.5d);
		else if (dsZmax > 0d)
			dZ1 = Math.rint(dsZmax-0.5d);
		else 
			dZ1 = 0;
		
		// Size the grid, careful with NGrid
		NX = (int) (dX1-dX0+1);
		NY = (int) (dY1-dY0+1);
		NZ = (int) (dZ1-dZ0+1);
		NGrid = NX*NY*NZ;
		
		FcGrid = new GridPoint[NGrid*4];
		
		// Generate the basic grid
		int nCount = 0;
		for (int k = (int)dZ0; k<= (int)dZ1; k++) {
			for (int j = (int)dY0; j<= (int)dY1; j++) {
				for (int i = (int)dX0; i<= (int)dX1; i++) {
					FcGrid[nCount] = new GridPoint((double)i*2, (double)j*2, (double)k*2);					
					nCount++;
				}
			}
		}

		// Translate through unit cell generating vector1
		for (nCount=0; nCount < NGrid; nCount++) {
			FcGrid[nCount+1*NGrid] = new GridPoint(FcGrid[nCount].getX()+1, FcGrid[nCount].getY()+1, FcGrid[nCount].getZ()+0);
		}
		
		// Translate through unit cell generating vector2
		for (nCount=0; nCount < NGrid; nCount++) {
			FcGrid[nCount+2*NGrid] = new GridPoint(FcGrid[nCount].getX()+0, FcGrid[nCount].getY()+1, FcGrid[nCount].getZ()+1);
		}

		// Translate through unit cell generating vector3
		for (nCount=0; nCount < NGrid; nCount++) {
			FcGrid[nCount+3*NGrid] = new GridPoint(FcGrid[nCount].getX()+1, FcGrid[nCount].getY()+0, FcGrid[nCount].getZ()+1);
		}
		
		// Scale the grid
		NGrid = NGrid*4;
		for (nCount=0; nCount < NGrid; nCount++) {
			FcGrid[nCount].setX(FcGrid[nCount].getX()*dScale);
			FcGrid[nCount].setY(FcGrid[nCount].getY()*dScale);
			FcGrid[nCount].setZ(FcGrid[nCount].getZ()*dScale);
		}
		
		// Rotate the grid.  Add later
	}

 //Returns FcGrid
	public GridPoint[] getFcGrid( ) {
		return FcGrid;
	}
	
 //Returns nX
	public int getNX( ) {
		return NX;
	}
	
 //Returns nY
	public int getNY( ) {
		return NY;
	}
 //Returns nZ
	public int getNZ( ) {
		return NZ;
	}
 //Returns nGrid
	public int getNGrid( ) {
		return NGrid;
	}
	
}
