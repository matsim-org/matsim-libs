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

public enum CsvFleetReader {
	;
	public static List<String> csvLineToList(String line) {
		return Stream.of(line.split(";")).collect(Collectors.toList());
	}

	public static DayTaxiRecord from(File file) throws Exception {
		GlobalAssert.that(file.isFile());
		DayTaxiRecord dayTaxiRecord = new DayTaxiRecord(10); // bin size = 10
																// sec
		IdIntegerDatabase vehicleIdIntegerDatabase = new IdIntegerDatabase();

		int dataline = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			{
				String line = br.readLine();
				List<String> list = csvLineToList(line);
				int count = 0;
				for (String token : list) {
					System.out.println(count + " " + token);
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
						Double.parseDouble(list.get(10)), // TODO not sure if
															// lat-lon or
															// lon-lat
						Double.parseDouble(list.get(11)));
				// System.out.println( + " " + list.get(11));
				dayTaxiRecord.insert(list.get(0), taxiStamp);
				++dataline;
			}
		} finally {
		}
		System.out.println("#LNE " + dataline);
		System.out.println("#VEH " + vehicleIdIntegerDatabase.size());
		System.out.println("#TIM " + dayTaxiRecord.keySet().size());
		return dayTaxiRecord;
	}

	public static void main(String[] args) throws Exception {
		File file = new File("/media/datahaki/media/ethz/taxi", "2017-06-27 - GPS Fahrtstrecken-Protokoll.csv");
		// extract from csv file
		DayTaxiRecord dayTaxiRecord = CsvFleetReader.from(file);
		// generate sim objects and store
		SimulationFleetDump.of(dayTaxiRecord);

	}
}
