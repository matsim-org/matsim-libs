package playground.andreas.intersection.zuerich;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.signalsystems.systems.SignalSystemDefinition;
import org.matsim.signalsystems.systems.SignalSystems;

public class LSASystemsReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(LSASystemsReader.class);

	private TabularFileParserConfig tabFileParserConfig;
	private HashMap<Integer, SignalSystemDefinition> lsaMap = new HashMap<Integer, SignalSystemDefinition>();

	private SignalSystems signals;

	public LSASystemsReader(SignalSystems lightSignalSystems) {
		this.signals = lightSignalSystems;
	}

	public void startRow(String[] row) throws IllegalArgumentException {

		if (row[0].contains("LSAID")) {
			log.info("Found header: " + row.toString());
		}
		else {
			// log.info("Added: " + row.toString());
			Id signalSystemId = new IdImpl(row[0]);

			if (this.signals.getSignalSystemDefinitions().containsKey(signalSystemId)) {
				log.error("Cannot create signal system definition id " + signalSystemId + " twice!");
			}
			SignalSystemDefinition lsa = this.signals.getFactory().createSignalSystemDefinition(signalSystemId);
			lsa.setDefaultCycleTime(Double.parseDouble(row[1]));
			lsa.setDefaultInterGreenTime(3.0);
			lsa.setDefaultSynchronizationOffset(0.0);
			this.signals.addSignalSystemDefinition(lsa);
			log.info("created signalSystemDefinition id " + signalSystemId);
		}

	}

	private HashMap<Integer, SignalSystemDefinition> readFile(String filename) throws IOException {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] { " ", "\t" }); // \t
		// this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		return this.lsaMap;
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
