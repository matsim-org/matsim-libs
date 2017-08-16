// code by jph
package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;

import playground.clruch.net.IdIntegerDatabase;
import playground.clruch.utils.GlobalAssert;

public class CsvFleetReader {

	public static List<String> csvLineToList(String line) {
		return Stream.of(line.split(";")).collect(Collectors.toList());
	}

	final DayTaxiRecord dayTaxiRecord;

	public CsvFleetReader(DayTaxiRecord dayTaxiRecord) {
		this.dayTaxiRecord = dayTaxiRecord;
	}

	public DayTaxiRecord populate(File file) throws Exception {
		GlobalAssert.that(file.isFile());
		IdIntegerDatabase vehicleIdIntegerDatabase = new IdIntegerDatabase();

		int dataline = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			{
				String line = br.readLine();
				List<String> list = csvLineToList(line);
				int count = 0;
				System.out.println("CSV HEADER");
				for (String token : list) {
					System.out.println(" col " + count + " = " + token);
					++count;
				}
			}
			while (true) {
				String line = br.readLine();
				if (Objects.isNull(line))
					break;
				List<String> list = csvLineToList(line);

				TaxiStamp taxiStamp = new TaxiStamp();
				taxiStamp.time = DateParser.from(list.get(0));
				taxiStamp.id = vehicleIdIntegerDatabase.getId(list.get(1));
				taxiStamp.gps = new Coord( //
						Double.parseDouble(list.get(10)), //
						Double.parseDouble(list.get(11)));
				dayTaxiRecord.insert(list.get(0), taxiStamp);
				++dataline;
			}
		} finally {
			// ---
		}
		System.out.println("lines      " + dataline);
		System.out.println("vehicles   " + vehicleIdIntegerDatabase.size());
		System.out.println("timestamps " + dayTaxiRecord.keySet().size());
		return dayTaxiRecord;
	}
}
