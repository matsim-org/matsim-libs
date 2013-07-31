package playground.vsp.bvwp2;

import playground.vsp.bvwp2.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp2.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp2.MultiDimensionalArray.Mode;

class Key /*implements Comparable<Key>*/ {
	private String delegate ; 
    
	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj.toString()) ;
	}
	
	@Override
	public int hashCode() {
		return delegate.hashCode() ;
	}
	
	private Key(Mode mode, Attribute attr, DemandSegment segm) {
		delegate = mode.toString() + "," + segm.toString() + "," + attr.toString() ;
	}

	static Key makeKey( Mode mode, DemandSegment segm, Attribute attr ) {
		return new Key( mode, attr, segm) ;
	}
	
	public String toString() {
		return delegate ;
	}

//	@Override
//	public int compareTo(Key arg0) {
//		return delegate.compareTo(arg0.toString()) ;
//	}
	
}