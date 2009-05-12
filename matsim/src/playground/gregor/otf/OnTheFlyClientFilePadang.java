package playground.gregor.otf;

import java.awt.Color;
import java.rmi.RemoteException;

import javax.swing.JFrame;

import org.geotools.data.FeatureSource;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.OnTheFlyClientFileQuad;
import org.matsim.vis.otfvis.opengl.OnTheFlyClientQuad;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.drawer.SimpleBackgroundDrawer;
import org.matsim.vis.otfvis.opengl.drawer.SimpleBackgroundFeatureDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleBackgroundLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;



public class OnTheFlyClientFilePadang extends OnTheFlyClientFileQuad{
	
	public static final String CVSROOT = "../../../workspace/vsp-cvs";
	private static final String BG_IMG_ROOT = CVSROOT + "/studies/padang/imagery/sliced/";
//	final String BUILDINGS_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_buildings.shp";
//	final String LINKS_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_links.shp";
//	final String NODES_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_nodes.shp";
	final String BUILDINGS_FILE =  "../../inputs/gis/shelters.shp";
	final String LINKS_FILE = "../../inputs/gis/links.shp";
	final String NODES_FILE = "../../inputs/gis/nodes.shp";
	final private static float [] buildingsColor = new float [] {.0f,0.f,1.0f,.8f};
	final private static float [] linksColor = new float [] {.5f,.5f,.5f,.7f};
	final private static float [] nodesColor = new float [] {.4f,.4f,.4f,.7f};
	
	public OnTheFlyClientFilePadang(final String filename2, final OTFConnectionManager connect, final boolean split) {
		super(filename2, connect, split);
		// TODO Auto-generated constructor stub
	}

	protected SimpleBackgroundDrawer background = null;
	static OTFConnectionManager connect1 = new OTFConnectionManager();
	static OTFConnectionManager connect2 = new OTFConnectionManager();

	public static void main2(final String[] args) {
		final String netFileName = "../../tmp/studies/padang/padang_net.xml"; 
		final String vehFileName = "../../tmp/studies/padang/run301.it100.colorized.T.veh.gz"; 

		if (Gbl.getConfig() == null) Gbl.createConfig(null);

		final String localDtdBase = "../matsimJ/dtd/";
		Gbl.getConfig().global().setLocalDtdBase(localDtdBase);
		
		final OnTheFlyClientQuad client = new OnTheFlyClientQuad("tveh:"+vehFileName + "@" + netFileName, connect1);
		client.run();
	}

