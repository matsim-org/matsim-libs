package playground.toronto.analysis.tripchains;


public interface TripChainComponent extends Comparable<TripChainComponent> {
	public double getStartTime();
	public double getEndtime();
	public boolean overlaps(TripChainComponent t);
	public void finishComponent(double endTime);
}
