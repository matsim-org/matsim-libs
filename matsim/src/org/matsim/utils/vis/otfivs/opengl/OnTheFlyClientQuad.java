package org.matsim.utils.vis.otfivs.opengl;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.vis.otfivs.data.OTFClientQuad;
import org.matsim.utils.vis.otfivs.data.OTFConnectionManager;
import org.matsim.utils.vis.otfivs.data.OTFDefaultNetWriterFactoryImpl;
import org.matsim.utils.vis.otfivs.data.OTFServerQuad;
import org.matsim.utils.vis.otfivs.data.OTFWriterFactory;
import org.matsim.utils.vis.otfivs.gui.OTFHostControlBar;
import org.matsim.utils.vis.otfivs.gui.OTFQueryControlBar;
import org.matsim.utils.vis.otfivs.gui.OTFVisConfig;
import org.matsim.utils.vis.otfivs.gui.PreferencesDialog;
import org.matsim.utils.vis.otfivs.handler.OTFAgentsListHandler;
import org.matsim.utils.vis.otfivs.handler.OTFDefaultLinkHandler;
import org.matsim.utils.vis.otfivs.handler.OTFDefaultNodeHandler;
import org.matsim.utils.vis.otfivs.handler.OTFLinkAgentsHandler;
import org.matsim.utils.vis.otfivs.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.utils.vis.otfivs.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfivs.interfaces.OTFQueryHandler;
import org.matsim.utils.vis.otfivs.opengl.drawer.OTFOGLDrawer;
import org.matsim.utils.vis.otfivs.opengl.layer.OGLAgentPointLayer;
import org.matsim.utils.vis.otfivs.opengl.layer.SimpleStaticNetLayer;
import org.matsim.utils.vis.otfivs.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;


public class OnTheFlyClientQuad extends Thread {
	String url;
	OTFConnectionManager connect = new OTFConnectionManager();
	final boolean isMac;
	
	public OnTheFlyClientQuad(String url) {
		this.url = url;
		
		isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if (isMac) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}

		connect.add(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
		connect.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
//		connect.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(QueueLink.class, OTFLinkAgentsHandler.Writer.class);
		connect.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect.add(OTFLinkAgentsHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		connect.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
		connect.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		connect.add(AgentPointDrawer.class, OGLAgentPointLayer.class);
		connect.add(OTFAgentsListHandler.class,  AgentPointDrawer.class);
	}

	public OnTheFlyClientQuad(String filename2, OTFConnectionManager connect) {
		this(filename2);
		this.connect = connect;
	}
	
	NetworkLayer network = new NetworkLayer();


	@Override
	public void run() {
		String id1 = "test1";
		String id2 = "test2";
		OTFServerQuad servQ;


		// Maybe later: connect.add(QueueLink.class, OTFDefaultLinkHandler.Writer.class);
		// connect.add(QueueNode.class, OTFDefaultNodeHandler.Writer.class);


		OTFVisConfig visconf = new OTFVisConfig();
		if (Gbl.getConfig() == null) Gbl.createConfig(null);
		Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, visconf);
		

		JFrame frame = new JFrame("MATSIM NetVis");
		if (isMac) {
			frame.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
		}
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JFrame.setDefaultLookAndFeelDecorated(true);
		//frame.setLayout( new GridLayout(0,2,10,10) ); 
		//frame.setLayout( new FlowLayout() ); 
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		pane.setContinuousLayout(true);
		pane.setOneTouchExpandable(true);
		frame.add(pane);

		try {
			OTFHostControlBar hostControl = new OTFHostControlBar(url);
			hostControl.frame = frame;
			frame.getContentPane().add(hostControl, BorderLayout.NORTH);
			PreferencesDialog.buildMenu(frame, visconf, hostControl);

			OTFDefaultNetWriterFactoryImpl factory = new OTFDefaultNetWriterFactoryImpl();
			if (connect.getEntries(QueueLink.class).isEmpty())	factory.setLinkWriterFac(new OTFLinkAgentsNoParkingHandler.Writer());
			else {
				Class linkhandler = connect.getEntries(QueueLink.class).iterator().next();
				factory.setLinkWriterFac((OTFWriterFactory<QueueLink>)linkhandler.newInstance());
			}
			OTFClientQuad clientQ = hostControl.createNewView(id1, factory, connect);

//			factory.setLinkWriterFac(new OTFDefaultLinkHandler.Writer());
//			servQ = host.getQuad(id2, new OTFDefaultNetWriterFactoryImpl());
//			OTFClientQuad clientQ2 = servQ.convertToClient(id2, connect);
//			clientQ2.createReceiver(connect); 
//			clientQ2.getConstData(host);


			OTFDrawer drawer = new OTFOGLDrawer(frame, clientQ);
			//OTFOGLDrawer drawer2 = new OTFOGLDrawer(frame, host, clientQ2);

			pane.setLeftComponent(drawer.getComponent());
			//pane.setRightComponent(drawer2.getCanvas());
			if(hostControl.isLiveHost()) frame.getContentPane().add(new OTFQueryControlBar("test", (OTFQueryHandler)drawer), BorderLayout.SOUTH);

			frame.setSize(1024, 600);
			drawer.invalidate(0);
			frame.setVisible(true);
			//drawer2.invalidate();
			hostControl.addHandler(id1, drawer);
			//hostControl.addHandler(id2, drawer2);

		} catch (Exception e) {
			// TODO DS Handle with grace!
			e.printStackTrace();
		}
		//host.play();

//		do 
//	{
//	host.step();
//clientQ.getDynData(host);
//		clientQ2.getDynData(host);
//		drawer.invalidate(null);
//		drawer2.invalidate(null);
//		Thread.sleep(20);

//		}while(true);					

//		visnet = host.getNet(getHandler());
//		visnet.connect();
//		System.out.println("get net time");
//		Gbl.printElapsedTime();
//		//DisplayNet network = prepareNet();
//		for (int i=0; i<5;i++){
//		host.step();
//		//Plan plan = host.getAgentPlan("66128");
//		//System.out.println("Plan:" + plan.toString());
//		Gbl.startMeasurement();
//		byte [] bbyte = host.getStateBuffer();
//		System.out.println("get state time");
//		Gbl.printElapsedTime();

//		Gbl.startMeasurement();
//		visnet.readMyself(new DataInputStream(new ByteArrayInputStream(bbyte,0,bbyte.length)));
//		System.out.println("SET state time");
//		Gbl.printElapsedTime();
//		}
//		startVis(host);

		//Thread.sleep(5000);
		//host.play();

	}

	public static void main(String[] args) {
		OnTheFlyClientQuad client = new OnTheFlyClientQuad("rmi:127.0.0.1:4019");
		client.start();
	}

//	private static DisplayableNetI prepareNet() {
//	String fileName = "E:\\Development\\tmp\\studies\\berlin-wip\\network\\wip_net.xml";
//	NetReaderI netReader = new MatsimNetReader_PLAIN();
//	NetBuffer buffer = new NetBuffer();
//	netReader.readNetwork(buffer, fileName);

//	DisplayNet network = new DisplayNet();
//	NetComposerI netComposer = new TrafficNetComposer();
//	netComposer.compose(network, buffer);
//	return network;

//	}
}
