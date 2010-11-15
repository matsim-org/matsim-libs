package playground.andreas.intersection.zuerich;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;

public class LSASystemsReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(LSASystemsReader.class);

	private TabularFileParserConfig tabFileParserConfig;

	private SignalSystemsData signals;

	public LSASystemsReader(SignalSystemsData signalSystems) {
		this.signals = signalSystems;
	}

	public void startRow(String[] row) throws IllegalArgumentException {

		if (row[0].contains("LSAID")) {
			log.info("Found header: " + row.toString());
		}
		else {
			// log.info("Added: " + row.toString());
			Id signalSystemId = new IdImpl(row[0]);

			if (this.signals.getSignalSystemData().containsKey(signalSystemId)) {
				log.error("Cannot create signal system definition id " + signalSystemId + " twice!");
			}
			SignalSystemData lsa = this.signals.getFactory().createSignalSystemData(signalSystemId);
			//TODO add to the right place if the code is ever needed again
			//			lsa.setDefaultCycleTime(Double.parseDouble(row[1]));
			this.signals.addSignalSystemData(lsa);
			log.info("created signalSystemDefinition id " + signalSystemId);
		}

	}

	private void readFile(String filename) throws IOException {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] { " ", "\t" }); // \t
		// this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
	}

	public void readBasicLightSignalSystemDefinition(String filename) {
		try {
			log.info("Start reading file...");
			this.readFile(filename);
			log.info("...finished.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
