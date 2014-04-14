package playground.vsp.demandde.freightChainsFromKiD;

public class Pwvm_Trip {
	int logbookId;
	int tripId;
	int tripNumber = 0;
	String sourcePLZ;
	String destPLZ;
	Pwvm_Purpose purpose;
	
	
	public Pwvm_Trip(int tripNumber,
			int logbookId, int tripId, String sourcePLZ, String destPLZ, Pwvm_Purpose purpose) {
		this.tripNumber = tripNumber;
		this.logbookId = logbookId;
		this.tripId = tripId;
		this.sourcePLZ = sourcePLZ;
		this.destPLZ = destPLZ;
		this.purpose = purpose;
	}
	
	public void printTripData(){
		System.out.printf("Fahrzeg %d, Fahrtid %d, Fahrtnummer %d%n", logbookId, tripId, tripNumber);
	}

	public int getLogbookId() {
		return logbookId;
	}


	public int getTripId() {
		return tripId;
	}


	public int getTripNumber() {
		return tripNumber;
	}


	public void setTripNumber(int tripNumber) {
		this.tripNumber = tripNumber;
	}

	public String getSourcePLZ() {
		return sourcePLZ;
	}


	public String getDestPLZ() {
		return destPLZ;
	}


	public Pwvm_Purpose getPurpose() {
		return purpose;
	}
	
}
