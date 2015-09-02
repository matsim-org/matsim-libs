package gunnar.ihop2.regent.unused;

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.utils.objectattributes.ObjectAttributes;

@Deprecated
public class MyObjectAttributes extends ObjectAttributes {

	final Set<String> allObjectIds = new LinkedHashSet<String>();
	
	final Set<String> allAttributes = new LinkedHashSet<String>();
	
	@Override
	public Object putAttribute(final String objectId, final String attribute, final Object value) {

		this.allObjectIds.add(objectId);
		this.allAttributes.add(attribute);
		
		return super.putAttribute(objectId, attribute, value);
	}

}
