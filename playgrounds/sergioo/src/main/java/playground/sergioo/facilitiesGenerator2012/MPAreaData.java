package playground.sergioo.facilitiesGenerator2012;

public class MPAreaData {

	//Attributes
	private int id;
	private String type;
	private double maxArea;
	private double modeShare;

	//Methods
	public MPAreaData(int id, String type, double maxArea, double modeShare) {
		super();
		this.id = id;
		this.type = type;
		this.maxArea = maxArea;
		this.modeShare = modeShare;
	}
	public int getId() {
		return id;
	}
	public String getType() {
		return type;
	}
	public double getMaxArea() {
		return maxArea;
	}
	public double getModeShare() {
		return modeShare;
	}

}
