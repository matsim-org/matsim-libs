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

	private final JLabel coordSystemLabel = new JLabel(tr("Coordinate System:"));
	private final JLabel saveToLabel = new JLabel(tr("Save to:"));
	protected final static JTextField exportFilePath = new JTextField(
			Main.pref.get("matsim_exportFolder",
					System.getProperty("user.home"))
					+ "\\josm_matsim_export");
	private final JButton fileChooser = new JButton("Choose..");
	private final JButton defaults = new JButton("set OSM defaults");
	private final JButton filter = new JButton("set filter");

	public ExportDialog() {

		GridBagConstraints c = new GridBagConstraints();
		setLayout(new GridBagLayout());

		c.insets = new Insets(4, 4, 4, 4);
		c.gridwidth = 1;
		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;

		c.gridx = 0;
		c.gridy = 0;
		add(coordSystemLabel, c);

		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 1;
		add(saveToLabel, c);

		c.gridwidth = 2;
		c.gridx = 1;
		add(exportFilePath, c);

		c.gridwidth = 1;
		c.gridx = 3;
		add(fileChooser, c);

		c.gridy = 2;
		c.gridx = 1;
		add(defaults, c);

		c.gridx = 2;
		add(filter, c);

		if (Main.main.getActiveLayer() instanceof NetworkLayer)
			filter.setEnabled(false);
		if (Main.main.getActiveLayer() instanceof NetworkLayer)
			defaults.setEnabled(false);

		fileChooser.setActionCommand("fileChooser");
		fileChooser.addActionListener(this);

		defaults.setActionCommand("defaults");
		defaults.addActionListener(this);

		filter.setActionCommand("filter");
		filter.addActionListener(this);
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
		} else if (e.getActionCommand().equals("defaults")) {
			OsmExportDefaultsDialog dialog = new OsmExportDefaultsDialog();
			JOptionPane pane = new JOptionPane(dialog,
					JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			dialog.setOptionPane(pane);
			JDialog dlg = pane.createDialog(Main.parent, tr("Defaults"));
			dlg.setAlwaysOnTop(true);
			dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

			dlg.setVisible(true);
			if (pane.getValue() != null) {
				if (((Integer) pane.getValue()) == JOptionPane.OK_OPTION) {
					Defaults.handleInput(dialog.getInput());
				}
			}
			dlg.dispose();
		} else if (e.getActionCommand().equals("filter")) {
			FilterDialog dialog = new FilterDialog();
			JOptionPane pane = new JOptionPane(dialog,
					JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			dialog.setOptionPane(pane);
			JDialog dlg = pane.createDialog(Main.parent, tr("Filter:"));
			dlg.setAlwaysOnTop(true);
			dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

			dlg.setVisible(true);
			if (pane.getValue() != null) {
				if (((Integer) pane.getValue()) == JOptionPane.OK_OPTION) {
					// do something
				}
			}
			dlg.dispose();
		}

	}

}
