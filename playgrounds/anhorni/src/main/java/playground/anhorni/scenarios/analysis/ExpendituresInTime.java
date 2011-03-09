package playground.anhorni.scenarios.analysis;

public class ExpendituresInTime {
	
	private double expenditures[][] = new double[5][24];
	
	public void add(int day, int hour, double expenditure) {
		this.expenditures[day][hour] = expenditure;
	}
	
	public double getExpenditure(int day, int hour) {
		return this.expenditures[day][hour];
	}
	
	public double getAverageHourlyExpenditures(int hour) {
		double avg = 0.0;
		for (int day = 0; day < 5; day++) {
			avg += this.expenditures[day][hour];
		}
		avg /= 5.0;
		return avg;
	}
}
