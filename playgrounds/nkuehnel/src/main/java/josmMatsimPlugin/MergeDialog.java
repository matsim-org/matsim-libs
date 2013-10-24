package josmMatsimPlugin;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class MergeDialog extends JPanel
{

	private JOptionPane optionPane;
	private Map<Layer, JCheckBox> checkBoxes= new HashMap<Layer, JCheckBox>();
	
	public MergeDialog()
	{
		GridBagConstraints c = new GridBagConstraints();

		setLayout(new GridBagLayout());

		c.insets = new Insets(4, 4, 4, 4);
		c.gridwidth = 1;
		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		
		List<Layer> layers = Main.map.mapView.getAllLayersAsList();
		
		for (int i = 0; i<layers.size(); i++)
		{
			c.gridy=i;
			JCheckBox cB= new JCheckBox(layers.get(i).getName());
			checkBoxes.put(layers.get(i), cB);
			add(cB, c);
		}
		
	}

	public void setOptionPane(JOptionPane optionPane)
	{
		this.optionPane = optionPane;
	}
	
	protected Map<Layer, JCheckBox> getCheckBoxes()
	{
		return this.checkBoxes;
	}

}
