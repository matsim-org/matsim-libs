package org.matsim.contrib.dvrp.load;

import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface DvrpLoadType {
	DvrpLoad fromMap(Map<String, Number> map);

	DvrpLoad getEmptyLoad();

	List<String> getDimensions();

	int size();

	DvrpLoad deserialize(String representation);

	String serialize(DvrpLoad load);

	/**
	 * This method is mainly used in unit tests to quickly set up loads
	 */
	static DvrpLoad fromArray(DvrpLoadType loadType, Number... values) {
		Preconditions.checkArgument(values.length == loadType.size());
		ImmutableMap.Builder<String, Number> builder = ImmutableMap.builder();

		for (int k = 0; k < values.length; k++) {
			builder.put(loadType.getDimensions().get(k), values[k]);
		}

		return loadType.fromMap(builder.build());
	}
}
