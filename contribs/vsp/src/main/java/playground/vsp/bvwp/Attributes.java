package playground.vsp.bvwp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;

class Attributes {
	private Map<Attribute,Double> aa = new HashMap<Attribute,Double>() ;
	double getByEntry( Attribute attr ) {
		Double dbl = aa.get(attr) ;
		if ( dbl != null ) {
			return aa.get(attr) ;
		} else {
			return 0. ;
		}
	}
	Double addEntry( Attribute attr, Double val ) {
		return aa.put( attr,  val ) ;
	}
	@Override
	public String toString() {
		StringBuilder strb = new StringBuilder() ;
		for ( Entry<Attribute,Double> entry : aa.entrySet() ) {
			strb.append( "\n\t" + entry.getKey() ) ;
			strb.append( "=" + entry.getValue() ) ; 
		}
		return strb.toString();
	}
}