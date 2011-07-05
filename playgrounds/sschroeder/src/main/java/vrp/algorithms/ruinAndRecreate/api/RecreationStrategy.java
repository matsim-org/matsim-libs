package vrp.algorithms.ruinAndRecreate.api;

import java.util.List;

import vrp.algorithms.ruinAndRecreate.basics.Shipment;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.algorithms.ruinAndRecreate.recreation.RecreationListener;


/**
 * 
 * @author stefan schroeder
 *
 */

public interface RecreationStrategy {
	
	public void run(Solution tentativeSolution, List<Shipment> itemsWithoutService);
	
	public void addListener(RecreationListener l);

}
