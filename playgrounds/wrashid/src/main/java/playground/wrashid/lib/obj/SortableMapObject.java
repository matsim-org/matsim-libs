package playground.wrashid.lib.obj;

public class SortableMapObject<Key> implements Comparable<SortableMapObject> {

	private final Key key;
	private final double score;

	/**
	 * attention: it gives you the smaller number. if you are using negative
	 * scores and need the best score, use multiply by -1.0, before insertion
	 * 
	 * @param key
	 * @param score
	 */
	public SortableMapObject(Key key, double score) {
		this.key = key;
		this.score = score;
	}

	@Override
	public int compareTo(SortableMapObject other) {
		if (getScore() > other.getScore())
			return 1;

		if (getScore() < other.getScore())
			return -1;

		return 0;
	}

	public double getScore() {
		return score;
	}

	public Key getKey() {
		return key;
	}

}
