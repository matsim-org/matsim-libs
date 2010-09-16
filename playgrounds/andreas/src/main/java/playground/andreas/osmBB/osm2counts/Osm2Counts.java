package playground.andreas.osmBB.osm2counts;

import java.io.File;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;

import playground.mzilske.osm.JOSMTolerantFastXMLReader;

public class Osm2Counts {
	
	private final static Logger log = Logger.getLogger(Osm2Counts.class);
	
	private final String infFile;
	private HashMap<String, String> shortNameMap;
	private HashMap<String, String> unitNameMap;
	
	public Osm2Counts(String infFile){
		this.infFile = infFile;
	}

	public static void main(String[] args) {
		new Osm2Counts("f:/cgtest/berlinbrandenburg_filtered.osm").prepareOsm();		
	}

	public void prepareOsm(){
		log.info("Start...");
		log.info("Reading " + this.infFile);
		
		JOSMTolerantFastXMLReader reader = new JOSMTolerantFastXMLReader(new File(this.infFile), true, CompressionMethod.None);
		NodeSink nodeSink = new NodeSink();
		reader.setSink(nodeSink);
		reader.run();

		this.shortNameMap = nodeSink.getShortNameMap();
		this.unitNameMap = nodeSink.getUnitNameMap();

		log.info("Done...");
	}

	public HashMap<String, String> getShortNameMap() {
		return this.shortNameMap;
	}

	public HashMap<String, String> getUnitNameMap() {
		return this.unitNameMap;
	}

}
