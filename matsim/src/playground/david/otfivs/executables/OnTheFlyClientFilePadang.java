package playground.david.otfivs.executables;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;

import javax.swing.JFrame;

import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.otfivs.data.OTFClientQuad;
import org.matsim.utils.vis.otfivs.data.OTFConnectionManager;
import org.matsim.utils.vis.otfivs.gui.OTFVisConfig;
import org.matsim.utils.vis.otfivs.handler.OTFAgentsListHandler;
import org.matsim.utils.vis.otfivs.handler.OTFDefaultNodeHandler;
import org.matsim.utils.vis.otfivs.handler.OTFLinkAgentsHandler;
import org.matsim.utils.vis.otfivs.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.utils.vis.otfivs.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfivs.opengl.OnTheFlyClientFileQuad;
import org.matsim.utils.vis.otfivs.opengl.OnTheFlyClientQuad;
import org.matsim.utils.vis.otfivs.opengl.drawer.OTFOGLDrawer;
import org.matsim.utils.vis.otfivs.opengl.drawer.SimpleBackgroundDrawer;
import org.matsim.utils.vis.otfivs.opengl.layer.OGLAgentPointLayer;
import org.matsim.utils.vis.otfivs.opengl.layer.OGLSimpleBackgroundLayer;
import org.matsim.utils.vis.otfivs.opengl.layer.SimpleStaticNetLayer;


public class OnTheFlyClientFilePadang extends OnTheFlyClientFileQuad{
	
	private static final String BG_IMG_ROOT = "../vsp-cvs/studies/padang/imagery/sliced/";
	
	public OnTheFlyClientFilePadang(String filename2, OTFConnectionManager connect, boolean split) {
		super(filename2, connect, split);
		// TODO Auto-generated constructor stub
	}

	protected SimpleBackgroundDrawer background = null;
	static OTFConnectionManager connect1 = new OTFConnectionManager();
	static OTFConnectionManager connect2 = new OTFConnectionManager();

	public static void main2(String[] args) {
		String netFileName = "../../tmp/studies/padang/padang_net.xml"; 
		String vehFileName = "../../tmp/studies/padang/run301.it100.colorized.T.veh.gz"; 

		if (Gbl.getConfig() == null) Gbl.createConfig(null);

		String localDtdBase = "../matsimJ/dtd/";
		Gbl.getConfig().global().setLocalDtdBase(localDtdBase);
		
		OnTheFlyClientQuad client = new OnTheFlyClientQuad("tveh:"+vehFileName + "@" + netFileName, connect1);
		client.run();
	}

	@Override
	public OTFDrawer getLeftDrawerComponent(JFrame frame) throws RemoteException {
		 ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setLinkWidth(2);
		OTFClientQuad clientQ = hostControl.createNewView(null, null, connect1);
		//clientQ.setCachingAllowed(false);
		
		OTFDrawer drawer = new OTFOGLDrawer(frame, clientQ);


		// DS TODO Repair painting of this colored net drawer.isActiveNet = true;
		return drawer;
	}

	private void loadSlicedBackgroundLayer(int ux, int uy, int num_x, int num_y, int size, String dir) {
		int x=ux;
		int y=uy;
		
		for (int xc = 0; xc < num_x; xc++) {
			y=uy;
			for (int yc = 0; yc < num_y; yc++) {
				String slice =  BG_IMG_ROOT + dir + "/slice_"+ xc + "_" + yc + ".png";
				System.out.println(slice);
				OGLSimpleBackgroundLayer.addPersistentItem(new SimpleBackgroundDrawer(slice, new Rectangle2D.Float(x,y,-size,-size)));				
				y-=size;
				
			}
			x-=size;
			
		}
		
		
	}
	
	
	@Override
	public OTFDrawer getRightDrawerComponent(JFrame frame) throws RemoteException {
		OTFClientQuad clientQ2 = hostControl.createNewView(null, null, connect1);
		//clientQ2.setCachingAllowed(false);

		OTFDrawer drawer2 = new OTFOGLDrawer(frame, clientQ2);
		loadSlicedBackgroundLayer(660000, 9915000, 4, 5, 5000, "low_res");
		loadSlicedBackgroundLayer(655000, 9900000, 3, 4, 2500, "high_res");
		
		return drawer2;
	}
	
	public static void main(String[] args) {
//		String filename = "../MatsimJ/output/OTFQuadfileSCHWEIZ2.3.mvi.gz";
//		String filename = "../MatsimJ/output/testSWI2.mvi.gz";
//		String filename = "test/padang.mvi";


//		String filename = "../OnTheFlyVis/test/padang.mvi"; //Flooding.mvi";
//		String filename = "../OnTheFlyVis/test/testPadabang1.3.mvi"; //Flooding.mvi";
//		String filename = "../vsp-cvs/runs/run306/output/ITERS/it.100/100.movie.mvi";
		String filename = "output/testPadabang1.3.mvi";

		connect1.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect1.add(SimpleBackgroundDrawer.class, OGLSimpleBackgroundLayer.class);
		connect1.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect1.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect1.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		connect1.add(OTFAgentsListHandler.class, OGLAgentPointLayer.AgentPadangTimeDrawer.class);
		connect1.add(OGLAgentPointLayer.AgentPadangTimeDrawer.class, OGLAgentPointLayer.class);
		connect1.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		
		
		connect2.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect2.add(SimpleBackgroundDrawer.class, OGLSimpleBackgroundLayer.class);
		connect2.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect2.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect2.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		connect2.add(OTFAgentsListHandler.class, OGLAgentPointLayer.AgentPadangRegionDrawer.class);
		connect2.add(OGLAgentPointLayer.AgentPadangTimeDrawer.class, OGLAgentPointLayer.class);
		connect2.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
//	
		//main2(args);
		
//		OTFOGLDrawer.linkWidth =2;
		
		Gbl.createConfig(null);
		OTFVisConfig conf = new OTFVisConfig();
		conf.setLinkWidth(10);
		Gbl.getConfig().addModule("otfvis", conf);
		
		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setLinkWidth(10); 
		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setNetworkColor(new Color(50,50,50));
		OnTheFlyClientFileQuad client = new OnTheFlyClientFilePadang(filename, null, false);
		
//		new OnTheFlyClientFilePadang()
		client.run();
		
	}

 
}
