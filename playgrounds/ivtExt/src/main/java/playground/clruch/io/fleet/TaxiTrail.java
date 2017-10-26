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
        // int lastTimeStamp = now;
        //
        // Entry<Integer, TaxiStamp> lastEntry = sortedMap.lowerEntry(now);
        // if (Objects.nonNull(lastEntry)) {
        // lastTimeStamp = lastEntry.getKey();
        // }
        //
        taxiStamp.avStatus = StringStatusMapper.apply(now, list.get(3), list.get(4), list.get(5));
        //
        // // Propagate offservice status to last entry aswell
        // if (taxiStamp.avStatus == AVStatus.OFFSERVICE) {
        // TaxiStamp lastTaxiStamp = lastEntry.getValue();
        // lastTaxiStamp.avStatus = AVStatus.OFFSERVICE;
        // sortedMap.put(lastTimeStamp, lastTaxiStamp);
        // }

        taxiStamp.gps = new Coord( //
                Double.parseDouble(list.get(10)), //
                Double.parseDouble(list.get(11)));

        if (sortedMap.containsKey(now)) {
            System.err.println("override");
            ++override;
        }

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
        return entry;
    }

    /*** Off sourced check offservice avstatus to its own method which can be called
     * either from taxitrail or simulationfleetdump */
    public void check_offservice(int now) {
        
        // Get last two values
        Entry<Integer, TaxiStamp> nowEntry = sortedMap.floorEntry(now);
        
        if (Objects.nonNull(nowEntry)) {
            int nowTimeStamp = nowEntry.getKey();
                        
            // Check validity & time difference between last two timestamps
            if (Math.abs(now - nowTimeStamp) >= 2700) { // TODO magic const.
            
                    // Change avstatus to offservice 
                    TaxiStamp nowTaxiStamp = nowEntry.getValue();
                    nowTaxiStamp.avStatus = AVStatus.OFFSERVICE;
                    sortedMap.put(nowTimeStamp, nowTaxiStamp);
                    
                    // Check if entry before that also exist and propagate offservice status
                    Entry<Integer, TaxiStamp> lastEntry = sortedMap.lowerEntry(nowTimeStamp);
                    if (Objects.nonNull(lastEntry)) {
                        TaxiStamp lastTaxiStamp = lastEntry.getValue();
                        int lastTimeStamp = lastEntry.getKey();
                        lastTaxiStamp.avStatus = AVStatus.OFFSERVICE;
                        sortedMap.put(lastTimeStamp, lastTaxiStamp);
                    }
            }
        }
    }

}
