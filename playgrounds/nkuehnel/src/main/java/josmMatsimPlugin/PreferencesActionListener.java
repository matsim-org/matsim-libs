package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;

public class PreferencesActionListener implements ActionListener,
		ChangeListener {

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("showIds")) {
			if (!Preferences.showIds.isSelected()) {
				Main.pref.put("matsim_showIds", false);
			} else {
				Main.pref.put("matsim_showIds", true);
			}
		} else if (e.getActionCommand().equals("renderMatsim")) {
			if (!Preferences.renderMatsim.isSelected()) {
				Main.pref.put("matsim_renderer", false);
				Preferences.showIds.setEnabled(false);
				Preferences.wayOffset.setEnabled(false);
				Preferences.wayOffsetLabel.setEnabled(false);
			} else {
				Main.pref.put("matsim_renderer", true);
				Preferences.showIds.setEnabled(true);
				Preferences.showIds.setSelected(Main.pref
						.getBoolean("matsim_showIds", false));
				Preferences.wayOffset.setEnabled(true);
				Preferences.wayOffsetLabel.setEnabled(true);
			}
		} else if (e.getActionCommand().equals("cleanNetwork")) {
			if (!Preferences.cleanNetwork.isSelected()) {
				Main.pref.put("matsim_cleanNetwork", false);
			} else {
				Main.pref.put("matsim_cleanNetwork", true);
			}
		} else if (e.getActionCommand().equals("convertDefaults")) {
			OsmConvertDefaultsDialog dialog = new OsmConvertDefaultsDialog();
			JOptionPane pane = new JOptionPane(dialog,
					JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			dialog.setOptionPane(pane);
			JDialog dlg = pane.createDialog(Main.parent, tr("Defaults"));
			dlg.setAlwaysOnTop(true);
			dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

			dlg.setVisible(true);
			if (pane.getValue() != null) {
				if (((Integer) pane.getValue()) == JOptionPane.OK_OPTION) {
					dialog.handleInput();
				}
			}
			dlg.dispose();
		} else if (e.getActionCommand().equals("convertSystem")) {
			Main.pref.put("matsim_convertSystem",
					(String) Preferences.coordSystem.getSelectedItem());
		} else if (e.getActionCommand().equals("showInternalIds")) {
			if (Preferences.showInternalIds.isSelected()) {
				Main.pref.put("matsim_showInternalIds", true);
			} else {
				Main.pref.put("matsim_showInternalIds", false);
			}
		} else if (e.getActionCommand().equals("keepPaths")) {
			if (Preferences.keepPaths.isSelected()) {
				Main.pref.put("matsim_convertDefaults_keepPaths", true);
			} else {
				Main.pref.put("matsim_convertDefaults_keepPaths", false);
			}
		}
		
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		JSlider source = (JSlider) e.getSource();
		if (!source.getValueIsAdjusting()) {
			int temp = source.getValue();
			double offset = ((double) temp) * 0.03; 
			Main.pref.putDouble("matsim_wayOffset", offset);
		}

	}

}
