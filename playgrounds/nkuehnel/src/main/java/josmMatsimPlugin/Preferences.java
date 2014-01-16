package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane.PreferencePanel;

public final class Preferences extends DefaultTabPreferenceSetting {

	private static PreferencesActionListener listener = new PreferencesActionListener();

	// Visualization tab
	protected final static JCheckBox renderMatsim = new JCheckBox(
			"Activate MATSim Renderer");
	protected final static JCheckBox showIds = new JCheckBox("Show Ids");

	// Export tab
	private JLabel defaultExportFolder = new JLabel("Default Export Folder");
	protected final JButton fileChooser = new JButton("Choose..");
	protected static String exportFolder = Main.pref.get("matsim_exportFolder",
			System.getProperty("user.home"));
	protected final static JLabel folderLabel = new JLabel(exportFolder);
	
	private JLabel optionsLabel = new JLabel("Options");
	protected final static JCheckBox cleanNetwork = new JCheckBox(
			"Clean network");
	
	private JLabel targetCoordSystemLabel = new JLabel("Target coord system: ");
	
	private static String[] coordSystems = { "WGS84", "ATLANTIS",
			"CH1903_LV03", "GK4", "WGS84_UTM47S", "WGS84_UTM48N",
			"WGS84_UTM35S", "WGS84_UTM36S", "WGS84_Albers", "WGS84_SA_Albers",
			"WGS84_UTM33N", "DHDN_GK4", "WGS84_UTM29N", "CH1903_LV03_GT",
			"WGS84_SVY21", "NAD83_UTM17N", "WGS84_TM" };
	private JButton convertingDefaults = new JButton ("Set converting defaults");
	
	protected final static JComboBox exportSystem = new JComboBox(coordSystems);
	
	// Import Tab
	protected final static JComboBox importSystem = new JComboBox(coordSystems);
	private JLabel originCoordSystemLabel = new JLabel("Origin coord system: ");
	
	

	public static class Factory implements PreferenceSettingFactory {
		@Override
		public PreferenceSetting createPreferenceSetting() {
			return new Preferences();
		}
	}

	private Preferences() {
		super(null, tr("MASim preferences"),
				tr("Configure the MATSim plugin."), false, new JTabbedPane());
	}

	private JPanel buildVisualizationPanel() {
		JPanel pnl = new JPanel(new GridBagLayout());
		GridBagConstraints cOptions = new GridBagConstraints();

		showIds.setActionCommand("showIds");
		showIds.addActionListener(listener);

		renderMatsim.setActionCommand("renderMatsim");
		renderMatsim.addActionListener(listener);

		showIds.setSelected(Main.pref.getBoolean("matsim_showIds", false)
				&& Main.pref.getBoolean("matsim_renderer", true));
		renderMatsim.setSelected(Main.pref.getBoolean("matsim_renderer", true));

		cOptions.anchor = GridBagConstraints.NORTHWEST;
		
		cOptions.insets = new Insets(4, 4, 4, 4);
		
		cOptions.gridx = 0;
		cOptions.gridy = 0;
		pnl.add(renderMatsim, cOptions);
		
		
		cOptions.gridx = 1;
		cOptions.weightx = 1.0;
		cOptions.weighty = 1.0;
		pnl.add(showIds, cOptions);

		return pnl;
	}

	private JPanel buildExportPanel() {
		JPanel pnl = new JPanel(new GridBagLayout());
		GridBagConstraints cOptions = new GridBagConstraints();

		defaultExportFolder.setFont(defaultExportFolder.getFont().deriveFont(
				Font.BOLD));

		fileChooser.setActionCommand("fileChooser");
		fileChooser.addActionListener(listener);

		optionsLabel.setFont(optionsLabel.getFont().deriveFont(Font.BOLD));

		cleanNetwork.setSelected(Main.pref.getBoolean("matsim_cleanNetwork",
				true));
		cleanNetwork.setActionCommand("cleanNetwork");
		cleanNetwork.addActionListener(listener);
		
		exportSystem.setSelectedItem(Main.pref.get("matsim_exportSystem",
				"WGS84"));
		exportSystem.setActionCommand("exportSystem");
		exportSystem.addActionListener(listener);
		
		convertingDefaults.setActionCommand("convertDefaults");
		convertingDefaults.addActionListener(listener);

		cOptions.anchor = GridBagConstraints.NORTHWEST;
		
		cOptions.insets = new Insets(4, 4, 4, 4);

		cOptions.gridx = 0;
		cOptions.gridy = 0;
		pnl.add(defaultExportFolder, cOptions);
		
		cOptions.gridy = 1;
		pnl.add(fileChooser, cOptions);

		cOptions.gridx = 1;
		pnl.add(folderLabel, cOptions);

		cOptions.gridy = 4;
		cOptions.gridx = 0;
		pnl.add(optionsLabel, cOptions);

		cOptions.gridy = 5;
		pnl.add(cleanNetwork, cOptions);
		
		cOptions.gridy = 6;
		pnl.add(targetCoordSystemLabel, cOptions);
		cOptions.gridx = 1;
		cOptions.weightx = 1.0;
		pnl.add(exportSystem, cOptions);
		
		cOptions.weightx = 0.0;
		cOptions.weighty = 1.0;
		cOptions.gridx = 0;
		cOptions.gridy = 7;
		pnl.add(convertingDefaults, cOptions);

		return pnl;
	}
	
	private JPanel buildImportPanel() {
		JPanel pnl = new JPanel(new GridBagLayout());
		GridBagConstraints cOptions = new GridBagConstraints();

		optionsLabel.setFont(optionsLabel.getFont().deriveFont(Font.BOLD));

		importSystem.setSelectedItem(Main.pref.get("matsim_importSystem",
				"WGS84"));
		importSystem.setActionCommand("importSystem");
		importSystem.addActionListener(listener);
		
		cOptions.anchor = GridBagConstraints.NORTHWEST;
		
		cOptions.insets = new Insets(4, 4, 4, 4);

		cOptions.gridy = 0;
		cOptions.gridx = 0;
		pnl.add(optionsLabel, cOptions);

		cOptions.gridy = 1;
		pnl.add(originCoordSystemLabel, cOptions);
		cOptions.gridx = 1;
		cOptions.weightx = 1.0;
		cOptions.weighty = 1.0;
		pnl.add(importSystem, cOptions);
		
		return pnl;
	}

	protected JTabbedPane buildContentPane() {
		JTabbedPane pane = getTabPane();
		pane.addTab(tr("Visualization"), buildVisualizationPanel());
		pane.addTab(tr("Export"), buildExportPanel());
		pane.addTab(tr("Import"), buildImportPanel());
		return pane;
	}

	@Override
	public void addGui(final PreferenceTabbedPane gui) {
		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.fill = GridBagConstraints.BOTH;
		PreferencePanel panel = gui.createPreferenceTab(this);
		panel.add(buildContentPane(), gc);
	}
	
	@Override
	public boolean ok() {
		// TODO Auto-generated method stub
		return false;
	}

}
