package gunnar.ihop2.transmodeler.networktransformation;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.unquote;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * TODO Are there nodes that should be left out?
 * 
 * TODO Are there further node attributes that could be used?
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerNodesReader extends AbstractTabularFileHandlerWithHeaderLine {

	private final String nodeIdLabel = "ID";

	private final String longitudeLabel = "Longitude";

	private final String latitudeLabel = "Latitude";

	final Map<String, TransmodelerNode> id2node = new LinkedHashMap<String, TransmodelerNode>();

	TransmodelerNodesReader(final String nodesFileName) throws IOException {
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(nodesFileName, this);
	}

	@Override
	protected String preprocessColumnLabel(final String label) {
		return unquote(label);
	}

	@Override
	public void startDataRow(final String[] row) {
		final String nodeId = row[this.index(this.nodeIdLabel)];
		final TransmodelerNode node = new TransmodelerNode(nodeId,
				Double.parseDouble(row[this.index(this.longitudeLabel)]),
				Double.parseDouble(row[this.index(this.latitudeLabel)]));
		this.id2node.put(nodeId, node);
		System.out.println("read node: " + node);
	}
}
