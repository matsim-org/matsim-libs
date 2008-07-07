package playground.david;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.nio.ByteBuffer;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSplitPane;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.network.Link;
import org.matsim.network.LinkImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plan;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.vis.otfvis.data.OTFClientQuad;
import org.matsim.utils.vis.otfvis.data.OTFConnectionManager;
import org.matsim.utils.vis.otfvis.data.OTFNetWriterFactory;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;
import org.matsim.utils.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.gui.PreferencesDialog;
import org.matsim.utils.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;
import org.matsim.utils.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.utils.vis.snapshots.writers.PositionInfo.VehicleState;

class CALink extends QueueLink {
	public static boolean ueberholverbot = false;
	
	public static int LINKLEN = 500;
	public static int LANECOUNT = 5;

	public CALink(Link l, QueueNetworkLayer queueNetworkLayer, QueueNode toNode) {
		super(l, queueNetworkLayer, toNode);
	}

	@Override
	public Collection<PositionInfo> getVehiclePositions(Collection<PositionInfo> positions) {
		for(int i=0;i<100;i++)positions.add( new PositionInfo(new IdImpl("0"),this.getLink(), Math.random()*LINKLEN, Gbl.random.nextInt(LANECOUNT) + 1, Gbl.random.nextInt(100), VehicleState.Driving, ""));
		return positions;
	}

	/* (non-Javadoc)
	 * @see org.matsim.mobsim.QueueLink#moveLink(double)
	 */
	@Override
	protected boolean moveLink(double now) {
		return true;
	}
	
};

class OTFServerQUADCA extends OTFServerQuad {

	
	private static final long serialVersionUID = 1L;
	
	public CALink link;
	
	public OTFServerQUADCA(double minX, double minY, double maxX, double maxY) {
		super(minX, minY, maxX, maxY);
		NetworkLayer net = new NetworkLayer();
		BasicNode node1 = net.createNode("0","0","500","");
		BasicNode node2 = net.createNode("1",new Integer(CALink.LINKLEN).toString(),"500","");
		
		Link lk = new LinkImpl(new IdImpl("0"), node1, node2, net, CALink.LINKLEN,0,0, CALink.LANECOUNT);
		link = new CALink(lk, null, null);
	}
	
	@Override
	public void fillQuadTree(OTFNetWriterFactory writers) {
		OTFLinkAgentsHandler.Writer writer = new OTFLinkAgentsHandler.Writer();
		writer.setSrc(link);
		put(CALink.LINKLEN/2, 500, writer);
	}
	
	
}

class CALiveServer implements OTFLiveServerRemote{
	private transient final ByteBuffer buf = ByteBuffer.allocate(20000000);
	OTFServerQUADCA quad;

	int time = 0;

	public int getLocalTime() throws RemoteException {
		return time;
	}

	public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers) throws RemoteException {
		quad = new OTFServerQUADCA(0,0,CALink.LINKLEN , 1000);
		quad.fillQuadTree(writers);
		return quad;
	}

	public  byte[] getQuadStateBuffer(boolean isConst) {
		buf.position(0);
		if(isConst) quad.writeConstData(buf); 
		else quad.writeDynData(null, buf);
		
		byte [] result;
		synchronized (buf) {
			result = buf.array();
		}
		return result;
	}
	
	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		return getQuadStateBuffer(true);
	}	

	public byte[] getQuadDynStateBuffer(String id, Rect bounds) throws RemoteException {
		return getQuadStateBuffer(false);
	}

	public boolean isLive() throws RemoteException {
		return true;
	}

	public Collection<Double> getTimeSteps() throws RemoteException {
		return null;
	}

	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
		if(time > this.time) this.time++;
		quad.link.moveLink(this.time);
		return false;
	}

	// not needed
	public void pause() throws RemoteException {
	}
	public void play() throws RemoteException {
	}
	public void setStatus(int status) throws RemoteException {
	}
	public void step() throws RemoteException {
	}
	public Plan getAgentPlan(String id) throws RemoteException {
		return null;
	}
	public OTFQuery answerQuery(OTFQuery query) throws RemoteException {
		return null;
	}
}

public class CALinkOTFVis extends Thread { 
	
	public static class  MyControlBar extends OTFHostControlBar {
		JButton verbot;
		
		public MyControlBar(String address, Class res) throws RemoteException, InterruptedException, NotBoundException {
			super(address, res);
			this.DELAYSIM = 50;
			
			verbot = createButton("†-Verbot ist AUS", "vb", null, "toggle †berholverbot");
			verbot.putClientProperty("JButton.buttonType","text");
			verbot.setBorderPainted(true);
			verbot.setMargin(new Insets(10, 10, 10, 10));
			add(verbot);

		}

		@Override
		protected boolean onAction(String command) {
			if(command.equals("vb")) {
				CALink.ueberholverbot = !CALink.ueberholverbot;
				verbot.setText(CALink.ueberholverbot ? "†-Verbot ist AN" :"†-Verbot ist AUS");
			}
			return false; // return id command was handeled
		}


		/* (non-Javadoc)
		 * @see org.matsim.utils.vis.otfvis.gui.OTFHostControlBar#invalidateHandlers()
		 */
		@Override
		public void invalidateHandlers() {
			// TODO Auto-generated method stub
			super.invalidateHandlers();
			try {
				sleep(DELAYSIM);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


		@Override
		protected void openAddress(String address) throws RemoteException, InterruptedException, NotBoundException {
			this.host = new CALiveServer();
			if (host != null) liveHost = host.isLive();
		}

		private static final long serialVersionUID = 1L;
		
	}	
	
	
	@Override
	public void run() {
		if (Gbl.getConfig() == null) Gbl.createConfig(null);
		
		OTFVisConfig visconf = (OTFVisConfig) Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
		if (visconf == null) {
			visconf = new OTFVisConfig();
			Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, visconf);
		}
	
		((OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME)).setAgentSize(30.f);
		((OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME)).setLinkWidth(3.75f*CALink.LANECOUNT +5);
		
		MyControlBar hostControl;
		try {
			hostControl = new MyControlBar("", CALinkOTFVis.class);
			JFrame frame = new JFrame("MATSim OTFVis");
			
			boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
			if (isMac) {
				frame.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
			}

			hostControl.frame = frame;

			frame.getContentPane().add(hostControl, BorderLayout.NORTH);
			JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			pane.setContinuousLayout(true);
			pane.setOneTouchExpandable(true);
			frame.getContentPane().add(pane);
			PreferencesDialog.buildMenu(frame, visconf, hostControl);

			OTFConnectionManager connectR = new OTFConnectionManager();

			connectR.add(OTFLinkAgentsHandler.Writer.class,  OTFLinkAgentsHandler.class);
			connectR.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
			connectR.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
			connectR.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
			connectR.add(OGLAgentPointLayer.AgentPointDrawer.class, OGLAgentPointLayer.class);


			OTFClientQuad clientQ2 = hostControl.createNewView(null, null, connectR);

			OTFDrawer drawer2 = new OTFOGLDrawer(frame, clientQ2);
			pane.setRightComponent(drawer2.getComponent());
			hostControl.addHandler("test2", drawer2);
			drawer2.invalidate(0);

			hostControl.finishedInitialisition();
	        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setSize(screenSize.width/2,screenSize.height/2);
			frame.setVisible(true);

			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public CALinkOTFVis() {
		boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if (isMac) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
	}

	public static void main(String[] args) {
		CALinkOTFVis me = new CALinkOTFVis();
		me.run();
	}
	
}
