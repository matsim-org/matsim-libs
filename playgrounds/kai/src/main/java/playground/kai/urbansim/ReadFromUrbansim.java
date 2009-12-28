/**
 * 
 */
package playground.kai.urbansim;

import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.world.Layer;

/**
 * Simple interface so that ReadFromUrbansimCellModel and ReadFromUrbansimParcelModel can be called via the same syntax.
 * Not used.
 * 
 * @author nagel
 *
 */
@Deprecated
public interface ReadFromUrbansim {
	

	public void readFacilities ( ActivityFacilitiesImpl facilities ) ;

	public void readPersons( PopulationImpl population, ActivityFacilitiesImpl facilities, double fraction ) ;
	
	public void readZones( ActivityFacilitiesImpl zones, Layer parcels ) ;
}
