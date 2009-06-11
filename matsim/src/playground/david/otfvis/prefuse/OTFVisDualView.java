package playground.david.otfvis.prefuse;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.OnTheFlyClientFileQuad;
import org.matsim.vis.otfvis.opengl.OnTheFlyClientQuad;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.drawer.SimpleBackgroundDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFFileSettingsSaver;
import org.matsim.vis.otfvis.opengl.layer.ColoredStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleBackgroundLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

import playground.david.otfvis.prefuse.TreeView.MTree;
import prefuse.Visualization;
import prefuse.controls.ControlAdapter;
import prefuse.data.Node;
import prefuse.data.Tree;
import prefuse.util.FontLib;
import prefuse.util.ui.JSearchPanel;
import prefuse.visual.VisualItem;



public class OTFVisDualView extends OnTheFlyClientFileQuad{
	
	private static final String BG_IMG_ROOT = "../vsp-cvs/studies/padang/imagery/sliced/";
	protected String url;
	protected PopDrawer popD;
	
	public OTFVisDualView(String filename2, OTFConnectionManager connect, boolean split) {
		super(filename2, connect, split);
		if(filename2.startsWith("file:")){
			this.filename = filename2.substring(5);
			this.url = filename2;
		} else {
			this.filename = "./";
		}

		this.url = filename2;
	}

	protected SimpleBackgroundDrawer background = null;
	static OTFConnectionManager connect1 = new OTFConnectionManager();
	static OTFConnectionManager connect2 = new OTFConnectionManager();

	public class PopDrawer extends TreeView {

		public PopDrawer(final Tree t) {
			super(t, "name");
	        // create a search panel for the tree map
	        JSearchPanel search = new JSearchPanel(this.getVisualization(),
	            treeNodes, Visualization.SEARCH_ITEMS, "name", true, true);
	        search.setShowResultCount(true);
	        search.setBorder(BorderFactory.createEmptyBorder(5,5,4,0));
	        search.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 11));
//	        search.setBackground(BACKGROUND);
//	        search.setForeground(FOREGROUND);

	        addControlListener(new ControlAdapter() {
	        	public void itemClicked(VisualItem item, MouseEvent e) { 
	                if(item.canGetInt("level")){
	                	String ids = item.getString("name");
	                	int level = item.getInt("level");
	                	if(level<=1){
	                		MTree tree = (MTree)t;
	                		int row = item.getRow();
	                		Node parent = tree.getNode(row);
	                		if(level > 0) {
	                        	int id = Integer.parseInt(ids);
	                    		int step = (int)Math.pow(10, level-1);
	                    		tree.generateLayer(parent, id, id+10*step, 0, -1);
	                    		invalidate();
	                		} else if(level < 0) {
	                			Object ref = parent.get("ref");
	                    		try {
									tree.recurseObject(ref, "", parent, 1);
								} catch (IllegalArgumentException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								} catch (IllegalAccessException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
	                    		invalidate();
	                		} else {
	                			// level == 0 --> this is an already expanded item
	                		}
	                	}
	                }
	        		} 
	        		 
	        		@Override 
	        		public void itemReleased(VisualItem item, MouseEvent e) { 
	        		// Your stuff to do when released. 
	        		} 
	        });

		}
	}

	@Override
	public OTFDrawer getRightDrawerComponent(JFrame frame) throws RemoteException {
		connect1.remove(OTFLinkAgentsHandler.class);

		connect1.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect1.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		connect1.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
		connect1.add(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
		connect1.add(OGLAgentPointLayer.AgentPointDrawer.class, OGLAgentPointLayer.class);
		this.connect1.add(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
		this.connect1.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connect1.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connect1.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		this.connect1.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		this.connect1.add(OTFAgentsListHandler.class,  OGLAgentPointLayer.AgentPointDrawer.class);
		this.connect1.add(AgentPointDrawer.class, OGLAgentPointLayer.class);
		this.connect1.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		this.connect1.add(QueueLink.class, OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		this.connect1.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect1.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect1.add(SimpleBackgroundDrawer.class, OGLSimpleBackgroundLayer.class);
		connect1.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		connect1.add(OTFLinkLanesAgentsNoParkingHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect1.add(OTFLinkLanesAgentsNoParkingHandler.class, OGLAgentPointLayer.AgentPointDrawer.class);
		connect1.add(OGLAgentPointLayer.AgentPointDrawer.class, OGLAgentPointLayer.class);
		connect1.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);

		OTFClientQuad clientQ = hostControl.createNewView(null, connect1);
		//clientQ.setCachingAllowed(false);
		
		OTFDrawer drawer = new OTFOGLDrawer(frame, clientQ);

		setLeftComponent(clientQ);
		// DS TODO Repair painting of this colored net drawer.isActiveNet = true;
		return drawer;
	}
	
	public void setLeftComponent(OTFClientQuad clientQ) {
		Gbl.printMemoryUsage();
		System.out.println("Creating tree");
		if(url.startsWith("file:")){
			popD = new PopDrawer(new MTree(new PopProviderFile(this.filename)));
		}
		else {
			
			popD = new PopDrawer(new MTree(new PopProviderServer(clientQ)));
		}
		pane.setLeftComponent(popD);
		QueryAgentPlanSyncView.popD = this.popD;
		
		Gbl.printMemoryUsage();
	}
	
	@Override
	protected OTFFileSettingsSaver getFileSaver() {
		// TODO Auto-generated method stub
		return super.getFileSaver();
	}

	@Override
	protected String getURL() {
		return url;
	}

 	public static void main(String[] args) {
//		String filename = "../MatsimJ/output/OTFQuadfileSCHWEIZ2.3.mvi.gz";
//		String filename = "../MatsimJ/output/testSWI2.mvi.gz";
//		String filename = "test/padang.mvi";
//		String filename = "../OnTheFlyVis/test/padang.mvi"; //Flooding.mvi";
//		String filename = "../OnTheFlyVis/test/testPadabang1.3.mvi"; //Flooding.mvi";
//		String filename = "../vsp-cvs/runs/run306/output/ITERS/it.100/100.movie.mvi";
		// ACHTUNG SPECIAL FILE with population inside
		String filename = "file:output/OTFQuadfile10p+pop.mvi";

		//main2(args);
		
//		OTFOGLDrawer.linkWidth =2;
		
		Gbl.createConfig(null);
		OTFVisConfig conf = new OTFVisConfig();
		conf.setLinkWidth(10);
		Gbl.getConfig().addModule("otfvis", conf);
		
//		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setLinkWidth(10); 
//		((OTFVisConfig)Gbl.getConfig().getModule("otfvis")).setNetworkColor(new Color(50,50,50));
		OnTheFlyClientFileQuad client = new OTFVisDualView(filename, null, false);
//		new OnTheFlyClientFilePadang()
		client.run();
		
	}


}
