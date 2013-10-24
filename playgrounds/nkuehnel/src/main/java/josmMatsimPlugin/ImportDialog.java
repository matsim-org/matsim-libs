package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * the import dialog
 * 
 * @author nkuehnel
 * 
 */
public class ImportDialog extends JPanel
{
	// the JOptionPane that contains this dialog. required for the closeDialog()
	// method.
	private JOptionPane optionPane;

	public ImportDialog()
	{
		//--------------layout---------------------------------------------
		
		GridBagConstraints c = new GridBagConstraints();
		setLayout(new GridBagLayout());

		//--------------displayed elements---------------------------------
		
		final JComboBox coordCombo = new JComboBox(Defaults.coordSystems);
		final JLabel coordSystemLabel = new JLabel(tr("original coord system:"));
		final JLabel openLabel =new JLabel(tr("Open:"));
		final JTextField path = new JTextField(Defaults.importPath);
		JButton fileChooser = new JButton("Choose..");

		//--------------arrangement of elements-----------------------------
		
		c.gridwidth = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		add(coordSystemLabel, c);
		
		c.gridx = 1;
		add(coordCombo, c);

		c.gridx = 0;
		c.gridy = 1;
		add(openLabel, c);
		
		c.gridwidth = 2;
		c.gridx = 1;
		add(path, c);
		
		c.gridwidth = 1;
		c.gridx = 3;
		add(fileChooser, c);
		
		//--------------action listeners------------------------------------

		fileChooser.addActionListener(new ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent e)
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
				int result = chooser.showOpenDialog(null);
				if (result == JFileChooser.APPROVE_OPTION
						&& chooser.getSelectedFile().getAbsolutePath() != null)
				{
					Defaults.importPath = chooser.getSelectedFile()
							.getAbsolutePath();
					path.setText(Defaults.importPath);
				}
			}
		});
		
		coordCombo.addActionListener(new ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				Defaults.originSystem = (String) coordCombo.getSelectedItem();
			}
		});
	}

	public void setOptionPane(JOptionPane optionPane)
	{
		this.optionPane = optionPane;
	}

}
