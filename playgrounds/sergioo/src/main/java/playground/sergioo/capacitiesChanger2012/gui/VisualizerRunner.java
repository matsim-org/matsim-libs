package playground.sergioo.capacitiesChanger2012.gui;

import java.io.IOException;

import playground.sergioo.visualizer2D2012.LayersWindow;

public class VisualizerRunner {

	/**
	 * @param args: 0 - Title, 1 - MATSim XML network file, 2 - MATSim XML public transport schedule file
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		LayersWindow window = new SimpleNetworkWindow();
		window.setVisible(true);
	}

}
