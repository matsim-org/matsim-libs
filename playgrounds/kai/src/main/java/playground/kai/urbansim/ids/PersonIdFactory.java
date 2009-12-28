/**
 * 
 */
package playground.kai.urbansim.ids;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * @author nagel
 *
 */
public class PersonIdFactory implements IdFactory {
	private static final Logger log = Logger.getLogger(PersonIdFactory.class);

	/* (non-Javadoc)
	 * @see playground.kai.urbansim.ids.IdFactory#createId(java.lang.String)
	 */
	public PersonId createId(String str) {
		return new PersonId( str ) ;
	}

	/* (non-Javadoc)
	 * @see playground.kai.urbansim.ids.IdFactory#createId(long)
	 */
	public PersonId createId(long ii) {
		return new PersonId( ii ) ;
	}
}
