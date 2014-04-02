package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane.PreferencePanel;

import com.kitfox.svg.Filter;

public final class Preferences extends DefaultTabPreferenceSetting {

	// Visualization tab
	private final static JCheckBox renderMatsim = new JCheckBox(
			"Activate MATSim Renderer");
	private final static JCheckBox showIds = new JCheckBox("Show Ids");
	private final static JSlider wayOffset = new JSlider(0, 100);
	private final static JLabel wayOffsetLabel = new JLabel(
			"Link offset for overlapping links");
	private final static JCheckBox showInternalIds = new JCheckBox(
			"Show internal Ids in table");

	private final static JCheckBox keepPaths = new JCheckBox("Keep paths");
	private final static JCheckBox cleanNetwork = new JCheckBox("Clean network");

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

	protected final static JCheckBox filterActive = new JCheckBox(
			"Activate Filter");
	private final static JLabel hierarchyLabel = new JLabel(
			"Only convert hierarchies up to: ");
	final static JTextField hierarchyLayer = new JTextField();

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

		wayOffset
				.setValue((int) ((Main.pref.getDouble("matsim_wayOffset", 0)) / 0.03));

		showIds.setSelected(Main.pref.getBoolean("matsim_showIds")
				&& Main.pref.getBoolean("matsim_renderer"));
		renderMatsim.setSelected(Main.pref.getBoolean("matsim_renderer"));
		wayOffset.setEnabled(Main.pref.getBoolean("matsim_renderer"));
		showIds.setEnabled(Main.pref.getBoolean("matsim_renderer"));
		wayOffsetLabel.setEnabled(Main.pref.getBoolean("matsim_renderer"));
		showInternalIds.setSelected(Main.pref.getBoolean(
				"matsim_showInternalIds", false));

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

		cOptions.gridx = 0;
		cOptions.gridy = 2;
		pnl.add(showInternalIds, cOptions);

		return pnl;
	}

	private JPanel buildConvertPanel() {
		JPanel pnl = new JPanel(new GridBagLayout());
		GridBagConstraints cOptions = new GridBagConstraints();

		cleanNetwork.setSelected(Main.pref.getBoolean("matsim_cleanNetwork",
				true));
		keepPaths.setSelected(Main.pref.getBoolean(
				"matsim_convertDefaults_keepPaths", false));
		convertingDefaults.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("convertDefaults")) {
					OsmConvertDefaultsDialog dialog = new OsmConvertDefaultsDialog();
					JOptionPane pane = new JOptionPane(dialog,
							JOptionPane.PLAIN_MESSAGE,
							JOptionPane.OK_CANCEL_OPTION);
					dialog.setOptionPane(pane);
					JDialog dlg = pane
							.createDialog(Main.parent, tr("Defaults"));
					dlg.setAlwaysOnTop(true);
					dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dlg.setVisible(true);
					if (pane.getValue() != null) {
						if (((Integer) pane.getValue()) == JOptionPane.OK_OPTION) {
							dialog.handleInput();
						}
					}
					dlg.dispose();
				}
			}
		});

		filterActive.setSelected(Main.pref.getBoolean("matsim_filterActive",
				false));
		hierarchyLayer.setText(String.valueOf(Main.pref.getInteger(
				"matsim_filter_hierarchy", 6)));

		cOptions.anchor = GridBagConstraints.NORTHWEST;

		cOptions.insets = new Insets(4, 4, 4, 4);

		cOptions.gridx = 0;
		cOptions.gridy = 0;
		pnl.add(cleanNetwork, cOptions);

		cOptions.gridy = 1;
		pnl.add(keepPaths, cOptions);

		cOptions.gridy = 2;
		pnl.add(convertingDefaults, cOptions);

		cOptions.gridy = 3;
		pnl.add(filterActive, cOptions);

		cOptions.gridy = 4;
		pnl.add(hierarchyLabel, cOptions);
		cOptions.gridx = 1;
		pnl.add(hierarchyLayer, cOptions);

		return pnl;
	}

	protected JTabbedPane buildContentPane() {
		JTabbedPane pane = getTabPane();
		pane.addTab(tr("Visualization"), buildVisualizationPanel());
		pane.addTab(tr("Converter Options"), buildConvertPanel());
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
		if (!showIds.isSelected()) {
			Main.pref.put("matsim_showIds", false);
		} else {
			Main.pref.put("matsim_showIds", true);
		}
		if (!renderMatsim.isSelected()) {
			Main.pref.put("matsim_renderer", false);
		} else {
			Main.pref.put("matsim_renderer", true);
		}
		if (!cleanNetwork.isSelected()) {
			Main.pref.put("matsim_cleanNetwork", false);
		} else {
			Main.pref.put("matsim_cleanNetwork", true);
		}
		if (showInternalIds.isSelected()) {
			Main.pref.put("matsim_showInternalIds", true);
		} else {
			Main.pref.put("matsim_showInternalIds", false);
		}
		if (Preferences.keepPaths.isSelected()) {
			Main.pref.put("matsim_convertDefaults_keepPaths", true);
		} else {
			Main.pref.put("matsim_convertDefaults_keepPaths", false);
		}
		if (filterActive.isSelected()) {
			Main.pref.put("matsim_filterActive", true);
		} else {
			Main.pref.put("matsim_filterActive", false);
		}
		Main.pref.putInteger("matsim_filter_hierarchy",
				Integer.parseInt(hierarchyLayer.getText()));
		int temp = wayOffset.getValue();
		double offset = ((double) temp) * 0.03;
		Main.pref.putDouble("matsim_wayOffset", offset);
		return false;

	}

}
