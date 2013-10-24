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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.openstreetmap.josm.Main;

/**
 * the export dialog
 * 
 * @author nkuehnel
 * 
 */
public class ExportDialog extends JPanel
{
	private JOptionPane optionPane;

	public ExportDialog()
	{
		//--------------layout---------------------------------------------
		
		GridBagConstraints c = new GridBagConstraints();
		setLayout(new GridBagLayout());
		
		
		//--------------displayed elements---------------------------------
		
		final JComboBox coordCombo = new JComboBox(Defaults.coordSystems);
		final JLabel coordSystemLabel= new JLabel(tr("Coordinate System:"));
		final JLabel saveToLabel= new JLabel(tr("Save to:"));
		final JTextField path = new JTextField(Defaults.exportPath);
		final JButton fileChooser = new JButton("Choose..");
		final JButton defaults = new JButton("set OSM defaults");
		final JButton filter = new JButton("set filter");
		final JCheckBox cleanNetBox = new JCheckBox("clean network");
		
		//--------------arrangement of elements-----------------------------
		
		c.insets = new Insets(4, 4, 4, 4);
		c.gridwidth = 1;
		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		c.gridx = 0;
		c.gridy = 0;
		add(coordSystemLabel, c);
		c.gridx = 1;
		c.weightx = 1.5;
		add(coordCombo, c);
		
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 1;
		add(saveToLabel, c);
		
		c.gridwidth = 2;
		c.gridx = 1;
		add(path, c);
		
		c.gridwidth = 1;
		c.gridx = 3;
		add(fileChooser, c);
		
		c.gridx = 0;
		c.gridy = 2;
		add(cleanNetBox,c);
		
		c.gridx = 1;
		add(defaults, c);
		
		c.gridx = 2;
		add(filter, c);
		
		//--------------configure initial setup-----------------------------
		
		cleanNetBox.setSelected(true);
		if (Main.main.getActiveLayer() instanceof NetworkLayer)
			filter.setEnabled(false);
		if (Main.main.getActiveLayer() instanceof NetworkLayer)
			defaults.setEnabled(false);
		
		//--------------action listeners------------------------------------
		
		coordCombo.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Defaults.targetSystem = (String) coordCombo.getSelectedItem();
			}
		});

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
					Defaults.exportPath=chooser.getSelectedFile().getAbsolutePath();
					path.setText(Defaults.exportPath);
			}
		});

		defaults.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				OsmExportDefaultsDialog dialog = new OsmExportDefaultsDialog();
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
						Defaults.handleInput(dialog.getInput());
					}
				}
				dlg.dispose();
			}
		});
		
		filter.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				FilterDialog dialog = new FilterDialog();
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
		
		cleanNetBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(cleanNetBox.isSelected())
				{
					Defaults.cleanNet =true;
				}
				else
					Defaults.cleanNet =false;
			}
		});
		
	}

	public void setOptionPane(JOptionPane optionPane)
	{
		this.optionPane = optionPane;
	}
}