package playground.gregor.ctsim.simulation.physics;

public class CTCellFace {

	double x0;
	double x1;
	double y1;
	double y0;
	CTCell nb;

	public CTCellFace(double x0, double y0, double x1, double y1, CTCell neighbor) {
		this.x0 = x0;
		this.x1 = x1;
		this.y1 = y1;
		this.y0 = y0;
		this.nb = neighbor;
	}
	
}
