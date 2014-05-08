package org.matsim.contrib.wagonSim.schedule.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author mrieser / senozon
 */
public class NetworkEditsReader {

	private final static Logger log = Logger.getLogger(NetworkEditsReader.class);
	
	private final List<NetworkEdit> edits;
	
	public NetworkEditsReader(final List<NetworkEdit> edits) {
		this.edits = edits;
	}
	
	public void readFile(final String filename) {
		try {
			BufferedReader reader = IOUtils.getBufferedReader(filename);
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split("\t");
				if (parts[0].equals("MERGE_NODE")) {
					edits.add(new MergeNodes(new IdImpl(parts[1]), new IdImpl(parts[2])));
				} else if (parts[0].equals("NEW_LINK")) {
					edits.add(new AddLink(new IdImpl(parts[1]), new IdImpl(parts[2]), new IdImpl(parts[3])));
				} else if (parts[0].equals("REPLACE_LINK")) {
					List<Id> ids = new ArrayList<Id>();
					for (String part : parts[2].split(",")) {
						ids.add(new IdImpl(part));
					}
					edits.add(new ReplaceLink(new IdImpl(parts[1]), ids));
				} else {
					log.error("Unknown network edit instruction: " + parts[0]); 
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
