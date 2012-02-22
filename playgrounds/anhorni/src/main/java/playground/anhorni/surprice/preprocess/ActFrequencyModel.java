package playground.anhorni.surprice.preprocess;

public class ActFrequencyModel {
	
	private String type;
	private double weekFrequency;
		
	public ActFrequencyModel(String type, double weekFrequency) {
		this.type = type;
		this.weekFrequency = weekFrequency;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double getWeekFrequency() {
		return this.weekFrequency;
	}
}
