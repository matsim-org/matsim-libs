package playground.anhorni.PLOC.analysis;


public class Run {
	private int id;
	private ExpendituresInTime[] facilityExpenditures;
	
	private double[][] arrivals = new double[5][24];
	private double[][] departures = new double[5][24];
	
	public Run(int id, int numberOfFacilities){
		this.id = id;
		this.facilityExpenditures = new ExpendituresInTime[numberOfFacilities];
		
		for (int i = 0; i < numberOfFacilities; i++) {
			this.facilityExpenditures[i] = new ExpendituresInTime();
		}
	}
	
	public void addDepartures(int day, int time, double deps) {
		int hour = (int)(time / 3600.0);
		if (hour < 24) {
			this.departures[day][hour] += deps;
		}
	}
	
	public void addArrivals(int day, int time, double arrs) {
		int hour = (int)(time / 3600.0);
		if (hour < 24) {
			this.arrivals[day][hour] += arrs;
		}
	}
	
	public void addTotalExpenditure(int facIndex, int day, int hour, double expenditure) {
		this.facilityExpenditures[facIndex].add(day, hour, expenditure);
	}
	
	public double getTotalExpenditure(int facIndex, int day, int hour) {
		return this.facilityExpenditures[facIndex].getExpenditure(day, hour);
	}
	
	public double getAvgDays_ExpendituresPerHourPerFacility(int facIndex, int hour) {
		return this.facilityExpenditures[facIndex].getAvgDays_ExpendituresPerHour(hour);
	}
	
	public int getId() {
		return this.id;
	}

	public double[][] getArrivals() {
		return arrivals;
	}
	public double[][] getDepartures() {
		return departures;
	}
}
