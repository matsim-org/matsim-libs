package vrp.algorithms.ruinAndRecreate.factories;

import java.util.Collection;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.api.RuinAndRecreateListener;
import vrp.api.SingleDepotVRP;
import vrp.basics.Tour;

public interface RuinAndRecreateFactory {

	/**
	 * Standard ruin and recreate without time windows. This algo is configured according to Schrimpf et. al (2000).
	 * @param vrp
	 * @param initialTours
	 * @param vehicleCapacity
	 * @return
	 */
	public abstract RuinAndRecreate createAlgorithm(SingleDepotVRP vrp, Collection<Tour> initialTours, int vehicleCapacity);

	public abstract void addRuinAndRecreateListener(RuinAndRecreateListener l);
	
	public abstract void setIterations(int iterations);
	
	public abstract void setWarmUp(int nOfWarmUpIterations);
}