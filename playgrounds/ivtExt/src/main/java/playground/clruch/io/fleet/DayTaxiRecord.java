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

    private final List<TaxiTrail> trails = new ArrayList<>();
    private Long midnight = null;
    public String lastTimeStamp = null;
    public final Set<String> status = new HashSet<>();

	public void insert(List<String> list, int taxiStampID) {
		String timeStamp = list.get(3);
		// System.out.println("In UNIX "+timeStamp);
		long unixSeconds = Long.valueOf(timeStamp).longValue();
		Date date = new Date(unixSeconds * 1000L); // *1000 is to convert seconds to milliseconds
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); // the format of your date
		String formattedDate = sdf.format(date);
		timeStamp = formattedDate;
		System.out.println("INFO DATA "+timeStamp);

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

		final int taxiStamp_id = taxiStampID;
		if (taxiStamp_id == trails.size())
			trails.add(new TaxiTrail());

        trails.get(taxiStamp_id).insert((int)cmp, list);
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

	public static void head(List<java.io.File> trailFilesComplete, java.io.File headerDirectory,
			List<java.io.File> trailFiles) throws IOException {
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

	public static ArrayList<String> name( java.io.File idDirectory) throws IOException {
        Scanner id = new Scanner(idDirectory);
		ArrayList<String> people = new ArrayList<String>();
		while (id.hasNext()) {
			id.useDelimiter("\"");
			id.next();
			people.add(id.next());
			id.nextLine();
		}
		id.close();
		return people;
	}
	
	public static ArrayList<String> id( java.io.File idDirectory) throws IOException {
        Scanner id = new Scanner(idDirectory);
		ArrayList<String> people = new ArrayList<String>();
		ArrayList<String> number = new ArrayList<String>();

		while (id.hasNext()) {
			id.useDelimiter("\"");
			id.next();
			id.next();
			id.next();
			number.add(id.next());
			id.nextLine();
		}
		id.close();		
		return number;
	}
}
