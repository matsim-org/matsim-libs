package org.matsim.utils.objectattributes;

import gunnar.ihop2.utils.FractionalIterable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

	public static List<String> allAttributeKeys(
			final ObjectAttributes objectAttributes) {
		final Set<String> result = new LinkedHashSet<String>();
		for (String objectKey : allObjectKeys(objectAttributes)) {
			result.addAll(ObjectAttributesUtils.getAllAttributeNames(
					objectAttributes, objectKey));
		}
		return new ArrayList<String>(result);
	}

	public static ObjectAttributes newFractionalSubset(
			final ObjectAttributes parent, final double fraction) {
		final ObjectAttributes subset = new ObjectAttributes();
		for (Map.Entry<String, Map<String, Object>> objKey2attrs : new FractionalIterable<>(
				parent.attributes.entrySet(), fraction)) {
			subset.attributes.put(objKey2attrs.getKey(),
					objKey2attrs.getValue());
		}
		return subset;
	}
}
