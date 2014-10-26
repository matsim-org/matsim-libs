package playground.andreas.bvg4;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class ReadRBLfahrzeit implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(ReadRBLfahrzeit.class);

	private TabularFileParserConfig tabFileParserConfig;
	private LinkedList<FahrzeitEvent> fahrzeitEventList = new LinkedList<FahrzeitEvent>();
	private FahrzeitEventSink sink = new ListAdder();
	private int linesRejected = 0;

	static interface FahrzeitEventSink {
		void process(FahrzeitEvent fahrzeitEvent);
	}

	class ListAdder implements FahrzeitEventSink {
		@Override
		public void process(FahrzeitEvent fahrzeitEvent) {
			fahrzeitEventList.add(fahrzeitEvent);
		}
	}

	public ReadRBLfahrzeit(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
	}

	public static LinkedList<FahrzeitEvent> readFahrzeitEvents(String filename) throws IOException {
		ReadRBLfahrzeit reader = new ReadRBLfahrzeit(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.fahrzeitEventList.size() + " lines");
		return reader.fahrzeitEventList;		
	}	

	public void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if(row[0].trim().matches("[0-9]+")){
			try {
				int rblDate = Integer.parseInt(row[0].trim());
				int kurs = Integer.parseInt(row[1].trim());
				String departureDateIst = row[2].split(" ")[0].trim();
				double departureTimeIst = Time.parseTime(row[2].split(" ")[1].trim());
				String zeitBasis = row[3].trim();
				Id<Vehicle> vehId = Id.create(row[4].trim(), Vehicle.class);
				int runningNumber = Integer.parseInt(row[5].trim());
				Id<TransitStopFacility> stopId = Id.create(row[6].trim(), TransitStopFacility.class);
				String stopNameShort = row[7].trim();
				String stopName = row[8].trim();
				String departureDateIstAtStop = row[9].split(" ")[0].trim();
				double departureTimeIstAtStop = Time.parseTime(row[9].split(" ")[1].trim());
				String arrivalDateIstAtStop = row[10].split(" ")[0].trim();
				double arrivalTimeIstAtStop = Time.parseTime(row[10].split(" ")[1].trim());
				int distanceStreckeIst = Integer.parseInt(row[11].trim());
				
				boolean statusOfDoor;
				if(row[12].trim().equalsIgnoreCase("N")){
					statusOfDoor = false;
				} else if(row[12].trim().equalsIgnoreCase("J")){
					statusOfDoor = true;
				} else {
					throw new NumberFormatException();
				}
				
				boolean statusLokalisierung;
				if(row[13].trim().equalsIgnoreCase("N")){
					statusLokalisierung = false;
				} else if(row[13].trim().equalsIgnoreCase("J")){
					statusLokalisierung = true;
				} else {
					throw new NumberFormatException();
				}				
				
				FahrzeitEvent fahrzeitEvent = new FahrzeitEvent(rblDate, kurs, departureDateIst, departureTimeIst, zeitBasis, vehId, runningNumber, stopId, stopNameShort, stopName, departureDateIstAtStop, departureTimeIstAtStop, arrivalDateIstAtStop, arrivalTimeIstAtStop, distanceStreckeIst, statusOfDoor, statusLokalisierung);
				sink.process(fahrzeitEvent);
			} catch (NumberFormatException e) {
				this.linesRejected++;
				log.info("Ignoring line : " + Arrays.asList(row));
			}
			
		} else {
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			this.linesRejected++;
			log.info("Ignoring: " + tempBuffer);
		}
	}

	public void setSink(FahrzeitEventSink sink) {
		this.sink = sink;
	}
}