package playground.gregor.otf;

import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.Iterator;

import javax.swing.JFrame;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.gbl.Gbl;
import org.matsim.utils.gis.ShapeFileReader;
import org.matsim.utils.vis.otfvis.data.OTFClientQuad;
import org.matsim.utils.vis.otfvis.data.OTFConnectionManager;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.utils.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.utils.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.utils.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.opengl.OnTheFlyClientFileQuad;
import org.matsim.utils.vis.otfvis.opengl.OnTheFlyClientQuad;
import org.matsim.utils.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.utils.vis.otfvis.opengl.drawer.SimpleBackgroundDrawer;
import org.matsim.utils.vis.otfvis.opengl.drawer.SimpleBackgroundFeatureDrawer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLSimpleBackgroundLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.SimpleStaticNetLayer;



public class OnTheFlyClientFilePadang extends OnTheFlyClientFileQuad{
	
	private static final String CVSROOT = "../vsp-cvs";
	private static final String BG_IMG_ROOT = CVSROOT + "/studies/padang/imagery/sliced/";
	final String BUILDINGS_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_buildings.shp";
	final String LINKS_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_links.shp";
	final String NODES_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_nodes.shp";
	final private static float [] buildingsColor = new float [] {.1f,.1f,.1f,.99f};
	final private static float [] linksColor = new float [] {.1f,.1f,.1f,7.f};
	final private static float [] nodesColor = new float [] {.0f,.0f,.0f,7.f};
	
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
		final OTFClientQuad clientQ = this.hostControl.createNewView(null, null, connect2);

		final OTFDrawer drawer = new OTFOGLDrawer(frame, clientQ);


		// DS TODO Repair painting of this colored net drawer.isActiveNet = true;
		return drawer;
	}

	private void loadSlicedBackgroundLayer(final int ux, final int uy, final int num_x, final int num_y, final int size, final String dir) {
		int x=ux;
		int y=uy;
		
		for (int xc = 0; xc < num_x; xc++) {
			y=uy;
			for (int yc = 0; yc < num_y; yc++) {
				final String slice =  BG_IMG_ROOT + dir + "/slice_"+ xc + "_" + yc + ".png";
				System.out.println(slice);
				OGLSimpleBackgroundLayer.addPersistentItem(new SimpleBackgroundDrawer(slice, new Rectangle2D.Float(x,y,-size,-size)));				
				y-=size;
				
			}
			x-=size;
			
		}
		
		
	}
	private void loadFeatureLayer(final String shapeFile, final float [] color) throws Exception {
		final FeatureSource fs = ShapeFileReader.readDataFile(shapeFile);

		final Iterator<Feature> it = fs.getFeatures().iterator();
		while (it.hasNext()){
			final Feature ft = it.next();
			OGLSimpleBackgroundLayer.addPersistentItem(new SimpleBackgroundFeatureDrawer(ft,color));
		}
		
		
	}

	
	
	@Override
	public OTFDrawer getRightDrawerComponent(final JFrame frame) throws RemoteException {
		final OTFClientQuad clientQ2 = this.hostControl.createNewView(null, null, connect1);

		final OTFOGLDrawer drawer2 = new OTFOGLDrawer(frame, clientQ2);
//		drawer2.createTexture(filename)
//		OTFGLOverlay overlay = new OTFGLOverlay();
//		drawer2.addOverlay(overlay)
//		loadSlicedBackgroundLayer(660000, 9915000, 4, 5, 5000, "low_res");
//		loadSlicedBackgroundLayer(655000, 9900000, 3, 4, 2500, "high_res");
		try {
			loadFeatureLayer(this.BUILDINGS_FILE,buildingsColor);
			loadFeatureLayer(this.NODES_FILE,linksColor);
			loadFeatureLayer(this.LINKS_FILE,nodesColor);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return drawer2;
	}
	

	public static void main(final String[] args) {
//		String filename = "../MatsimJ/output/OTFQuadfileSCHWEIZ2.3.mvi.gz";
//		String filename = "../MatsimJ/output/testSWI2.mvi.gz";
//		String filename = "test/padang.mvi";

		
//		String filename = "../OnTheFlyVis/test/padang.mvi"; //Flooding.mvi";
//		String filename = "../OnTheFlyVis/test/testPadabang1.4.mvi"; //Flooding.mvi";
//		final String filename =  CVSROOT + "/runs/run314/output/ITERS/it.200/200.movie.mvi";
//		final String filename =  CVSROOT + "/runs/run313/output/ITERS/it.201/201.movie.mvi";
		final String filename =  "../outputs/output/ITERS/it.80/80.movie.mvi";
		
//		String filename = "./jam/jam.mvi";
		

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
		connect2.add(OGLAgentPointLayer.AgentPadangRegionDrawer.class, OGLAgentPointLayer.class);		
		connect2.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
//	
		//main2(args);
		
//		OTFOGLDrawer.linkWidth =2;
		
		Gbl.createConfig(null);
		final OTFVisConfig conf = new OTFVisConfig();
//		conf.setLinkWidth(10);
		Gbl.getConfig().addModule("otfvis", conf);
		
//		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setLinkWidth(0); 
//		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setNetworkColor(new Color(50,50,50,255));
		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setAgentSize(200.f);
		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setDrawTime(true);
		
		final OnTheFlyClientFileQuad client = new OnTheFlyClientFilePadang(filename, null, false);
		
//		new OnTheFlyClientFilePadang()
		client.run();
		
	}

 
}
