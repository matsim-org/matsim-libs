package playground.anhorni.locationchoice.analysis.facilities;


public class CapacityPerNOGAType {

	private String type;
	private double sumCapacity = 0.0;
	private double minCapacity = 999999999999999999999.0;
	private double maxCapacity = -1.0;
	private int cnt = 0;
	
	public void addCapacity(double capacity) {
		this.cnt++;
		this.sumCapacity += Math.max(capacity, 1.0);
		if (capacity > maxCapacity) {
			maxCapacity = capacity; 
		}
		if (capacity < minCapacity) {
			minCapacity = Math.max(capacity, 1.0); 
		}
	}
	public CapacityPerNOGAType(String type) {
		this.type = type;
	}

	public double getSumCapacity() {
		return sumCapacity;
	}
	public void setSumCapacity(double sumCapacity) {
		this.sumCapacity = sumCapacity;
	}
	public double getMinCapacity() {
		return minCapacity;
	}
	public void setMinCapacity(double minCapacity) {
		this.minCapacity = minCapacity;
	}
	public double getMaxCapacity() {
		return maxCapacity;
	}
	public void setMaxCapacity(double maxCapacity) {
		this.maxCapacity = maxCapacity;
	}
	public int getCnt() {
		return cnt;
	}
	public void setCnt(int cnt) {
		this.cnt = cnt;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
}
