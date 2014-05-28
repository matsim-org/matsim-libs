package org.matsim.contrib.josm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * The Test which is used for the validation of MATSim content.
 * 
 * 
 */
class MATSimTest extends Test {

	Map<String, ArrayList<Way>> linkIds;
	Map<String, ArrayList<Node>> nodeIds;
	NetworkLayer layer;
	Network network;

	Map<TestError, String> links2Fix;
	Map<TestError, String> nodes2Fix;

	int visitCount = 0;

	protected final static int DUPLICATE_LINK_ID = 3001;
	protected final static int DUPLICATE_NODE_ID = 3002;
	protected final static int DOUBTFUL_LINK_ATTRIBUTE = 3003;

	public MATSimTest() {
		super(tr("MATSimValidation"),
				tr("Validates MATSim-related network data"));
	}

	@Override
	public void startTest(ProgressMonitor monitor) {
		super.startTest(monitor);
		this.nodeIds = new HashMap<String, ArrayList<Node>>();
		this.linkIds = new HashMap<String, ArrayList<Way>>();
		if (Main.main.getActiveLayer() instanceof NetworkLayer) {
			layer = (NetworkLayer) Main.main.getActiveLayer();
			this.network = ((NetworkLayer) layer).getMatsimNetwork();
			monitor.setTicksCount(layer.data.getNodes().size()
					+ layer.data.getWays().size());
		}
	}

	@Override
	public void visit(Way w) {
		if (this.network != null) {
			for (Link link : layer.getWay2Links().get(w)) {
				String origId = ((LinkImpl) link).getOrigId();
				if (!linkIds.containsKey(origId)) {
					linkIds.put(origId, new ArrayList<Way>());
				}

				linkIds.get(origId).add(w);

				if (doubtfulAttributes(link)) {
					String msg = ("Link contains doubtful attributes");
					Collection<Way> way = Collections.singleton(w);
					errors.add(new TestError(this, Severity.WARNING, msg,
							DOUBTFUL_LINK_ATTRIBUTE, way, way));
				}
			}
		}
		visitCount++;
	}

	private boolean doubtfulAttributes(Link link) {
		if (link.getFreespeed() == 0 || link.getCapacity() == 0
				|| link.getLength() == 0 || link.getNumberOfLanes() == 0) {
			return true;
		}
		return false;
	}

	@Override
	public void visit(Node n) {
		if (this.network != null) {
			org.matsim.api.core.v01.network.Node node = network.getNodes().get(
					new IdImpl(n.getUniqueId()));
			String origId = ((NodeImpl) node).getOrigId();
			if (!nodeIds.containsKey(origId)) {
				nodeIds.put(origId, new ArrayList<Node>());
			}
			nodeIds.get(origId).add(n);
		}
		visitCount++;
	}

	@Override
	public void endTest() {

		links2Fix = new HashMap<TestError, String>();
		for (Entry<String, ArrayList<Way>> entry : linkIds.entrySet()) {
			if (entry.getValue().size() > 1) {
				List<WaySegment> segments = new ArrayList<WaySegment>();
				for (Way way : entry.getValue()) {
					List<Link> links = layer.getWay2Links().get(way);
					for (Link link : links) {
						if (((LinkImpl) link).getOrigId().equalsIgnoreCase(
								entry.getKey())) {
							segments.addAll(layer.getLink2Segments().get(link));
						}
					}
				}

				String msg = "Duplicated Id " + (entry.getKey() + " not allowed.");
				TestError error = new TestError(this, Severity.ERROR, msg,
						DUPLICATE_LINK_ID, entry.getValue(), segments);
				errors.add(error);
				links2Fix.put(error, entry.getKey());
			}

		}
		for (Entry<String, ArrayList<Node>> entry : nodeIds.entrySet()) {
			if (entry.getValue().size() > 1) {

				String msg = "Duplicated Id " +  (entry.getKey() + " not allowed.");
				TestError error = new TestError(this, Severity.ERROR, msg,
						DUPLICATE_NODE_ID, entry.getValue(), entry.getValue());
				errors.add(error);

			}
		}
		super.endTest();
		linkIds = null;
		nodeIds = null;
	}

	@Override
	public boolean isFixable(TestError testError) {
		 if (testError.getCode() == DUPLICATE_LINK_ID
		 || testError.getCode() == DUPLICATE_NODE_ID) {
		 return true;
		 }
		return false;
	}

	@Override
	public Command fixError(TestError testError) {
		if (!isFixable(testError)) {
			return null;
		}
		if (testError.getCode() == 3001 || testError.getCode() == 3002) {
			int i = 1;
			int j = 1;
			for (OsmPrimitive primitive : testError.getPrimitives()) {
				if (primitive instanceof Way) {
					if (links2Fix.containsKey(testError)) {
						String id2Fix = links2Fix.get(testError);
						for (Link link : layer.getWay2Links().get(primitive)) {
							if (((LinkImpl) link).getOrigId().equalsIgnoreCase(
									id2Fix)) {
								((LinkImpl) link).setOrigId(id2Fix + "(" + i
										+ ")");
								i++;
							}
						}
					}
				} else if (primitive instanceof Node) {
					org.matsim.api.core.v01.network.Node node = this.network
							.getNodes()
							.get(new IdImpl(((Node) primitive).getUniqueId()));
					String origId = ((NodeImpl) node).getOrigId();
					((NodeImpl) node).setOrigId(origId + "(" + j + ")");
					j++;
				}
			}

		}
		return null;// undoRedo handling done in mergeNodes
	}
}
