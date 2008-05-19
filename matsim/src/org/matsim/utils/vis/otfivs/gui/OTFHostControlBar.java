/* *********************************************************************** *
 * project: org.matsim.*
 * OTFHostControlBar.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.otfivs.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.swing.ImageIcon;
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

import org.matsim.gbl.Gbl;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.otfivs.data.OTFClientQuad;
import org.matsim.utils.vis.otfivs.data.OTFConnectionManager;
import org.matsim.utils.vis.otfivs.data.OTFNetWriterFactory;
import org.matsim.utils.vis.otfivs.data.OTFServerQuad;
import org.matsim.utils.vis.otfivs.interfaces.OTFDataReader;
import org.matsim.utils.vis.otfivs.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfivs.interfaces.OTFLiveServerRemote;
import org.matsim.utils.vis.otfivs.interfaces.OTFQuery;
import org.matsim.utils.vis.otfivs.interfaces.OTFServerRemote;
import org.matsim.utils.vis.otfivs.server.OTFQuadFileHandler;
import org.matsim.utils.vis.otfivs.server.OTFTVehServer;


public class OTFHostControlBar extends JToolBar implements ActionListener, ItemListener, ChangeListener {

	private static final String CONNECT = "connect";
	private static final String TO_START = "to_start";
	private static final String PAUSE = "pause";
	private static final String PLAY = "play";
	private static final String STEP_F = "step_f";
	private static final String STEP_FF = "step_ff";
	private static final String STOP = "stop";
	private static final String SET_TIME = "set_time";
	private static final String TOGGLE_SYNCH = "Synch";
	private static final String TOGGLE_LINK_LABELS = "Link Labels";
	private static final String STEP_BB = "step_bb";
	private static final String STEP_B = "step_b";
	private static final String FULLSCREEN = "fullscreen";

	private static final int SKIP = 30;

	// -------------------- MEMBER VARIABLES --------------------

	//private DisplayableNetI network;
	private MovieTimer movieTimer = null;
	private JButton playButton;
	private JFormattedTextField timeField;
	private int simTime = 0;
	private boolean synchronizedPlay = true;
	private boolean liveHost = false;

	String address;
	private OTFServerRemote host = null;
	private final Map <String,OTFDrawer> handlers = new HashMap<String,OTFDrawer>();
	private static Class resourceHandler = null;

	private ImageIcon playIcon = null;
	private ImageIcon pauseIcon = null;

	public JFrame frame = null;
	
	private Rectangle windowBounds = null;
	
	// -------------------- CONSTRUCTION --------------------

	public OTFHostControlBar(String address, Class res) throws RemoteException, InterruptedException, NotBoundException {
		openAddress(address);
		this.resourceHandler = res;

		addButtons();
	}

	public OTFHostControlBar(String address) throws RemoteException, InterruptedException, NotBoundException {
		openAddress(address);
		addButtons();
	}

	private void openAddress(String address) throws RemoteException, InterruptedException, NotBoundException {
		// try to open/connect to host if given a string of form
		// connection type (rmi or file or tveh)
		// rmi:ip  [: port]
		// file:mvi-filename
		// tveh:T.veh-filename @ netfilename
		// e.g. "file:../MatsimJ/otfvis.mvi" or "rmi:127.0.0.1:4019" or "tveh:../MatsimJ/output/T.veh@../../studies/wip/network.xml"
		if(address == null) address = "rmi:127.0.0.1:4019";

		this.address = address;
		String type = address.substring(0,address.indexOf(':'));
		String connection = address.substring(address.indexOf(':')+1, address.length());
		if (type.equals("rmi")) {
			int port = 4019;
			String [] connparse = connection.split(":");
			if (connparse.length > 1 ) port = Integer.parseInt(connparse[1]);
			this.host = openRMI(connparse[0], port);

		} else if (type.equals("ssl")) {
			int port = 4019;
			String [] connparse = connection.split(":");
			if (connparse.length > 1 ) port = Integer.parseInt(connparse[1]);
			this.host = openSSL(connparse[0], port);

		} else if (type.equals("file")) {
			this.host = openFile(connection);

		} else if (type.equals("tveh")) {
			String [] connparse = connection.split("@");
			this.host = openTVehFile(connparse[1], connparse[0]);

		} else throw new UnsupportedOperationException("Connctiontype " + type + " not known");

		if (host != null) liveHost = host.isLive();
	}

	private OTFServerRemote openSSL(String hostname, int port) throws InterruptedException, RemoteException, NotBoundException {
		System.setProperty("javax.net.ssl.keyStore", "input/keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "vspVSP");
		System.setProperty("javax.net.ssl.trustStore", "input/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "vspVSP");

		Thread.sleep(1000);
		Registry registry = LocateRegistry.getRegistry(hostname, port, new SslRMIClientSocketFactory());

		String[] liste = registry.list();
		for (String name : liste) {
			if (name.indexOf("DSOTFServer_") != -1){
				this.host = (OTFServerRemote)registry.lookup(name);
				host.pause();
			}
		}
		return host;
	}

	private OTFServerRemote openRMI(String hostname, int port) throws InterruptedException, RemoteException, NotBoundException {
		Thread.sleep(1000);
		Registry registry = LocateRegistry.getRegistry(hostname, port);

		String[] liste = registry.list();
		for (String name : liste) {
			if (name.indexOf("DSOTFServer_") != -1){
				this.host = (OTFServerRemote)registry.lookup(name);
				host.pause();
			}
		}
		return host;
	}

	private void buildIndex() {
		// Read through the whole file to find the indexes for the time steps...

	}
	private OTFServerRemote openFile( String fileName) throws RemoteException {
		OTFServerRemote host = new OTFQuadFileHandler.Reader(fileName);
		host.pause();
		Gbl.printMemoryUsage();
		return host;
	}

	private OTFServerRemote openTVehFile(String netname, String vehname) throws RemoteException {
		OTFServerRemote host = new OTFTVehServer(netname,vehname);
		host.pause();
		return host;
	}

	public OTFClientQuad createNewView(String id, OTFNetWriterFactory factory, OTFConnectionManager connect) throws RemoteException {
		OTFVisConfig config = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);

		if(config.getFileVersion() < OTFQuadFileHandler.VERSION || config.getFileMinorVersion() < OTFQuadFileHandler.MINORVERSION) {
			// go through every reader class and look for the appropriate Reader Version for this fileformat
			connect.adoptFileFormat(OTFDataReader.getVersionString(config.getFileVersion(), config.getFileMinorVersion()));
		}

		System.out.println("Getting Quad");
		OTFServerQuad servQ = host.getQuad(id, factory);
		System.out.println("Converting Quad");
		OTFClientQuad clientQ = servQ.convertToClient(id, host, connect);
		System.out.println("Creating receivers");
		clientQ.createReceiver(connect);
		clientQ.getConstData();
		// if this is a recorded session, build random access index
		if (!liveHost ) buildIndex();
		simTime = host.getLocalTime();
		updateTimeLabel();
		return clientQ;
	}

	public void addHandler(String id, OTFDrawer handler) {
		handlers.put(id, handler);
	}

	public OTFDrawer getHandler( String id) {
		return handlers.get(id);
	}

	public void invalidateHandlers() {
		for (OTFDrawer handler : handlers.values()) {
			try {
				handler.invalidate(simTime);
			} catch (RemoteException e) {
				// Possibly lost contact to host DS TODO Handle this!
				e.printStackTrace();
			}
		}
	}

	public void clearCaches() {
		for (OTFDrawer handler : handlers.values()) {
			handler.clearCache();
		}
	}

	private void addButtons() {

		this.setFloatable(false);
		
		URL imageURL = resourceHandler != null ? resourceHandler.getResource("res/otfvis/buttonPlay.png") : null;
		Image image = imageURL != null ? Toolkit.getDefaultToolkit().getImage(imageURL):Toolkit.getDefaultToolkit().getImage("res/otfvis/buttonPlay.png");
    playIcon = new ImageIcon(image, "Play");
    imageURL = resourceHandler != null ? resourceHandler.getResource("res/otfvis/buttonPause.png") : null;
    image = imageURL != null ? Toolkit.getDefaultToolkit().getImage(imageURL):Toolkit.getDefaultToolkit().getImage("res/otfvis/buttonPause.png");
    pauseIcon = new ImageIcon(image, "Pause");

		add(createButton("Restart", STOP, "buttonRestart", "restart the server/simulation"));
		if (!this.liveHost) {
			add(createButton("<<", STEP_BB, "buttonStepBB", "go several timesteps backwards"));
			add(createButton("<", STEP_B, "buttonStepB", "go one timestep backwards"));
		}
		playButton = createButton("PLAY", PLAY, "buttonPlay", "press to play simulation continuously");
		add(playButton);
		add(createButton(">", STEP_F, "buttonStepF", "go one timestep forward"));
		add(createButton(">>", STEP_FF, "buttonStepFF", "go several timesteps forward"));
		timeField = new JFormattedTextField( new MessageFormat("{0,number,00}:{1,number,00}:{2,number,00}"));
		timeField.setMaximumSize(new Dimension(100,30));
		timeField.setMinimumSize(new Dimension(80,30));
		timeField.setActionCommand(SET_TIME);
		timeField.setHorizontalAlignment(JTextField.CENTER);
		add( timeField );
		timeField.addActionListener( this );

//		add(createButton("fullscreen", FULLSCREEN, "buttonFullscreen", "toggles to fullscreen and back"));
		
		//add(createButton("--", ZOOM_OUT));
		//add(createButton("+", ZOOM_IN));

		createCheckBoxes();

//		Integer value = new Integer(50);
//		Integer min = new Integer(0);
//		Integer max = new Integer(200);
//		Integer step = new Integer(1);
//		SpinnerNumberModel model = new SpinnerNumberModel(new Integer(155), min, max, step);
//		JSpinner spin = addLabeledSpinner(this, "Lanewidth", model);
//		spin.setMaximumSize(new Dimension(75,30));
//		spin.addChangeListener(this);
//		JButton button = createButton(address,CONNECT, null, "Server connection established");
//		add(button);
		add(new JLabel(address));
	}

	private JButton createButton(String altText, String actionCommand, String imageName, String toolTipText) {
		JButton button;

	    //Create and initialize the button.
		button = new JButton();
		button.putClientProperty("JButton.buttonType","icon");
		button.setActionCommand(actionCommand);
		button.addActionListener(this);
	  button.setToolTipText(toolTipText);
	  button.setBorderPainted(false);
	  button.setMargin(new Insets(0, 0, 0, 0));

	    if (imageName != null ) {                      //image found
			//Look for the image.
		    String imgLocation = "res/otfvis/"
		                         + imageName
		                         + ".png";

		    URL imageURL = resourceHandler != null ? resourceHandler.getResource(imgLocation) : null;
			Image image = imageURL != null ? Toolkit.getDefaultToolkit().getImage(imageURL):Toolkit.getDefaultToolkit().getImage(imgLocation);
	    	ImageIcon icon =new ImageIcon(image, altText);
	        if(icon.getIconHeight() != -1) button.setIcon(icon);
	        else button.setText(altText);
	    } else {                                     //no image found
	        button.setText(altText);
	    }

		return button;
	}

	public void updateTimeLabel() {
		timeField.setText(Time.writeTime(simTime));
	}

	// ---------- IMPLEMENTATION OF ActionListener INTERFACE ----------

	private void stopMovie() {
		if (movieTimer != null) {
			movieTimer.terminate();
			movieTimer.setActive(false);
			movieTimer = null;
			playButton.setSelected(false);
		}
	}

	private void pressed_TO_START() throws IOException {
		//host.restart()
	}

	private void pressed_PAUSE() throws IOException {
		stopMovie();
		host.pause();
		playButton.setIcon(playIcon);
	}

	private void pressed_PLAY() throws IOException {
		if (movieTimer == null) {
 	  	movieTimer = new MovieTimer();
 	 	  movieTimer.start();
 			movieTimer.setActive(true);
 			playButton.setIcon(pauseIcon);
// 			playButton.setSelected(true);
 	  } else {
 	   	pressed_PAUSE();
 	  }
	}
	
	private void pressed_FULLSCREEN() {
		if (this.frame == null) {
			return;
		}
		GraphicsDevice gd = this.frame.getGraphicsConfiguration().getDevice();
		if (gd.getFullScreenWindow() == null) {
			System.out.println("enter fullscreen");
			this.windowBounds = frame.getBounds();
	  	frame.dispose();
	  	frame.setUndecorated(true);
	  	gd.setFullScreenWindow(frame);
		} else {
			System.out.println("exit fullscreen");
			gd.setFullScreenWindow(null);			
			frame.dispose();
			frame.setUndecorated(false);
			frame.setBounds(this.windowBounds);
			frame.setVisible(true);
		}
	}

	private boolean requestTimeStep(int newTime, OTFServerRemote.TimePreference prefTime)  throws IOException {
		stopMovie();
		if (host.requestNewTime(newTime, prefTime)) {
			simTime = host.getLocalTime();
			invalidateHandlers();
			return true;
		} else {
			if ( prefTime == OTFServerRemote.TimePreference.EARLIER) System.out.println("No previous timestep found");
			else System.out.println("No succeeding timestep found");
			return false;
		}
	}

	private void pressed_STEP_F() throws IOException {
		requestTimeStep(simTime+1, OTFServerRemote.TimePreference.LATER);
	}

	private void pressed_STEP_FF() throws IOException {
		int bigStep = ((OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME)).getBigTimeStep();
		requestTimeStep(simTime+bigStep, OTFServerRemote.TimePreference.LATER);
	}

	private void pressed_STEP_B() throws IOException {
		requestTimeStep(simTime-1, OTFServerRemote.TimePreference.EARLIER);
	}

	private void pressed_STEP_BB() throws IOException {
		int bigStep = ((OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME)).getBigTimeStep();
		requestTimeStep(simTime-bigStep, OTFServerRemote.TimePreference.EARLIER);
}

	private void pressed_STOP() throws IOException {
		pressed_PAUSE();
	}

	int gotoTime = 0;
	private OTFAbortGoto progressBar = null;
	public void gotoTime() {
		try {
			if (!requestTimeStep(gotoTime, OTFServerRemote.TimePreference.EARLIER))
				requestTimeStep(gotoTime, OTFServerRemote.TimePreference.LATER);
			progressBar.terminate = true;
			simTime = host.getLocalTime();
			updateTimeLabel();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void changed_SET_TIME(ActionEvent event) throws IOException {
		String newTime = ((JFormattedTextField)event.getSource()).getText();
		int newTime_s = (int)Time.parseTime(newTime);
		stopMovie();
		progressBar  = new OTFAbortGoto(host, newTime_s);
		progressBar.start();
		gotoTime = newTime_s;
		new Thread (){@Override
		public void run() {gotoTime();}}.start();
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
			else if (STEP_B.equals(command))
				pressed_STEP_B();
			else if (STEP_BB.equals(command))
				pressed_STEP_BB();
			else if (STOP.equals(command))
				pressed_STOP();
			else if (FULLSCREEN.equals(command)) {
				pressed_FULLSCREEN();
			}
			else if (command.equals(SET_TIME))
				changed_SET_TIME(event);
		} catch (IOException e) {
			System.err.println("ControlToolbar encountered problem: " + e);
		}

		updateTimeLabel();

		repaint();

		//networkScrollPane.repaint();
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
		if ( liveHost) {
			JCheckBox SynchBox = new JCheckBox(TOGGLE_SYNCH);
			SynchBox.setMnemonic(KeyEvent.VK_V);
			SynchBox.setSelected(synchronizedPlay);
			SynchBox.addItemListener(this);
			add(SynchBox);
		}

//		JCheckBox linkLabelBox = new JCheckBox(TOGGLE_LINK_LABELS);
//		linkLabelBox.setMnemonic(KeyEvent.VK_L);
//		linkLabelBox.setSelected(true);
//		linkLabelBox.addItemListener(this);
//		add(linkLabelBox);
	}
	public void itemStateChanged(ItemEvent e) {
		JCheckBox source = (JCheckBox)e.getItemSelectable();
		if (source.getText().equals(TOGGLE_SYNCH)) {
			synchronizedPlay = e.getStateChange() != ItemEvent.DESELECTED;
			if (movieTimer != null) movieTimer.updateSyncPlay();
		} else if (source.getText().equals(TOGGLE_LINK_LABELS)) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				//visConfig.set(VisConfig.SHOW_LINK_LABELS, "false");
			} else {
				//visConfig.set(VisConfig.SHOW_LINK_LABELS, "true");
			}
		}
		repaint();
		//networkScrollPane.repaint();
	}

	public void stateChanged(ChangeEvent e) {
		JSpinner spinner = (JSpinner)e.getSource();
		int i = ((SpinnerNumberModel)spinner.getModel()).getNumber().intValue();

		//visConfig.set(VisConfig.LINK_WIDTH_FACTOR, Integer.toString(i));
		repaint();
		//networkScrollPane.repaint();
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

		private synchronized void updateSyncPlay() {
			if (!isActive) return;

			try {
				if (synchronizedPlay) host.pause();
				else host.play();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public synchronized void setActive(boolean isActive) {
			this.isActive = isActive;

			updateSyncPlay();
		}

		public synchronized void terminate() {
			this.terminate = true;
		}

		@Override
		public void run() {

			int actTime = 0;

			while (!terminate) {
				try {
					sleep(30);
					if (isActive && synchronizedPlay) {
						if(!host.requestNewTime(simTime+1, OTFServerRemote.TimePreference.LATER))
							host.requestNewTime(0, OTFServerRemote.TimePreference.LATER);
					}

					actTime = simTime;
					simTime = host.getLocalTime();
					if (simTime != actTime) {
						updateTimeLabel();
						repaint();
						if (isActive)  invalidateHandlers();
					}
					//simTime = actTime;
				} catch (Exception e) {
					stopMovie();
				}
			}
		}
	}

	/**
	 * @return the liveHost
	 */
	public boolean isLiveHost() {
		return liveHost;
	}

	// consolidate this with the OTFQuadClient Query method , there shoul only be ONE way to send queies,
	// apparently quereies are not dependen on a certain view right now, so it should be hot.doQuery
	
	public OTFQuery doQuery(OTFQuery query) {
		OTFQuery result = null;
		try {
			if(host.isLive()) {
				result = ((OTFLiveServerRemote)host).answerQuery(query);
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}



}
