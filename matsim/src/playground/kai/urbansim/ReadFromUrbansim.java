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
	

	/**
	 * This path has this weird level of indirection (../opus) so that the package can also be called from within
	 * matsim if it is checked out at the same hierarchy.  Very useful for debugging. 
	 */
	public static final String PATH_TO_OPUS_MATSIM = "../opus/opus_matsim/" ;

	public void readFacilities ( Facilities facilities ) ;

	public void readPersons( Population population, Facilities facilities, double fraction ) ;
	
	public void readZones( Facilities zones, Layer parcels ) ;
}
