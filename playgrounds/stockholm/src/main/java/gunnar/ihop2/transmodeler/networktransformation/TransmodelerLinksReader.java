package gunnar.ihop2.transmodeler.networktransformation;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.unquote;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.DIR;
import gunnar.ihop2.utils.AbstractTabularFileHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerLinksReader extends AbstractTabularFileHandler {

	// -------------------- CONSTANTS --------------------

	private final String idLabel = "ID";

	private final String dirLabel = "Dir";

	private final String aNodeLabel = "ANode";

	private final String bNodeLabel = "BNode";

	private final String classLabel = "Class";

	// -------------------- MEMBERS --------------------

	private final Map<String, TransmodelerNode> id2node;

	final Map<String, TransmodelerLink> id2link = new LinkedHashMap<String, TransmodelerLink>();

	private int ignoredLinksCnt = 0;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerLinksReader(final String linksFileName,
			final Map<String, TransmodelerNode> id2node) throws IOException {
		this.id2node = id2node;
		this.ignoredLinksCnt = 0;
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(linksFileName, this);
	}

	// -------------------- MISC --------------------

	int getIgnoredLinksCnt() {
		return this.ignoredLinksCnt;
	}

	// ---------- IMPLEMENTATION OF AbstractTabularFileHandler -----------

	@Override
	protected String preprocessColumnLabel(final String label) {
		return unquote(label);
	}

	@Override
	public void startDataRow(final String[] row) {

		final String bidirLinkId = row[this.index(this.idLabel)];
		final TransmodelerNode aNode = this.id2node.get(row[this
				.index(this.aNodeLabel)]);
		final TransmodelerNode bNode = this.id2node.get(row[this
				.index(this.bNodeLabel)]);

		if (aNode.equals(bNode)) {
			System.out.println("ignoring circular link " + bidirLinkId);
			this.ignoredLinksCnt++;
			return; // -------------------------------------------------
		}

		final String type = unquote(row[this.index(this.classLabel)]);
		final String dir = row[this.index(this.dirLabel)];

		if ("1".equals(dir) || "0".equals(dir)) {
			final TransmodelerLink link = new TransmodelerLink(bidirLinkId,
					DIR.AB, aNode, bNode, type);
			this.id2link.put(link.getId(), link);
			System.out.println("read link: " + link);
		}

		if ("-1".equals(dir) || "0".equals(dir)) {
			final TransmodelerLink link = new TransmodelerLink(bidirLinkId,
					DIR.BA, bNode, aNode, type);
			this.id2link.put(link.getId(), link);
			System.out.println("read link: " + link);
		}
	}
}
