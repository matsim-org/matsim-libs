package org.matsim.contrib.dvrp.fleet;

public interface DvrpLoadSerializer {

	DvrpLoad deSerialize(String loadRepr, String loadTypeName);

	String serialize(DvrpLoad dvrpLoad);
}
