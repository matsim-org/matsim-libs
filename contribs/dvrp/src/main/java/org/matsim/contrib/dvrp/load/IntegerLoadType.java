package org.matsim.contrib.dvrp.load;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class IntegerLoadType implements DvrpLoadType {
	private final IntegerLoad EMPTY = new IntegerLoad(0);

	private final String name;

	public IntegerLoadType(String name) {
		this.name = name;
	}

	public IntegerLoad fromInt(int load) {
		return new IntegerLoad(load);
	}

	public final IntegerLoad getEmptyLoad() {
		return EMPTY;
	}

	@Override
	public String serialize(DvrpLoad dvrpLoad) {
		Preconditions.checkArgument(dvrpLoad instanceof IntegerLoad);
		return String.valueOf(dvrpLoad.getElement(0));
	}

	@Override
	public DvrpLoad deserialize(String representation) {
		return fromInt(Integer.parseInt(representation));
	}

	@Override
	public DvrpLoad fromMap(Map<String, Number> map) {
		Preconditions.checkArgument(map.size() <= 1);
		Preconditions.checkArgument(map.isEmpty() || map.containsKey(name));
		return fromNumber(map.getOrDefault(name, 0));
	}

	@Override
	public List<String> getDimensions() {
		return Collections.singletonList(name);
	}

	@Override
	public int size() {
		return 1;
	}

	private IntegerLoad fromNumber(Number load) {
		return fromInt(load.intValue());
	}
}
