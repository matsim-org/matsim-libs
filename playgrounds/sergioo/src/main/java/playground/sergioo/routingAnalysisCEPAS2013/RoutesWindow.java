package playground.sergioo.routingAnalysisCEPAS2013;

import java.awt.BorderLayout;
import java.awt.Toolkit;

import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.LayersWindow;

public class RoutesWindow extends LayersWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private enum PanelIds implements LayersWindow.PanelIds {
		ONE;
	}
	
	public RoutesWindow(LayersPanel panel) {
		layersPanels.put(PanelIds.ONE, panel);
		this.setLayout(new BorderLayout());
		this.add(panel, BorderLayout.CENTER);
		super.setSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	@Override
	public void refreshLabel(Labels label) {
		
	}

}
