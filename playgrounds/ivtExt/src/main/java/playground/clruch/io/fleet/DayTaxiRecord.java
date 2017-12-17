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
    public String lastTimeStamp = null;
    public final Set<String> status = new HashSet<>();

    public void insert(List<String> list) {
        final String timeStamp = list.get(0);

        // final long taxiStamp_millis = DateParser.from(timeStamp);
        // System.out.println("2 " + taxiStamp_millis);

        long cmp = DateParser.from(timeStamp.substring(0, 11) + "00:00:00");
        if (midnight == null) {
            midnight = cmp;
            System.out.println("INFO midnight = " + midnight);
        } else {
            if (midnight != cmp) {
                // System.out.println("INFO drop " + timeStamp);
                // throw new RuntimeException();
            }
            cmp = DateParser.from(timeStamp);
            if (cmp % 3600000 == 0)
                System.out.println("INFO reading " + timeStamp);
        }

        final int taxiStamp_id = vehicleIdIntegerDatabase.getId(list.get(1));
        if (taxiStamp_id == trails.size())
            trails.add(new TaxiTrail());

        trails.get(taxiStamp_id).insert(getNow(timeStamp), list);
        status.add(list.get(3));
    }

    public final int getNow(String timeStamp) {
        final long taxiStamp_millis = DateParser.from(timeStamp);
        final int now = (int) ((taxiStamp_millis - midnight) / 1000);
        // now -= now % modulus;
        return now;
    }

    public int size() {
        return trails.size();
    }

    public TaxiTrail get(int vehicleIndex) {
        return trails.get(vehicleIndex);
    }
}
