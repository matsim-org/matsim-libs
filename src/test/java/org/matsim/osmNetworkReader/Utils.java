package org.matsim.osmNetworkReader;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.pbf.seq.PbfWriter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

@Log4j2
public class Utils {

    static final CoordinateTransformation transformation = new IdentityTransformation();
    static final String MOTORWAY = "motorway";
    static final String TRUNK = "trunk";
    static final String TERTIARY = "tertiary";


	static void writeOsmData(Collection<OsmNode> nodes, Collection<OsmWay> ways, Path file) {

		try (OutputStream outputStream = Files.newOutputStream(file)) {
			var writer = new PbfWriter(outputStream, true);
			for (OsmNode node : nodes) {
				writer.write(node);
			}

			for (OsmWay way : ways) {
				writer.write(way);
			}
			writer.complete();
		} catch (IOException e) {
			log.error("could not write osm data");
			e.printStackTrace();
		}
	}

	static WaysAndLinks createSingleLink() {
		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 100, 100);
		var node3 = new Node(3, 0, 200);
		var nodeReference = new TLongArrayList(new long[]{node1.getId(), node2.getId(), node3.getId()});
		var tags = List.of(new Tag(OsmTags.HIGHWAY, Utils.MOTORWAY));
		var way = new Way(1, nodeReference, tags);

		return new WaysAndLinks(List.of(node1, node2, node3), List.of(way));
	}

	static WaysAndLinks createTwoIntersectingLinksWithDifferentLevels() {
		var node1 = new Node(1, 0, 0);
		var node2 = new Node(2, 100, 100);
		var node3 = new Node(3, 0, 200);
		var node4 = new Node(4, 200, 0);
		var node5 = new Node(5, 200, 200);
		var nodeReferenceForWay1 = new TLongArrayList(new long[]{node1.getId(), node2.getId(), node3.getId()});
		var nodeReferenceForWay2 = new TLongArrayList(new long[]{node4.getId(), node2.getId(), node5.getId()});
		var way1 = new Way(1, nodeReferenceForWay1, List.of(new Tag(OsmTags.HIGHWAY, Utils.MOTORWAY)));
		var way2 = new Way(2, nodeReferenceForWay2, List.of(new Tag(OsmTags.HIGHWAY, Utils.TERTIARY)));
		return new WaysAndLinks(List.of(node1, node2, node3, node4, node5), List.of(way1, way2));
	}

	@Getter
	@RequiredArgsConstructor
	static class WaysAndLinks {

		private final List<OsmNode> nodes;
		private final List<OsmWay> ways;
	}
}
