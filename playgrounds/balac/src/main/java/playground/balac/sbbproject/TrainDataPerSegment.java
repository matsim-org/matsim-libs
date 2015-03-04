package playground.balac.sbbproject;

public class TrainDataPerSegment {
	private int trainNumber;
	private String type;
	private String didokFrom;
	private String didokTo;
	private String arrivalTime;
	private String departureTime;
	public TrainDataPerSegment(int trainNumber, String type, String didokFrom, String didokTo, String arrivalTime, String departureTime) {
		
		this.trainNumber = trainNumber;
		this.type = type;
		this.didokFrom = didokFrom;
		this.didokTo = didokTo;
		this.arrivalTime = arrivalTime;
		this.departureTime = departureTime;
		
	}
	public int getTrainNumber() {
		return trainNumber;
	}
	public String getType() {
		return type;
	}
	public String getDidokFrom() {
		return didokFrom;
	}
	public String getDidokTo() {
		return didokTo;
	}
	public String getArrivalTime() {
		return arrivalTime;
	}
	public String getDepartureTime() {
		return departureTime;
	}

}
