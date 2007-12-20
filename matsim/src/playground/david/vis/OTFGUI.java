/* *********************************************************************** *
 * project: org.matsim.*
 * OTFGUI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.david.vis;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.MessageFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;

import org.matsim.plans.Plan;
import org.matsim.utils.misc.Time;

import playground.david.vis.interfaces.OTFServerRemote;


public class OTFGUI {

	public static interface NetVisResizable {
		public void scaleNetwork(float scale);
		public float getScale();
		public void repaint();
	}

	class myNetVisScrollPane extends NetVisScrollPane implements NetVisResizable {

		private float scale = 1.f;
		public myNetVisScrollPane(NetJComponent networkComponent) {
			super(networkComponent);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void scaleNetwork(float scale){
			this.scale = scale;
			super.scaleNetwork(scale);
		}
		
		public float getScale() {
			return scale;
		}

	}

	
	private final JFrame vizFrame;
	private final ControlToolbar buttonComponent;
	private final NetJComponent networkComponent;
	private myNetVisScrollPane networkScrollPane = null;
	OTFVisNet visnet = null;
	OTFServerRemote host = null;
	OTFAgentRenderer agentRenderer;

	public OTFGUI(OTFVisNet visnet,OTFServerRemote host) {
		this.visnet = visnet;
		this.host = host;
		// ----- 1. create frame -----

		vizFrame = new JFrame("Test");
		vizFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JFrame.setDefaultLookAndFeelDecorated(true);

		// ----- 2. create control bar -----


		RendererA backgroundRenderer = new BackgroundRenderer();
//		LinkSetRendererVehiclesOnly linkSetRenderer = new LinkSetRendererVehiclesOnly<OTFVisNet>(visConfig, visnet);
		OTFLinkSetRendererVehiclesOnly linkSetRenderer = new OTFLinkSetRendererVehiclesOnly( visnet);
		linkSetRenderer.setControlToolbar(null);
		linkSetRenderer.append(backgroundRenderer);
		agentRenderer = new OTFAgentRenderer(visnet);
		agentRenderer.append(linkSetRenderer);

		networkComponent = new NetJComponent(visnet, agentRenderer);
		linkSetRenderer.setTargetComponent(networkComponent);
		agentRenderer.setTargetComponent(networkComponent);
		networkScrollPane = new myNetVisScrollPane(networkComponent);
		buttonComponent = new ControlToolbar(host, visnet, networkScrollPane);
		vizFrame.getContentPane().add(buttonComponent, BorderLayout.NORTH);
		vizFrame.getContentPane().add(networkScrollPane, BorderLayout.CENTER);

		vizGuiHandler handi = new vizGuiHandler();
		networkScrollPane.addMouseMotionListener(handi);
		networkScrollPane.addMouseListener(handi);
		networkScrollPane.getViewport().addChangeListener(handi);

		vizFrame.pack();
		vizFrame.setVisible(true);
	}
	class vizGuiHandler extends MouseInputAdapter implements ChangeListener {
		public Point start = null;

		public Rectangle currentRect = null;

		public int button = 0;

		@Override
		public void mousePressed(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			button = e.getButton();
			start = new Point(x, y);
			// networkComponent.repaint();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (button == 1)
				updateSize(e);
			else if (button == 2) {
				int deltax = start.x - e.getX();
				int deltay = start.y - e.getY();
				start.x = e.getX();
				start.y = e.getY();
				networkScrollPane.moveNetwork(deltax, deltay);
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (button == 1) {
				updateSize(e);
				if ((currentRect.getHeight() > 10)
						&& (currentRect.getWidth() > 10)) {
					float scale =  networkScrollPane.getScale();
					scale = networkScrollPane.scaleNetwork(currentRect,scale);
				} else {
					// try to find agent under mouse
					// calc mouse pos to component pos
			        Rectangle rect = networkScrollPane.getViewport().getViewRect();
			    	Point2D.Double p =  networkComponent.getNetCoord(e.getX() + rect.getX(), e.getY()+ + rect.getY());
					String id = visnet.getAgentId(p);
					Plan plan = null;
					try {
						plan = host.getAgentPlan(id);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					if(plan != null) agentRenderer.setPlan(plan);

					networkScrollPane.invalidate();
					networkScrollPane.repaint();
				}
				currentRect = null;
			}
			button = 0;
		}

		void updateSize(MouseEvent e) {
			currentRect = new Rectangle(start);
			currentRect.add(e.getX(), e.getY());
			networkScrollPane.getGraphics().drawRect(currentRect.x,
					currentRect.y, currentRect.width, currentRect.height);
			networkScrollPane.repaint();
		}

		public void stateChanged(ChangeEvent e) {
			networkScrollPane.updateViewClipRect();
		}
	}


	public static class ControlToolbar extends JToolBar implements ActionListener, ItemListener, ChangeListener {

		private static final String TO_START = "to_start";
		private static final String PAUSE = "pause";
		private static final String PLAY = "play";
		private static final String STEP_F = "step_f";
		private static final String STEP_FF = "step_ff";
		private static final String STOP = "stop";
		private static final String ZOOM_IN = "zoom_in";
		private static final String ZOOM_OUT = "zoom_out";
		private static final String SET_TIME = "set_time";
		private static final String TOGGLE_AGENTS = "Agents";
		private static final String TOGGLE_LINK_LABELS = "Link Labels";

		private static final int SKIP = 30;

		// -------------------- MEMBER VARIABLES --------------------

		//private DisplayableNetI network;
		private final MovieTimer movieTimer = new MovieTimer();
		private JButton playButton;
		private JFormattedTextField timeField;
		private int simTime = 0;

		private NetVisResizable networkScrollPane = null;
		OTFVisNet visnet = null;
		OTFServerRemote host = null;


		// -------------------- CONSTRUCTION --------------------

		public ControlToolbar(OTFServerRemote host, OTFVisNet network,
				NetVisResizable networkScrollPane) {
			super();
			this.visnet = network;
			this.host = host;
			this.networkScrollPane = networkScrollPane;

			addButtons();
		}

		private void addButtons() {
			add(createButton("Pause", PAUSE));
			playButton = createButton("PLAY", PLAY);
			add(playButton);
			add(createButton(">", STEP_F));
			add(createButton(">>", STEP_FF));
			add(createButton("STOP", STOP));

			timeField = new JFormattedTextField( new MessageFormat("{0,number,00}-{1,number,00}-{2,number,00}"));
			timeField.setMaximumSize(new Dimension(75,30));
			timeField.setActionCommand(SET_TIME);
			timeField.setHorizontalAlignment(JTextField.CENTER);
			add( timeField );
			timeField.addActionListener( this );

			add(createButton("--", ZOOM_OUT));
			add(createButton("+", ZOOM_IN));

			createCheckBoxes();

			Integer value = new Integer(50);
			Integer min = new Integer(0);
			Integer max = new Integer(200);
			Integer step = new Integer(1);
			SpinnerNumberModel model = new SpinnerNumberModel(new Integer(155), min, max, step);
			JSpinner spin = addLabeledSpinner(this, "Lanewidth", model);
			spin.setMaximumSize(new Dimension(75,30));
			spin.addChangeListener(this);

			movieTimer.start();

		}

		private JButton createButton(String display, String actionCommand) {
			JButton button;

			button = new JButton();
			button.setActionCommand(actionCommand);
			button.addActionListener(this);
			button.setText(display);

			return button;
		}

		public void updateTimeLabel() {
			timeField.setText(Time.strFromSec(simTime));
		}

		// ---------- IMPLEMENTATION OF ActionListener INTERFACE ----------

		private void stopMovie() {
			if (movieTimer != null) {
				movieTimer.setActive(false);
				playButton.setText("PLAY");
			}
		}

		private void pressed_TO_START() throws IOException {
			//host.restart()
		}

		private void pressed_PAUSE() throws IOException {
			movieTimer.setActive(false);
			playButton.setSelected(false);
			host.pause();
		}

		private void pressed_PLAY() throws RemoteException {
			host.play();
			movieTimer.setActive(true);
			playButton.setSelected(true);
		}

		private void pressed_STEP_F() throws IOException {
			playButton.setSelected(false);
			movieTimer.setActive(false);
			byte [] bbyte = host.getStateBuffer();
			if (bbyte == null) System.out.println("End of movie reached!");
			else visnet.readMyself(new DataInputStream(new ByteArrayInputStream(bbyte,0,bbyte.length)));
		}

		private void pressed_STEP_FF() throws IOException {
			host.step();
		}

		private void pressed_STOP() throws IOException {
			//host.stop()
		}

		private void pressed_ZOOM_OUT() {
			float scale = networkScrollPane.getScale() / 1.42f;
			networkScrollPane.scaleNetwork(scale);
		}

		private void pressed_ZOOM_IN() {
			float scale = networkScrollPane.getScale() * 1.42f;
			networkScrollPane.scaleNetwork(scale);
		}

		private void changed_SET_TIME(ActionEvent event) throws IOException {
			String newTime = ((JFormattedTextField)event.getSource()).getText();
			int newTime_s = Time.secFromStr(newTime);
			stopMovie();
			//reader.toTimeStep(newTime_s);
		}

		@Override
		public void paint(Graphics g) {
			//    updateTimeLabel();
			super.paint(g);
		}

		public void actionPerformed(ActionEvent event) {
			String command = event.getActionCommand();

			try {
				if (TO_START.equals(command))
					pressed_TO_START();
				else if (PAUSE.equals(command))
					pressed_PAUSE();
				else if (PLAY.equals(command))
					pressed_PLAY();
				else if (STEP_F.equals(command))
					pressed_STEP_F();
				else if (STEP_FF.equals(command))
					pressed_STEP_FF();
				else if (STOP.equals(command))
					pressed_STOP();
				else if (ZOOM_OUT.equals(command))
					pressed_ZOOM_OUT();
				else if (ZOOM_IN.equals(command))
					pressed_ZOOM_IN();
				else if (command.equals(SET_TIME))
					changed_SET_TIME(event);
			} catch (IOException e) {
				System.err.println("ControlToolbar encountered problem: " + e);
			}

			updateTimeLabel();

			repaint();

			networkScrollPane.repaint();
		}


		protected JSpinner addLabeledSpinner(Container c,   String label,  SpinnerModel model)
		{
			JLabel l = new JLabel(label);
			c.add(l);
			JSpinner spinner = new JSpinner(model);
			l.setLabelFor(spinner);
			c.add(spinner);
			return spinner;
		}
		private void createCheckBoxes() {
			JCheckBox VehBox = new JCheckBox(TOGGLE_AGENTS);
			VehBox.setMnemonic(KeyEvent.VK_V);
			VehBox.setSelected(true);
			VehBox.addItemListener(this);
			add(VehBox);

			JCheckBox linkLabelBox = new JCheckBox(TOGGLE_LINK_LABELS);
			linkLabelBox.setMnemonic(KeyEvent.VK_L);
			linkLabelBox.setSelected(true);
			linkLabelBox.addItemListener(this);
			add(linkLabelBox);
		}
		public void itemStateChanged(ItemEvent e) {
			JCheckBox source = (JCheckBox)e.getItemSelectable();
			if (source.getText().equals(TOGGLE_AGENTS)) {
				// toggle Labels to
				if (e.getStateChange() == ItemEvent.DESELECTED) {
					//visConfig.set("ShowAgents", "false");
				} else {
					//visConfig.set("ShowAgents", "true");
				}
			} else if (source.getText().equals(TOGGLE_LINK_LABELS)) {
				if (e.getStateChange() == ItemEvent.DESELECTED) {
					//visConfig.set(VisConfig.SHOW_LINK_LABELS, "false");
				} else {
					//visConfig.set(VisConfig.SHOW_LINK_LABELS, "true");
				}
			}
			repaint();
			networkScrollPane.repaint();
		}

		public void stateChanged(ChangeEvent e) {
			JSpinner spinner = (JSpinner)e.getSource();
			int i = ((SpinnerNumberModel)spinner.getModel()).getNumber().intValue();
			
			//visConfig.set(VisConfig.LINK_WIDTH_FACTOR, Integer.toString(i));
			repaint();
			networkScrollPane.repaint();
		}

		class MovieTimer extends Thread {
			boolean isActive = false;
			boolean terminate = false;

			public MovieTimer() {
				setDaemon(true);
			}

			public synchronized boolean isActive() {
				return isActive;
			}

			public synchronized void setActive(boolean isActive) {
				this.isActive = isActive;
			}

			public synchronized void terminate() {
				this.terminate = true;
			}

			@Override
			public void run() {

				int actTime = 0;

				while (!terminate) {
					try {
						sleep(500);
						if (isActive) {
							byte [] bbyte = host.getStateBuffer();
							if (bbyte == null) {
								System.out.println("End of movie reached!");
								pressed_PAUSE();
							}
							else visnet.readMyself(new DataInputStream(new ByteArrayInputStream(bbyte,0,bbyte.length)));
							networkScrollPane.repaint();
						}
						actTime = host.getLocalTime();
						if (simTime != actTime) {
							simTime = actTime;
							updateTimeLabel();
							repaint();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
