/**
 * 
 */
package playground.kai.urbansim.ids;

import org.matsim.core.basic.v01.IdImpl;

/**
 * See comment under IdFactory
 * 
 * @author nagel
 *
 */
@Deprecated
public class ParcelId extends IdImpl {

	/**
	 * @param id
	 */
	public ParcelId(String id) {
		super(id);
	}

	/**
	 * @param id
	 */
	public ParcelId(long id) {
		super(id);
	}

}
