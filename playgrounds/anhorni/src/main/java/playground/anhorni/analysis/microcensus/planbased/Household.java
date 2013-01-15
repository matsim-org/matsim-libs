package playground.anhorni.analysis.microcensus.planbased;

public class Household {
	private int id;
	private int hhIncome;
	private int size;
	
	public Household(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	public int getHhIncome() {
		return hhIncome;
	}
	public void setId(int id) {
		this.id = id;
	}
	public void setHhIncome(int hhIncome) {
		this.hhIncome = hhIncome;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
}
