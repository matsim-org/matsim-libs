package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * This dialog is used to show and edit the values which are stored in ExportDefaults
 * @author nkuehnel
 * 
 */
public class MATSimDefaultsDialog extends JPanel
{

	private JOptionPane optionPane;
	private Map<String, JComponent> input = new HashMap<String, JComponent>();
	private GridBagConstraints c = new GridBagConstraints();
	
	static String[] types =
		{ "motorway", "motorway_link", "trunk", "trunk_link", "primary",
				"primary_link", "secondary", "tertiary", "minor",
				"unclassified", "residential", "living_street" };
		
	static String[] attributes =
		{ "hierarchy", "lanes", "freespeed", "freespeedfactor", "laneCapacity"};
	
	public MATSimDefaultsDialog()
	{
		setLayout(new GridBagLayout());
		c.insets = new Insets(4, 4, 4, 4);
		c.gridwidth = 1;
		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		for (int i=0; i<types.length; i++)
		{
			for (int j=0; j<attributes.length; j++)
			{
				if(i==0)
				{
					c.gridy=0;
					c.gridx=(j+1);
					add(new JLabel(tr(attributes[j])), c);
				}
				if(j==0)
				{
					c.gridx=0;
					c.gridy=(i+1);
					add(new JLabel(tr(types[i])), c);
				}
				c.gridy=(i+1);
				c.gridx=(j+1);
				switch(j)
				{
				case 0: 
					JTextField tF_hierarchy = new JTextField(Integer.toString(ExportDefaults.defaults.get(types[i]).hierarchy));
					add(tF_hierarchy, c);
					input.put(i+"_"+j, tF_hierarchy);
					System.out.println(types[i]+"_"+attributes[j]);
					break;
				case 1: 
					JTextField tF_lanes = new JTextField(Double.toString(ExportDefaults.defaults.get(types[i]).lanes));
					add(tF_lanes, c);
					input.put(i+"_"+j, tF_lanes);
					System.out.println(types[i]+"_"+attributes[j]);
					break;
				case 2:
					JTextField tF_freespeed = new JTextField(Double.toString(ExportDefaults.defaults.get(types[i]).freespeed));
					add(tF_freespeed, c); 
					input.put(i+"_"+j, tF_freespeed);
					System.out.println(types[i]+"_"+attributes[j]);
					break;
				case 3: 
					JTextField tF_freespeedFactor = new JTextField(Double.toString(ExportDefaults.defaults.get(types[i]).freespeedFactor));
					add(tF_freespeedFactor, c); 
					input.put(i+"_"+j, tF_freespeedFactor);
					System.out.println(types[i]+"_"+attributes[j]);
					break;
				case 4: 
					JTextField tF_laneCapacity = new JTextField(Double.toString(ExportDefaults.defaults.get(types[i]).laneCapacity));
					add(tF_laneCapacity, c); 
					input.put(i+"_"+j, tF_laneCapacity);
					System.out.println(types[i]+"_"+attributes[j]);
					break;
				}
			}
			c.gridx=(attributes.length+1);
			JCheckBox oneway = new JCheckBox("oneway");
			oneway.setSelected(ExportDefaults.defaults.get(types[i]).oneway);
			add(oneway, c);
			input.put(i+"_"+attributes.length, oneway);
		}
		c.gridx=(attributes.length+1);
		add(new JLabel(tr("oneway")), c);

		
		
		JButton reset = new JButton("reset");
		c.gridx=0;
		c.gridy=(types.length+1);
		c.gridwidth=4;
		add(reset,c);
		
		reset.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				ExportDefaults.initialize();
				reset();
				
			}
		});
	}
	
	public Map<String, JComponent> getInput(){
		return this.input;
	}
	
	protected void reset()
	{
		for (int i=0; i<types.length; i++)
		{
			for (int j=0; j<attributes.length; j++)
			{
				switch(j)
				{
					case 0:
						((JTextField)input.get(i+"_"+j)).setText(Integer.toString(ExportDefaults.defaults.get(types[i]).hierarchy)); break;
					case 1:
						((JTextField)input.get(i+"_"+j)).setText(Double.toString(ExportDefaults.defaults.get(types[i]).lanes)); break;
					case 2:
						((JTextField)input.get(i+"_"+j)).setText(Double.toString(ExportDefaults.defaults.get(types[i]).freespeed)); break;
					case 3:
						((JTextField)input.get(i+"_"+j)).setText(Double.toString(ExportDefaults.defaults.get(types[i]).freespeedFactor)); break;
					case 4:
						((JTextField)input.get(i+"_"+j)).setText(Double.toString(ExportDefaults.defaults.get(types[i]).laneCapacity)); break;
				}
			}
			((JCheckBox)input.get(i+"_"+attributes.length)).setSelected(ExportDefaults.defaults.get(types[i]).oneway);
		}
		
	}
	

	public void setOptionPane(JOptionPane optionPane)
	{
		this.optionPane = optionPane;
	}

}
