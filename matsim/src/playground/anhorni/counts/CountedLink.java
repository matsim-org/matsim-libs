package playground.anhorni.counts;

public class CountedLink {
	
	private String id;
	
	private double counts [] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
	private double sims [] = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public double[] getCounts() {
		return counts;
	}
	public void setCount(int hour, double count) {
		this.counts[hour] = count;
	}
	public void setSim(int hour, double sim) {
		this.sims[hour] = sim;
	}
	public double[] getSims() {
		return sims;
	}
}
