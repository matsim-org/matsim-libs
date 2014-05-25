package org.matsim.contrib.josm;

import org.openstreetmap.josm.Main;

import javax.swing.*;
import java.awt.*;

/**
 * the import dialog
 * 
 * @author nkuehnel
 * 
 */
class ImportDialog extends JPanel {
	// the JOptionPane that contains this dialog. required for the closeDialog()
	// method.
	private JOptionPane optionPane;
	protected static JLabel path = new JLabel();
	private JLabel importSystemLabel = new JLabel("origin system:");

	protected final static JComboBox importSystem = new JComboBox(
			Preferences.coordSystems);

	public ImportDialog() {
		GridBagConstraints c = new GridBagConstraints();
		setLayout(new GridBagLayout());

		importSystem.setSelectedItem(Main.pref.get("matsim_importSystem",
				"WGS84"));

		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		c.gridy = 0;
		add(path, c);

		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;

		add(importSystemLabel, c);

		c.gridx = 1;
		add(importSystem, c);

	}

	public void setOptionPane(JOptionPane optionPane) {
		this.optionPane = optionPane;
	}

}
