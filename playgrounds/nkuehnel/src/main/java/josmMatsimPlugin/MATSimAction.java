package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Adds the MATSim button to the tools bar.
 * 
 * @author nkuehnel
 * 
 */
public class MATSimAction {

	public JosmAction getExportAction() {
		return new ExportAction();
	}

	public JosmAction getImportAction() {
		return new ImportAction();
	}

	public JosmAction getNewNetworkAction() {
		return new NewNetworkAction();
	}

	public class ExportAction extends JosmAction {

		public ExportAction() {
			super(tr("Export MATSim network"), null,
					tr("Export MATSim network file"), Shortcut
							.registerShortcut("menu:matsimexport",
									tr("Menu: {0}", tr("MATSim Export")),
									KeyEvent.VK_G, Shortcut.ALT_CTRL), false);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			ExportDialog dialog = new ExportDialog();
			JOptionPane pane = new JOptionPane(dialog,
					JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			dialog.setOptionPane(pane);
			JDialog dlg = pane.createDialog(Main.parent, tr("Export"));
			dlg.setAlwaysOnTop(true);
			dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dlg.setVisible(true);
			if (pane.getValue() != null) {
				if (((Integer) pane.getValue()) == JOptionPane.OK_OPTION) {
					ExportTask task = new ExportTask();
					Main.worker.execute(task);
				}
			}
			dlg.dispose();

		}
	}

	public class ImportAction extends JosmAction {

		public ImportAction() {
			super(tr("Import MATSim network"), null,
					tr("Import MATSim network file"), Shortcut
							.registerShortcut("menu:matsimImport",
									tr("Menu: {0}", tr("MATSim Export")),
									KeyEvent.VK_G, Shortcut.ALT_CTRL), false);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			ImportDialog dialog = new ImportDialog();
			JOptionPane pane = new JOptionPane(dialog,
					JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			dialog.setOptionPane(pane);
			JDialog dlg = pane.createDialog(Main.parent, tr("Import"));
			dlg.setAlwaysOnTop(true);
			dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

			dlg.setVisible(true);
			if (pane.getValue() != null) {
				if (((Integer) pane.getValue()) == JOptionPane.OK_OPTION) {
					ImportTask task = new ImportTask();
					Main.worker.execute(task);
				}
			}
			dlg.dispose();
		}
	}

	public class NewNetworkAction extends JosmAction {

		public NewNetworkAction() {
			super(tr("New MATSim network"), null, tr("Create new Network"),
					Shortcut.registerShortcut("menu:matsimNetwork",
							tr("Menu: {0}", tr("New MATSim Network")),
							KeyEvent.VK_G, Shortcut.ALT_CTRL), false);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			DataSet dataSet = new DataSet();
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			NetworkLayer layer = new NetworkLayer(dataSet, "new Layer",
					new File("new Layer"), scenario.getNetwork(), "WGS84");
			dataSet.addDataSetListener(new NetworkListener(layer,
					new HashMap<Way, List<Link>>(), "WGS84"));
			Main.main.addLayer(layer);
		}
	}
}
