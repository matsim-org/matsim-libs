// code by jph
package playground.clruch.io.fleet;

import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.RequestStatus;

public class TaxiTrail {
    @SuppressWarnings("unused")
    private int override = 0;
    public final NavigableMap<Integer, TaxiStamp> sortedMap = new TreeMap<>();

    public void insert(int now, List<String> list) {
        TaxiStamp taxiStamp = new TaxiStamp();
        taxiStamp.avStatus = StringStatusMapper.apply(now, list.get(3), list.get(4), list.get(5));
        taxiStamp.gps = new Coord( //
                Double.parseDouble(list.get(10)), //
                Double.parseDouble(list.get(11)));

        if (sortedMap.containsKey(now)) {
            System.err.println("override");
            ++override;
        }
            // checkOffService(now);
        sortedMap.put(now, taxiStamp);
    }

    /*** Changed method to return the whole entry instead only the getvalue() part
     * so we also know the timestamp it "interpolated" to.
     * 
     * @param now
     * @return */
    public Entry<Integer, TaxiStamp> interp(int now) {
        // less than or equal to the given key
        Entry<Integer, TaxiStamp> entry = sortedMap.floorEntry(now);
        if (Objects.nonNull(entry))
            return entry;
        entry = sortedMap.higherEntry(now); // strictly greater
        GlobalAssert.that(Objects.nonNull(entry));
        if (Objects.nonNull(entry))
            return entry;
        return null;
    }

    public Entry<Integer, TaxiStamp> getLastEntry(int now) {
        Entry<Integer, TaxiStamp> lastEntry = sortedMap.lowerEntry(interp(now).getKey());
        if (Objects.nonNull(lastEntry))
            return lastEntry;
        // GlobalAssert.that(Objects.nonNull(lastEntry));
        return null;
    }

    public Entry<Integer, TaxiStamp> getNextEntry(int now) {
        Entry<Integer, TaxiStamp> nextEntry = sortedMap.higherEntry(interp(now).getKey());
        if (Objects.nonNull(nextEntry))
            return nextEntry;
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

}
