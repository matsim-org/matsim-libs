/**
 * 
 */
package playground.kai.urbansim.ids;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;

/**
 * @author nagel
 *
 */
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
