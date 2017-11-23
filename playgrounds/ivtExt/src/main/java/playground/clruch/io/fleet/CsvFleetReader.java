// code by jph
package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import ch.ethz.idsc.queuey.datalys.csv.CSVUtils;

public class CsvFleetReader {

	private final DayTaxiRecord dayTaxiRecord;

	public CsvFleetReader(DayTaxiRecord dayTaxiRecord) {
		this.dayTaxiRecord = dayTaxiRecord;
	}

	public DayTaxiRecord populateFrom(File file, File dataDirectory, int number) throws Exception {
		dayTaxiRecord.lastTimeStamp = null;
		if (file.isFile()) {
			String source = File.separator + file.getAbsolutePath();
			String dest = File.separator + dataDirectory + "/usefordata/a" + file.getName();
			File fin = new File(source);
			FileInputStream fis = new FileInputStream(fin);
			BufferedReader in = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(file)));
			FileWriter fstream = new FileWriter(dest, true);
			BufferedWriter out = new BufferedWriter(fstream);
			String aLine = null;
			out.write("LATITUDE LONGITUDE OCCUPANCY TIME" + "\n");
			while ((aLine = in.readLine()) != null) {
				java.util.List<String> lista = CSVUtils.csvLineToList(aLine, " ");
				Long timeStamp = Long.parseLong(lista.get(3));
				if (timeStamp < 1211147969 && timeStamp > 1211061600) {
					out.write(aLine);
					out.newLine();
				}
			}
			in.close();
			fis.close();
			out.close();
		}

		File trail = (new File(dataDirectory, "/usefordata/a" + file.getName()));
		try (BufferedReader br = new BufferedReader(new FileReader(trail))) {
			{
				Scanner id = new Scanner(file.getName());
				ArrayList<String> nameFile = new ArrayList<String>();
				while (id.hasNext()) {
					id.useDelimiter("_").next();
					nameFile.add(id.next());
				}
				id.close();
				System.out.println("INFO name: " + nameFile.toString().replace(".txt]", "").replace("[", ""));
				System.out.println("INFO Id: " + number);
				String line = br.readLine();
				List<String> list = CSVUtils.csvLineToList(line, " ");
				int count = 0;
				System.out.println("CSV HEADER");
				for (String token : list) {
					System.out.println(" col " + count + " = " + token);
					++count;
				}
			}
			while (true) {
				if (trail.length() == 34L)
					break;
				String line = br.readLine();
				if (Objects.isNull(line))
					break;
				List<String> list = CSVUtils.csvLineToList(line, " ");
				dayTaxiRecord.insert(list, number);
				String timeStamp = list.get(3);
				long unixSeconds = Long.valueOf(timeStamp).longValue();
				Date date = new Date(unixSeconds * 1000L); // *1000 is to convert seconds to milliseconds
				SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"); // the format of your date
				String formattedDate = sdf.format(date);
				timeStamp = formattedDate;
				dayTaxiRecord.lastTimeStamp = timeStamp;
			}
		} finally {
			// ...
		}

		// Going through all timestamps and check for offservice vehicles && parse
		// requests

		if (trail.length() != 34) {
			final int MAXTIME = dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp);
			final int TIMESTEP = 5;

			// System.out.println(
			// "INFO Checking for OFFSERVICE & RequestStatus for " + dayTaxiRecord.size() +
			// " vehicles...");
			for (int now = 0; now < MAXTIME; now += TIMESTEP) {
				// if (now % 10000 == 0)
				// System.out.println("now=" + now);
				for (int vehicleIndex = 0; vehicleIndex < dayTaxiRecord.size(); ++vehicleIndex) {
					TaxiTrail taxiTrail = dayTaxiRecord.get(vehicleIndex);

					// Check and propagate offservice status
					taxiTrail.checkOffService(now);
					// taxiTrail.setRequestStatus(now, RequestStatusParser.parseRequestStatus(now,
					// taxiTrail));

				}
			}

			// System.out.println("INFO lines " + dataline);
			// System.out.println("INFO vehicles " + dayTaxiRecord.size());
			// // System.out.println("timestamps " + dayTaxiRecord.keySet().size());
			// // System.out.println(dayTaxiRecord.status);
		}
		trail.delete();
		return dayTaxiRecord;
	}
}
