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
import javax.swing.JSlider;
import javax.swing.JTabbedPane;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
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
	protected final static JSlider wayOffset = new JSlider(0, 100);
	protected final static JLabel wayOffsetLabel = new JLabel("link offset for overlapping links");

	// Export tab
	private JLabel defaultExportFolder = new JLabel("Default Export Folder");
	protected final JButton fileChooser = new JButton("Choose..");
	protected static String exportFolder = Main.pref.get("matsim_exportFolder",
			System.getProperty("user.home"));
	protected final static JLabel folderLabel = new JLabel(exportFolder);

	private JLabel optionsLabel = new JLabel("Options");
	protected final static JCheckBox cleanNetwork = new JCheckBox(
			"Clean network");

	private JLabel targetCoordSystemLabel = new JLabel(
			"Default Target coord system: ");

	protected static String[] coordSystems = { TransformationFactory.WGS84,
			TransformationFactory.ATLANTIS, TransformationFactory.CH1903_LV03,
			TransformationFactory.GK4, TransformationFactory.WGS84_UTM47S,
			TransformationFactory.WGS84_UTM48N,
			TransformationFactory.WGS84_UTM35S,
			TransformationFactory.WGS84_UTM36S,
			TransformationFactory.WGS84_Albers,
			TransformationFactory.WGS84_SA_Albers,
			TransformationFactory.WGS84_UTM33N, TransformationFactory.DHDN_GK4,
			TransformationFactory.WGS84_UTM29N,
			TransformationFactory.CH1903_LV03_GT,
			TransformationFactory.WGS84_SVY21,
			TransformationFactory.NAD83_UTM17N, TransformationFactory.WGS84_TM };
	private JButton convertingDefaults = new JButton("Set converting defaults");

	protected final static JComboBox exportSystem = new JComboBox(coordSystems);

	// Import Tab
	protected final static JComboBox importSystem = new JComboBox(coordSystems);
	private JLabel originCoordSystemLabel = new JLabel(
			"Default Origin coord system: ");

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
		
		wayOffset.addChangeListener(listener);
		wayOffset.setValue((int) ((Main.pref.getDouble("matsim_wayOffset", 1.5)) / 0.03));

		showIds.setSelected(Main.pref.getBoolean("matsim_showIds", false)
				&& Main.pref.getBoolean("matsim_renderer", true));
		renderMatsim.setSelected(Main.pref.getBoolean("matsim_renderer", true));
		wayOffset.setEnabled(Main.pref.getBoolean("matsim_renderer", true));

		cOptions.anchor = GridBagConstraints.NORTHWEST;

		cOptions.insets = new Insets(4, 4, 4, 4);

		cOptions.gridx = 0;
		cOptions.gridy = 0;
		pnl.add(renderMatsim, cOptions);

		cOptions.gridx = 1;
		cOptions.weightx = 1.0;
		cOptions.weighty = 1.0;
		pnl.add(showIds, cOptions);
		
		cOptions.gridx = 0;
		cOptions.gridy = 1;
		pnl.add(wayOffsetLabel, cOptions);
		
		cOptions.gridx = 1;
		pnl.add(wayOffset, cOptions);

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
