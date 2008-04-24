package playground.david.vis.executables;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import org.matsim.gbl.Gbl;

import playground.david.vis.data.OTFClientQuad;
import playground.david.vis.data.OTFConnectionManager;
import playground.david.vis.data.OTFDefaultNetWriterFactoryImpl;
import playground.david.vis.gui.NetJComponent;
import playground.david.vis.gui.OTFHostControlBar;
import playground.david.vis.gui.OTFVisConfig;
import playground.david.vis.gui.PreferencesDialog;
import playground.david.vis.handler.OTFDefaultNodeHandler;
import playground.david.vis.handler.OTFLinkAgentsHandler;
import playground.david.vis.handler.OTFLinkAgentsNoParkingHandler;
import playground.david.vis.interfaces.OTFDrawer;


public class OnTheFlyClientQuadSwing{

	static OTFConnectionManager connect2 = new OTFConnectionManager();

	
	public static void main(String[] args) {
		String arg0 = "file:../OnTheFlyVis/test/OTFQuadfileNoParking10p_wip.mvi";
		OTFDefaultNetWriterFactoryImpl factory = null;

		if (args != null && args.length == 1) {
			arg0 = args[0];
			factory = new OTFDefaultNetWriterFactoryImpl();
		}
		
		connect2.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect2.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect2.add(OTFLinkAgentsHandler.class,  NetJComponent.SimpleQuadDrawer.class);
		connect2.add(OTFLinkAgentsHandler.class,  NetJComponent.AgentDrawer.class);
	
		//main2(args);
		
		OTFVisConfig visconf = new OTFVisConfig();
		if (Gbl.getConfig() == null) Gbl.createConfig(null);
		Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, visconf);


//		hostControl = new OTFHostControlBar("file:../MatsimJ/output/OTFQuadfile10p.mvi.gz");
		OTFHostControlBar hostControl;
		try {
			hostControl = new OTFHostControlBar(arg0, OnTheFlyClientQuadSwing.class);
			JFrame frame = new JFrame("MATSIM NetVis");

			frame.getContentPane().add(hostControl, BorderLayout.NORTH);
			JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			pane.setContinuousLayout(true);
			pane.setOneTouchExpandable(true);
			frame.getContentPane().add(pane);
			PreferencesDialog.buildMenu(frame, visconf, hostControl);


			OTFClientQuad clientQ2 = hostControl.createNewView(null, factory, connect2);
			OTFDrawer drawer2 = new NetJComponent(frame, clientQ2);
			pane.setRightComponent(drawer2.getComponent());
			hostControl.addHandler("test2", drawer2);
			drawer2.invalidate(0);

			System.out.println("Finished init");
			pane.setDividerLocation(0.5);
	        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setSize(screenSize.width/2,screenSize.height/2);
			frame.setVisible(true);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

 
}
