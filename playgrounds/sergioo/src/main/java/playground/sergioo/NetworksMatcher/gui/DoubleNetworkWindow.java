package playground.sergioo.NetworksMatcher.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import playground.sergioo.NetworkVisualizer.gui.Camera;
import playground.sergioo.NetworkVisualizer.gui.NetworkPanel;
import playground.sergioo.NetworkVisualizer.gui.NetworkWindow;
import playground.sergioo.NetworkVisualizer.gui.networkPainters.NetworkPainter;

public class DoubleNetworkWindow extends NetworkWindow {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Constants
	private static int GAPX = 50;
	private static int GAPY = 120;
	public static int MAX_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width-GAPX;
	public static int MAX_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height-GAPY;
	public static int MIN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width/2;
	public static int MIN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height/3;
	public static int FRAMESIZE = 20;
	
	//Attributes
	private JButton readyButton;
	private NetworkPanel panelB;
	private NetworkPainter networkPainterA;
	private NetworkPainter networkPainterB;
	private boolean networksSeparated = true;
	private JPanel panelsPanel;
	
	//Methods
	public DoubleNetworkWindow(String title, NetworkPainter networkPainterA, NetworkPainter networkPainterB) {
		setTitle(title);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		this.networkPainterA = networkPainterA;
		this.networkPainterB = networkPainterB;
		panel = new NetworkPanel(this, networkPainterA);
		panelB = new NetworkPanel(this, networkPainterB);
		panelsPanel = new JPanel();
		panelsPanel.setLayout(new GridLayout(1,2));
		panelsPanel.add(panel, BorderLayout.WEST);
		panelsPanel.add(panelB, BorderLayout.EAST);
		this.add(panelsPanel, BorderLayout.CENTER);
		this.setSize(width+GAPX, height+GAPY);
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridLayout(Option.values().length,1));
		for(Option option:Option.values()) {
			JButton optionButton = new JButton(option.caption);
			optionButton.setActionCommand(option.name());
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
		labelsPanel.setLayout(new GridLayout(1,Label.values().length));
		labelsPanel.setBorder(new TitledBorder("Information"));
		labels = new JLabel[Label.values().length];
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
	}
	public void setNetworksSeparated() {
		networksSeparated = !networksSeparated;
		if(networksSeparated) {
			this.remove(panel);
			panel = new NetworkPanel(this, networkPainterA);
			panelB = new NetworkPanel(this, networkPainterB);
			panelsPanel = new JPanel();
			panelsPanel.setLayout(new GridLayout(1,2));
			panelsPanel.add(panel, BorderLayout.WEST);
			panelsPanel.add(panelB, BorderLayout.EAST);
			this.add(panelsPanel, BorderLayout.CENTER);
		}
		else {
			this.remove(panelsPanel);
			panel = new DoubleNetworkPanel(this, networkPainterA, networkPainterB);
			this.add(panel, BorderLayout.CENTER);
		}
	}
	@Override
	public void cameraChange(Camera camera) {
		if(networksSeparated) {
			panel.getCamera().setCamera(camera.getUpLeftCorner(), camera.getSize());
			panelB.getCamera().setCamera(camera.getUpLeftCorner(), camera.getSize());
			panel.repaint();
			panelB.repaint();
		}
	}
	
}
