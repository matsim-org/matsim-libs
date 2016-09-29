package besttimeresponseintegration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param L
 *            location type
 * @param M
 *            mode type
 *
 */
public class TravelTimeCache<L, M> {

	// -------------------- INTERNAL DATA STRUCTURE --------------------

	private class Key {
		private final List<Object> keyData;

		private Key(final Integer timeStep, final L origin, final L destination, final M mode) {
			this.keyData = new ArrayList<>(4);
			this.keyData.add(timeStep);
			this.keyData.add(origin);
			this.keyData.add(destination);
			this.keyData.add(mode);
		}

		@Override
		public int hashCode() {
			return this.keyData.hashCode();
		}

		@Override
		public boolean equals(final Object other) {
			if (other instanceof TravelTimeCache<?, ?>.Key) {
				return this.keyData.equals(((TravelTimeCache<?, ?>.Key) other).keyData);
			} else {
				return false;
			}
		}
	}

	private final Map<Key, Double> key2tt_s = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public TravelTimeCache() {
	}

	// -------------------- CONTENT ACCESS --------------------

	public int size() {
		return this.key2tt_s.size();
	}
	
	public void clear() {
		this.key2tt_s.clear();
	}

	public void putTT_s(final Integer timeStep, final L origin, final L destination, final M mode, final Double tt_s) {
		this.key2tt_s.put(new Key(timeStep, origin, destination, mode), tt_s);
	}

	public Double getTT_s(final Integer timeStep, final L origin, final L destination, final M mode) {
		return this.key2tt_s.get(new Key(timeStep, origin, destination, mode));
	}

}
