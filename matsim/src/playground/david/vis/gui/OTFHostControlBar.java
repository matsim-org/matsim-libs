package playground.david.vis.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
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

import playground.david.vis.OTFQuadFileHandler;
import playground.david.vis.OTFTVehServer;
import playground.david.vis.data.OTFClientQuad;
import playground.david.vis.data.OTFConnectionManager;
import playground.david.vis.data.OTFNetWriterFactory;
import playground.david.vis.data.OTFServerQuad;
import playground.david.vis.interfaces.OTFDataReader;
import playground.david.vis.interfaces.OTFServerRemote;

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

	private static final int SKIP = 30;

	// -------------------- MEMBER VARIABLES --------------------

	//private DisplayableNetI network;
	private MovieTimer movieTimer = null;
	private JButton playButton;
	private JFormattedTextField timeField;
	private int simTime = 0;
	private boolean synchronizedPlay = true;

	String address;
	private OTFServerRemote host = null;
	private final Map <String,OTFEventHandler> handlers = new HashMap<String,OTFEventHandler>(); 
	private Class resourceHandler = null;

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
		long freeMem = Runtime.getRuntime().freeMemory();

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
		if (!host.isLive() ) buildIndex();
		return clientQ;
	}
	
	public void addHandler(String id, OTFEventHandler handler) {
		handlers.put(id, handler);
	}
	
	public OTFEventHandler getHandler( String id) {
		return handlers.get(id);
	}
	
	public void invalidateHandlers() {
		for (OTFEventHandler handler : handlers.values()) {
			try {
				handler.invalidate(simTime);
			} catch (RemoteException e) {
				// Possibly lost contact to host DS TODO Handle this!
				e.printStackTrace();
			}
		}
	}

	private void addButtons() {
		add(createButton("Restart", STOP, "buttonRestart", "restart the server/simulation"));
		add(createButton(address,CONNECT, null, "Server connection established"));
		add(createButton("Pause", PAUSE, "buttonPause", "Press to pause server"));
		add(createButton("<<", STEP_BB, "buttonStepBB", "go several timesteps backwards"));
		add(createButton("<", STEP_B, "buttonStepB", "go one timestep backwards"));
		add(createButton("Pause", PAUSE, "buttonPause", "Press to pause server"));
		playButton = createButton("PLAY", PLAY, "buttonPlay", "press to play simulation continuously");
		playButton.setText("Play");
		add(playButton);
		add(createButton(">", STEP_F, "buttonStepF", "go one timestep forward"));
		add(createButton(">>", STEP_FF, "buttonStepFF", "go several timesteps forward"));

		timeField = new JFormattedTextField( new MessageFormat("{0,number,00}-{1,number,00}-{2,number,00}"));
		timeField.setMaximumSize(new Dimension(75,30));
		timeField.setActionCommand(SET_TIME);
		timeField.setHorizontalAlignment(JTextField.CENTER);
		add( timeField );
		timeField.addActionListener( this );

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
	}

	private JButton createButton(String altText, String actionCommand, String imageName, String toolTipText) {
		JButton button;

	    //Create and initialize the button.
		button = new JButton();
		button.putClientProperty("JButton.buttonType","icon");
		button.setActionCommand(actionCommand);
		button.addActionListener(this);
	    button.setToolTipText(toolTipText);

		//Look for the image.
	    String imgLocation = "images/"
	                         + imageName
	                         + ".png";
	    URL imageURL = resourceHandler != null ? resourceHandler.getResource(imgLocation) : null;
		Image image = imageURL != null ? Toolkit.getDefaultToolkit().getImage(imageURL):Toolkit.getDefaultToolkit().getImage(imgLocation);
		
	    if (imageName != null ) {                      //image found
	    	ImageIcon icon =new ImageIcon(image, altText);
	        button.setIcon(icon);
	    } else {                                     //no image found
	        button.setText(altText);
	        System.err.println("Resource not found: " + imgLocation);
	    }

		return button;
	}

	public void updateTimeLabel() {
		timeField.setText(Time.strFromSec(simTime));
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
	}

	private void pressed_PLAY() throws RemoteException {
 	    if (movieTimer == null) movieTimer = new MovieTimer();
 	    movieTimer.start();
		movieTimer.setActive(true);
		playButton.setSelected(true);
	}

	private void pressed_STEP_F() throws IOException {
		stopMovie();
		host.step();
		simTime = host.getLocalTime();
		invalidateHandlers();
	}

	private void pressed_STEP_FF() throws IOException {
		pressed_STEP_F();
	}

	private void pressed_STEP_B() throws IOException {
		stopMovie();

		if(isCachedTime(simTime-1)){
			simTime -= 1;
			invalidateHandlers();
		} else if (host.requestNewTime(simTime-1, OTFServerRemote.TimePreference.EARLIER)) {
			simTime = host.getLocalTime();
			invalidateHandlers();
		} else {
			System.out.println("No prevoius timestep found");
		}
	}

	private boolean isCachedTime(int time) {
		boolean result = true;
		for (OTFEventHandler handler : handlers.values()) {
			result &= handler.isCached(time);
		}
		return result;
	}

	private void pressed_STEP_BB() throws IOException {
		pressed_STEP_F();
	}

	private void pressed_STOP() throws IOException {
		pressed_PAUSE();
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
			else if (STEP_B.equals(command))
				pressed_STEP_B();
			else if (STEP_BB.equals(command))
				pressed_STEP_FF();
			else if (STOP.equals(command))
				pressed_STOP();
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
		JCheckBox SynchBox = new JCheckBox(TOGGLE_SYNCH);
		SynchBox.setMnemonic(KeyEvent.VK_V);
		SynchBox.setSelected(synchronizedPlay);
		SynchBox.addItemListener(this);
		add(SynchBox);

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
					if (isActive && synchronizedPlay) host.step();
					
					actTime = simTime;
					simTime = host.getLocalTime();
					if (simTime != actTime) {
						updateTimeLabel();
						repaint();
						if (isActive)  invalidateHandlers();
					}
					simTime = actTime;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}


}
