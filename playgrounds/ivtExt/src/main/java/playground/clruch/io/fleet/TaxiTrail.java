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

public class TaxiTrail {
    @SuppressWarnings("unused")
    private int override = 0;
    private final NavigableMap<Integer, TaxiStamp> sortedMap = new TreeMap<>();

    public void insert(int now, List<String> list) {
        TaxiStamp taxiStamp = new TaxiStamp();
        int timeStamp = 0;

        Entry<Integer, TaxiStamp> lastEntry = sortedMap.floorEntry(now);
        if (Objects.nonNull(lastEntry)) {
            timeStamp = lastEntry.getKey();
        }

        taxiStamp.avStatus = StringStatusMapper.apply(now, timeStamp, list.get(3), list.get(4), list.get(5));

        // Propagate offservice status to last entry aswell
        if (taxiStamp.avStatus == AVStatus.OFFSERVICE && Objects.nonNull(lastEntry)) {
            TaxiStamp lastTaxiStamp = lastEntry.getValue();
            lastTaxiStamp.avStatus = AVStatus.OFFSERVICE;
            TaxiStamp retSt = sortedMap.put(timeStamp, lastTaxiStamp);
            GlobalAssert.that(retSt != null);
        }

        taxiStamp.gps = new Coord( //
                Double.parseDouble(list.get(10)), //
                Double.parseDouble(list.get(11)));

        if (sortedMap.containsKey(now)) {
            System.err.println("override");
            ++override;
        }

        TaxiStamp retSt = sortedMap.put(now, taxiStamp);
        GlobalAssert.that(retSt == null);

        checkConsistency();

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
        return entry;
    }

    // TODO remove this
    private void checkConsistency() {
        int cd = 10; // checkdepth

        AVStatus lastStatus = null;
        Coord lastCoord = null;

        int countWrong = 0;

        for (Entry<Integer, TaxiStamp> entry : sortedMap.entrySet()) {

            AVStatus nowStatus = entry.getValue().avStatus;
            Coord nowCoord = entry.getValue().gps;

            if (nowStatus.equals(AVStatus.DRIVEWITHCUSTOMER)) {
                if (nowStatus.equals(lastStatus) && nowCoord.equals(lastCoord)) {
                    countWrong++;
                } else {
                    countWrong = 0;
                }

            } else {
                countWrong = 0;
            }

            lastStatus = nowStatus;
            lastCoord = nowCoord;

        }

        GlobalAssert.that(countWrong < cd);

    }

}
