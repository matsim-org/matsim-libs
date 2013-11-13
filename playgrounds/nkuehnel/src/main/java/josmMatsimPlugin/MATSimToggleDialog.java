package josmMatsimPlugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.openstreetmap.josm.Main;
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
	private Map<NetworkLayer, JTable> tableBuffer = new HashMap<NetworkLayer, JTable>();

	private JTable table;
	private GridBagConstraints c;
	private JPanel topBar;
	private JLabel counts;
	protected JScrollPane scrollPane;
	private List<Integer> highlightedRows= new ArrayList<Integer>();

	private String[] columnNames =
	{ "link", "length", "freespeed", "capacity", "numoflanes" };

	public MATSimToggleDialog()
	{
		super("MATSimToggle", "logo.png", "MATSimToggle", null, 150, true);

		DataSet.addSelectionListener(this);

		final JButton refresh = new JButton("refresh");
		final JButton clear = new JButton("clear");
		topBar = new JPanel(new GridBagLayout());

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;

		topBar.add(refresh, c);
		c.gridx = 1;
		topBar.add(clear, c);

		add(topBar, BorderLayout.NORTH);

		refresh.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent event)
			{
				updateTable((NetworkLayer) Main.main.getActiveLayer());
			}
		});

		clear.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				clearTable();
			}
		});
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

	@Override
	public void layerAdded(Layer newLayer)
	{
		if (newLayer instanceof NetworkLayer)
		{
			this.tableBuffer.put((NetworkLayer) newLayer,
					createTable(((NetworkLayer) newLayer).getMatsimNetwork()));
		}
	}

	private void clearTable()
	{
		remove(scrollPane);
		scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);
	}

	private JTable createTable(Network network)
	{
		highlightedRows.clear();
		Object[][] data = new Object[network.getLinks().size()][5];
		int counter = 0;
		for (Link link : network.getLinks().values())
		{
			data[counter][0] = link.getId().toString();
			data[counter][1] = link.getLength();
			data[counter][2] = link.getFreespeed();
			data[counter][3] = link.getCapacity();
			data[counter][4] = link.getNumberOfLanes();
			for(int i=1; i<5; i++)
			{
				if(Double.parseDouble(data[counter][i].toString())==0)
				{
					highlightedRows.add(counter);
				}
			}
			counter++;
		}
		table = new JTable(data, columnNames);
		table.setDefaultRenderer(Object.class, new MATSimTableRenderer());
		table.setEnabled(false);
		table.setAutoCreateRowSorter(true);
		return table;
	}

	@Override
	public void layerRemoved(Layer oldLayer)
	{
		tableBuffer.remove(oldLayer);
	}

	public void paintTable(NetworkLayer layer)
	{
		if (scrollPane != null)
		{
			this.remove(scrollPane);
		}
		if (counts != null)
		{
			topBar.remove(counts);
		}
	
		counts = new JLabel("#Links: "+layer.getMatsimNetwork().getLinks().size()+ " | #Nodes: "+layer.getMatsimNetwork().getNodes().size());
		
		c.gridx = 2;
		topBar.add(counts, c);
		
		scrollPane = new JScrollPane(tableBuffer.get(layer));
		add(scrollPane, BorderLayout.CENTER);
		this.repaint();
	}

	public void updateTable(NetworkLayer layer)
	{
		this.tableBuffer.put((NetworkLayer) layer,
				createTable(((NetworkLayer) layer).getMatsimNetwork()));
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
			if (highlightedRows.contains(row))
			{
				setBackground(Color.YELLOW);
			}
			return tableCellRendererComponent;
		}
	}

}
