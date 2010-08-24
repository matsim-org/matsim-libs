/**
 * 
 */
package playground.kai.urbansim.ids;



/**
 * @author nagel
 *
 */
public class ParcelIdFactory implements IdFactory {

	/* (non-Javadoc)
	 * @see playground.kai.urbansim.IdBuilder#createId(java.lang.String)
	 */
	public ParcelId createId(String str) {
		return new ParcelId( str ) ;
	}

	/* (non-Javadoc)
	 * @see playground.kai.urbansim.IdBuilder#createId(long)
	 */
	public ParcelId createId(long ii) {
		return new ParcelId( ii ) ;
	}

}
