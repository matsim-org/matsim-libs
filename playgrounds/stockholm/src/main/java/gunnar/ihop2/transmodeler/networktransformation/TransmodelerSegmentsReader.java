package gunnar.ihop2.transmodeler.networktransformation;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.newUnidirectionalId;
import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.newUnidirectionalLinkId;
import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.unquote;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.DIR;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerSegmentsReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private final String idLabel = "ID";

	private final String dirLabel = "Dir";

	private final String abLabel = "AB";

	private final String baLabel = "BA";

	private final String linkLabel = "Link";

	private final String lengthLabel = "Length";

	private final String lanesAbLabel = "Lanes_AB";

	private final String lanesBaLabel = "Lanes_BA";

	private final String positionLabel = "Position";

	// -------------------- MEMBERS --------------------

	private final Map<String, TransmodelerLink> linkId2link;

	final Map<String, TransmodelerLink> unidirSegmentId2link = new LinkedHashMap<>();

	private int ignoredSegmentCnt;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerSegmentsReader(final String segmentFileName,
			final Map<String, TransmodelerLink> linkId2link) throws IOException {
		this.linkId2link = linkId2link;
		this.ignoredSegmentCnt = 0;
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(segmentFileName, this);
	}

	// -------------------- MISC --------------------

	int getIgnoredSegmentCnt() {
		return this.ignoredSegmentCnt;
	}

	// -------------------- INTERNALS --------------------

	private void addSegment(final String bidirectionalSegmentId,
			final String bidirectionalLinkId, final DIR dir,
			final String abDir, final String baDir, final int lanes,
			final double length, final int position) {
		final TransmodelerLink link = this.linkId2link
				.get(newUnidirectionalLinkId(bidirectionalLinkId, dir, abDir,
						baDir));
		if (link == null) {
			this.ignoredSegmentCnt++;
			System.out.println("ignored segment: " + bidirectionalSegmentId);
		} else {
			final String unidirectionalSegmentId = newUnidirectionalId(
					bidirectionalSegmentId, dir);
			final TransmodelerSegment segment = new TransmodelerSegment(
					unidirectionalSegmentId, lanes, length, position);
			this.unidirSegmentId2link.put(unidirectionalSegmentId, link);
			link.segments.add(segment);
			System.out.println("read segment: " + segment);
		}
	}

	// ---------- OVERRIDING OF AbstractTabularFileHandler ----------

	@Override
	protected String preprocessColumnLabel(final String label) {
		return unquote(label);
	}

	@Override
	public void startDataRow(final String[] row) {

		final String bidirectionalSegmentId = row[this.index(this.idLabel)];
		final String bidirectionalLinkId = row[this.index(this.linkLabel)];
		final int position = parseInt(row[this.index(this.positionLabel)]);
		final double length = parseDouble(row[this.index(this.lengthLabel)]);
		final String dir = row[this.index(this.dirLabel)];
		final String ab = unquote(row[this.index(this.abLabel)]);
		final String ba = unquote(row[this.index(this.baLabel)]);

		if ("1".equals(dir) || "0".equals(dir)) {
			this.addSegment(bidirectionalSegmentId, bidirectionalLinkId,
					DIR.AB, ab, ba,
					parseInt(row[this.index(this.lanesAbLabel)]), length,
					position);
		}

		if ("-1".equals(dir) || "0".equals(dir)) {
			this.addSegment(bidirectionalSegmentId, bidirectionalLinkId,
					DIR.BA, ab, ba,
					parseInt(row[this.index(this.lanesBaLabel)]), length,
					position);
		}
	}
}
