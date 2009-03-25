/**
 * 
 */
package playground.kai.urbansim;

import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.population.Population;
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
	

	public void readFacilities ( Facilities facilities ) ;

	public void readPersons( Population population, Facilities facilities, double fraction ) ;
	
	public void readZones( Facilities zones, Layer parcels ) ;
}
