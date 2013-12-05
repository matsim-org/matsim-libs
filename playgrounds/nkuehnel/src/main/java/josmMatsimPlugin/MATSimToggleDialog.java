package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
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

    private String[] columnNames = { "id", "internal-id", "length",
	    "freespeed", "capacity", "permlanes" };

    public MATSimToggleDialog() {
	super("Links/Nodes", "logo.png", "Links/Nodes", null, 150, true);
	DataSet.addSelectionListener(this);
	table = new JTable();

	table.setDefaultRenderer(Object.class, new MATSimTableRenderer());
	table.setEnabled(false);
	table.setAutoCreateRowSorter(true);

	Object[][] data = new Object[0][6];

	table.setModel(new DefaultTableModel(data, columnNames));
	createLayout(table, true, null);
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
	Object[][] data = new Object[0][6];
	MATSimTableModel model = new MATSimTableModel(data, columnNames);
	tableModels.put(layer, model);

	Network network = (layer).getMatsimNetwork();
	for (Link link : network.getLinks().values()) {
	    addLink(layer, link);
	}

    }

    public void addLink(NetworkLayer layer, Link link) {

	Object[] linkInfo = new Object[6];
	String id = ((LinkImpl) link).getOrigId();
	linkInfo[0] = id;
	linkInfo[1] = link.getId().toString();
	linkInfo[2] = link.getLength();
	linkInfo[3] = link.getFreespeed();
	linkInfo[4] = link.getCapacity();
	linkInfo[5] = link.getNumberOfLanes();

	tableModels.get(layer).addRow(linkInfo);
	title(layer);
    }

    public void removeLink(NetworkLayer layer, Link link) {
	title(layer);
	tableModels.get(layer).removeLinkEntry(link);
    }

    private void title(NetworkLayer layer) {
	setTitle(tr("Links: {0} / Nodes: {1}", layer.getMatsimNetwork()
		.getLinks().size(), layer.getMatsimNetwork().getNodes().size()));
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
	if (newLayer instanceof NetworkLayer) {
	    paintTable((NetworkLayer) newLayer);
	} else
	    clearTable();
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
    public void selectionChanged(Collection<? extends OsmPrimitive> arg0) {
	table.clearSelection();
	for (OsmPrimitive prim : arg0) {
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

    private class MATSimTableModel extends DefaultTableModel {

	public MATSimTableModel(Object[][] data, String[] columnNames) {
	    super(data, columnNames);
	}

	protected void removeLinkEntry(Link link) {
	    String id = link.getId().toString();
	    for (int i = 0; i < this.getRowCount(); i++) {
		if (id.equalsIgnoreCase(this.getValueAt(i, 1).toString())) {
		    this.removeRow(i);
		}
	    }
	}
    }

}
