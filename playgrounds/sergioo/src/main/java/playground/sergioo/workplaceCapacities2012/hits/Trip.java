package playground.sergioo.workplaceCapacities2012.hits;

public class Trip {
	
	//Attributes
	private String purpose;
	private Integer startTime;
	private Integer endTime;
	private String placeType;
	
	//Constructors
	public Trip(String purpose, Integer startTime, Integer endTime, String placeType) {
		super();
		this.purpose = purpose;
		this.startTime = startTime;
		this.endTime = endTime;
		this.placeType = placeType;
	}
	
	//Methods
	
	public String getPurpose() {
		return purpose;
	}
	public Integer getStartTime() {
		return startTime;
	}
	public Integer getEndTime() {
		return endTime;
	}
	public String getPlaceType() {
		return placeType;
	}
	
}
