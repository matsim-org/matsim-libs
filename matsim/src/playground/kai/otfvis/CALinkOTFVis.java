package playground.kai.otfvis;

import java.awt.BorderLayout;
import java.awt.Color; 
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


class MyCAVeh {
	int speed ;
	int maxSpeed ;
	double speedOffset = 0. ;
	boolean truck=false ;


	public boolean isTruck() {
		return truck;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public MyCAVeh ( int maxSpeed ) {
		this.maxSpeed = maxSpeed ; 
	}

	public void setTruck(boolean truck) {
		this.truck = truck;
	}

	public double getSpeedOffset() {
		return speedOffset;
	}

	public void setSpeedOffset(double speedOffset) {
		this.speedOffset = speedOffset;
	}

}

class CALink extends QueueLink {
	public static boolean ueberholverbot = false;

	public static int LINKLEN = 1000;
	public static int LANECOUNT = 2;

	int VMAX=5 ;

	MyCAVeh[][] cells = new MyCAVeh[LANECOUNT][LINKLEN] ; 

	public CALink(Link l, QueueNetworkLayer queueNetworkLayer, QueueNode toNode) {
		super(l, queueNetworkLayer, toNode);
		
		double rnd = Gbl.random.nextDouble() ;
		
		for ( int lane=0 ; lane<LANECOUNT; lane++ ) {
			MyCAVeh veh = new MyCAVeh(3) ;
			veh.setTruck(true) ;
			if ( lane==0 ) { veh.setSpeedOffset(0.1) ; }
			cells[lane][0] = veh ;
			for ( int len=1 ; len<LINKLEN ; len++ ) {
				if ( Gbl.random.nextDouble() < 0.05 ) {
					if ( Gbl.random.nextDouble() < 0.1 ) {
						veh = new MyCAVeh(3) ;
						veh.setTruck(true) ;
						cells[lane][len] = veh ;
					} else {
						if ( Gbl.random.nextDouble() < 0.5 ) {
							cells[lane][len] = new MyCAVeh(5) ;
						} else {
							cells[lane][len] = new MyCAVeh(5) ;
						}
					}
				} else {
					cells[lane][len] = null ;
				}
			}
		}
	}

	@Override
	public Collection<PositionInfo> getVehiclePositions(Collection<PositionInfo> positions) {
		for ( int lane=0 ; lane<LANECOUNT; lane++ ) {
			for ( int len=0 ; len<LINKLEN ; len++ ) {
				if ( cells[lane][len] != null ) {
					MyCAVeh veh = cells[lane][len] ;
					double speed = 4.*veh.getSpeed() ;
					if ( veh.isTruck() ) {
						positions.add( new PositionInfo(new IdImpl("1"),this.getLink(), len, 5*lane+2, 500., VehicleState.Driving, ""));
					} else {
						positions.add( new PositionInfo(new IdImpl("0"),this.getLink(), len, 5*lane+2, speed, VehicleState.Driving, ""));
					}
				}
			}
		}
		return positions;
	}

	int getGap ( int lane, int len, int speed ) {
		int gap = 0 ;
		while ( gap < speed ) {
			if ( cells[lane][(len+gap+1)%LINKLEN] != null ) {
				return gap ;
			}
			gap++ ;
		}
		return gap ;
	}
	
