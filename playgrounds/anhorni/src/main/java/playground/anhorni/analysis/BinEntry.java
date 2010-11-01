package playground.anhorni.analysis;

public class BinEntry {
	
	private double value;
	private double weight;
	
	public BinEntry(double value, double weight) {
		this.value = value;
		this.weight = weight;
	}
	
	
	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
}
