package josmMatsimPlugin;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.FileExporter;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The FileExporter that handles file output when saving. Runs validation tests
 * in advance.
 * 
 * 
 */
final class MATSimNetworkFileExporter extends FileExporter {

	MATSimNetworkFileExporter() {
		super(new ExtensionFileFilter("xml", "xml",
				"MATSim Network Files (*.xml)"));
	}

	@Override
	public boolean acceptFile(File pathname, Layer layer) {
		return layer instanceof NetworkLayer;
	}

	@Override
	public void exportData(File file, Layer layer) throws IOException {

		MATSimTest test = new MATSimTest();
		test.startTest(NullProgressMonitor.INSTANCE);
		test.visit(((OsmDataLayer) layer).data.allPrimitives());
		test.endTest();

		boolean okToExport = true;

		for (TestError error : test.getErrors()) {
			if (error.getSeverity().equals(Severity.WARNING)) {
				int proceed = JOptionPane.showConfirmDialog(Main.parent,
						"Validaton resulted in warnings.\n Proceed?",
						"Warning", JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE);
				if (proceed == JOptionPane.NO_OPTION) {
					okToExport = false;
					break;
				} else if (proceed == JOptionPane.YES_OPTION) {
					break;
				}
			}
		}

		for (TestError error : test.getErrors()) {
			if (error.getSeverity().equals(Severity.ERROR)) {
				JOptionPane
						.showMessageDialog(
								Main.parent,
								"Export failed due to validation errors. See validation layer for details.",
								"Failure", JOptionPane.ERROR_MESSAGE,
								new ImageProvider("warning-small").setWidth(16)
										.get());
				okToExport = false;
				break;
			}
		}

		if (okToExport) {
			ExportTask task = new ExportTask(file);
			Main.worker.execute(task);
		}

		OsmValidator.initializeErrorLayer();
		Main.map.validatorDialog.unfurlDialog();
		Main.main.getEditLayer().validationErrors.clear();
		Main.main.getEditLayer().validationErrors.addAll(test.getErrors());
		Main.map.validatorDialog.tree.setErrors(test.getErrors());

	}
}