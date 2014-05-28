package org.matsim.contrib.josm;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

/**
 * a layer which contains MATSim-network data to differ from normal OSM layers
 * 
 * @author nkuehnel
 * 
 */
class NetworkLayer extends OsmDataLayer {
	private Network matsimNetwork;
	private String coordSystem;

	private Map<Way, List<Link>> way2Links = new HashMap<Way, List<Link>>();
	private Map<Link, List<WaySegment>> link2Segment = new HashMap<Link, List<WaySegment>>();

	public String getCoordSystem() {
		return coordSystem;
	}

	public NetworkLayer(DataSet data, String name, File associatedFile,
			Network network, String coordSystem,
			HashMap<Way, List<Link>> way2Links, Map<Link, List<WaySegment>> link2Segment) {
		super(data, name, associatedFile);
		this.matsimNetwork = network;
		this.coordSystem = coordSystem;
		this.way2Links = way2Links;
		this.link2Segment = link2Segment;
		NetworkListener listener;
		try {
			listener = new NetworkListener(this, network, way2Links, link2Segment);
		} catch (IllegalArgumentException e) {
			 JOptionPane.showMessageDialog(
		                Main.parent,
		                "Could not initialize network listener with the given coordinate system.\nChanges on layer data will NOT affect the network.",
		                tr("Error"),
		                JOptionPane.ERROR_MESSAGE);
			 listener=null;
		}
		if (listener!=null) {
			data.addDataSetListener(listener);
		} 
	}

	public Map<Way, List<Link>> getWay2Links() {
		return way2Links;
	}
	
	public Map<Link, List<WaySegment>> getLink2Segments() {
		return link2Segment;
	}

	public Network getMatsimNetwork() {
		return matsimNetwork;
	}

	@Override
	public Action[] getMenuEntries() {
		if (Main.applet)
			return new Action[] {
					LayerListDialog.getInstance().createActivateLayerAction(
							this),
					LayerListDialog.getInstance().createShowHideLayerAction(),
					LayerListDialog.getInstance().createDeleteLayerAction(),
					SeparatorLayerAction.INSTANCE,
					new RenameLayerAction(getAssociatedFile(), this),
					SeparatorLayerAction.INSTANCE,
					new LayerListPopup.InfoAction(this) };
		List<Action> actions = new ArrayList<Action>();
		actions.addAll(Arrays.asList(new Action[] {
				LayerListDialog.getInstance().createActivateLayerAction(this),
				LayerListDialog.getInstance().createShowHideLayerAction(),
				LayerListDialog.getInstance().createDeleteLayerAction(),
				SeparatorLayerAction.INSTANCE, new LayerSaveAsAction(this), }));
		actions.addAll(Arrays.asList(new Action[] {
				SeparatorLayerAction.INSTANCE,
				new RenameLayerAction(getAssociatedFile(), this) }));
		actions.addAll(Arrays.asList(new Action[] {
				SeparatorLayerAction.INSTANCE,
				new LayerListPopup.InfoAction(this) }));
		return actions.toArray(new Action[actions.size()]);
	}

	@Override
	public boolean isMergable(final Layer other) {
		return false;
	}

	/**
	 * Replies true if the data managed by this layer needs to be uploaded to
	 * the server because it contains at least one modified primitive.
	 * 
	 * @return true if the data managed by this layer needs to be uploaded to
	 *         the server because it contains at least one modified primitive;
	 *         false, otherwise
	 */
	public boolean requiresUploadToServer() {
		return false;
	}

	@Override
	public Object getInfoComponent() {
		final DataCountVisitor counter = new DataCountVisitor();
		for (final OsmPrimitive osm : data.allPrimitives()) {
			osm.accept(counter);
		}
		final JPanel p = new JPanel(new GridBagLayout());

		String nodeText = trn("{0} node", "{0} nodes", counter.nodes,
				counter.nodes);
		if (counter.deletedNodes > 0) {
			nodeText += " ("
					+ trn("{0} deleted", "{0} deleted", counter.deletedNodes,
							counter.deletedNodes) + ")";
		}

		String wayText = trn("{0} way", "{0} ways", counter.ways, counter.ways);
		if (counter.deletedWays > 0) {
			wayText += " ("
					+ trn("{0} deleted", "{0} deleted", counter.deletedWays,
							counter.deletedWays) + ")";
		}

		String relationText = trn("{0} relation", "{0} relations",
				counter.relations, counter.relations);
		if (counter.deletedRelations > 0) {
			relationText += " ("
					+ trn("{0} deleted", "{0} deleted",
							counter.deletedRelations, counter.deletedRelations)
					+ ")";
		}

		p.add(new JLabel(tr("{0} consists of:", getName())), GBC.eol());
		p.add(new JLabel(nodeText, ImageProvider.get("data", "node"),
				JLabel.HORIZONTAL), GBC.eop().insets(15, 0, 0, 0));
		p.add(new JLabel(wayText, ImageProvider.get("data", "way"),
				JLabel.HORIZONTAL), GBC.eop().insets(15, 0, 0, 0));
		p.add(new JLabel(relationText, ImageProvider.get("data", "relation"),
				JLabel.HORIZONTAL), GBC.eop().insets(15, 0, 0, 0));
		p.add(new JLabel(tr("API version: {0}",
				(data.getVersion() != null) ? data.getVersion() : tr("unset"))),
				GBC.eop().insets(15, 0, 0, 0));
		p.add(new JLabel(this.matsimNetwork.getLinks().size() + " MATSim links"),
				GBC.eop().insets(15, 0, 0, 0));
		p.add(new JLabel(this.matsimNetwork.getNodes().size() + " MATSim nodes"),
				GBC.eop().insets(15, 0, 0, 0));

		return p;
	}
}
