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
public class ZoneId extends IdImpl {

	/**
	 * @param id
	 */
	public ZoneId(String id) {
		super(id);
	}

	/**
	 * @param id
	 */
	public ZoneId(long id) {
		super(id);
	}

}
