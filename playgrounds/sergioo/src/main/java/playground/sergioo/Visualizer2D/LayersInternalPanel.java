package playground.sergioo.Visualizer2D;

import javax.swing.JPanel;


public class LayersInternalPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Attributes
	private LayersPanel container;
	
	//Methods
	public LayersInternalPanel(LayersPanel container) {
		this.container = container;
	}
	@Override
	public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        container.setAspectRatio();
    }
	
}
