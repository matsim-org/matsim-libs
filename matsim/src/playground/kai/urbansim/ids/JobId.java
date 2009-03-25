package playground.kai.urbansim.ids;

import org.matsim.core.basic.v01.IdImpl;

/**
 * See comment under IdFactory
 * 
 * @author nagel
 *
 */
@Deprecated
public class JobId extends IdImpl {

	public JobId(String id) {
		super(id);
	}

	public JobId(long id) {
		super(id);
	}

}
