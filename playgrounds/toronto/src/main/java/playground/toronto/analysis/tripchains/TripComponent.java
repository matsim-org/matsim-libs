package playground.toronto.analysis.tripchains;


public interface TripComponent extends Comparable<TripComponent> {
	public double getStartTime();
	public double getEndtime();
	public boolean overlaps(TripComponent t);
	public void finishComponent(double endTime);
}
