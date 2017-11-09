// code by jph
package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import playground.clruch.net.IdIntegerDatabase;

public class DayTaxiRecord {

    private final List<TaxiTrail> trails = new ArrayList<>();
    private Long midnight = null;
    public String lastTimeStamp = null;
    public final Set<String> status = new HashSet<>();

    public void insert(List<String> list, int taxiStampID) {
        final String timeStamp = list.get(3);

        System.out.println("INFO reading " + timeStamp);
        // final long taxiStamp_millis = DateParser.from(timeStamp);
        // System.out.println("2 " + taxiStamp_millis);

        long cmp = Long.parseLong(timeStamp) * 1000;
        // DateParser.from(timeStamp.substring(0, 11) + "00:00:00");
        if (midnight == null) {
            midnight = cmp;
            System.out.println("midnight=" + midnight);
        } else {
            if (midnight != cmp) {

                // throw new RuntimeException();
            }
        }
        
      
        final int taxiStamp_id =  taxiStampID; 
        //0; // TODO do in more elegant way?
        // vehicleIdIntegerDatabase.getId(list.get(1));
        if (taxiStamp_id == trails.size())
            trails.add(new TaxiTrail());

        trails.get(taxiStamp_id).insert((int)cmp, list);
        status.add(list.get(3));
}

	public final int getNow(String timeStamp) {
		final long taxiStamp_millis = DateParser.from(timeStamp);
		final int now = (int) ((taxiStamp_millis - midnight)) / 1000;
		// now -= now % modulus;
		return now;
	}

	public int size() {
		return trails.size();
	}

	public TaxiTrail get(int vehicleIndex) {
		return trails.get(vehicleIndex);
	}

	public static void head(List<File> trailFilesComplete, File headerDirectory, List<File> trailFiles)
			throws IOException {
		for (int i = 0; i < trailFilesComplete.size(); ++i) {
			BufferedReader reader = new BufferedReader(new FileReader(headerDirectory));
			String line = null;
			StringBuilder stringBuilder = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line + " ");
			}
			String Header = stringBuilder.toString();
			BufferedReader reader1 = new BufferedReader(new FileReader(trailFilesComplete.get(i)));
			ArrayList<String> lista = new ArrayList<String>();
			String line1 = reader1.readLine();
			reader1.close();

			if (!line1.equals(Header)) {
				reader1 = new BufferedReader(new FileReader(trailFilesComplete.get(i)));
				String dataRow = reader1.readLine();
				while (dataRow != null) {
					dataRow = reader1.readLine();
					lista.add(dataRow);
				}
				FileWriter writer = new FileWriter(trailFilesComplete.get(i));
				writer.append(Header + "\n" + line1);
				for (int x = 0; x < lista.size() - 1; x++) {
					writer.append("\n" + lista.get(x));
				}

				reader1.close();
				writer.flush();
				writer.close();
				reader.close();
			}
		}

	}

	public static void id(List<File> trailFilesComplete, File idDirectory, List<File> trailFiles) {
		// TODO Auto-generated method stub
		
	}
}