package playground.anhorni.surprice;

public class Utilities {
	private double[] utilitiesPerDay = new double[7];

	public double getUtilityPerDay(String day) {
		return this.utilitiesPerDay[Surprice.days.indexOf(day)];
	}

	public void setUtilityPerDay(String day, double utility) {
		this.utilitiesPerDay[Surprice.days.indexOf(day)] = utility;
	} 
}
