package vrp.algorithms.ruinAndRecreate.api;

import java.util.List;

import vrp.algorithms.ruinAndRecreate.basics.Shipment;
import vrp.algorithms.ruinAndRecreate.basics.Solution;


/**
 * 
 * @author stefan schroeder
 *
 */

public interface RuinStrategy {
	public void run(Solution initialSolution);
	
	public List<Shipment> getShipmentsWithoutService();
}
