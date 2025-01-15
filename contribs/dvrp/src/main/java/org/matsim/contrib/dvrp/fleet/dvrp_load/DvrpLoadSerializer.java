package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.matsim.api.core.v01.Id;

/**
 * This interface represents serialization and deserialization procedures that allow to convert {@link DvrpLoad} objects to Strings and vice-versa.
 * @author Tarek Chouaki (tkchouaki)
 */
public interface DvrpLoadSerializer {

	DvrpLoad deSerialize(String loadRepr, Id<DvrpLoadType> dvrpLoadTypeId);

	String serialize(DvrpLoad dvrpLoad);
}
