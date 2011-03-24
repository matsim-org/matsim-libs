package playground.anhorni.PLOC.analysis;

public class Run {
	private int id;
	private ExpendituresInTime[] facilityExpenditures;
	
	public Run(int id, int numberOfFacilities){
		this.id = id;
		this.facilityExpenditures = new ExpendituresInTime[numberOfFacilities];
		
		for (int i = 0; i < numberOfFacilities; i++) {
			this.facilityExpenditures[i] = new ExpendituresInTime();
		}
	}
	
	public void addExpenditure(int facIndex, int day, int hour, double expenditure) {
		this.facilityExpenditures[facIndex].add(day, hour, expenditure);
	}
	
	public double getExpenditure(int facIndex, int day, int hour) {
		return this.facilityExpenditures[facIndex].getExpenditure(day, hour);
	}
	
	public double getAverageHourlyExpenditures(int facIndex, int hour) {
		return this.facilityExpenditures[facIndex].getAverageHourlyExpenditures(hour);
	}
	
	public int getId() {
		return this.id;
	}
}
