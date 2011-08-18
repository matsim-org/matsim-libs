package playground.sergioo.NetworkVisualizer.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

public abstract class LayersPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	protected final Camera camera;
	protected final List<Layer> layers;
	
	//Methods
	public LayersPanel() {
		layers = new ArrayList<Layer>();
		camera = new Camera();
	}
	
}
