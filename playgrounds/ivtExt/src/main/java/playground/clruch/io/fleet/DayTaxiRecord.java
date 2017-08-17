// code by jph
package playground.clruch.io.fleet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import playground.clruch.net.IdIntegerDatabase;

public class DayTaxiRecord {
	private final IdIntegerDatabase vehicleIdIntegerDatabase = new IdIntegerDatabase();
	private final List<TaxiTrail> trails = new ArrayList<>();
	private Long midnight = null;
	public final Set<String> status = new HashSet<>();

	public void insert(List<String> list) {
		final String timeStamp = list.get(0);
		final long taxiStamp_millis = DateParser.from(timeStamp);
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
		final int now = (int) ((taxiStamp_millis - midnight) / 1000);
		// now -= now % modulus;

		final int taxiStamp_id = vehicleIdIntegerDatabase.getId(list.get(1));
		if (taxiStamp_id == trails.size())
			trails.add(new TaxiTrail());

		trails.get(taxiStamp_id).insert(now, list);
		status.add(list.get(3));
	}

	public int size() {
		return trails.size();
	}

	public TaxiTrail get(int vehicleIndex) {
		return trails.get(vehicleIndex);
	}
}
