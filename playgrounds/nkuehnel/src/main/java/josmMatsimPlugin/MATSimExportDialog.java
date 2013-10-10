/**
 * 
 */
package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import josmMatsimPlugin.ExportDefaults.OsmHighwayDefaults;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.josm.Main;

/**
 * the export dialog
 * 
 * @author nkuehnel
 * 
 */
public class MATSimExportDialog extends JPanel
{
	private JOptionPane optionPane;
	private JComboBox<String> coordCombo;
	static JTextField path;
	private JButton fileChooser;
	private JButton defaults;
	private JButton filter;
	static JCheckBox keepPaths;

	public MATSimExportDialog()
	{
		GridBagConstraints c = new GridBagConstraints();

		setLayout(new GridBagLayout());
		String[] coordSystems =
		{ TransformationFactory.WGS84, TransformationFactory.ATLANTIS,
				TransformationFactory.CH1903_LV03, TransformationFactory.GK4,
				TransformationFactory.WGS84_UTM47S,
				TransformationFactory.WGS84_UTM48N,
				TransformationFactory.WGS84_UTM35S,
				TransformationFactory.WGS84_UTM36S,
				TransformationFactory.WGS84_Albers,
				TransformationFactory.WGS84_SA_Albers,
				TransformationFactory.WGS84_UTM33N,
				TransformationFactory.DHDN_GK4,
				TransformationFactory.WGS84_UTM29N,
				TransformationFactory.CH1903_LV03_GT,
				TransformationFactory.WGS84_SVY21,
				TransformationFactory.NAD83_UTM17N,
				TransformationFactory.WGS84_TM };
		
		coordCombo = new JComboBox<String>(coordSystems);

		c.insets = new Insets(4, 4, 4, 4);
		c.gridwidth = 1;
		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		add(new JLabel(tr("Coordinate System:")), c);

		c.gridwidth = 1;
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.5;
		add(coordCombo, c);

		coordCombo.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				ExportTask.targetSystem = (String) coordCombo.getSelectedItem();
			}
		});

		c.gridwidth = 1;
		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		add(new JLabel(tr("Save to:")), c);

		path = new JTextField(System.getProperty("user.home")
				+ "\\josm_matsim_export");
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 1.;
		add(path, c);

		fileChooser = new JButton("Choose..");
		c.gridwidth = 1;
		c.gridx = 3;
		c.gridy = 1;
		c.weightx = 1.;
		add(fileChooser, c);

		fileChooser.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser chooser = new JFileChooser(System
						.getProperty("user.home"));
				chooser.setDialogTitle("MATSim-Export");
				chooser.setApproveButtonText("Confirm");
				FileFilter filter = new FileNameExtensionFilter("Network-XML",
						"xml");
				chooser.setFileFilter(filter);
				File file = new File(System.getProperty("user.home")
						+ "/josm_matsim_export");
				chooser.setSelectedFile(file);
				int result = chooser.showSaveDialog(null);
				if (result == JFileChooser.APPROVE_OPTION
						&& chooser.getSelectedFile().getAbsolutePath() != null)
					path.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		});

		keepPaths = new JCheckBox("keep paths");
		keepPaths.setSelected(false);
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1.;
		add(keepPaths, c);

		defaults = new JButton("set defaults");
		c.gridwidth = 1;
		c.gridx = 1;
		c.gridy = 2;
		c.weightx = 1.;
		if (Main.main.getActiveLayer() instanceof NetworkLayer)
			defaults.setEnabled(false);
		add(defaults, c);

		defaults.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				MATSimDefaultsDialog dialog = new MATSimDefaultsDialog();
				JOptionPane pane = new JOptionPane(dialog,
						JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				dialog.setOptionPane(pane);
				JDialog dlg = pane.createDialog(Main.parent, tr("Defaults"));
				dlg.setAlwaysOnTop(true);
				dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

				dlg.setVisible(true);
				if (pane.getValue() != null)
				{
					if (((Integer) pane.getValue()) == JOptionPane.OK_OPTION)
					{
						handleInput(dialog.getInput());
					}
				}
				dlg.dispose();
			}
		});
		
		filter = new JButton("set filter");
		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy = 2;
		c.weightx = 1.;
		
		if (Main.main.getActiveLayer() instanceof NetworkLayer)
			filter.setEnabled(false);
		add(filter, c);

		filter.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				MATSimFilterDialog dialog = new MATSimFilterDialog();
				JOptionPane pane = new JOptionPane(dialog,
						JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				dialog.setOptionPane(pane);
				JDialog dlg = pane.createDialog(Main.parent, tr("Filter:"));
				dlg.setAlwaysOnTop(true);
				dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

				dlg.setVisible(true);
				if (pane.getValue() != null)
				{
					if (((Integer) pane.getValue()) == JOptionPane.OK_OPTION)
					{
						// do something
					}
				}
				dlg.dispose();
			}
		});

	}

	private void handleInput(Map<String, JComponent> input)
	{
		for (int i =0; i<12; i++)
		{
			Map<String, String> values = new HashMap<String, String>();
			for (int j=0; j<5; j++)
			{
				String value= ((JTextField) input.get(i+"_"+j)).getText();
				values.put(i+"_"+j, value);
			}
			
			int hierarchy = Integer.parseInt(values.get(i+"_0"));
			double lanes = Double.parseDouble(values.get(i+"_1"));
			double freespeed = Double.parseDouble(values.get(i+"_2"));
			double freespeedFactor = Double.parseDouble(values.get(i+"_3"));
			double laneCapacity = Double.parseDouble(values.get(i+"_4"));
			ExportDefaults.defaults.put(MATSimDefaultsDialog.types[i], new OsmHighwayDefaults(hierarchy, lanes, freespeed, freespeedFactor, laneCapacity, ((JCheckBox)input.get(i+"_5")).isSelected()));
		}
	}

	public void setOptionPane(JOptionPane optionPane)
	{
		this.optionPane = optionPane;
	}
}