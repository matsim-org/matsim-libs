package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.matsim.api.core.v01.network.Network;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveActionBase;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.DataCountVisitor;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

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

		ExportDialog dialog = new ExportDialog(this.coordSystem);

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

	@Override
	public void mergeFrom(final Layer from) {

	}

	/**
	 * merges the primitives in dataset <code>from</code> into the dataset of
	 * this layer
	 * 
	 * @param from
	 *            the source data set
	 */
	public void mergeFrom(final DataSet from) {

	}
	
	@Override public boolean isMergable(final Layer other) {
        return false;
    }

	
	 /**
     * Replies true if the data managed by this layer needs to be uploaded to
     * the server because it contains at least one modified primitive.
     *
     * @return true if the data managed by this layer needs to be uploaded to
     * the server because it contains at least one modified primitive; false,
     * otherwise
     */
    public boolean requiresUploadToServer() {
        return false;
    }
    
    /**
     * Initializes the layer after a successful save of OSM data to a file
     *
     */
    public void onPostSaveToFile() {
    }
    
    @Override public Object getInfoComponent() {
        final DataCountVisitor counter = new DataCountVisitor();
        for (final OsmPrimitive osm : data.allPrimitives()) {
            osm.accept(counter);
        }
        final JPanel p = new JPanel(new GridBagLayout());

        String nodeText = trn("{0} node", "{0} nodes", counter.nodes, counter.nodes);
        if (counter.deletedNodes > 0) {
            nodeText += " ("+trn("{0} deleted", "{0} deleted", counter.deletedNodes, counter.deletedNodes)+")";
        }

        String wayText = trn("{0} way", "{0} ways", counter.ways, counter.ways);
        if (counter.deletedWays > 0) {
            wayText += " ("+trn("{0} deleted", "{0} deleted", counter.deletedWays, counter.deletedWays)+")";
        }

        String relationText = trn("{0} relation", "{0} relations", counter.relations, counter.relations);
        if (counter.deletedRelations > 0) {
            relationText += " ("+trn("{0} deleted", "{0} deleted", counter.deletedRelations, counter.deletedRelations)+")";
        }
        
        p.add(new JLabel(tr("{0} consists of:", getName())), GBC.eol());
        p.add(new JLabel(nodeText, ImageProvider.get("data", "node"), JLabel.HORIZONTAL), GBC.eop().insets(15,0,0,0));
        p.add(new JLabel(wayText, ImageProvider.get("data", "way"), JLabel.HORIZONTAL), GBC.eop().insets(15,0,0,0));
        p.add(new JLabel(relationText, ImageProvider.get("data", "relation"), JLabel.HORIZONTAL), GBC.eop().insets(15,0,0,0));
        p.add(new JLabel(tr("API version: {0}", (data.getVersion() != null) ? data.getVersion() : tr("unset"))), GBC.eop().insets(15,0,0,0));
        p.add(new JLabel(this.matsimNetwork.getLinks().size()+" MATSim links" ), GBC.eop().insets(15,0,0,0));
        p.add(new JLabel(this.matsimNetwork.getNodes().size()+" MATSim nodes" ), GBC.eop().insets(15,0,0,0));
       

        return p;
    }
  
}
