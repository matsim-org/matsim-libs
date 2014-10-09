package org.matsim.contrib.josm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.FileExporter;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.SAXException;

/**
 * The ToggleDialog that shows link information of selected ways and route
 * information of selected route relation elements
 * 
 * @author Nico
 */

@SuppressWarnings("serial")
class MATSimToggleDialog extends ToggleDialog implements LayerChangeListener,
		PreferenceChangedListener {
	private JTable table_links;
	private JTable table_pt;
	private OsmDataLayer layer;
	private MATSimTableModel_links tableModel_links;
	private MATSimTableModel_pt tableModel_pt;
	private JButton networkAttributes = new JButton(new ImageProvider(
			"dialogs", "edit").setWidth(16).get());
	private JButton manualConvert = new JButton(new ImageProvider("restart")
			.setWidth(16).get());
	private List<FileExporter> exporterCopy = new ArrayList<FileExporter>();
	private Scenario currentScenario;
	private Map<Way, List<Link>> way2Links = new HashMap<Way, List<Link>>();
	private Map<Link, List<WaySegment>> link2Segments = new HashMap<Link, List<WaySegment>>();
	private Map<Relation, TransitRoute> relation2Route = new HashMap<Relation, TransitRoute>();
	private NetworkListener osmNetworkListener;

	public MATSimToggleDialog() {
		super("Links/Nodes", "logo.png", "Links/Nodes", null, 150, true,
				Preferences.class);
		Main.pref.addPreferenceChangeListener(this);

		exporterCopy.addAll(ExtensionFileFilter.exporters);

		// table for link data
		table_links = new JTable();
		table_links.setDefaultRenderer(Object.class, new MATSimTableRenderer());
		table_links.setAutoCreateRowSorter(true);
		table_links.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// table for route data
		table_pt = new JTable();
		table_pt.setDefaultRenderer(Object.class, new MATSimTableRenderer());
		table_pt.setAutoCreateRowSorter(true);
		table_pt.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane tableContainer_links = new JScrollPane(table_links);
		JScrollPane tableContainer_pt = new JScrollPane(table_pt);

		JTabbedPane tabPane = new JTabbedPane();
		tabPane.addTab("Links", tableContainer_links);
		tabPane.addTab("Routes", tableContainer_pt);
		createLayout(tabPane, false, null);

		networkAttributes.setToolTipText(tr("edit network attributes"));
		networkAttributes.setBorder(BorderFactory.createEmptyBorder());
		networkAttributes.addActionListener(new ActionListener() {
			@Override
			// open dialog that allows editing of global network attributes
			public void actionPerformed(ActionEvent e) {
				NetworkAttributes dialog = new NetworkAttributes();
				JOptionPane pane = new JOptionPane(dialog,
						JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				dialog.setOptionPane(pane);
				JDialog dlg = pane.createDialog(Main.parent,
						tr("Network Attributes"));
				dlg.setAlwaysOnTop(true);
				dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dlg.setVisible(true);
				if (pane.getValue() != null) {
					if (((Integer) pane.getValue()) == JOptionPane.OK_OPTION) {
						dialog.apply();
					}
				}
				dlg.dispose();
			}
		});
		this.titleBar.add(networkAttributes,
				this.titleBar.getComponentCount() - 3);

		// invoke conversion of whole layer
		manualConvert.setToolTipText(tr("convert layer from scratch"));
		manualConvert.setBorder(BorderFactory.createEmptyBorder());
		manualConvert.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Layer tmpLayer = Main.main.getActiveLayer();
				if (tmpLayer instanceof OsmDataLayer) {
					currentScenario = ScenarioUtils.createScenario(ConfigUtils
							.createConfig());
					currentScenario.getConfig().scenario().setUseTransit(true);
					currentScenario.getConfig().scenario().setUseVehicles(true);
					way2Links = new HashMap<Way, List<Link>>();
					link2Segments = new HashMap<Link, List<WaySegment>>();
					relation2Route = new HashMap<Relation, TransitRoute>();
					LayerChangeTask task = new LayerChangeTask(
							(OsmDataLayer) tmpLayer);
					task.run();

					tableModel_links = new MATSimTableModel_links(
							currentScenario.getNetwork());
					table_links.setModel(tableModel_links);
					tableModel_pt = new MATSimTableModel_pt();
					table_pt.setModel(tableModel_pt);
					notifyDataChanged(currentScenario);
					networkAttributes.setEnabled(true);
					layer = (OsmDataLayer) tmpLayer;
					checkInternalIdColumn();
				}
			}
		});
		this.titleBar.add(manualConvert, this.titleBar.getComponentCount() - 3);
	}

	// called when MATSim data changes to update title bar
	public void notifyDataChanged(Scenario scenario) {
		setTitle(tr(
				"Links: {0} / Nodes: {1} / Stops: {2} / Lines: {3} / Routes: {4}",
				scenario.getNetwork().getLinks().size(), scenario.getNetwork()
						.getNodes().size(), scenario.getTransitSchedule()
						.getFacilities().size(), scenario.getTransitSchedule()
						.getTransitLines().size(), relation2Route.size()));
		tableModel_links.networkChanged();
		tableModel_pt.scheduleChanged();
	}

	@Override
	// react to active layer (active data set) changes by setting the current
	// data mappings
	// MATSim layers contain data mappings while OsmDataLayers must first be
	// converted
	// also adjusts standard file export formats
	public void activeLayerChange(Layer oldLayer, Layer newLayer) {
		DataSet.removeSelectionListener(tableModel_links);
		DataSet.removeSelectionListener(tableModel_pt);

		// clear old data set listeners
		if (osmNetworkListener != null && oldLayer != null
				&& oldLayer instanceof OsmDataLayer) {
			((OsmDataLayer) oldLayer).data.clearSelection();
			((OsmDataLayer) oldLayer).data
					.removeDataSetListener(osmNetworkListener);
		}
		table_links.getSelectionModel().removeListSelectionListener(
				tableModel_links);
		table_links.getSelectionModel().removeListSelectionListener(
				tableModel_pt);

		if (newLayer instanceof OsmDataLayer) {
			if (newLayer instanceof MATSimLayer) {
				currentScenario = ((MATSimLayer) newLayer).getMatsimScenario();
				currentScenario.getConfig().scenario().setUseTransit(true);
				currentScenario.getConfig().scenario().setUseVehicles(true);
				way2Links = ((MATSimLayer) newLayer).getWay2Links();
				link2Segments = ((MATSimLayer) newLayer).getLink2Segments();
				relation2Route = ((MATSimLayer) newLayer).getRelation2Route();
				ExtensionFileFilter.exporters.clear();
				ExtensionFileFilter.exporters.add(0,
						new MATSimNetworkFileExporter());
				this.manualConvert.setEnabled(false);
			} else {
				this.manualConvert.setEnabled(true);
				currentScenario = ScenarioUtils.createScenario(ConfigUtils
						.createConfig());
				currentScenario.getConfig().scenario().setUseTransit(true);
				currentScenario.getConfig().scenario().setUseVehicles(true);
				way2Links = new HashMap<Way, List<Link>>();
				link2Segments = new HashMap<Link, List<WaySegment>>();
				relation2Route = new HashMap<Relation, TransitRoute>();
				LayerChangeTask task = new LayerChangeTask(
						(OsmDataLayer) newLayer);
				task.run();

				if (oldLayer instanceof MATSimLayer || oldLayer == null) {
					ExtensionFileFilter.exporters.clear();
					ExtensionFileFilter.exporters.addAll(this.exporterCopy);
				}
			}
			if (currentScenario != null) {
				tableModel_links = new MATSimTableModel_links(
						currentScenario.getNetwork());
				table_links.setModel(tableModel_links);
				tableModel_pt = new MATSimTableModel_pt();
				table_pt.setModel(tableModel_pt);
				notifyDataChanged(currentScenario);
				this.networkAttributes.setEnabled(true);
				this.layer = (OsmDataLayer) newLayer;
				checkInternalIdColumn();
			} else { // empty data mappings if no MATSim scenario is set
				table_links.setModel(new DefaultTableModel());
				table_pt.setModel(new DefaultTableModel());
				setTitle(tr("Links/Nodes"));
				networkAttributes.setEnabled(false);
				this.layer = null;
			}
		} else { // empty data mappings if no data layer is active
			table_links.setModel(new DefaultTableModel());
			table_pt.setModel(new DefaultTableModel());
			setTitle(tr("Links/Nodes"));
			networkAttributes.setEnabled(false);
			way2Links = null;
			link2Segments = null;
			relation2Route = null;
		}

		// set converted links that are to be drawn blue by map renderer
		MapRenderer.setWay2Links(way2Links);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void preferenceChanged(PreferenceChangeEvent e) {
		if (e.getKey().equalsIgnoreCase("matsim_showInternalIds")) {
			checkInternalIdColumn();
		}
	}

	// hecks if internal-id should be shown
	private void checkInternalIdColumn() {
		if (!Main.pref.getBoolean("matsim_showInternalIds", false)) {
			table_links.getColumn("internal-id").setMinWidth(0);
			table_links.getColumn("internal-id").setMaxWidth(0);
		} else {
			table_links.getColumn("internal-id").setMaxWidth(
					table_links.getColumn("id").getMaxWidth());
			table_links.getColumn("internal-id").setWidth(
					table_links.getColumn("id").getWidth());
		}
	}

	private class MATSimTableRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			setBackground(null);
			Component tableCellRendererComponent = super
					.getTableCellRendererComponent(table, value, isSelected,
							hasFocus, row, column);
			return tableCellRendererComponent;
		}
	}

	// handles the underlying data of the links table
	private class MATSimTableModel_links extends AbstractTableModel implements
			SelectionChangedListener, ListSelectionListener {

		private String[] columnNames = { "id", "internal-id", "length",
				"freespeed", "capacity", "permlanes", "modes" };

		private Network network;

		private Map<Integer, Id<Link>> links;

		MATSimTableModel_links(Network network) {
			this.network = network;
			this.links = new HashMap<Integer, Id<Link>>();
			DataSet.addSelectionListener(this);
			table_links.getSelectionModel().addListSelectionListener(this);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0) {
				return String.class;
			} else if (columnIndex == 1) {
				return String.class;
			} else if (columnIndex == 2) {
				return Double.class;
			} else if (columnIndex == 3) {
				return Double.class;
			} else if (columnIndex == 4) {
				return Double.class;
			} else if (columnIndex == 5) {
				return Double.class;
			} else if (columnIndex == 6) {
				return String.class;
			}
			throw new RuntimeException();
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		public void networkChanged() {
			if (layer != null) {
				if (!layer.data.selectionEmpty()) {
					selectionChanged(layer.data.getSelected());
				}
			}
			fireTableDataChanged();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return links.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Id<Link> id = links.get(rowIndex);
			Link link = network.getLinks().get(id);
			if (columnIndex == 0) {
				return ((LinkImpl) link).getOrigId();
			} else if (columnIndex == 1) {
				return link.getId().toString();
			} else if (columnIndex == 2) {
				return link.getLength();
			} else if (columnIndex == 3) {
				return link.getFreespeed();
			} else if (columnIndex == 4) {
				return link.getCapacity();
			} else if (columnIndex == 5) {
				return link.getNumberOfLanes();
			} else if (columnIndex == 6) {
				return link.getAllowedModes().toString();
			}
			throw new RuntimeException();
		}

		@Override
		// change shown link information of selected elements when selection
		// changes
		public void selectionChanged(
				Collection<? extends OsmPrimitive> newSelection) {
			layer.data.clearHighlightedWaySegments();
			this.links = new HashMap<Integer, Id<Link>>();
			int i = 0;
			for (OsmPrimitive primitive : newSelection) {
				if (primitive instanceof Way) {
					if (way2Links.containsKey(primitive)) {
						for (Link link : way2Links.get(primitive)) {
							links.put(i, link.getId());
							i++;
						}
					}
				}
			}
			fireTableDataChanged();
		}

		@Override
		// highlight and zoom to way segments that refer to the selected link in
		// the table
		public void valueChanged(ListSelectionEvent e) {
			if (!layer.data.selectionEmpty() && !e.getValueIsAdjusting()
					&& !((ListSelectionModel) e.getSource()).isSelectionEmpty()) {
				int row = table_links.getRowSorter().convertRowIndexToModel(
						table_links.getSelectedRow());
				String tempId = (String) this.getValueAt(row, 1);
				Link link = network.getLinks().get(
						Id.create(tempId, Link.class));
				if (link2Segments.containsKey(link)) {
					List<WaySegment> segments = link2Segments.get(link);
					layer.data.setHighlightedWaySegments(segments);
					Collection<OsmPrimitive> zoom = new ArrayList<OsmPrimitive>();
					if (!segments.isEmpty()) {
						zoom.add(segments.get(0).way);
						AutoScaleAction.zoomTo(zoom);
						Main.map.mapView.repaint();
					}
				}
			}
		}
	}

	// handles the underlying data of the routes table
	private class MATSimTableModel_pt extends AbstractTableModel implements
			SelectionChangedListener, ListSelectionListener {

		private String[] columnNames = { "route id", "mode", "#stops", "#links" };


		private Map<Integer, TransitRoute> routes;

		MATSimTableModel_pt() {
			this.routes = new HashMap<Integer, TransitRoute>();
			DataSet.addSelectionListener(this);
			table_pt.getSelectionModel().addListSelectionListener(this);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0) {
				return String.class;
			} else if (columnIndex == 1) {
				return String.class;
			} else if (columnIndex == 2) {
				return Integer.class;
			} else if (columnIndex == 3) {
				return Integer.class;
			}
			throw new RuntimeException();
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		public void scheduleChanged() {
			if (layer != null) {
				if (!layer.data.selectionEmpty()) {
					selectionChanged(layer.data.getSelected());
				}
			}
			fireTableDataChanged();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return routes.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			TransitRoute route = routes.get(rowIndex);
			if (columnIndex == 0) {
				return route.getId();
			} else if (columnIndex == 1) {
				return route.getTransportMode();
			} else if (columnIndex == 2) {
				return route.getStops().size();
			} else if (columnIndex == 3) {
				return route.getRoute().getLinkIds().size() + 2;
			}
			throw new RuntimeException();
		}

		@Override
		// change shown route information of selected elements when selection
		// changes
		public void selectionChanged(
				Collection<? extends OsmPrimitive> newSelection) {
			layer.data.clearHighlightedWaySegments();
			this.routes = new HashMap<Integer, TransitRoute>();
			int i = 0;
			for (OsmPrimitive primitive : newSelection) {
				for (OsmPrimitive primitive_2 : primitive.getReferrers()) {
					if (relation2Route.containsKey(primitive_2)) {
						routes.put(i, relation2Route.get(primitive_2));
						i++;
					}
				}
			}
			fireTableDataChanged();
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {

		}
	}

	// dialog that handles editing of global network attributes
	private class NetworkAttributes extends JPanel {

		private JLabel laneWidth = new JLabel("effective lane width [m]:");
		private JLabel capacityPeriod = new JLabel("capacity period [s]:");
		private JTextField laneWidthValue = new JTextField();
		private JTextField capacityPeriodValue = new JTextField();

		public NetworkAttributes() {
			Layer layer = Main.main.getActiveLayer();
			if (layer instanceof MATSimLayer) {
				laneWidthValue.setText(String.valueOf(((MATSimLayer) layer)
						.getMatsimScenario().getNetwork()
						.getEffectiveLaneWidth()));
				capacityPeriodValue.setText(String
						.valueOf(((MATSimLayer) layer).getMatsimScenario()
								.getNetwork().getCapacityPeriod()));
			}
			add(laneWidth);
			add(laneWidthValue);
			add(capacityPeriod);
			add(capacityPeriodValue);
		}

		public void setOptionPane(JOptionPane optionPane) {
		}

		protected void apply() {
			Layer layer = Main.main.getActiveLayer();
			if (layer instanceof MATSimLayer) {
				String lW = laneWidthValue.getText();
				String cP = capacityPeriodValue.getText();
				if (!lW.isEmpty()) {
					((NetworkImpl) ((MATSimLayer) layer).getMatsimScenario()
							.getNetwork()).setEffectiveLaneWidth(Double
							.parseDouble(lW));
				}
				if (!cP.isEmpty()) {
					((NetworkImpl) ((MATSimLayer) layer).getMatsimScenario()
							.getNetwork()).setCapacityPeriod(Double
							.parseDouble(cP));
				}
			}
		}
	}

	@Override
	public void layerAdded(Layer newLayer) {
	}

	@Override
	public void layerRemoved(Layer oldLayer) {

	}

	private class LayerChangeTask extends PleaseWaitRunnable {

		private OsmDataLayer newLayer;

		public LayerChangeTask(OsmDataLayer newLayer) {
			super("Converting to MATSim Network");
			this.newLayer = newLayer;
		}

		@Override
		protected void cancel() {

		}

		@Override
		protected void finish() {
			notifyDataChanged(currentScenario);
			osmNetworkListener = new NetworkListener(currentScenario,
					way2Links, link2Segments, relation2Route);
			newLayer.data.addDataSetListener(osmNetworkListener);

		}

		@Override
		protected void realRun() throws SAXException, IOException,
				OsmTransferException {
			NewConverter.convertOsmLayer(newLayer, currentScenario, way2Links,
					link2Segments, relation2Route);
		}
	}
}
