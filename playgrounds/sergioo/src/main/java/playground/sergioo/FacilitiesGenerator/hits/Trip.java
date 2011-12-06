package playground.sergioo.FacilitiesGenerator.hits;

public class Trip {
	
	//Attributes
	private String purpose;
	private Integer startTime;
	private Integer endTime;
	
	//Constructors
	public Trip(String purpose, Integer startTime, Integer endTime) {
		super();
		this.purpose = purpose;
		this.startTime = startTime;
		this.endTime = endTime;
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
	
}
