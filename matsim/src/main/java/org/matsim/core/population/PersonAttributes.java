package org.matsim.core.population;

import org.matsim.utils.objectattributes.ObjectAttributes;

@Deprecated // for refactoring only
public class PersonAttributes extends ObjectAttributes{
	@Override public Object getAttribute( String personId, String key) {
		throw new RuntimeException( msg("getAttribute") ) ;
	}
	@Override public Object putAttribute( String personId, String key, Object value) {
		throw new RuntimeException( msg("putAttribute")) ;
	}
	@Override public Object removeAttribute( String personId, String key ) {
		throw new RuntimeException( msg("removeAttribute") ) ;
	}
	@Override public void removeAllAttributes( String personId ) {
		throw new RuntimeException( msg("removeAllAttributes") ) ;
	}

	// for retrofitting.  Called from PopulationUtils only.  Remove eventually.  kai, may'19
	Object getAttributeDirectly( String personId, String key ){
		return super.getAttribute( personId, key ) ;
	}
	Object removeAttributeDirectly( String personId, String key ){
		return super.removeAttribute( personId, key ) ;
	}
	void removeAllAttributesDirectly( String personId ) {
		super.removeAllAttributes( personId );
	}

	private static String msg (String str) {
		return "population.getPersonAttributes()." + str + " will be deprecated; use PopulationUtils" +
				   ".get/put/...PersonAttribute... instead.  kai, may'19" ;
	}
}
