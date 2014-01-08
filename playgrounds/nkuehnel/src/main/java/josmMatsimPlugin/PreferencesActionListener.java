package josmMatsimPlugin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JFileChooser;

import org.openstreetmap.josm.Main;

public class PreferencesActionListener implements ActionListener, ItemListener {

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("showIds")) {
			if (!Preferences.showIds.isSelected()) {
				Main.pref.put("matsim_showIds", false);
			} else {
				Main.pref.put("matsim_showIds", true);
			}
		} else if (e.getActionCommand().equals("renderMatsim")) {
			if (!Preferences.renderMatsim.isSelected()) {
				Main.pref.put("matsim_renderer", false);
				Preferences.showIds.setEnabled(false);
			} else {
				Main.pref.put("matsim_renderer", true);
				Preferences.showIds.setEnabled(true);
				Preferences.showIds.setSelected(Main.pref.getBoolean("matsim_showIds"));
			}
		} else if (e.getActionCommand().equals("fileChooser")) {
			JFileChooser chooser = new JFileChooser(Preferences.exportFolder);
			chooser.setDialogTitle("MATSim-Export");
			chooser.setApproveButtonText("Confirm");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = chooser.showSaveDialog(null);
			if (result == JFileChooser.APPROVE_OPTION) {
				String newPath = chooser.getSelectedFile().getAbsolutePath();
				Main.pref.put("matsim_exportFolder", newPath);
				Preferences.folderLabel.setText(newPath);
				Preferences.exportFolder = newPath;
				ExportDialog.exportFilePath.setText(newPath + "\\josm_matsim_export");
			}
		} else if (e.getActionCommand().equals("cleanNetwork")) {
			if (!Preferences.cleanNetwork.isSelected()) {
				Main.pref.put("matsim_cleanNetwork", false);
			} else {
				Main.pref.put("matsim_cleanNetwork", true);
			}
		} 
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			Main.pref.put("matsim_exportSystem", (String) e.getItem());
		}
		
	}
}
