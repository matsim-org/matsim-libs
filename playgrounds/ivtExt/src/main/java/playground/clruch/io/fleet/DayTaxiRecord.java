package playground.clruch.io.fleet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import playground.clruch.utils.GlobalAssert;

public class DayTaxiRecord {
	private final SortedMap<Integer, List<TaxiStamp>> sortedMap = new TreeMap<>();
	private final int modulus;
	private Long midnight = null;

	public DayTaxiRecord(int modulus) {
		this.modulus = modulus;
	}

	public void insert(String timeStamp, TaxiStamp taxiStamp) {
		long cmp = DateParser.from(timeStamp.substring(0, 11) + "00:00:00");
		if (midnight == null) {
			midnight = cmp;
			System.out.println("midnight=" + midnight);
		} else {
			if (midnight != cmp) {
				System.out.println("drop " + timeStamp);
				// throw new RuntimeException();
			}
		}
		int key = (int) ((taxiStamp.time - midnight) / 1000);
		key -= key % modulus;

		if (!sortedMap.containsKey(key)) {
			// System.out.println(key);
			sortedMap.put(key, new ArrayList<>());
		}
		sortedMap.get(key).add(taxiStamp);
	}

	public Collection<Integer> keySet() {
		return sortedMap.keySet();
	}

	public Collection<TaxiStamp> get(int now) {
		GlobalAssert.that(now % modulus == 0);
		return sortedMap.containsKey(now) ? sortedMap.get(now) : new ArrayList<>();
	}
}
