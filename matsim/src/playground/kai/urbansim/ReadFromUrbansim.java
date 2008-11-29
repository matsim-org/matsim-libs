/**
 * 
 */
package playground.kai.urbansim;

import org.matsim.basic.v01.BasicPopulation;
import org.matsim.facilities.Facilities;
import org.matsim.population.Population;
import org.matsim.world.Layer;

/**
 * Simple interface so that ReadFromUrbansimCellModel and ReadFromUrbansimParcelModel can be called via the same syntax.
 * 
 * @author nagel
 *
 */
public interface ReadFromUrbansim {
	

	public void readFacilities ( Facilities facilities ) ;

	public void readPersons( Population population, Facilities facilities, double fraction ) ;
	
	public void readZones( Facilities zones, Layer parcels ) ;
}
