/**
 * 
 */
package josmMatsimPlugin;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;

/**
 * the export dialog
 * 
 * @author nkuehnel
 * 
 */
@SuppressWarnings("serial")
public class ExportDialog extends JPanel {

	private JOptionPane optionPane;

	private JLabel exportSystemLabel = new JLabel("export system:");

	protected final static JComboBox exportSystem = new JComboBox(
			Preferences.coordSystems);
	protected final JLabel exportFilePath = new JLabel((Main.pref.get("matsim_exportFolder", System.getProperty("user.home"))+"\\josm_matsim_export.xml"));

	public ExportDialog(String coordSystem) {

		GridBagConstraints c = new GridBagConstraints();
		setLayout(new GridBagLayout());

		if(coordSystem == null) {
			exportSystem.setSelectedItem(Main.pref.get("matsim_exportSystem", "WGS84"));
		} else {
			exportSystem.setSelectedItem(coordSystem);
		}
		
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		c.gridy = 0;
		add(exportFilePath, c);
		
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		add(exportSystemLabel, c);
		
		c.gridx = 1;
		add(exportSystem, c);
		
	}

	public void setOptionPane(JOptionPane optionPane) {
		this.optionPane = optionPane;
	}

}
