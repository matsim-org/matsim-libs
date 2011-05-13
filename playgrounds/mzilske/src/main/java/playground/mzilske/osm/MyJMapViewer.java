package playground.mzilske.osm;

import javax.swing.JPanel;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.Tile;

public class MyJMapViewer extends JMapViewer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JPanel panel;

	public MyJMapViewer(JPanel compositePanel) {
		this.panel = compositePanel;
	}

	@Override
	public void tileLoadingFinished(Tile tile, boolean success) {
		super.tileLoadingFinished(tile, success);
		// We need to notify our parent component that we are finished drawing tiles, since
		// our parent component is probably an overlay over this map, which needs to be redrawn now.
		panel.repaint();
	}
	
	

}
