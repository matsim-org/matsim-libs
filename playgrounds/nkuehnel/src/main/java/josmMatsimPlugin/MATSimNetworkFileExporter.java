package josmMatsimPlugin;

import java.io.File;
import java.io.IOException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.FileExporter;

final class MATSimNetworkFileExporter extends FileExporter {
	
	MATSimNetworkFileExporter() {
		super(new ExtensionFileFilter("xml", "xml", "MATSim Network Files (*.xml)"));
	}

	@Override
	public boolean acceptFile(File pathname, Layer layer) {
		return layer instanceof NetworkLayer;
	}

	@Override
	public void exportData(File file, Layer layer) throws IOException {
		ExportTask task = new ExportTask(file);
		Main.worker.execute(task);
	}
	
	
	
}