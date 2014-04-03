package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.io.FileExporter;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The ToggleDialog that shows link information of selected ways
 * 
 * 
 */
public class MATSimToggleDialog extends ToggleDialog implements
		LayerChangeListener, PreferenceChangedListener {
	private JTable table;
	private NetworkLayer layer;
	private MATSimTableModel tableModel;
	private JButton networkAttributes = new JButton(new ImageProvider(
			"dialogs", "edit").setWidth(16).get());
	private List<FileExporter> exporterCopy = new ArrayList<FileExporter>();

	public MATSimToggleDialog() {
		super("Links/Nodes", "logo.png", "Links/Nodes", null, 150, true,
				Preferences.class);
		Main.pref.addPreferenceChangeListener(this);

		exporterCopy.addAll(ExtensionFileFilter.exporters);

		table = new JTable();
		table.setDefaultRenderer(Object.class, new MATSimTableRenderer());
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane tableContainer = new JScrollPane(table);
		createLayout(tableContainer, false, null);

		networkAttributes.setToolTipText(tr("edit network attributes"));
		networkAttributes.setBorder(BorderFactory.createEmptyBorder());
		networkAttributes.addActionListener(new ActionListener() {
			@Override
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
		this.titleBar.add(networkAttributes);
	}

	public void notifyDataChanged(NetworkLayer layer) {
		setTitle(tr("Links: {0} / Nodes: {1}", layer.getMatsimNetwork()
				.getLinks().size(), layer.getMatsimNetwork().getNodes().size()));
		tableModel.networkChanged();
	}

	@Override
	public void activeLayerChange(Layer oldLayer, Layer newLayer) {
		if (newLayer instanceof NetworkLayer) {
			layer = (NetworkLayer) newLayer;
			tableModel = new MATSimTableModel(layer.getMatsimNetwork());
			table.setModel(tableModel);

			notifyDataChanged(layer);
			this.networkAttributes.setEnabled(true);
			if (!(oldLayer instanceof NetworkLayer)) {
				ExtensionFileFilter.exporters.clear();
				ExtensionFileFilter.exporters.add(0,
						new MATSimNetworkFileExporter());
			}
			networkAttributes.setEnabled(true);
			checkInternalIdColumn();
		} else {
			if (oldLayer instanceof NetworkLayer) {
				ExtensionFileFilter.exporters.clear();
				ExtensionFileFilter.exporters.addAll(this.exporterCopy);
			}
			table.setModel(new DefaultTableModel());
			setTitle(tr("Links/Nodes"));
			layer = null;
			networkAttributes.setEnabled(false);
		}

	}

	@Override
	public void preferenceChanged(PreferenceChangeEvent e) {
		if (e.getKey().equalsIgnoreCase("matsim_showInternalIds")) {
			checkInternalIdColumn();
		}
	}

	private void checkInternalIdColumn() {
		if (!Main.pref.getBoolean("matsim_showInternalIds", false)) {
			table.getColumn("internal-id").setMinWidth(0);
			table.getColumn("internal-id").setMaxWidth(0);
		} else {
			table.getColumn("internal-id").setMaxWidth(
					table.getColumn("id").getMaxWidth());
			table.getColumn("internal-id").setWidth(
					table.getColumn("id").getWidth());
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

	private class MATSimTableModel extends AbstractTableModel implements
			SelectionChangedListener, ListSelectionListener {

		private String[] columnNames = { "id", "internal-id", "length",
				"freespeed", "capacity", "permlanes" };

		private Network network;

		private Map<Integer, Id> links;

		final HighlightHelper helper = new HighlightHelper();

		MATSimTableModel(Network network) {
			this.network = network;
			this.links = new HashMap<Integer, Id>();
			DataSet.addSelectionListener(this);
			table.getSelectionModel().addListSelectionListener(this);
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
			}
			throw new RuntimeException();
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		public void networkChanged() {
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
			Id id = links.get(rowIndex);
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
			}
			throw new RuntimeException();
		}

		@Override
		public void selectionChanged(
				Collection<? extends OsmPrimitive> newSelection) {
			if (Main.main.getActiveLayer() instanceof NetworkLayer) {
				this.links = new HashMap<Integer, Id>();
				int i = 0;
				for (OsmPrimitive primitive : newSelection) {
					if (primitive instanceof Way) {
						if (layer.getWay2Links().containsKey(primitive)) {
							for (Link link : layer.getWay2Links()
									.get(primitive)) {
								links.put(i, link.getId());
								i++;
							}
						}
					}
				}
				fireTableDataChanged();
			}
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			helper.clear();
			if (!layer.data.selectionEmpty() && !e.getValueIsAdjusting()
					&& !((ListSelectionModel) e.getSource()).isSelectionEmpty()) {
				int row = table.getRowSorter().convertRowIndexToModel(
						table.getSelectedRow());
				long id = Long.parseLong((String) this.getValueAt(row, 1));
				Way way = (Way) layer.data.getPrimitiveById(id,
						OsmPrimitiveType.WAY);
				helper.highlightOnly(way);
				AutoScaleAction.zoomTo(Collections
						.singleton(((OsmPrimitive) (way))));
				Main.map.mapView.repaint();
			}
		}
	}

	private class NetworkAttributes extends JPanel {

		private JOptionPane optionPane;
		private JLabel laneWidth = new JLabel("effective lane width [m]:");
		private JLabel capacityPeriod = new JLabel("capacity period [s]:");
		private JTextField laneWidthValue = new JTextField();
		private JTextField capacityPeriodValue = new JTextField();

		public NetworkAttributes() {
			Layer layer = Main.main.getActiveLayer();
			if (layer instanceof NetworkLayer) {
				laneWidthValue.setText(String.valueOf(((NetworkLayer) layer)
						.getMatsimNetwork().getEffectiveLaneWidth()));
				capacityPeriodValue.setText(String
						.valueOf(((NetworkLayer) layer).getMatsimNetwork()
								.getCapacityPeriod()));
			}
			add(laneWidth);
			add(laneWidthValue);
			add(capacityPeriod);
			add(capacityPeriodValue);
		}

		public void setOptionPane(JOptionPane optionPane) {
			this.optionPane = optionPane;
		}

		protected void apply() {
			Layer layer = Main.main.getActiveLayer();
			if (layer instanceof NetworkLayer) {
				String lW = laneWidthValue.getText();
				String cP = capacityPeriodValue.getText();
				if (!lW.isEmpty()) {
					((NetworkImpl) ((NetworkLayer) layer).getMatsimNetwork())
							.setEffectiveLaneWidth(Double.parseDouble(lW));
				}
				if (!cP.isEmpty()) {
					((NetworkImpl) ((NetworkLayer) layer).getMatsimNetwork())
							.setCapacityPeriod(Double.parseDouble(cP));
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

}
