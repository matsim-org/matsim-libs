package playground.andreas.bvg4;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class ReadNewStops implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(ReadNewStops.class);

	private TabularFileParserConfig tabFileParserConfig;
	private List<TransitStopFacility> newStopsList = new LinkedList<TransitStopFacility>();
	private LineSink sink = new ListAdder();
	private int linesRejected = 0;
	
	private TransitScheduleFactoryImpl fac = new TransitScheduleFactoryImpl();

	static interface LineSink {
		void process(TransitStopFacility stop);
	}

	class ListAdder implements LineSink {
		@Override
		public void process(TransitStopFacility stop) {
			newStopsList.add(stop);
		}
	}

	public ReadNewStops(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
	}

	public static List<TransitStopFacility> readNewStopsList(String filename){
		ReadNewStops reader = new ReadNewStops(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.newStopsList.size() + " lines");
		return reader.newStopsList;		
	}	

	public void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if(!row[0].trim().startsWith("#")){
			try {
				Id<TransitStopFacility> stopId = Id.create(row[0].trim(), TransitStopFacility.class);
				double xCoord = Double.valueOf(row[1].trim());
				double yCoord = Double.valueOf(row[2].trim());
				Id<Link> linkId = Id.create(row[3].trim(), Link.class);
				String stopName = row[4].trim();

				TransitStopFacility stop = fac.createTransitStopFacility(stopId, new Coord(xCoord, yCoord), false);
				stop.setLinkId(linkId);
				stop.setName(stopName);

				sink.process(stop);
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

	public void setSink(LineSink sink) {
		this.sink = sink;
	}
}