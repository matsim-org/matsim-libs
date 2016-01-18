package gunnar.ihop2.transmodeler.networktransformation;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.newUnidirectionalId;
import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.unquote;
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
class TransmodelerLaneReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private final String idLabel = "ID";

	private final String dirLabel = "Dir";

	private final String segmentLabel = "Segment";

	// -------------------- MEMBERS --------------------

	private final Map<String, TransmodelerLink> unidirSegmentId2link;

	// lane IDs are unique; no directional information needed
	final Map<String, TransmodelerLink> upstrLaneId2link = new LinkedHashMap<>();
	final Map<String, TransmodelerLink> downstrLaneId2link = new LinkedHashMap<>();

	private int ignoredLaneCnt;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerLaneReader(final String lanesFileName,
			final Map<String, TransmodelerLink> unidirSegmentId2link)
			throws IOException {
		this.unidirSegmentId2link = unidirSegmentId2link;
		this.ignoredLaneCnt = 0;
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(lanesFileName, this);
	}

	// -------------------- MISC --------------------

	int getIgnoredLaneCnt() {
		return this.ignoredLaneCnt;
	}

	private void addLane(final String laneId, final String bidirSegmentId,
			final DIR dir) {
		final String unidirSegmentId = newUnidirectionalId(bidirSegmentId, dir);
		final TransmodelerLink link = this.unidirSegmentId2link
				.get(unidirSegmentId);
		if (link == null) {
			this.ignoredLaneCnt++;
		} else {
			final boolean segmentIsUpstream = link
					.segmentIsUpstream(unidirSegmentId);
			final boolean segmentIsDownstream = link
					.segmentIsDownstream(unidirSegmentId);
			if (segmentIsUpstream) {
				this.upstrLaneId2link.put(laneId, link);
				System.out.println("added upstream lane " + laneId);
			}
			if (segmentIsDownstream) {
				this.downstrLaneId2link.put(laneId, link);
				System.out.println("added downstream lane " + laneId);
			}
			if (!segmentIsUpstream && !segmentIsDownstream) {
				this.ignoredLaneCnt++;
			}
		}
	}

	// --------------- OVERRIDING OF AbstractTabularFileHandler ---------------

	@Override
	protected String preprocessColumnLabel(final String label) {
		return unquote(label);
	}

	@Override
	public void startDataRow(final String[] row) {
		final String laneId = row[this.index(this.idLabel)];
		final String bidirSegmentId = row[this.index(this.segmentLabel)];
		final String dir = row[this.index(this.dirLabel)];
		if ("1".equals(dir) || "0".equals(dir)) {
			this.addLane(laneId, bidirSegmentId, DIR.AB);
		}
		if ("-1".equals(dir) || "0".equals(dir)) {
			this.addLane(laneId, bidirSegmentId, DIR.BA);
		}
	}
}
