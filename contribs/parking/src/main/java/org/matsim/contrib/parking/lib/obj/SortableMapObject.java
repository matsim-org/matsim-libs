package org.matsim.contrib.parking.lib.obj;

public class SortableMapObject<Key> implements Comparable<SortableMapObject> {

	private final Key key;
	private final double weight;

	/**
	 * attention: it gives you the smaller number. If you are using negative
	 * weights and need the best score, use multiply by -1.0, before insertion
	 * 
	 * @param key
	 * @param weight
	 */
	public SortableMapObject(Key key, double weight) {
		this.key = key;
		this.weight = weight;
	}

	@Override
	public int compareTo(SortableMapObject other) {
		if (getWeight() > other.getWeight())
			return 1;

		if (getWeight() < other.getWeight())
			return -1;

		return 0;
	}

	public double getWeight() {
		return weight;
	}

	public Key getKey() {
		return key;
	}

}
