package playground.kai.urbansim.ids;

import org.matsim.core.basic.v01.IdImpl;

/**
 * See comment under IdFactory
 * 
 * @author nagel
 *
 */
@Deprecated
public class LocationId extends IdImpl {

	public LocationId(String id) {
		super(id);
	}

	public LocationId(long id) {
		super(id);
	}

}
