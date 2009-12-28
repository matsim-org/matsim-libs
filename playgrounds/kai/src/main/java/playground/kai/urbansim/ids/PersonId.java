/**
 * 
 */
package playground.kai.urbansim.ids;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;

/**
 * See comment under IdFactory
 * 
 * @author nagel
 *
 */
@Deprecated
public class PersonId extends IdImpl {
	private static final Logger log = Logger.getLogger(PersonId.class);

	/**
	 * @param id
	 */
	public PersonId(String id) {
		super(id);
	}

	/**
	 * @param id
	 */
	public PersonId(long id) {
		super(id);
	}
}
