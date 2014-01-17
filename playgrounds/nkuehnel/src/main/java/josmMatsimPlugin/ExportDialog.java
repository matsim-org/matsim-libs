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
public class ExportDialog extends JPanel implements ActionListener {

	private JOptionPane optionPane;

	private JLabel exportSystemLabel = new JLabel("export system:");

	protected final static JComboBox exportSystem = new JComboBox(
			Preferences.coordSystems);
	protected final static JLabel exportFilePath = new JLabel();

	public ExportDialog() {

		GridBagConstraints c = new GridBagConstraints();
		setLayout(new GridBagLayout());

		exportSystem.setSelectedItem(Main.pref.get("matsim_exportSystem", "WGS84"));
		
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

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("fileChooser")) {

		}
	}
}
