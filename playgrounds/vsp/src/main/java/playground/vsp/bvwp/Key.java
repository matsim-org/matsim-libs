package playground.vsp.bvwp;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.ChangeType;
import playground.vsp.bvwp.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp.MultiDimensionalArray.Mode;

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
	
	private Key(Mode mode, Attribute attr, DemandSegment segm, ChangeType ct) {
		ChangeType ctt;
		if (ct==null) {ctt = ChangeType.INDUZIERT
				;
		}
		else {ctt = ct ;
		
		}
		
		delegate = mode.toString() + "," + segm.toString() + "," + attr.toString() +"," + ctt ;
	}

	static Key makeKey( Mode mode, DemandSegment segm, Attribute attr, ChangeType ct ) {
		return new Key( mode, attr, segm, ct) ;
	}
	
	static Key makeKey( Mode mode, DemandSegment segm, Attribute attr) {
		return new Key( mode, attr, segm, null) ;
	}
	
	public String toString() {
		return delegate ;
	}

//	@Override
//	public int compareTo(Key arg0) {
//		return delegate.compareTo(arg0.toString()) ;
//	}
	
}