package josmMatsimPlugin;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.Layer;

public class MATSimToggleDialog extends ToggleDialog implements
		LayerChangeListener, SelectionChangedListener
{
	private JTable table;

	private String[] columnNames =
	{ "id", "length", "freespeed", "capacity", "permlanes" };

	public MATSimToggleDialog()
	{
		super("Links/Nodes", "logo.png", "Links/Nodes", null, 150, true);
		DataSet.addSelectionListener(this);
		table = new JTable();
		table.setDefaultRenderer(Object.class, new MATSimTableRenderer());
		table.setEnabled(false);
		table.setAutoCreateRowSorter(true);
		createLayout(table, true, null);
	}

	@Override
	public void activeLayerChange(Layer oldLayer, Layer newLayer)
	{
		if (newLayer instanceof NetworkLayer)
		{
			paintTable((NetworkLayer) newLayer);
		} else
			clearTable();
	}

	private void clearTable()
	{
		table.setModel(new DefaultTableModel());
		setTitle(tr("Links/Nodes"));
	}

	private void paintTable(NetworkLayer layer)
	{
		setTitle(tr("Links: {0} / Nodes: {1}",layer.getMatsimNetwork().getLinks().size(), layer.getMatsimNetwork().getNodes().size()));
		Network network = layer.getMatsimNetwork();
		Object[][] data = new Object[network.getLinks().size()][5];
		int counter = 0;
		for (Link link : network.getLinks().values())
		{
			data[counter][0] = link.getId().toString();
			data[counter][1] = link.getLength();
			data[counter][2] = link.getFreespeed();
			data[counter][3] = link.getCapacity();
			data[counter][4] = link.getNumberOfLanes();
			counter++;
		}
		table.setModel(new DefaultTableModel(data, columnNames));
		selectionChanged(layer.data.getSelectedWays());
	}

	public void updateTable(NetworkLayer layer)
	{
		paintTable(layer);
	}

	@Override
	public void selectionChanged(Collection<? extends OsmPrimitive> arg0)
	{
		table.clearSelection();
		for (OsmPrimitive prim : arg0)
		{
			if (prim instanceof Way)
			{
				String id = String.valueOf(prim.getId());
				for (int i = 0; i < table.getRowCount(); i++)
				{
					if (id.equalsIgnoreCase(table.getValueAt(i, 0).toString()))
					{
						table.addRowSelectionInterval(i, i);
					}
				}
			}
		}
	}

	private class MATSimTableRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column)
		{
			setBackground(null);
			Component tableCellRendererComponent = super.getTableCellRendererComponent(table, 
					value, isSelected, hasFocus, row, column);
			return tableCellRendererComponent;
		}
	}

	@Override
	public void layerAdded(Layer newLayer) {
		
	}

	@Override
	public void layerRemoved(Layer oldLayer) {
		
	}

}
