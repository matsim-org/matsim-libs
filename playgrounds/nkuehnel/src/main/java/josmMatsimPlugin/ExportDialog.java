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

	private final JLabel saveToLabel = new JLabel(tr("Save to:"));
	protected final static JTextField exportFilePath = new JTextField(
			Main.pref.get("matsim_exportFolder",
					System.getProperty("user.home"))
					+ "\\josm_matsim_export");
	private final JButton fileChooser = new JButton("Choose..");

	public ExportDialog() {

		GridBagConstraints c = new GridBagConstraints();
		setLayout(new GridBagLayout());

		c.insets = new Insets(4, 4, 4, 4);
		c.gridwidth = 1;
		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;

		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		add(saveToLabel, c);

		c.gridwidth = 2;
		c.gridx = 1;
		add(exportFilePath, c);

		c.gridwidth = 1;
		c.gridx = 3;
		add(fileChooser, c);

		fileChooser.setActionCommand("fileChooser");
		fileChooser.addActionListener(this);

	}

	public void setOptionPane(JOptionPane optionPane) {
		this.optionPane = optionPane;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("fileChooser")) {
			String path = Main.pref.get("matsim_exportFolder",
					System.getProperty("user.home"));
			JFileChooser chooser = new JFileChooser(path);
			chooser.setDialogTitle("MATSim-Export");
			chooser.setApproveButtonText("Confirm");
			FileFilter filter = new FileNameExtensionFilter("Network-XML",
					"xml");
			chooser.setFileFilter(filter);
			File file = new File(path + "/josm_matsim_export");
			chooser.setSelectedFile(file);
			int result = chooser.showSaveDialog(null);
			if (result == JFileChooser.APPROVE_OPTION
					&& chooser.getSelectedFile().getAbsolutePath() != null) {
				exportFilePath.setText(chooser.getSelectedFile()
						.getAbsolutePath());
			}
		}
	}

}
