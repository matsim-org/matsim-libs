package opdytsintegration;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RecursiveCountAverage {

	// -------------------- MEMBERS --------------------

	private double initialTime;

	private double lastTime;

	private int lastValue;

	private double averageValue;

	// -------------------- CONSTRUCTION --------------------

	public RecursiveCountAverage(final double initialTime) {
		this.reset(initialTime);
	}

	// -------------------- IMPLEMENTATION --------------------

	public void reset(final double time) {
		this.initialTime = time;
		this.lastTime = time;
		this.lastValue = 0;
		this.averageValue = 0.0;
	}

	public void advanceTo(final double time) {
		if (time < this.lastTime) {
			throw new IllegalArgumentException("current time " + time
					+ " is before last time " + this.lastTime);
		}
		final double innoWeight = (time - this.lastTime)
				/ Math.max(1e-8, time - this.initialTime);
		this.averageValue = (1.0 - innoWeight) * this.averageValue + innoWeight
				* this.lastValue;
		this.lastTime = time;
	}

	public void inc(final double time) {
		this.advanceTo(time);
		this.lastValue++;
	}

	public void dec(final double time) {
		this.advanceTo(time);
		this.lastValue--;
	}

	public double getAverage() {
		return this.averageValue;
	}

	public double getInitialTime() {
		return this.initialTime;
	}

	public double getFinalTime() {
		return this.lastTime;
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		final RecursiveCountAverage avg = new RecursiveCountAverage(0.0);
		avg.inc(1.0);
		avg.inc(2.0);
		avg.dec(4.0);
		avg.dec(5.0);
		avg.advanceTo(6.0);
		System.out.println("Average between " + avg.getInitialTime() + " and "
				+ avg.getFinalTime() + " is " + avg.getAverage());
	}
}