	@Override
	public OTFDrawer getLeftDrawerComponent(final JFrame frame) throws RemoteException {
		 ((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setLinkWidth(2);
		final OTFClientQuad clientQ = this.hostControl.createNewView(null, connect2);

		final OTFDrawer drawer = new OTFOGLDrawer(frame, clientQ);


		// DS TODO Repair painting of this colored net drawer.isActiveNet = true;
		return drawer;
	}

	
	
	private static void loadTileDrawer() {
		OGLSimpleBackgroundLayer.addPersistentItem(new TileDrawer());
		
	}
	
	private void loadFeatureLayer(final String shapeFile, final float [] color) throws Exception {
		final FeatureSource fs = ShapeFileReader.readDataFile(shapeFile);
		OGLSimpleBackgroundLayer.addPersistentItem(new SimpleBackgroundFeatureDrawer(fs,color));

		
		
	}

	
	
	
	
	@Override
	public OTFDrawer getRightDrawerComponent(final JFrame frame) throws RemoteException {
		final OTFClientQuad clientQ2 = this.hostControl.createNewView(null, connect1);
		
		final OTFOGLDrawer drawer2 = new OTFOGLDrawer(frame, clientQ2);
//		drawer2.createTexture(filename)
//		OTFGLOverlay overlay = new OTFGLOverlay();
//		drawer2.addOverlay(overlay)
//		loadSlicedBackgroundLayer(660000, 9915000, 4, 5, 5000, "low_res");
//		loadSlicedBackgroundLayer(655000, 9900000, 3, 4, 2500, "high_res");
//		loadTilesFromServer();
		
		
//		try {
//			loadFeatureLayer(this.BUILDINGS_FILE,buildingsColor);
//			loadFeatureLayer(this.NODES_FILE,linksColor);
//			loadFeatureLayer(this.LINKS_FILE,nodesColor);
//		} catch (final Exception e) {
//			e.printStackTrace();
//		}
//		FloodingReader fr = new FloodingReader("../../inputs/networks/flooding.sww");
		
//		OTFInundationDrawer x = new OTFInundationDrawer(fr,clientQ2.offsetEast,clientQ2.offsetNorth);
//		Dummy.myDrawer = x;
//		OGLSimpleBackgroundLayer.addPersistentItem(x);
		
//		clientQ2.addAdditionalElement(new InundationDataReader(x));
//		OGLSimpleBackgroundLayer.addPersistentItem(new ScalelableBackgroundDraw("test.png","scalebar2.png","scalebar3.png"));
		
//		InundationDataFromNetcdfReader n = new InundationDataFromNetcdfReader(clientQ2.offsetNorth,clientQ2.offsetEast);
//		InundationDataFromBinaryFileReader n = new InundationDataFromBinaryFileReader();
//		OTFInundationDrawer x = new OTFInundationDrawer(drawer2);
//		x.setData(n.readData());
		
//		OGLSimpleBackgroundLayer.addPersistentItem(x);
//		n = null;
		drawer2.addOverlay(new OTFScaleBarDrawer("./res/sb_background.png","./res/scalebar.png"));
		return drawer2;
	}
	





	public static void main(final String[] args) {
		String filename;
		if (args.length == 1) {
			filename = args[0];
		} else {
 //		String filename = "../MatsimJ/output/OTFQuadfileSCHWEIZ2.3.mvi.gz";
//		String filename = "../MatsimJ/output/testSWI2.mvi.gz";
//		String filename = "test/padang.mvi";

		
//		String filename = "../OnTheFlyVis/test/padang.mvi"; //Flooding.mvi";
//		String filename = "../OnTheFlyVis/test/testPadabang1.4.mvi"; //Flooding.mvi";
//		final String filename =  CVSROOT + "/runs/run314/output/ITERS/it.100/100.movie.mvi";
//		final String filename =  CVSROOT + "/runs/run313/output/ITERS/it.201/201.movie.mvi";
		filename =  "../../outputs/output/ITERS/it.0/0.movie.mvi";
//		filename =  "../../outputs/output_shelter_noWave/ITERS/it.200/200.movie.mvi";
//			filename =  "../../outputs/output/ITERS/it.0/0.movie.mvi";
//		final String filename =  "/home/laemmel/arbeit/svn/runs-svn/run316/stage2/output/ITERS/it.201/201.movie.mvi";
		}
		
//		String filename = "./jam/jam.mvi";
		

//		connect1.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
//		connect1.add(SimpleBackgroundDrawer.class, OGLSimpleBackgroundLayer.class);
////		connect1.add(SimpleBackgroundDrawer.class, OTFScaleBarDrawer.class);
//		connect1.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
//		connect1.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.NoQuadDrawer.class);
//		connect1.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
//		connect1.add(OTFAgentsListHandler.class, OGLAgentPointLayer.AgentPadangTimeDrawer.class);
//		connect1.add(OGLAgentPointLayer.AgentPadangTimeDrawer.class, OGLAgentPointLayer.class);
//		connect1.add(SimpleStaticNetLayer.NoQuadDrawer.class, SimpleStaticNetLayer.class);
//		connect1.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
//		connect1.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
//		connect1.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.NoQuadDrawer.class);
//		connect1.add(InundationDataWriter.class,InundationDataReader.class);
//		connect1.add(InundationDataReader.class,Dummy.class);
		//connect1.add(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
		
		loadTileDrawer();
		connect2.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect2.add(SimpleBackgroundDrawer.class, OGLSimpleBackgroundLayer.class);
		connect2.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect2.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.NoQuadDrawer.class);
		connect2.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		connect2.add(OTFAgentsListHandler.class, OGLAgentPointLayer.AgentPadangRegionDrawer.class);
		connect2.add(OGLAgentPointLayer.AgentPadangRegionDrawer.class, OGLAgentPointLayer.class);		
		connect2.add(SimpleStaticNetLayer.NoQuadDrawer.class, SimpleStaticNetLayer.class);
		connect2.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		connect2.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect2.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.NoQuadDrawer.class);
		//connect2.add(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
//	
		//main2(args);
		
//		OTFOGLDrawer.linkWidth =2;
		
//		Gbl.createConfig(null);
//		final OTFVisConfig conf = new OTFVisConfig();
////		conf.setLinkWidth(10);
//		Gbl.getConfig().addModule("otfvis", conf);
//		
//		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setLinkWidth(0); 
//		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setNetworkColor(new Color(50,50,50,255));
//		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setAgentSize(100.f);
//		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setDrawTime(true);
//		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setDrawOverlays(true);
		
		
		final OnTheFlyClientFileQuad client = new OnTheFlyClientFilePadang(filename, null, false);
		
//		new OnTheFlyClientFilePadang()
		client.run();
		
	}

 
}
