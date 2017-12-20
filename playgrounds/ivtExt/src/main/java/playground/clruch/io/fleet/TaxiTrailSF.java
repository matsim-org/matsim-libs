// code by jph
package playground.clruch.io.fleet;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.RequestStatus;

public class TaxiTrailSF {
    @SuppressWarnings("unused")
    private int override = 0;
	private final NavigableMap<Integer, TaxiStamp> sortedMap = new TreeMap<>();

	public void insert(int now, List<String> list, String status) {
		TaxiStamp taxiStamp = new TaxiStamp();
		if (status.equals("DRIVEWITHCOSTUMER"))
			taxiStamp.avStatus = AVStatus.DRIVEWITHCUSTOMER;
		if (status.equals("REBALANCE"))
			taxiStamp.avStatus = AVStatus.REBALANCEDRIVE;
		if (status.equals("DRIVETOCUSTOMER"))
			taxiStamp.avStatus = AVStatus.DRIVETOCUSTOMER;
		if (status.equals("OFFSERVICE"))
			taxiStamp.avStatus = AVStatus.OFFSERVICE;
		if (status.equals("STAY"))
			taxiStamp.avStatus = AVStatus.STAY;

		// TODO make the real status here.
		taxiStamp.gps = new Coord( //
				Double.parseDouble(list.get(1)), //
				Double.parseDouble(list.get(0)));

		if (sortedMap.containsKey(now)) {
			System.err.println("override");
			++override;
		}
		sortedMap.put(now, taxiStamp);
	}



	/***
	 * Changed method to return the whole entry instead only the getvalue() part so
	 * we also know the timestamp it "interpolated" to.
	 * 
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
        if (Objects.nonNull(entry))
            return entry;
        return null; // TODO <- this case will never happen due to GlobalAssert, so "return entry;" is sufficient  
    }

    public Entry<Integer, TaxiStamp> getLastEntry(int now) {
        Optional<Entry<Integer, TaxiStamp>> lastEntry = Optional.ofNullable(sortedMap.lowerEntry(interp(now).getKey()));
        if (lastEntry.isPresent())
            return lastEntry.get();
        // GlobalAssert.that(Objects.nonNull(lastEntry));
        return null;
    }

    public Entry<Integer, TaxiStamp> getNextEntry(int now) {
        Optional<Entry<Integer, TaxiStamp>> nextEntry = Optional.ofNullable(sortedMap.higherEntry(interp(now).getKey()));
        if (nextEntry.isPresent())
            return nextEntry.get();
        // GlobalAssert.that(Objects.nonNull(nextEntry));
        return null;
    }

    /*** Off sourced check offservice avstatus to its own method which can be called
     * either from taxitrail or simulationfleetdump */
    public void checkOffService(int now) {
        // Get last two values
        Entry<Integer, TaxiStamp> nowEntry = interp(now);
        if (now == 0) {
            // nowEntry = sortedMap.ceilingEntry(now);
            if (nowEntry.getValue().avStatus == AVStatus.STAY || nowEntry.getValue().avStatus == AVStatus.REBALANCEDRIVE)
                setOffService(nowEntry.getKey());
            return;
        } // else
          // nowEntry = sortedMap.floorEntry(now);

        if (Objects.nonNull(nowEntry)) {
            int nowTimeStamp = nowEntry.getKey();
            // Check validity & time difference between last two timestamps
            if (Math.abs(now - nowTimeStamp) >= 2700) { // TODO magic const.
                // Change avstatus to offservice
                setOffService(nowEntry.getKey());

                // Check if entry before that also exist and propagate offservice status
                Entry<Integer, TaxiStamp> lastEntry = getLastEntry(now);
                if (Objects.nonNull(lastEntry))
                    setOffService(lastEntry.getKey());
            }
        }
    }
    
    public void setLinkData(int now, int linkIndex, double linkSpeed) {
        // less than or equal to the given key
        Entry<Integer, TaxiStamp> entry = interp(now);
        if (Objects.nonNull(entry)) {
            TaxiStamp taxiStamp = entry.getValue();
            int timeStamp = entry.getKey();
            taxiStamp.linkIndex = linkIndex;
            taxiStamp.linkSpeed = linkSpeed;
            sortedMap.replace(timeStamp, taxiStamp);
        }
    }

    public void setRequestIndex(int now, int requestIndex) {
        // less than or equal to the given key
        Entry<Integer, TaxiStamp> entry = interp(now);
        if (Objects.nonNull(entry)) {
            TaxiStamp taxiStamp = entry.getValue();
            int timeStamp = entry.getKey();
            taxiStamp.requestIndex = requestIndex;
            sortedMap.replace(timeStamp, taxiStamp);
        }
    }

    public void setRequestStatus(int now, RequestStatus requestStatus) {
        // less than or equal to the given key
        Entry<Integer, TaxiStamp> entry = interp(now);
        if (Objects.nonNull(entry)) {
            TaxiStamp taxiStamp = entry.getValue();
            int timeStamp = entry.getKey();
            taxiStamp.requestStatus = requestStatus;
            sortedMap.replace(timeStamp, taxiStamp);
        }
    }
  
    private void setOffService(int now) {
        // less than or equal to the given key
        Entry<Integer, TaxiStamp> entry = interp(now);
        if (Objects.nonNull(entry)) {
            TaxiStamp taxiStamp = entry.getValue();
            int timeStamp = entry.getKey();
            taxiStamp.avStatus = AVStatus.OFFSERVICE;
            sortedMap.replace(timeStamp, taxiStamp);
        }
    }
    
    public Set<Integer> getKeySet() {
        return Collections.unmodifiableSet(sortedMap.keySet());
    }

}