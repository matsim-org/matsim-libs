package org.matsim.api.core.v01;

/**
 * Represents a single partition, which is a collect of object ids.
 */
public interface Partition<T> {

	/**
	 * Return the index of this partition.
	 */
	int getIndex();

	/**
	 * Check whether an object is contained on the partition.
	 */
	boolean contains(Id<T> id);

}
