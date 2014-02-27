package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class MATSimTest extends Test {

	Map<String, ArrayList<Way>> linkIds;
	Map<String, ArrayList<Node>> nodeIds;
	Network network;

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
		this.linkIds = new HashMap<String, ArrayList<Way>>();
		this.nodeIds = new HashMap<String, ArrayList<Node>>();
		Layer layer = Main.main.getActiveLayer();
		if (layer instanceof NetworkLayer) {
			this.network = ((NetworkLayer) layer).getMatsimNetwork();
		}
	}

	@Override
	public void visit(Way w) {
		if (this.network != null) {
			for (Link link : network.getLinks().values()) {
				if (String.valueOf(w.getUniqueId()).equalsIgnoreCase(
						link.getId().toString())) {
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
		}
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
			for (org.matsim.api.core.v01.network.Node node : network.getNodes()
					.values()) {
				if (String.valueOf(n.getUniqueId()).equalsIgnoreCase(
						node.getId().toString())) {
					String origId = ((NodeImpl) node).getOrigId();
					if (!nodeIds.containsKey(origId)) {
						nodeIds.put(origId, new ArrayList<Node>());
					}
					nodeIds.get(origId).add(n);
				}
			}
		}
	}

	@Override
	public void endTest() {
		for (Entry<String, ArrayList<Way>> entry : linkIds.entrySet()) {
			if (entry.getValue().size() > 1) {
				String msg = ("Duplicated Id " + entry.getKey() + " not allowed.");
				errors.add(new TestError(this, Severity.ERROR, msg,
						DUPLICATE_LINK_ID, entry.getValue(), entry.getValue()));
			}
		}
		for (Entry<String, ArrayList<Node>> entry : nodeIds.entrySet()) {
			if (entry.getValue().size() > 1) {

				String msg = ("Duplicated Id " + entry.getKey() + " not allowed.");
				errors.add(new TestError(this, Severity.ERROR, msg,
						DUPLICATE_NODE_ID, entry.getValue(), entry.getValue()));
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
					Link link = this.network.getLinks().get(
							new IdImpl(((Way) primitive).getUniqueId()));
					String origId = ((LinkImpl) link).getOrigId();
					((LinkImpl) link).setOrigId(origId + "(" + i + ")");
					i++;
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
