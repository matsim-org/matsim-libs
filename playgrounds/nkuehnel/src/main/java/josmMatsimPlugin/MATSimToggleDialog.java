package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
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
    protected final static JRadioButton renderMatsim = new JRadioButton(
	    "Activate MATSim Renderer");
    protected final static JRadioButton showIds = new JRadioButton("Show Ids");

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

	JScrollPane tableContainer = new JScrollPane(table);

	JPanel overview = new JPanel(new BorderLayout());
	overview.add(tableContainer, BorderLayout.CENTER);

	JPanel options = new JPanel(new GridBagLayout());
	GridBagConstraints cOptions = new GridBagConstraints();

	showIds.addActionListener(new ShowIdsListener());
	renderMatsim.addActionListener(new RenderMatsimListener());
	
	showIds.setSelected(true);
	renderMatsim.setSelected(true);

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

    // private void checkReverseSelection(Link link) {
    //
    // if (lastSelection.equals(link)) {
    // for (Link link2 : currentLayer.getMatsimNetwork().getLinks().values()) {
    // if (link2.getFromNode().equals(link.getToNode())
    // && link2.getToNode().equals(link.getFromNode())) {
    // System.out.println("selection reversed!");
    // this.lastSelection = null;
    // currentLayer.data.setSelected(currentLayer.data.getPrimitiveById(Long.parseLong(link2.getId().toString()),
    // OsmPrimitiveType.WAY));
    // }
    // }
    // }
    // }

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

    private class ShowIdsListener implements ActionListener {
	@Override
	public void actionPerformed(ActionEvent arg0) {
	    if (!showIds.isSelected()) {
		Defaults.showIds = false;
	    } else
		Defaults.showIds = true;
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
	}
    }

}
