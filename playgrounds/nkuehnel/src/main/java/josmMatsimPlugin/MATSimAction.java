package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
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

	public JosmAction getImportAction() {
		return new ImportAction();
	}

	public JosmAction getNewNetworkAction() {
		return new NewNetworkAction();
	}

	
	public class ImportAction extends JosmAction {

		public ImportAction() {
			super(tr("Import MATSim network"), "open.png",
					tr("Import MATSim network file"), Shortcut
							.registerShortcut("menu:matsimImport",
									tr("Menu: {0}", tr("MATSim Import")),
									KeyEvent.VK_G, Shortcut.ALT_CTRL), true);
		}

		@Override
		public void actionPerformed(ActionEvent e) {

			JFileChooser chooser = new JFileChooser(
					System.getProperty("user.home"));
			chooser.setDialogTitle("MATSim-Import");
			chooser.setApproveButtonText("Confirm");
			FileFilter filter = new FileNameExtensionFilter("Network-XML",
					"xml");
			chooser.setFileFilter(filter);
			int result = chooser.showOpenDialog(null);
			if (result == JFileChooser.APPROVE_OPTION
					&& chooser.getSelectedFile().getAbsolutePath() != null) {
				String path = chooser.getSelectedFile().getAbsolutePath();
				ImportDialog.path.setText(path);
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
						ImportTask task = new ImportTask(path);
						Main.worker.execute(task);
					}
				}
				dlg.dispose();
			}

		}
	}

	public class NewNetworkAction extends JosmAction {

		public NewNetworkAction() {
			super(tr("New MATSim network"), "new.png", tr("Create new Network"),
					Shortcut.registerShortcut("menu:matsimNetwork",
							tr("Menu: {0}", tr("New MATSim Network")),
							KeyEvent.VK_G, Shortcut.ALT_CTRL), true);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			DataSet dataSet = new DataSet();
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			NetworkLayer layer = new NetworkLayer(dataSet, "new Layer", null,
					scenario.getNetwork(), TransformationFactory.WGS84, new HashMap<Way, List<Link>>());
			Main.main.addLayer(layer);
		}
	}
}
