// code by jph
package playground.clruch.io.fleet;

import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.opengis.referencing.SpatialReferenceSystemUsingGeographicIdentifier;

import ch.ethz.idsc.queuey.util.GlobalAssert;

public class TaxiTrail {
	@SuppressWarnings("unused")
	private int override = 0;
	private final NavigableMap<Integer, TaxiStamp> sortedMap = new TreeMap<>();

	public void insert(int now, List<String> list) {
		TaxiStamp taxiStamp = new TaxiStamp();
		taxiStamp.avStatus = StringStatusMapper.apply(list.get(3),list.get(4),list.get(5));

		taxiStamp.gps = new Coord( //
				Double.parseDouble(list.get(10)), //
				Double.parseDouble(list.get(11)));

		if (sortedMap.containsKey(now)) {
			System.err.println("override");
			++override;
		}

		sortedMap.put(now, taxiStamp);
	}

	/***
	 * Changed method to return the whole entry instead only the getvalue() part
	 * so we also know the timestamp it "interpolated" to.
	 * @param now
	 * @return
	 */
	public Entry<Integer, TaxiStamp> interp(int now) {
		// less than or equal to the given key
		Entry<Integer, TaxiStamp> entry = sortedMap.floorEntry(now);
		if (Objects.nonNull(entry))
			return entry;
		entry = sortedMap.higherEntry(now); // strictly greater
		GlobalAssert.that(Objects.nonNull(entry));
		return entry;
	}
}
