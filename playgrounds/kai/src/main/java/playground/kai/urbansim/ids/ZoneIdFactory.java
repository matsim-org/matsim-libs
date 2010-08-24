/**
 * 
 */
package playground.kai.urbansim.ids;



/**
 * @author nagel
 *
 */
public class ZoneIdFactory implements IdFactory {

	/* (non-Javadoc)
	 * @see playground.kai.urbansim.IdBuilder#createId(java.lang.String)
	 */
	public ZoneId createId(String str) {
		return new ZoneId( str ) ;
	}

	/* (non-Javadoc)
	 * @see playground.kai.urbansim.IdBuilder#createId(long)
	 */
	public ZoneId createId(long ii) {
		return new ZoneId( ii ) ;
	}

}
