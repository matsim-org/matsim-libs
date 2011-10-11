package playground.sergioo.CountsFileGenerator;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.sergioo.Visualizer2D.LayersWindow;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.SimpleSelectionNetworkPainter;

public class CountsWindow extends LayersWindow implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	//Enumerations
	private enum PanelIds implements LayersWindow.PanelIds {
		ONE;
	}
	public enum Options implements LayersWindow.Options {
		SELECT_LINK("<html>L<br/>I<br/>N<br/>K</html>"),
		SELECT_NODE("<html>N<br/>O<br/>D<br/>E</html>"),
		ZOOM("<html>Z<br/>O<br/>O<br/>M</html>");
		private String caption;
		private Options(String caption) {
			this.caption = caption;
		}
		@Override
		public String getCaption() {
			return caption;
		}
	}
	public enum Labels implements LayersWindow.Labels {
		LINK("Link"),
		NODE("Node");
		private String text;
		private Labels(String text) {
			this.text = text;
		}
		@Override
		public String getText() {
			return text;
		}
	}
	
	//Attributes
	private JButton readyButton;
	
	//Methods
	public CountsWindow(String title, Network network) {
		this(title, new SimpleSelectionNetworkPainter(network));
	}
	
	public CountsWindow(String title, NetworkPainter networkPainter) {
		setTitle(title);
		setSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout()); 
		layersPanels.put(PanelIds.ONE, new CountsNetworkPanel(this, networkPainter));
		this.add(layersPanels.get(PanelIds.ONE), BorderLayout.CENTER);
		option = Options.ZOOM;
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(Options.values().length,1));
		for(Options option:Options.values()) {
			JButton optionButton = new JButton(option.getCaption());
			optionButton.setActionCommand(option.getCaption());
			optionButton.addActionListener(this);
			buttonsPanel.add(optionButton);
		}
		this.add(buttonsPanel, BorderLayout.EAST);
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		readyButton = new JButton("Ready to exit");
		readyButton.addActionListener(this);
		readyButton.setActionCommand(READY_TO_EXIT);
		infoPanel.add(readyButton, BorderLayout.WEST);
		JPanel labelsPanel = new JPanel();
		labelsPanel.setLayout(new GridLayout(1,Labels.values().length));
		labelsPanel.setBorder(new TitledBorder("Information"));
		labels = new JTextField[Labels.values().length];
		for(int i=0; i<Labels.values().length; i++) {
			labels[i]=new JTextField("");
			labels[i].setEditable(false);
			labels[i].setBackground(null);
			labels[i].setBorder(null);
			labelsPanel.add(labels[i]);
		}
		infoPanel.add(labelsPanel, BorderLayout.CENTER);
		JPanel coordsPanel = new JPanel();
		coordsPanel.setLayout(new GridLayout(1,2));
		coordsPanel.setBorder(new TitledBorder("Coordinates"));
		coordsPanel.add(lblCoords[0]);
		coordsPanel.add(lblCoords[1]);
		infoPanel.add(coordsPanel, BorderLayout.EAST);
		this.add(infoPanel, BorderLayout.SOUTH);
		setSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		for(Options option:Options.values())
			if(e.getActionCommand().equals(option.getCaption()))
				this.option = option;
		if(e.getActionCommand().equals(READY_TO_EXIT)) {
			setVisible(false);
			readyToExit = true;
		}
	}
	public void addPoint(Coord point) {
		((CountsNetworkPanel)layersPanels.get(PanelIds.ONE)).addPoint(point);
	}
	public void selectPoint(Coord point) {
		((CountsNetworkPanel)layersPanels.get(PanelIds.ONE)).selectPoint(point);
	}
	public Link getSelectedLink() {
		return ((CountsNetworkPanel)layersPanels.get(PanelIds.ONE)).getSelectedLink();
	}

	@Override
	public void refreshLabel(playground.sergioo.Visualizer2D.LayersWindow.Labels label) {
		labels[label.ordinal()].setText(((CountsNetworkPanel)layersPanels.get(PanelIds.ONE)).getLabelText(label));
	}

}