	int getVel ( int lane, int pos, int look ) {
		int gap = 0 ;
		while ( gap <= 16 ) {
			MyCAVeh veh = cells[lane][(pos+gap+1)%LINKLEN] ;
			if ( veh != null ) {
				return veh.getSpeed() ;
			}
			gap++ ;
		}
		return 99 ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.mobsim.QueueLink#moveLink(double)
	 */
	@Override
	protected boolean moveLink(double now) {
		// #############################
		// lane changing:
		
		final int MODE = 2 ;
		if ( LANECOUNT >= 2 ) {
			int mainLane = 0 ;
			int otherLane = 1 ;
			if ( now%2==1 ) {
				mainLane = 1 ;
				otherLane = 0 ;
			}

			for ( int len=0 ; len<LINKLEN ; len++ ) {
				if ( cells[mainLane][len] != null ) {
					MyCAVeh veh = cells[mainLane][len] ;

					// incentive:
					int otherGap = getGap( otherLane, len, veh.getSpeed()+1 ) ;
					if ( MODE==0 ) { // symmetric
//						int thisGap = getGap( mainLane, len, veh.getSpeed()+1 ) ;
//							if ( thisGap >= otherGap ) {
//							continue ; // next cell
//						}
//					} else if ( MODE==1 ) {
//						int otherGap = getGap( otherLane, len, veh.getSpeed()+1 ) ;
//						int thisGap = getGap( mainLane, len, veh.getSpeed()+1 ) ;
//						if ( mainLane==1 ) { // from right to left
//							if ( otherGap <= thisGap ) {
//								continue ;
//							}
//						} else { // from left to right
//							if ( otherGap < veh.getSpeed()+1 && thisGap > otherGap ) {
//								continue ;
//							}
//						}
					} else if ( MODE==2 ) {
						int thisVel = getVel( mainLane, len, 2*veh.getSpeed()+1 ) ;
						int otherVel = getVel( otherLane, len, 2*veh.getSpeed()+1 ) ; 
						if ( mainLane==1 ) { // from right to left
							if ( veh.getSpeed()+1 < thisVel && veh.getSpeed()+1 < otherVel ) {
								continue ; // stay on right
							} else if ( veh.isTruck() && ueberholverbot ) {
								continue ; // stay on right
							} 
						} else { // from left to right
							if ( !( veh.isTruck() && ueberholverbot ) ) {
								if ( veh.getSpeed()+1 >= thisVel || veh.getSpeed()+1 >= otherVel ) {
									continue ; // stay on left
								}
							}
						}
					} else {
						System.err.println( " MODE not defined. Abort " ) ;
						System.exit(-1) ;
					}
					// safety
					if ( otherGap < veh.getSpeed() ) {
						continue ; // stay where you are
					}
					
					int backGap = 0 ;
					while ( backGap < VMAX ) {
						if ( cells[otherLane][(len-backGap+LINKLEN)%LINKLEN] != null ) {
							break ;
						}
						backGap++ ;
					}
					if ( backGap < VMAX ) {
						continue ;
					}

					cells[otherLane][len] = veh ;
					cells[mainLane][len] = null ;

				}
			}
		}

		// #############################
		// #############################
		// speed:

		for ( int lane=0 ; lane<LANECOUNT; lane++ ) {
			for ( int len=0 ; len<LINKLEN ; len++ ) {
				if ( cells[lane][len] != null ) {
					MyCAVeh veh = cells[lane][len] ;
					int origSpeed = veh.getSpeed() ;
					int speed = origSpeed+1 ;
					int gap = getGap( lane, len, speed ) ;
					if ( veh.isTruck() && ueberholverbot && lane==0 ) {
						if ( speed > veh.getMaxSpeed()-1 ) {
							speed = veh.getMaxSpeed()-1 ;
						}
					} else if ( speed > veh.getMaxSpeed() ) { 
						speed = veh.getMaxSpeed() ; 
					}
					if ( speed > gap ) { speed = gap ; }

					if ( origSpeed==0 ) {
						if ( speed >= 1 && Gbl.random.nextDouble() < 0.01 ) { speed -- ; }
					} else {
						if ( speed >= 1 && Gbl.random.nextDouble() < 0.05+0.05*lane-veh.getSpeedOffset() ) { speed -- ; }
					}
					veh.setSpeed(speed) ;
				}
			}
		}		
		
		// #############################
		// #############################
		// forward movement:

		for ( int lane=0 ; lane<LANECOUNT; lane++ ) {
			for ( int len=0 ; len<LINKLEN ; len++ ) {
				if ( cells[lane][len] != null ) {
					MyCAVeh veh = cells[lane][len] ;
					if ( veh.getSpeed() > 0 ) {
						cells[lane][(len+veh.getSpeed())%LINKLEN] = veh ;
						cells[lane][len] = null ;
						len+=veh.getSpeed() ;
					}
				}
			}
		}
		
		// #############################
//		// #############################
//		// tty:
//
//		for ( int lane=0 ; lane<LANECOUNT; lane++ ) {
//			StringBuilder str = new StringBuilder() ;
//			for ( int len=0 ; len<LINKLEN ; len++ ) {
//				if ( cells[lane][len] != null ) {
//					MyCAVeh veh = cells[lane][len] ;
//					if ( veh.isTruck() ) {
//						str.append('X') ;
//					} else {
//						str.append(veh.getSpeed()) ;
//					}
//				} else {
//					str.append('.') ;
//				}
//			}
//			System.out.println ( str ) ;
//		}
//		System.out.println() ;

		return true;
	}

};

// (end of class)
//###########################################
//###########################################

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
		return true;
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
			this.DELAYSIM = 20;

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

	private static OTFOGLDrawer.FastColorizer colorizer = new OTFOGLDrawer.FastColorizer(
			new double[] { 0.0, 30., 50., 500.}, new Color[] {
					Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE});

	@Override
	public void run() {
		if (Gbl.getConfig() == null) Gbl.createConfig(null);

		OTFVisConfig visconf = (OTFVisConfig) Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
		if (visconf == null) {
			visconf = new OTFVisConfig();
			Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, visconf);
		}

		((OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME)).setAgentSize(10.f);
		((OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME)).setLinkWidth(17.5f*CALink.LANECOUNT +5);
//		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setNetworkColor(new Color(0xeeeeee));
		OGLAgentPointLayer.AgentArrayDrawer.setAlpha(255);
		OGLAgentPointLayer.AgentArrayDrawer.setColorizer(colorizer);

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
			clientQ2.setCachingAllowed(false);

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
