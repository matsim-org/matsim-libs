package playground.wdoering.debugvisualization.model;

public class XYVxVyDataPoint extends DataPoint {
	
	private Double vX;
	private Double vY;

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
