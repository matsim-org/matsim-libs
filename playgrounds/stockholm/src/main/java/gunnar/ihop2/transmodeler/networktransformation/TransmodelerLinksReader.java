package gunnar.ihop2.transmodeler.networktransformation;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.unquote;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
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

	private final String idLabel = "ID";

	private final String aNodeLabel = "ANode";

	private final String bNodeLabel = "BNode";

	private final String abLabel = "AB";

	private final String baLabel = "BA";

	private final String classLabel = "Class";

	private final Map<String, TransmodelerNode> id2node;

	private Map<String, TransmodelerLink> id2link = new LinkedHashMap<String, TransmodelerLink>();

//	static Long maxId = 0l;
	
	TransmodelerLinksReader(final String linksFileName,
			final Map<String, TransmodelerNode> id2node) throws IOException {
		this.id2node = id2node;
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(linksFileName, this);
	}

	Map<String, TransmodelerLink> getLinks() {
		return this.id2link;
	}

	@Override
	protected String preprocessColumnLabel(final String label) {
		return unquote(label);
	}

	@Override
	public void startDataRow(final String[] row) {
		final String bidirectionalLinkId = row[this.index(this.idLabel)];
		
//		maxId = Math.max(maxId, Long.parseLong(bidirectionalLinkId));
//		System.out.println("maxId = " + maxId);
		
		final TransmodelerNode aNode = this.id2node.get(row[this
				.index(this.aNodeLabel)]);
		final TransmodelerNode bNode = this.id2node.get(row[this
				.index(this.bNodeLabel)]);
		final String type = unquote(row[this.index(this.classLabel)]);
		{
			final String abDirection = unquote(row[this.index(this.abLabel)]);
			if (abDirection.length() > 0) {
				final TransmodelerLink link = new TransmodelerLink(
						bidirectionalLinkId, abDirection, aNode, bNode, type,
						"");
				this.id2link.put(link.getId(), link);
				System.out.println("read link: " + link);
			}
		}
		{
			final String baDirection = unquote(row[this.index(this.baLabel)]);
			if (baDirection.length() > 0) {
				final TransmodelerLink link = new TransmodelerLink(
						bidirectionalLinkId, baDirection, bNode, aNode, type,
						"-");
				this.id2link.put(link.getId(), link);
				System.out.println("read link: " + link);
			}
		}
	}
}
