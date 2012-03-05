package playground.wdoering.debugvisualization.model;

public class XYVxVyDataPoint extends DataPoint {
	
	private Double vX;
	private Double vY;

	public Double getvX() {
		return vX;
	}

	public void setvX(Double vX) {
		this.vX = vX;
	}

	public Double getvY() {
		return vY;
	}

	public void setvY(Double vY) {
		this.vY = vY;
	}

	public XYVxVyDataPoint(Double posX, Double posY) {
		super(posX, posY);
		// TODO Auto-generated constructor stub
	}
	

	public XYVxVyDataPoint(Double time, Double posX, Double posY, double vX,
			double vY) {
		super(time,posX,posY);
		this.vX = vX;
		this.vY = vY;
		
		// TODO Auto-generated constructor stub
	}

}
