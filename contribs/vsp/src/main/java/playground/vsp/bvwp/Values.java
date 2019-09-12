package playground.vsp.bvwp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp.MultiDimensionalArray.Mode;
import static playground.vsp.bvwp.Key.*;

/**
 * @author nagel
 *
 */
class Values {
	private Map<Key,Double> m = new HashMap<Key,Double>() ;
	Double get( Key key ) {
		return m.get( key);
	}
	Double put( Key key, Double value ){
		return m.put( key,  value) ;
	}
	Double inc( Key key, double value ) {
		value += m.get(key); 
		return m.put( key, value ) ;
	}
	Values createDeepCopy() {
		Values n = new Values() ;
		for ( Entry<Key,Double> entry : m.entrySet() ) {
			n.put( entry.getKey() , entry.getValue() ) ;
		}
		return n ;
	}

	/**
	 * Convenience class, not strictly necessary:
	 */
	Attributes getAttributes( Mode mode, DemandSegment segm ) {
		Attributes attrs = new Attributes() ;
		for ( Attribute attr : Attribute.values() ) {
			attrs.addEntry( attr, this.get( makeKey( mode, segm, attr) ) ) ;
		}
		return attrs ;
	}
	
	public String toString() {
		StringBuilder strb = new StringBuilder() ;
		for ( Entry<Key,Double> entry : m.entrySet() ) {
			strb.append( "\n\t" + entry.getKey().toString() + "=" + entry.getValue() ) ;
		}
		return strb.toString();
	}
}