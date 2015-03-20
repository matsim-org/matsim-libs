package playground.balac.sbbproject;

public class TrainDataPerSegment {
	private int trainNumber;
	private String type;
	private String didokFrom;
	private String didokTo;
	private String arrivalTime;
	private String departureTime;
	private Double occupancy1;
	private Double occupancy2;

	public TrainDataPerSegment(int trainNumber, String type, String didokFrom, String didokTo, String arrivalTime,
			String departureTime, String occupancy1, String occupancy2) {
		
		this.trainNumber = trainNumber;
		this.type = type;
		this.didokFrom = didokFrom;
		this.didokTo = didokTo;
		
		String[] arr = arrivalTime.split(" ");
		if (arr.length == 2)
			this.arrivalTime = arr[1];
		else
			this.arrivalTime = arr[0];
		arr = departureTime.split(" ");
		if (arr.length == 2)
			this.departureTime = arr[1];
		else
			this.departureTime = arr[0];
		this.occupancy1 = Double.parseDouble(occupancy1);
		this.occupancy2 = Double.parseDouble(occupancy2);

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
	public double getOccupancy1() {
		
		return occupancy1;
	}
	
	public double getOccupancy2() {
		
		return occupancy2;
	}

}
