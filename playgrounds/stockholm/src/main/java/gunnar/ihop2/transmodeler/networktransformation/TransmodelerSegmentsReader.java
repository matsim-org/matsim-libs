package gunnar.ihop2.transmodeler.networktransformation;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.newUnidirectionalLinkId;
import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.unquote;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import gunnar.ihop2.utils.AbstractTabularFileHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerSegmentsReader extends AbstractTabularFileHandler {

	private final String linkLabel = "Link";

	private final String abLabel = "AB";

	private final String baLabel = "BA";

	private final String lengthLabel = "Length";

	private final String lanesAbLabel = "Lanes_AB";

	private final String lanesBaLabel = "Lanes_BA";

	private final String positionLabel = "Position";

	private final Map<String, TransmodelerLink> id2link;

	private final Map<TransmodelerLink, SortedSet<TransmodelerSegment>> link2segments = new LinkedHashMap<TransmodelerLink, SortedSet<TransmodelerSegment>>();

	TransmodelerSegmentsReader(final String segmentFileName,
			final Map<String, TransmodelerLink> id2link) throws IOException {
		this.id2link = id2link;
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(segmentFileName, this);
	}

	Map<TransmodelerLink, SortedSet<TransmodelerSegment>> getSegments() {
		return this.link2segments;
	}

	@Override
	protected String preprocessColumnLabel(final String label) {
		return unquote(label);
	}

	private void addSegment(final TransmodelerLink link,
			final TransmodelerSegment segment) {
		SortedSet<TransmodelerSegment> segments = this.link2segments.get(link);
		if (segments == null) {
			segments = new TreeSet<TransmodelerSegment>();
			this.link2segments.put(link, segments);
		}
		segments.add(segment);
	}

	@Override
	public void startDataRow(final String[] row) {
		final String bidirectionalLinkId = row[this.index(this.linkLabel)];
		final int position = parseInt(row[this.index(this.positionLabel)]);
		final double length = parseDouble(row[this.index(this.lengthLabel)]);
		{
			final String abDirection = unquote(row[this.index(this.abLabel)]);
			if (abDirection.length() > 0) {
				final TransmodelerSegment segment = new TransmodelerSegment(
						parseInt(row[this.index(this.lanesAbLabel)]), length,
						position);
				this.addSegment(this.id2link.get(newUnidirectionalLinkId(
						bidirectionalLinkId, abDirection)), segment);
				System.out.println("read segment: " + segment);
			}
		}
		{
			final String baDirection = unquote(row[this.index(this.baLabel)]);
			if (baDirection.length() > 0) {
				final TransmodelerSegment segment = new TransmodelerSegment(
						parseInt(row[this.index(this.lanesBaLabel)]), length,
						position);
				this.addSegment(this.id2link.get(newUnidirectionalLinkId(
						bidirectionalLinkId, baDirection)), segment);
				System.out.println("read segment: " + segment);
			}
		}
	}
}
