package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.matsim.api.core.v01.network.Network;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * a layer which contains MATSim-network data to differ from normal OSM layers
 * 
 * @author nkuehnel
 * 
 */
public class NetworkLayer extends OsmDataLayer {
	private Network matsimNetwork;
	private String coordSystem;

	public String getCoordSystem() {
		return coordSystem;
	}

	public NetworkLayer(DataSet data, String name, File associatedFile,
			Network network, String coordSystem) {
		super(data, name, associatedFile);
		this.matsimNetwork = network;
		this.coordSystem = coordSystem;

	}

	public Network getMatsimNetwork() {
		return matsimNetwork;
	}

	@Override
	public boolean checkSaveConditions() {
		if (isDataSetEmpty()) {
			ExtendedDialog dialog = new ExtendedDialog(Main.parent,
					tr("Empty document"), new String[] { tr("Save anyway"),
							tr("Cancel") });
			dialog.setContent(tr("The document contains no data."));
			dialog.setButtonIcons(new String[] { "save.png", "cancel.png" });
			dialog.showDialog();
			if (dialog.getValue() != 1)
				return false;
		}

		String path = this.getAssociatedFile() != null ? this
				.getAssociatedFile().getAbsolutePath() : Main.pref.get(
				"matsim_exportFolder", System.getProperty("user.home"))
				+ "/josm_matsim_export";
		JFileChooser chooser = new JFileChooser(path);
		chooser.setDialogTitle("MATSim-Export");
		chooser.setApproveButtonText("Confirm");
		FileFilter filter = new FileNameExtensionFilter("Network-XML", "xml");
		chooser.setFileFilter(filter);
		File file = new File(path);
		chooser.setSelectedFile(file);
		int result = chooser.showSaveDialog(null);

		ExportDialog dialog = new ExportDialog();

		if (result == JFileChooser.APPROVE_OPTION
				&& chooser.getSelectedFile().getAbsolutePath() != null) {
			path = chooser.getSelectedFile().getAbsolutePath();
			dialog.exportFilePath.setText(path);
			JOptionPane pane = new JOptionPane(dialog,
					JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			dialog.setOptionPane(pane);
			JDialog dlg = pane.createDialog(Main.parent, tr("Export"));
			dlg.setAlwaysOnTop(true);
			dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dlg.setVisible(true);
			if (pane.getValue() != null) {
				if (((Integer) pane.getValue()) == JOptionPane.OK_OPTION) {
					ExportTask task = new ExportTask(path);
					Main.worker.execute(task);
					if (task.exportResult == ExportTask.SUCCESS) {
						this.setAssociatedFile(chooser.getSelectedFile());
						this.setName(chooser.getSelectedFile()
								.getAbsolutePath());
					}
				}
			}
			dlg.dispose();
		}
		return false;
	}

	/**
	 * Check the data set if it would be empty on save. It is empty, if it
	 * contains no objects (after all objects that are created and deleted
	 * without being transferred to the server have been removed).
	 * 
	 * @return <code>true</code>, if a save result in an empty data set.
	 */
	private boolean isDataSetEmpty() {
		if (data != null) {
			for (OsmPrimitive osm : data.allNonDeletedPrimitives())
				if (!osm.isDeleted() || !osm.isNewOrUndeleted())
					return false;
		}
		return true;
	}

	@Override
	public Action[] getMenuEntries() {
		if (Main.applet)
			return new Action[] {
					LayerListDialog.getInstance().createActivateLayerAction(
							this),
					LayerListDialog.getInstance().createShowHideLayerAction(),
					LayerListDialog.getInstance().createDeleteLayerAction(),
					SeparatorLayerAction.INSTANCE,
					new RenameLayerAction(getAssociatedFile(), this),
					SeparatorLayerAction.INSTANCE,
					new LayerListPopup.InfoAction(this) };
		List<Action> actions = new ArrayList<Action>();
		actions.addAll(Arrays.asList(new Action[] {
				LayerListDialog.getInstance().createActivateLayerAction(this),
				LayerListDialog.getInstance().createShowHideLayerAction(),
				LayerListDialog.getInstance().createDeleteLayerAction(),
				SeparatorLayerAction.INSTANCE, new LayerSaveAsAction(this), }));
		actions.addAll(Arrays.asList(new Action[] {
				SeparatorLayerAction.INSTANCE,
				new RenameLayerAction(getAssociatedFile(), this) }));
		actions.addAll(Arrays.asList(new Action[] {
				SeparatorLayerAction.INSTANCE,
				new LayerListPopup.InfoAction(this) }));
		return actions.toArray(new Action[actions.size()]);
	}

	/**
	 * Creates a new "Save" dialog for this layer and makes it visible.<br/>
	 * When the user has chosen a file, checks the file extension, and confirms
	 * overwrite if needed.
	 * 
	 * @return The output {@code File}
	 * @since 5459
	 * @see SaveActionBase#createAndOpenSaveFileChooser
	 */
	@Override
	public File createAndOpenSaveFileChooser() {
		return null;
	}
}
