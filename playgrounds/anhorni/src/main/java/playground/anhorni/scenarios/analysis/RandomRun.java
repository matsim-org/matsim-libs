package playground.anhorni.scenarios.analysis;

public class RandomRun {
	private int id;
	private double expendituresPerLocation[];
	
	public RandomRun(int id, int numberOfShoppingLocs){
		this.id = id;
		this.expendituresPerLocation = new double[numberOfShoppingLocs];
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
