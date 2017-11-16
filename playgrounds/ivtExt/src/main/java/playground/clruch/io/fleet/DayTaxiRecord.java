// code by jph
package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TimeZone;

import com.jidesoft.icons.IconSet.File;

import ch.ethz.idsc.queuey.datalys.MultiFileReader;
import playground.clruch.net.IdIntegerDatabase;

public class DayTaxiRecord {
<<<<<<< HEAD
	private final IdIntegerDatabase vehicleIdIntegerDatabase = new IdIntegerDatabase();
	private final List<TaxiTrail> trails = new ArrayList<>();
	private Long midnight = null;
	public String lastTimeStamp = null;
	public final Set<String> status = new HashSet<>();

	public void insert(List<String> list, int taxiStampID) {
		String timeStamp = list.get(3);

//		System.out.println("In UNIX " + timeStamp);
		long unixSeconds = Long.valueOf(timeStamp).longValue();
		Date date = new Date(unixSeconds * 1000L); // *1000 is to convert seconds to milliseconds
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); // the format of your date
		String formattedDate = sdf.format(date);
		timeStamp = formattedDate;
		System.out.println("INFO DATA " + timeStamp);

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
			// if (cmp % 600000 == 0)
			// System.out.println("INFO reading " + timeStamp);
		}

		final int taxiStamp_id = vehicleIdIntegerDatabase.getId(Integer.toString(taxiStampID));
		if (taxiStamp_id == trails.size())
			trails.add(new TaxiTrail());

		trails.get(taxiStamp_id).insert(getNow(timeStamp), list);
		String bool = list.get(2);
		if (bool.startsWith("0")) {
			status.add("In Umgebung");
		}
		if (bool.startsWith("1")) {
			status.add("Besetzt mit Kunden");
		}
		
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
=======
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
            if (cmp % 600000 == 0)
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
>>>>>>> master
