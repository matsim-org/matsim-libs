/**
 * 
 */
package playground.kai.urbansim;

import org.matsim.interfaces.basic.v01.BasicPopulation;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Population;
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
