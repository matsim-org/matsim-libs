package org.matsim.utils.objectattributes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ObjectAttributeUtils2 {

	private ObjectAttributeUtils2() {
	}

	public static List<String> allObjectKeys(
			final ObjectAttributes objectAttributes) {
		return new ArrayList<String>(objectAttributes.attributes.keySet());
	}

	public static List<String> allAttributeKeys(final ObjectAttributes objectAttributes) {
		final Set<String> result = new LinkedHashSet<String>();
		for (String objectKey : allObjectKeys(objectAttributes)) {
			result.addAll(ObjectAttributesUtils.getAllAttributeNames(
					objectAttributes, objectKey));
		}
		return new ArrayList<String>(result);
	}
}
