package playground.sergioo.Visualizer2D;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;

public abstract class LayersWindow extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Enumerations
	public interface PanelId {
		public String name();
	}
	public interface Option {
		public String getCaption();
	}
	public interface Label {
		public String getText();
	}
	
	//Constants
	protected static final String READY_TO_EXIT = "exit";
	
	//Attributes
	protected Option option;
	protected JLabel[] labels;
	protected JLabel[] lblCoords = {new JLabel(),new JLabel()};
	protected boolean readyToExit = false;
	protected final Map<PanelId, LayersPanel> layersPanels = new HashMap<PanelId, LayersPanel>();
	
	//Methods
	public Option getOption() {
		return option;
	}
	public boolean isReadyToExit() {
		return readyToExit;
	}
	public void setCoords(double x, double y) {
		NumberFormat nF = NumberFormat.getInstance();
		nF.setMaximumFractionDigits(4);
		nF.setMinimumFractionDigits(4);
		lblCoords[0].setText(nF.format(x)+" ");
		lblCoords[1].setText(" "+nF.format(y));
	}
	
}

