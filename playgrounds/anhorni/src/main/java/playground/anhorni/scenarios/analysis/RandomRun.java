package playground.anhorni.scenarios.analysis;

public class RandomRun {
	private int id;
	private double expendituresPerLocation[];
	
	public RandomRun(int id, int numberOfCityShoppingLocs){
		this.id = id;
		this.expendituresPerLocation = new double[numberOfCityShoppingLocs];
	}
	
	public void addExpenditure(int locIndex, double expenditure) {
		this.expendituresPerLocation[locIndex] = expenditure;
	}
	
	public double getExpenditure(int locIndex) {
		return this.expendituresPerLocation[locIndex];
	}
	
	public int getId() {
		return this.id;
	}
}
