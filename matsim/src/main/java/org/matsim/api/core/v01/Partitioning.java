package org.matsim.api.core.v01;

/**
 * Represents a partitioning of a set of identifiable objects.
 * @param <T>
 */
public interface Partitioning<T> {

	/**
	 * Retrieve the partition with the given index.
	 */
	Partition<T> getPartition(int partition);

	/**
	 * Return the process responsible for the given object.
	 */
	int getPartition(Id<T> id);

}
