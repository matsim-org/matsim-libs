package gunnar.ihop2.transmodeler.networktransformation;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.unquote;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.DIR;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerLinksReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private final String idLabel = "ID";

	private final String dirLabel = "Dir";

	private final String abLabel = "AB";

	private final String baLabel = "BA";

	private final String aNodeLabel = "ANode";

	private final String bNodeLabel = "BNode";

	private final String classLabel = "Class";

	private final String tollLabel = "Toll";

	// -------------------- MEMBERS --------------------

	private final Map<String, TransmodelerNode> id2node;

	final Map<String, TransmodelerLink> id2link = new LinkedHashMap<>();

	final Map<String, TransmodelerLink> id2tollLink = new LinkedHashMap<>();

	private int circularLinksCnt = 0;

	final boolean ignoreCircularLinks = false;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerLinksReader(final String linksFileName,
			final Map<String, TransmodelerNode> id2node) throws IOException {
		this.id2node = id2node;
		this.circularLinksCnt = 0;
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(linksFileName, this);
	}

	// -------------------- MISC --------------------

	int getCircularLinksCnt() {
		return this.circularLinksCnt;
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
			System.out.println("circular link " + bidirLinkId);
			Logger.getLogger(this.getClass().getName()).warning(
					"circular link " + bidirLinkId);
			this.circularLinksCnt++;
			if (this.ignoreCircularLinks) {
				Logger.getLogger(this.getClass().getName()).warning(
						"ignoring this link: " + bidirLinkId);
				return; // -------------------------------------------------
			}
		}

		final String type = unquote(row[this.index(this.classLabel)]);
		final String dir = row[this.index(this.dirLabel)];
		final String abDir = unquote(row[this.index(this.abLabel)]);
		final String baDir = unquote(row[this.index(this.baLabel)]);

		final String toll;
		if (this.index(this.tollLabel) < row.length) {
			toll = row[this.index(this.tollLabel)];
		} else {
			toll = "0"; // no information means no toll
		}

		if ("1".equals(dir) || "0".equals(dir)) {
			final TransmodelerLink link = new TransmodelerLink(bidirLinkId,
					DIR.AB, abDir, baDir, aNode, bNode, type);
			this.id2link.put(link.getId(), link);
			if ("1".equals(toll)) {
				this.id2tollLink.put(link.getId(), link);
			}
			System.out.println("read link: " + link);
		}

		if ("-1".equals(dir) || "0".equals(dir)) {
			final TransmodelerLink link = new TransmodelerLink(bidirLinkId,
					DIR.BA, abDir, baDir, bNode, aNode, type);
			this.id2link.put(link.getId(), link);
			if ("1".equals(toll)) {
				this.id2tollLink.put(link.getId(), link);
			}
			System.out.println("read link: " + link);
		}
	}
}
