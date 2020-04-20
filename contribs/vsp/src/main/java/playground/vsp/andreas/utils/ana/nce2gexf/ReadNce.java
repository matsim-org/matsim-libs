package playground.vsp.andreas.utils.ana.nce2gexf;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class ReadNce implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(ReadNce.class);

	private TabularFileParserConfig tabFileParserConfig;
	private LinkedList<NceContainer> nceContainterList = new LinkedList<NceContainer>();
	private HashMap<String, Id<Node>> nceNodeStringNodeId2nodeId = new HashMap<>();
	private NceContainterSink sink = new ListAdder();
	private int linesRejected = 0;

	static interface NceContainterSink {
		void process(NceContainer nceContainer);
	}

	class ListAdder implements NceContainterSink {
		@Override
		public void process(NceContainer nceContainer) {
			nceContainterList.add(nceContainer);
		}
	}

	public ReadNce(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
	}

	public static LinkedList<NceContainer> readNce(String filename) throws IOException {
		ReadNce reader = new ReadNce(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.nceContainterList.size() + " lines");
		return reader.nceContainterList;		
	}	

	public void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if(row[0].trim().startsWith("#")){
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			this.linesRejected++;
			log.info("Ignoring: " + tempBuffer);
		} else {
			try {
				String fromNodeIdString = row[0].trim();
				if (this.nceNodeStringNodeId2nodeId.get(fromNodeIdString) == null) {
					this.nceNodeStringNodeId2nodeId.put(fromNodeIdString, Id.create(fromNodeIdString, Node.class));
				}
				
				double fromX = Double.parseDouble(row[1].trim());
				double fromY = Double.parseDouble(row[2].trim());
				Coord fromNodeCoord = new Coord(fromX, fromY);
				
				String toNodeStringId = row[3].trim();
				if (this.nceNodeStringNodeId2nodeId.get(toNodeStringId) == null) {
					this.nceNodeStringNodeId2nodeId.put(toNodeStringId, Id.create(toNodeStringId, Node.class));
				}
				
				double toX = Double.parseDouble(row[4].trim());
				double toY = Double.parseDouble(row[5].trim());
				Coord toNodeCoord = new Coord(toX, toY);
				
				double diffPerLink = Double.parseDouble(row[6].trim());
				
				NceContainer nceContainer = new NceContainer(this.nceNodeStringNodeId2nodeId.get(fromNodeIdString), fromNodeCoord, this.nceNodeStringNodeId2nodeId.get(toNodeStringId), toNodeCoord, diffPerLink);
				
				sink.process(nceContainer);
			} catch (NumberFormatException e) {
				this.linesRejected++;
				log.info("Ignoring line : " + Arrays.asList(row));
			}
		}
	}

	public void setSink(NceContainterSink sink) {
		this.sink = sink;
	}

	public LinkedList<NceContainer> getNceContainterList() {
		return nceContainterList;
	}

	public HashMap<String, Id<Node>> getNceNodeStringNodeId2nodeId() {
		return nceNodeStringNodeId2nodeId;
	}	
}