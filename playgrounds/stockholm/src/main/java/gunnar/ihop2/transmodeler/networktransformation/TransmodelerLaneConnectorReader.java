package gunnar.ihop2.transmodeler.networktransformation;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.unquote;
import static java.lang.Double.parseDouble;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

import java.io.IOException;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerLaneConnectorReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private final String upstreamLaneLabel = "Upstream Lane";

	private final String downstreamLaneLabel = "Downstream Lane";

	private final String lengthLabel = "Length";

	// -------------------- MEMBERS --------------------

	private final Map<String, TransmodelerLink> unidirUpstrLaneId2link;

	private final Map<String, TransmodelerLink> unidirDownstrLaneId2link;

	private int loadedConnectionCnt;

	private int ignoredConnectionCnt;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerLaneConnectorReader(final String turningMovesFileName,
			final Map<String, TransmodelerLink> unidirUpstreamLaneId2link,
			final Map<String, TransmodelerLink> unidirDownstreamLaneId2link)
			throws IOException {
		this.unidirUpstrLaneId2link = unidirUpstreamLaneId2link;
		this.unidirDownstrLaneId2link = unidirDownstreamLaneId2link;
		this.loadedConnectionCnt = 0;
		this.ignoredConnectionCnt = 0;
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(turningMovesFileName, this);
	}

	// -------------------- GETTERS --------------------

	int getLoadedConnectionCnt() {
		return this.loadedConnectionCnt;
	}

	int getIgnoredConnectionCnt() {
		return this.ignoredConnectionCnt;
	}

	// -------------------- INTERNALS --------------------

	private void addLaneConnection(final String upstrLaneId,
			final String downstrLaneId, final double length) {
		/*
		 * The upstream lane is located downstream in its link, and the
		 * downstream lane is located upstream in its link.
		 */
		final TransmodelerLink upstrLink = this.unidirDownstrLaneId2link
				.get(upstrLaneId);
		final TransmodelerLink downstrLink = this.unidirUpstrLaneId2link
				.get(downstrLaneId);
		if ((upstrLink != null) && (downstrLink != null)
				&& (!upstrLink.equals(downstrLink))) {
			upstrLink.downstreamLink2turnLength.put(downstrLink, length);
			System.out.println("read connection from link " + upstrLink.getId()
					+ " to link " + downstrLink.getId());
			this.loadedConnectionCnt++;
		} else {
			this.ignoredConnectionCnt++;
		}
	}

	// --------------- OVERRIDING OF AbstractTabularFileHandler ---------------

	@Override
	protected String preprocessColumnLabel(final String label) {
		return unquote(label);
	}

	@Override
	public void startDataRow(final String[] row) {
		final String upstrLaneId = row[this.index(this.upstreamLaneLabel)];
		final String downstrLaneId = row[this.index(this.downstreamLaneLabel)];
		final double length = parseDouble(row[this.index(this.lengthLabel)]);
		this.addLaneConnection(upstrLaneId, downstrLaneId, length);
	}
}
