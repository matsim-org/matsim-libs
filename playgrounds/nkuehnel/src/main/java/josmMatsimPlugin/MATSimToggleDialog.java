package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.Layer;

public class MATSimToggleDialog extends ToggleDialog implements
		LayerChangeListener, SelectionChangedListener {
	private JTable table;
	private Map<Layer, MATSimTableModel> tableModels = new HashMap<Layer, MATSimTableModel>();
	protected final static JCheckBox renderMatsim = new JCheckBox(
			"Activate MATSim Renderer");
	protected final static JCheckBox showIds = new JCheckBox("Show Ids");




	public MATSimToggleDialog() {
		super("Links/Nodes", "logo.png", "Links/Nodes", null, 150, true);
		DataSet.addSelectionListener(this);

		table = new JTable();
		table.setDefaultRenderer(Object.class, new MATSimTableRenderer());
		table.setEnabled(false);
		table.setAutoCreateRowSorter(true);

		JScrollPane tableContainer = new JScrollPane(table);

		JPanel overview = new JPanel(new BorderLayout());
		overview.add(tableContainer, BorderLayout.CENTER);

		JPanel options = new JPanel(new GridBagLayout());
		GridBagConstraints cOptions = new GridBagConstraints();

		showIds.addActionListener(new ShowIdsListener());
		renderMatsim.addActionListener(new RenderMatsimListener());

		showIds.setSelected(Defaults.showIds);
		renderMatsim.setSelected(Defaults.renderMatsim);

		cOptions.gridx = 0;
		cOptions.gridy = 0;
		options.add(renderMatsim, cOptions);
		cOptions.gridx = 1;
		options.add(showIds, cOptions);

		JTabbedPane pane = new JTabbedPane(JTabbedPane.TOP);
		pane.addTab("Overview", overview);
		pane.addTab("Options", options);
		createLayout(pane, false, null);
	}

	private void clearTable() {
		table.setModel(new DefaultTableModel());
		setTitle(tr("Links/Nodes"));
	}

	private void paintTable(NetworkLayer layer) {
		title(layer);
		table.setModel(tableModels.get(layer));
	}

	private void createTableModel(NetworkLayer layer) {
		MATSimTableModel model = new MATSimTableModel(layer.getMatsimNetwork());
		tableModels.put(layer, model);
	}

	public void title(NetworkLayer layer) {
		setTitle(tr("Links: {0} / Nodes: {1}", layer.getMatsimNetwork()
				.getLinks().size(), layer.getMatsimNetwork().getNodes().size()));
		tableModels.get(layer).networkChanged();
	}

	@Override
	public void activeLayerChange(Layer oldLayer, Layer newLayer) {
		if (newLayer instanceof NetworkLayer) {
			paintTable((NetworkLayer) newLayer);
		} else {
			clearTable();
		}
	}

	@Override
	public void layerAdded(Layer newLayer) {
		if (newLayer instanceof NetworkLayer)
			createTableModel((NetworkLayer) newLayer);
	}

	@Override
	public void layerRemoved(Layer oldLayer) {
		if (oldLayer instanceof NetworkLayer)
			tableModels.remove(oldLayer);
	}

	@Override
	public void selectionChanged(Collection<? extends OsmPrimitive> primitives) {
		table.clearSelection();
		for (OsmPrimitive prim : primitives) {
			if (prim instanceof Way) {
				String id = String.valueOf(prim.getUniqueId());
				for (int i = 0; i < table.getRowCount(); i++) {
					if (id.equalsIgnoreCase(table.getValueAt(i, 1).toString())) {
						table.addRowSelectionInterval(i, i);
					}
				}
			}
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

	private class MATSimTableModel extends AbstractTableModel {
		
		private String[] columnNames = { "id", "internal-id", "length",
				"freespeed", "capacity", "permlanes" };

		private Network network;

		private ArrayList<Id> links;

		MATSimTableModel(Network network) {
			this.network = network;
			this.links = new ArrayList<Id>(network.getLinks().keySet());
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
			this.links = new ArrayList<Id>(network.getLinks().keySet());
			fireTableDataChanged();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return network.getLinks().size();
		}

		@Override
		public Object getValueAt(int arg0, int arg1) {
			Link link = network.getLinks().get(links.get(arg0));
			if (arg1 == 0) {
				return ((LinkImpl) link).getOrigId();
			} else if (arg1 == 1) {
				return link.getId().toString();
			} else if (arg1 == 2) {
				return link.getLength();
			} else if (arg1 == 3) {
				return link.getFreespeed();
			} else if (arg1 == 4) {
				return link.getCapacity();
			} else if (arg1 == 5) {
				return link.getNumberOfLanes();
			}
			throw new RuntimeException();
		}
	}

	private class ShowIdsListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (!showIds.isSelected()) {
				Defaults.showIds = false;
			} else {
				Defaults.showIds = true;
			}
			Main.map.mapView.repaint();
		}
	}

	private class RenderMatsimListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (!renderMatsim.isSelected()) {
				Defaults.renderMatsim = false;
				showIds.setEnabled(false);
			} else {
				Defaults.renderMatsim = true;
				showIds.setEnabled(true);
			}
			Main.map.mapView.repaint();
		}
	}

}
