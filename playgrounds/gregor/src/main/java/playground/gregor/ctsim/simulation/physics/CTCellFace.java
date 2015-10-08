package playground.gregor.ctsim.simulation.physics;

public class CTCellFace {

	final double x0;
	final double x1;
	final double y1;
	final double y0;
	final CTCell nb;
	final double h_i; //direction of nb

	public CTCellFace(double x0, double y0, double x1, double y1, CTCell neighbor, double h_i) {
		this.x0 = x0;
		this.x1 = x1;
		this.y1 = y1;
		this.y0 = y0;
		this.nb = neighbor;
		this.h_i = h_i;
	}
	
}
