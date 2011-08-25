package playground.sergioo.NetworksMatcher.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import playground.sergioo.NetworksMatcher.gui.MatchingsPainter.MatchingOptions;
import playground.sergioo.NetworksMatcher.kernel.MatchingProcess;
import playground.sergioo.NetworksMatcher.kernel.NodesMatching;
import playground.sergioo.Visualizer2D.Camera;
import playground.sergioo.Visualizer2D.LayersWindow;

public class DoubleNetworkMatchingWindow extends LayersWindow implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Enumerations
	private enum PanelIds implements LayersWindow.PanelIds {
		A,
		B,
		ACTIVE,
		DOUBLE;
	}
	public enum Options implements LayersWindow.Options {
		SELECT_LINK("<html>L<br/>I<br/>N<br/>K</html>"),
		SELECT_NODE("<html>N<br/>O<br/>D<br/>E</html>"),
		SELECT_NODES("<html>N<br/>O<br/>D<br/>E<br/>S</html>"),
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
		NODE("Node"),
		ACTIVE("Active");
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
	private boolean networksSeparated = true;
	private JPanel panelsPanel;
	
	//Methods
	private DoubleNetworkMatchingWindow(String title) {
		setTitle(title);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		option = Options.ZOOM;
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(Options.values().length,1));
		for(Options option:Options.values()) {
			JButton optionButton = new JButton(option.caption);
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
		labels = new JLabel[Labels.values().length];
		labels[0]=new JLabel("");
		labelsPanel.add(labels[0]);
		labels[1]=new JLabel("");
		labelsPanel.add(labels[1]);
		infoPanel.add(labelsPanel, BorderLayout.CENTER);JPanel coordsPanel = new JPanel();
		coordsPanel.setLayout(new GridLayout(1,2));
		coordsPanel.setBorder(new TitledBorder("Coordinates"));
		coordsPanel.add(lblCoords[0]);
		coordsPanel.add(lblCoords[1]);
		infoPanel.add(coordsPanel, BorderLayout.EAST);
		this.add(infoPanel, BorderLayout.SOUTH);
		setSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
	}
	public DoubleNetworkMatchingWindow(String title, NetworkNodesPainter networkPainterA, NetworkNodesPainter networkPainterB) {
		this(title);
		layersPanels.put(PanelIds.A, new NetworkNodesPanel(this, networkPainterA));
		layersPanels.put(PanelIds.B, new NetworkNodesPanel(this, networkPainterB));
		layersPanels.get(PanelIds.A).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.get(PanelIds.B).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.put(PanelIds.ACTIVE, layersPanels.get(PanelIds.A));
		layersPanels.get(PanelIds.ACTIVE).requestFocus();
		layersPanels.put(PanelIds.DOUBLE, new DoubleNetworkMatchingPanel(this, networkPainterA, networkPainterB));
		panelsPanel = new JPanel();
		panelsPanel.setLayout(new GridLayout());
		panelsPanel.add(layersPanels.get(PanelIds.A), BorderLayout.WEST);
		panelsPanel.add(layersPanels.get(PanelIds.B), BorderLayout.EAST);
		this.add(panelsPanel, BorderLayout.CENTER);
	}
	public DoubleNetworkMatchingWindow(String title, Collection<NodesMatching> nodesMatchings, NetworkNodesPainter networkPainterA, NetworkNodesPainter networkPainterB) {
		this(title);
		layersPanels.put(PanelIds.A, new NetworkNodesPanel(nodesMatchings, MatchingOptions.A, this, networkPainterA));
		layersPanels.put(PanelIds.B, new NetworkNodesPanel(nodesMatchings, MatchingOptions.B, this, networkPainterB));
		layersPanels.get(PanelIds.A).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.get(PanelIds.B).setBorder(new LineBorder(Color.BLACK, 5));
		layersPanels.put(PanelIds.ACTIVE, layersPanels.get(PanelIds.A));
		layersPanels.get(PanelIds.ACTIVE).requestFocus();
		layersPanels.put(PanelIds.DOUBLE, new DoubleNetworkMatchingPanel(nodesMatchings, this, networkPainterA, networkPainterB));
		panelsPanel = new JPanel();
		panelsPanel.setLayout(new GridLayout());
		panelsPanel.add(layersPanels.get(PanelIds.A), BorderLayout.WEST);
		panelsPanel.add(layersPanels.get(PanelIds.B), BorderLayout.EAST);
		this.add(panelsPanel, BorderLayout.CENTER);
	}
	public DoubleNetworkMatchingWindow(String title, MatchingProcess matchingProcess) {
		this(title, matchingProcess.getFinalMatchings(), new NetworkNodesPainter(matchingProcess.getFinalNetworkA()), new NetworkNodesPainter(matchingProcess.getFinalNetworkB(), Color.BLACK, Color.CYAN));
	}
	public void setMatchings(Collection<NodesMatching> nodesMatchings) {
		((NetworkNodesPanel)layersPanels.get(PanelIds.A)).setMatchings(nodesMatchings, MatchingOptions.A);
		((NetworkNodesPanel)layersPanels.get(PanelIds.B)).setMatchings(nodesMatchings, MatchingOptions.B);
		((DoubleNetworkMatchingPanel)layersPanels.get(PanelIds.DOUBLE)).setMatchings(nodesMatchings);
	}
	public void setNetworksSeparated() {
		networksSeparated = !networksSeparated;
		if(networksSeparated) {
			panelsPanel.remove(layersPanels.get(PanelIds.DOUBLE));
			layersPanels.put(PanelIds.ACTIVE, layersPanels.get(PanelIds.A));
			panelsPanel.add(layersPanels.get(PanelIds.A), BorderLayout.WEST);
			panelsPanel.add(layersPanels.get(PanelIds.B), BorderLayout.EAST);
		}
		else {
			panelsPanel.remove(layersPanels.get(PanelIds.A));
			panelsPanel.remove(layersPanels.get(PanelIds.B));
			panelsPanel.add(layersPanels.get(PanelIds.DOUBLE), BorderLayout.CENTER);
		}
		setVisible(true);
		repaint();
		if(networksSeparated)
			layersPanels.get(PanelIds.ACTIVE).requestFocus();
		else
			layersPanels.get(PanelIds.DOUBLE).requestFocus();
	}
	public void cameraChange(Camera camera) {
		if(networksSeparated) {
			if(layersPanels.get(PanelIds.ACTIVE)==layersPanels.get(PanelIds.A)) {
				layersPanels.get(PanelIds.B).getCamera().setCamera(camera.getUpLeftCorner(), camera.getSize());
				layersPanels.get(PanelIds.B).repaint();
			}
			else {
				layersPanels.get(PanelIds.A).getCamera().setCamera(camera.getUpLeftCorner(), camera.getSize());
				layersPanels.get(PanelIds.A).repaint();
			}
		}
	}
	public void setActivePanel(NetworkNodesPanel panel) {
		layersPanels.put(PanelIds.ACTIVE, panel);
	}
	public void refreshLabel(Labels label) {
		labels[label.ordinal()].setText(((NetworkNodesPanel)layersPanels.get(PanelIds.ACTIVE)).getLabelText(label));
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
	
}
