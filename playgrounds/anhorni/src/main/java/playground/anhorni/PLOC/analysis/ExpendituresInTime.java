package playground.anhorni.PLOC.analysis;

public class ExpendituresInTime {
	
	private double expenditures[][] = new double[5][24];
	
	public ExpendituresInTime() {
		for (int i = 0; i < 24; i++) {
			for (int j = 0; j < 5; j++) {
				expenditures[j][i] = 0.0;
			}
		}
	}
	
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
