//Nico de Koker, University of Pretoria, August 2014

package playground.southafrica.sandboxes.ndekoker.FCCCentroid;

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