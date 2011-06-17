/**
 * 
 */
package vrp.api;


import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author stefan schroeder
 *
 */
public interface VRP { 
	
	public Id getDepotId();	
	
	public Constraints getConstraints();
	
	public Costs getCosts();
	
	public Customer getDepot();
	
	public Map<Id,Customer> getCustomers(); //inclusive depot
	
}
