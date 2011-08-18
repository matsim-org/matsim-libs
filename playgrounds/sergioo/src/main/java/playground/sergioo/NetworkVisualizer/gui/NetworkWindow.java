package playground.sergioo.NetworkVisualizer.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JFrame;
import javax.swing.JLabel;

public abstract class NetworkWindow extends JFrame implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Enumerations
	public enum Option {
		SELECT_LINK("<html>L<br/>I<br/>N<br/>K</html>"),
		SELECT_NODE("<html>N<br/>O<br/>D<br/>E</html>"),
		SELECT_LINE("<html>P<br/>O<br/>I<br/>N<br/>T</html>"),
		SELECT_POINT("<html>L<br/>I<br/>N<br/>E</html>"),
		ZOOM("<html>Z<br/>O<br/>O<br/>M</html>");
		public String caption;
		private Option(String caption) {
			this.caption = caption;
		}
	}
	public enum Label {
		LINK("Link"),
		NODE("Node"),
		LINE("Line"),
		POINT("Point");
		String text;
		private Label(String text) {
			this.text = text;
		}
	}
	
	//Constants
	protected static final String READY_TO_EXIT = "exit";
	
	//Attributes
	protected static int width;
	protected static int height;
	protected NetworkPanel panel;
	protected Option option = Option.SELECT_LINK;
	protected JLabel[] labels;
	protected JLabel[] lblCoords = {new JLabel(),new JLabel()};
	private boolean readyToExit = false;
	
	//Methods
	public void refreshLabel(Label label) {
		labels[label.ordinal()].setText(panel.getLabelText(label));
	}
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
	public abstract void cameraChange(Camera camera);
	@Override
	public void actionPerformed(ActionEvent e) {
		for(Option option:Option.values())
			if(e.getActionCommand().equals(option.name()))
				this.option = option;
		if(e.getActionCommand().equals(READY_TO_EXIT)) {
			setVisible(false);
			readyToExit = true;
		}
	}
	
}

