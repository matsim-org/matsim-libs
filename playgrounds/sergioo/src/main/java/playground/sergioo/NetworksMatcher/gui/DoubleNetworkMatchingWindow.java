package playground.sergioo.NetworksMatcher.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import playground.sergioo.NetworksMatcher.gui.MatchingsPainter.MatchingOptions;
import playground.sergioo.NetworksMatcher.kernel.core.MatchingProcess;
import playground.sergioo.NetworksMatcher.kernel.core.NodesMatching;
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
		NODES("Nodes"),
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
	private MatchingProcess matchingProcess;
	private int step;
	
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
		labelsPanel.setLayout(new FlowLayout());
		labelsPanel.setBorder(new TitledBorder("Information"));
		labels = new JTextField[Labels.values().length];
		for(int i=0; i<Labels.values().length; i++) {
			labels[i]=new JTextField("");
			labels[i].setEditable(false);
			labels[i].setBackground(null);
			labelsPanel.add(labels[i]);
		}
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
	public DoubleNetworkMatchingWindow(String title, MatchingProcess matchingProcess) {
		this(title);
		this.matchingProcess = matchingProcess;
		this.step = matchingProcess.getNumSteps()-1;
		initialise(true);
	}
	private void initialise(boolean finalNetworks) {
		Set<NodesMatching> nodesMatchings = matchingProcess.getMatchings(step);
		NetworkNodesPainter networkPainterA = null;
		NetworkNodesPainter networkPainterB = null;
		if(finalNetworks) {
			networkPainterA = new NetworkNodesPainter(matchingProcess.getFinalNetworkA());
			networkPainterB = new NetworkNodesPainter(matchingProcess.getFinalNetworkB(), Color.BLACK, Color.CYAN);
		}
		else {
			networkPainterA = new NetworkNodesPainter(matchingProcess.getNetworkA(step));
			networkPainterB = new NetworkNodesPainter(matchingProcess.getNetworkB(step), Color.BLACK, Color.CYAN);
		}
		List<Color> colors = MatchingsPainter.generateRandomColors(nodesMatchings.size());
		if(layersPanels.get(PanelIds.DOUBLE)!=null)
			this.remove(layersPanels.get(PanelIds.DOUBLE));
		if(panelsPanel!=null)
			this.remove(panelsPanel);
		layersPanels.clear();
		layersPanels.put(PanelIds.A, new NetworkNodesPanel(nodesMatchings, MatchingOptions.A, this, networkPainterA, colors));
		layersPanels.put(PanelIds.B, new NetworkNodesPanel(nodesMatchings, MatchingOptions.B, this, networkPainterB, colors));
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
	public void setMatchings(Collection<NodesMatching> nodesMatchings) {
		List<Color> colors = MatchingsPainter.generateRandomColors(nodesMatchings.size());
		((NetworkNodesPanel)layersPanels.get(PanelIds.A)).setMatchings(nodesMatchings, MatchingOptions.A, colors);
		((NetworkNodesPanel)layersPanels.get(PanelIds.B)).setMatchings(nodesMatchings, MatchingOptions.B, colors);
		((DoubleNetworkMatchingPanel)layersPanels.get(PanelIds.DOUBLE)).setMatchings(nodesMatchings);
	}
	public void setNetworksSeparated() {
		networksSeparated = !networksSeparated;
		if(networksSeparated) {
			layersPanels.put(PanelIds.ACTIVE, layersPanels.get(PanelIds.A));
			this.remove(layersPanels.get(PanelIds.DOUBLE));
			this.add(panelsPanel);
		}
		else {
			this.remove(panelsPanel);
			this.add(layersPanels.get(PanelIds.DOUBLE), BorderLayout.CENTER);
		}
		setVisible(true);
		repaint();
		if(networksSeparated)
			layersPanels.get(PanelIds.ACTIVE).requestFocus();
		else
			layersPanels.get(PanelIds.DOUBLE).requestFocus();
	}
	public void nextNetwork() {
		step++;
		if(step==matchingProcess.getNumSteps())
			step = 0;
		initialise(false);
		setNetworksSeparated();
		setNetworksSeparated();
	}
	public void previousNetwork() {
		step--;
		if(step<0)
			step = matchingProcess.getNumSteps()-1;
		initialise(false);
		setNetworksSeparated();
		setNetworksSeparated();
	}
	public void finalNetworks() {
		step = matchingProcess.getNumSteps()-1;
		initialise(true);
		setNetworksSeparated();
		setNetworksSeparated();
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
		if(label.equals(Labels.ACTIVE))
			labels[label.ordinal()].setText(layersPanels.get(PanelIds.ACTIVE)==layersPanels.get(PanelIds.A)?"A":"B");
		else
			labels[label.ordinal()].setText(((NetworkNodesPanel)layersPanels.get(PanelIds.ACTIVE)).getLabelText(label));
		repaint();
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
