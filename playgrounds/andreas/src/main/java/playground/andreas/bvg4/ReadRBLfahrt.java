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
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

public class ReadRBLfahrt implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(ReadRBLfahrt.class);

	private TabularFileParserConfig tabFileParserConfig;
	private LinkedList<FahrtEvent> fahrtEventList = new LinkedList<FahrtEvent>();
	private FahrtEventSink sink = new ListAdder();
	private int linesRejected = 0;

	static interface FahrtEventSink {
		void process(FahrtEvent fahrtEvent);
	}

	class ListAdder implements FahrtEventSink {
		@Override
		public void process(FahrtEvent fahrtEvent) {
			fahrtEventList.add(fahrtEvent);
		}
	}

	public ReadRBLfahrt(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
	}

	public static LinkedList<FahrtEvent> readFahrtEvents(String filename) throws IOException {
		ReadRBLfahrt reader = new ReadRBLfahrt(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.fahrtEventList.size() + " lines");
		return reader.fahrtEventList;		
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
				Id<TransitLine> lineId = Id.create(row[5].trim(), TransitLine.class);
				Id<TransitRoute> routeId = Id.create(row[6].trim(), TransitRoute.class);
				int distanceRouteIst = Integer.parseInt(row[7].trim());
				int travelTimeIst = Integer.parseInt(row[8].trim());
				
				boolean fahrtArtDispo;
				if(row[9].trim().equalsIgnoreCase("N")){
					fahrtArtDispo = false;
				} else if(row[9].trim().equalsIgnoreCase("J")){
					fahrtArtDispo = true;
				} else {
					throw new NumberFormatException();
				}
				
				boolean statusLokalisierung;
				if(row[10].trim().equalsIgnoreCase("N")){
					statusLokalisierung = false;
				} else if(row[10].trim().equalsIgnoreCase("J")){
					statusLokalisierung = true;
				} else {
					throw new NumberFormatException();
				}
				
				boolean statusErfasst;
				if(row[11].trim().equalsIgnoreCase("N")){
					statusErfasst = false;
				} else if(row[11].trim().equalsIgnoreCase("J")){
					statusErfasst = true;
				} else {
					throw new NumberFormatException();
				}
				
				boolean transmissionError;
				if(row[12].trim().equalsIgnoreCase("N")){
					transmissionError = false;
				} else if(row[12].trim().equalsIgnoreCase("J")){
					transmissionError = true;
				} else {
					throw new NumberFormatException();
				}
				
				FahrtEvent fahrtEvent = new FahrtEvent(rblDate, kurs, departureDateIst, departureTimeIst, zeitBasis, vehId, lineId, routeId, distanceRouteIst, travelTimeIst, fahrtArtDispo, statusLokalisierung, statusErfasst, transmissionError);
				sink.process(fahrtEvent);
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

	public void setSink(FahrtEventSink sink) {
		this.sink = sink;
	}
}