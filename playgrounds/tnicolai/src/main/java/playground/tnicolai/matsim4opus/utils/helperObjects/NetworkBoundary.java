package playground.tnicolai.matsim4opus.utils.helperObjects;


public class NetworkBoundary {
	
	private double xmin;
	private double ymin;
	private double xmax;
	private double ymax;
	
	/**
	 * constructor
	 * @param xmin
	 * @param xmax
	 */
	public NetworkBoundary(final double xmin, final double xmax, final double ymin, final double ymax){
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
	}
	
	public double getMaxX(){
		return this.xmax;
	}
	public double getMinX(){
		return this.xmin;
	}
	public double getMaxY(){
		return this.ymax;
	}
	public double getMinY(){
		return this.ymin;
	}
}
