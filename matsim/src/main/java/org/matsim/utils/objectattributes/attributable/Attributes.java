package org.matsim.utils.objectattributes.attributable;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public final class Attributes {
	// could be memory-optimized by storing into an array of exactly the right size
	// do as a second step if functionality appears to be desirable
	final Map<String, Object> map = new IdentityHashMap<>(2);

	@Override
	public String toString() {
		StringBuilder stb = new StringBuilder() ;
		for ( Map.Entry<String,Object> ee : map.entrySet() ) {
			String subkey = ee.getKey();
			stb.append("{ key=").append(subkey);
			stb.append("; object=").append(ee.getValue().toString());
			stb.append( " }" );
		}
		return stb.toString() ;
	}

	public Object putAttribute( final String attribute, final Object value) {
		return map.put(attribute.intern(), value);
	}

	public Object getAttribute( final String attribute) {
		return map.get(attribute.intern());
	}

	public Object removeAttribute( final String attribute) {
		return map.remove(attribute.intern());
	}

	public void clear() {
		map.clear();
	}

}
