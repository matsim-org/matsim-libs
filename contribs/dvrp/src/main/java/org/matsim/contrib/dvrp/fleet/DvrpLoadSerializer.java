package org.matsim.contrib.dvrp.fleet;

import org.matsim.api.core.v01.Id;

public interface DvrpLoadSerializer {

	DvrpLoad deSerialize(String loadRepr, Id<DvrpLoadType> dvrpLoadTypeId);

	String serialize(DvrpLoad dvrpLoad);
}
