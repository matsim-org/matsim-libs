package playground.david.otfvis.prefuse;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import org.matsim.api.core.v01.population.Person;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.ItemAction;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.LocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.filter.FisheyeTreeFilter;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.controls.ControlAdapter;
import prefuse.controls.FocusControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Node;
import prefuse.data.Tree;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.search.PrefixSearchTupleSet;
import prefuse.data.search.SearchTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.ui.JFastLabel;
import prefuse.util.ui.JSearchPanel;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;


/**
 * Demonstration of a node-link tree viewer
 *
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TreeView extends Display {

public static class MTree extends Tree {
	int max = 1001234;
	SortedSet<Integer> idSet;
	private SearchTupleSet m_searchTupleSet;
	private PopulationProvider m_pop = null;
	
	public int RECURESEDEPTH = 5;
	public static Visualization m_vis = null;
	private boolean fillRef = false;
	
	public PopulationProvider getPop() {
		return m_pop;
	}
	
	public Node createObjectNode(String name, String value, Node parent, Object ob, int layer) {
		Node child = this.addChild(parent);
		child.setString("name", name);
		child.setInt("level", layer);
		child.set("ref", ob);
		return child;
	}

	public void recurseObject(Object ob, String prelude, Node parent, int depth) throws IllegalArgumentException, IllegalAccessException {
		Class c = ob.getClass();
		if(depth == 0)return;
		parent.setInt("level", 0);
		
		while (c != null)
		{
		    Field[] fields = c.getDeclaredFields();
		    for (Field field: fields)
		    {
		        Class type = field.getType();
		        
		        if(type.getName().indexOf("log4j") != -1) continue;
		        if(Modifier.isStatic(field.getModifiers())) continue;
		        if(field.getName().indexOf("serialVersionUID")!= -1) continue;
		        
		        if (!Modifier.isPublic(field.getModifiers())) {
		        	field.setAccessible(true);
	        	}
		        if (Map.class.isAssignableFrom(type))
		        {
        			System.out.println(prelude + "MAP: " + field.getName() );
		            Map collection = (Map)field.get(ob);
		            if(collection != null) {
            			Node col = createObjectNode(field.getName() + "[" + collection.size() + "]", null, parent, collection, -depth);
		            	for(Object o : collection.keySet()) {
		            		if(o != null) {
		            			System.out.println(prelude + "Key: " + o.toString() );
		            			type = o.getClass();
		            			Node it = createObjectNode(o.toString()+ ": " + type.getSimpleName() , null, col, o, -depth);
		            			recurseObject(collection.get(o), prelude + "--", it,  depth-1);
		            		}
		            	}
		            }
		        }else 
			        if (Collection.class.isAssignableFrom(type))
			        {
			            Collection collection = (Collection)field.get(ob);
			            if(collection != null){
	            			Node col = createObjectNode(field.getName() + "[" + collection.size() + "]", null, parent, collection, -depth);
			            	for(Object o : collection) {
			            		if(o != null){
			            			type = o.getClass();
			            			Node it = createObjectNode(type.getSimpleName(), null, col, o, -depth);
			            			recurseObject(o, prelude + "--", it, depth-1);
			            		}
			            	}
			            }
			            
		        } else 
		        {
			        Object value = field.get(ob); 
			        if(value != null) {
            			Node it = createObjectNode(field.getName(), null, parent, value, -depth);
				        if(type.isPrimitive() || type == String.class){
				        	it.setInt("level",0);
	            			createObjectNode(value.toString(), null, it, value, 0);
					        System.out.println(prelude + field.getName() + " " + type.toString() + " val: " + value.toString());
				        } else {
					        System.out.println(prelude + field.getName() + "--> " + type.toString());
				        	recurseObject(value, prelude + "--", it, depth-1);
				        }
			        }
		        }
		    }
		    c = c.getSuperclass();
		}		
	}

	public MTree(PopulationProvider pop) {
		//this.max = max;
		this.m_pop = pop;
		
		idSet = m_pop.getIdSet();
		max = idSet.last();
			

		this.addColumn("name", String.class);
		this.addColumn("level", int.class);
		this.addColumn("ref", Object.class);
		Node root = this.addRoot();
		root.setString("name", Integer.toString(this.max));
		generateLayer(this.getRoot(), 0, this.max, (int)Math.log10(this.max)-1, -1);
		fillRef = true;

	}

	public boolean hasPersons(int start, int end) {
		SortedSet<Integer> part = idSet.subSet(start, end);
		if(part.size() > 0)return true;
		return false;
	}
	public boolean generateLayer(Node parent, int start, int end, int level, int minLevel) {
		if (level == minLevel) return false;
		if(!hasPersons(start, end)){
			// this complete subtree does not contain persons. do not create
			parent.setInt("level", 0);
			return true;
		}

		if(level == 0) {
			parent.setInt("level", 0); // is expanded
			for (int i = start; i<end; i++) {
				if(idSet.contains(i)) {
					Node child = this.addNode();
					//Node child = this.addChild(parent);
					child.setString("name", Integer.toString(i));
					child.setInt("level", -1);
					if(fillRef) {
						Person p = m_pop.getPerson(i);
						child.set("ref", p);
					}
					this.addChildEdge(parent, child);
					if(m_vis != null)m_searchTupleSet.index(m_vis.getVisualItem("tree.nodes", child), "name"); 
					//this.fireTupleEvent(child, prefuse.data.event.EventConstants.INSERT);
				}
			}
			return true;
		}

		int step = (int)Math.pow(10, level);
		for (int i = start; (i < end) && (i<max); i+=step) {
			if(!hasPersons(i, Math.min(i+step, end)))continue;
			
			Node child = this.addChild(parent);
			child.setString("name", Integer.toString(i));
			child.setInt("level", level);
			child.getRow();
			this.fireTupleEvent(child, prefuse.data.event.EventConstants.INSERT);
			generateLayer(child, i, Math.min(i+step, end), level-1, minLevel );
		}
		return true;
	}

	public void generateLayer(Node parent, int id, int i, int j, int k, VisualItem item) {
		m_vis = item.getVisualization();
		m_searchTupleSet = (SearchTupleSet)m_vis.getGroup(Visualization.SEARCH_ITEMS);

		generateLayer(parent, id, i, j, k);
		m_vis = null;
	}
	
}

public boolean focusOnId(String id){
	boolean res = true;
    String query = id;
    search.search(query);
    if ( search.getQuery().length() == 0 )
        return false;
    else {
        int r = search.getTupleCount();
        if(r == 1) {
        	int i=0;
        	i++;
        }
    }
	return res;
}

    public static final String TREE_CHI = "data/chi-ontology.xml.gz";
    
    private static final String tree = "tree";
    protected static final String treeNodes = "tree.nodes";
    private static final String treeEdges = "tree.edges";
    
    private LabelRenderer m_nodeRenderer;
    private EdgeRenderer m_edgeRenderer;
    
    private String m_label = "label";
    private int m_orientation = Constants.ORIENT_LEFT_RIGHT;

	private PrefixSearchTupleSet search;
    
    public TreeView(Tree t, String label) {
        super(new Visualization());
        m_label = label;

        m_vis.add(tree, t);
        
        m_nodeRenderer = new LabelRenderer(m_label);
        m_nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
        m_nodeRenderer.setHorizontalAlignment(Constants.LEFT);
        m_nodeRenderer.setRoundedCorner(8,8);
        m_edgeRenderer = new EdgeRenderer(Constants.EDGE_TYPE_CURVE);
        
        DefaultRendererFactory rf = new DefaultRendererFactory(m_nodeRenderer);
        rf.add(new InGroupPredicate(treeEdges), m_edgeRenderer);
        m_vis.setRendererFactory(rf);
               
        // colors
        ItemAction nodeColor = new NodeColorAction(treeNodes);
        ItemAction textColor = new ColorAction(treeNodes,
                VisualItem.TEXTCOLOR, ColorLib.rgb(0,0,0));
        m_vis.putAction("textColor", textColor);
        
        ItemAction edgeColor = new ColorAction(treeEdges,
                VisualItem.STROKECOLOR, ColorLib.rgb(200,200,200));
        
        // quick repaint
        ActionList repaint = new ActionList();
        repaint.add(nodeColor);
        repaint.add(new RepaintAction());
        m_vis.putAction("repaint", repaint);
        
        // full paint
        ActionList fullPaint = new ActionList();
        fullPaint.add(nodeColor);
        m_vis.putAction("fullPaint", fullPaint);
        
        // animate paint change
        ActionList animatePaint = new ActionList(400);
        animatePaint.add(new ColorAnimator(treeNodes));
        animatePaint.add(new RepaintAction());
        m_vis.putAction("animatePaint", animatePaint);
        
        // create the tree layout action
        NodeLinkTreeLayout treeLayout = new NodeLinkTreeLayout(tree,
                m_orientation, 50, 0, 8);
        treeLayout.setLayoutAnchor(new Point2D.Double(25,300));
        m_vis.putAction("treeLayout", treeLayout);
        
        CollapsedSubtreeLayout subLayout = 
            new CollapsedSubtreeLayout(tree, m_orientation);
        m_vis.putAction("subLayout", subLayout);
        
        AutoPanAction autoPan = new AutoPanAction();
        
        // create the filtering and layout
        ActionList filter = new ActionList();
        filter.add(new FisheyeTreeFilter(tree, 2));
        filter.add(new FontAction(treeNodes, FontLib.getFont("Tahoma", 16)));
        filter.add(treeLayout);
        filter.add(subLayout);
        filter.add(textColor);
        filter.add(nodeColor);
        filter.add(edgeColor);
        m_vis.putAction("filter", filter);
        
        // animated transition
        ActionList animate = new ActionList(1000);
        animate.setPacingFunction(new SlowInSlowOutPacer());
        animate.add(autoPan);
        animate.add(new QualityControlAnimator());
        animate.add(new VisibilityAnimator(tree));
        animate.add(new LocationAnimator(treeNodes));
        animate.add(new ColorAnimator(treeNodes));
        animate.add(new RepaintAction());
        m_vis.putAction("animate", animate);
        m_vis.alwaysRunAfter("filter", "animate");
        
        // create animator for orientation changes
        ActionList orient = new ActionList(2000);
        orient.setPacingFunction(new SlowInSlowOutPacer());
        orient.add(autoPan);
        orient.add(new QualityControlAnimator());
        orient.add(new LocationAnimator(treeNodes));
        orient.add(new RepaintAction());
        m_vis.putAction("orient", orient);
        
        // ------------------------------------------------
        
        // initialize the display
        setSize(700,600);
        setItemSorter(new TreeDepthItemSorter());
        addControlListener(new ZoomToFitControl());
        addControlListener(new ZoomControl());
        addControlListener(new WheelZoomControl());
        addControlListener(new PanControl());
        addControlListener(new FocusControl(1, "filter"));
        
        registerKeyboardAction(
            new OrientAction(Constants.ORIENT_LEFT_RIGHT),
            "left-to-right", KeyStroke.getKeyStroke("ctrl 1"), WHEN_FOCUSED);
        registerKeyboardAction(
            new OrientAction(Constants.ORIENT_TOP_BOTTOM),
            "top-to-bottom", KeyStroke.getKeyStroke("ctrl 2"), WHEN_FOCUSED);
        registerKeyboardAction(
            new OrientAction(Constants.ORIENT_RIGHT_LEFT),
            "right-to-left", KeyStroke.getKeyStroke("ctrl 3"), WHEN_FOCUSED);
        registerKeyboardAction(
            new OrientAction(Constants.ORIENT_BOTTOM_TOP),
            "bottom-to-top", KeyStroke.getKeyStroke("ctrl 4"), WHEN_FOCUSED);
        
        // ------------------------------------------------
        
        // filter graph and perform layout
        setOrientation(m_orientation);
        m_vis.run("filter");
        
        search = new PrefixSearchTupleSet(); 
        m_vis.addFocusGroup(Visualization.SEARCH_ITEMS, search);
        search.addTupleSetListener(new TupleSetListener() {
            public void tupleSetChanged(TupleSet ts, Tuple[] add, Tuple[] rem) {
                m_vis.cancel("animatePaint");
                m_vis.run("fullPaint");
                m_vis.run("animatePaint");
                if(ts.getTupleCount() == 1){
                	Tuple one = (Tuple)ts.tuples().next();
                	one.getRow();
                    TupleSet tsF = m_vis.getFocusGroup("_focus_");
                    if(tsF.getTupleCount() == 1 ) {
                    	Tuple two = (Tuple)tsF.tuples().next();
                    	if(two.equals(one))return;
                    }
                    tsF.setTuple(one);


                    m_vis.runAfter("animate","orient");
                    m_vis.run("filter");
                }
            }
        });
    }
    
    // ------------------------------------------------------------------------
    
    public void setOrientation(int orientation) {
        NodeLinkTreeLayout rtl 
            = (NodeLinkTreeLayout)m_vis.getAction("treeLayout");
        CollapsedSubtreeLayout stl
            = (CollapsedSubtreeLayout)m_vis.getAction("subLayout");
        switch ( orientation ) {
        case Constants.ORIENT_LEFT_RIGHT:
            m_nodeRenderer.setHorizontalAlignment(Constants.LEFT);
            m_edgeRenderer.setHorizontalAlignment1(Constants.RIGHT);
            m_edgeRenderer.setHorizontalAlignment2(Constants.LEFT);
            m_edgeRenderer.setVerticalAlignment1(Constants.CENTER);
            m_edgeRenderer.setVerticalAlignment2(Constants.CENTER);
            break;
        case Constants.ORIENT_RIGHT_LEFT:
            m_nodeRenderer.setHorizontalAlignment(Constants.RIGHT);
            m_edgeRenderer.setHorizontalAlignment1(Constants.LEFT);
            m_edgeRenderer.setHorizontalAlignment2(Constants.RIGHT);
            m_edgeRenderer.setVerticalAlignment1(Constants.CENTER);
            m_edgeRenderer.setVerticalAlignment2(Constants.CENTER);
            break;
        case Constants.ORIENT_TOP_BOTTOM:
            m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
            m_edgeRenderer.setHorizontalAlignment1(Constants.CENTER);
            m_edgeRenderer.setHorizontalAlignment2(Constants.CENTER);
            m_edgeRenderer.setVerticalAlignment1(Constants.BOTTOM);
            m_edgeRenderer.setVerticalAlignment2(Constants.TOP);
            break;
        case Constants.ORIENT_BOTTOM_TOP:
            m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
            m_edgeRenderer.setHorizontalAlignment1(Constants.CENTER);
            m_edgeRenderer.setHorizontalAlignment2(Constants.CENTER);
            m_edgeRenderer.setVerticalAlignment1(Constants.TOP);
            m_edgeRenderer.setVerticalAlignment2(Constants.BOTTOM);
            break;
        default:
            throw new IllegalArgumentException(
                "Unrecognized orientation value: "+orientation);
        }
        m_orientation = orientation;
        rtl.setOrientation(orientation);
        stl.setOrientation(orientation);
    }
    
    public int getOrientation() {
        return m_orientation;
    }
    
    // ------------------------------------------------------------------------
    
    public static void main(String argv[]) {
        String infile = TREE_CHI;
        String label = "name";
        if ( argv.length > 1 ) {
            infile = argv[0];
            label = argv[1];
        }
        JComponent treeview = demo(infile, label);
        
        JFrame frame = new JFrame("p r e f u s e  |  t r e e v i e w");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(treeview);
        frame.pack();
        frame.setVisible(true);
    }
    
    public static JComponent demo() {
        return demo(TREE_CHI, "name");
    }
    
    public static JComponent demo(String datafile, final String label) {
        Color BACKGROUND = Color.WHITE;
        Color FOREGROUND = Color.BLACK;
        
        Tree t = null;
//        try {
//            t = (Tree)new TreeMLReader().readGraph(datafile);
//        } catch ( Exception e ) {
//            e.printStackTrace();
//            System.exit(1);
//        }
        final MTree t2 = new MTree(new PopProviderFile("../MatsimJ/net+population.zip"));
        // create a new treemap
        final TreeView tview = new TreeView(t2, label);
        tview.setBackground(BACKGROUND);
        tview.setForeground(FOREGROUND);
        
        // create a search panel for the tree map
        JSearchPanel search = new JSearchPanel(tview.getVisualization(),
            treeNodes, Visualization.SEARCH_ITEMS, label, true, true);
        search.setShowResultCount(true);
        search.setBorder(BorderFactory.createEmptyBorder(5,5,4,0));
        search.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 11));
        search.setBackground(BACKGROUND);
        search.setForeground(FOREGROUND);
        
        final JFastLabel title = new JFastLabel("                 ");
        title.setPreferredSize(new Dimension(350, 20));
        title.setVerticalAlignment(SwingConstants.BOTTOM);
        title.setBorder(BorderFactory.createEmptyBorder(3,0,0,0));
        title.setFont(FontLib.getFont("Tahoma", Font.PLAIN, 16));
        title.setBackground(BACKGROUND);
        title.setForeground(FOREGROUND);
   
       
        tview.addControlListener(new ControlAdapter() {
            @Override
						public void itemEntered(VisualItem item, MouseEvent e) {
                if ( item.canGetString(label) ) {
                	String ids = item.getString(label);
                    title.setText(ids);
                }
            }
            
            @Override
						public void itemExited(VisualItem item, MouseEvent e) {
                title.setText(null);
            }
        });
        tview.addControlListener(new ControlAdapter() {
        	@Override
					public void itemClicked(VisualItem item, MouseEvent e) { 
                if(item.canGetInt("level")){
                	String ids = item.getString(label);
                	int level = item.getInt("level");
                	if(level<=1){
                		MTree tree = (MTree)t2;
                		int row = item.getRow();
                		Node parent = tree.getNode(row);
                		if(level > 0) {
//                        	int id = Integer.parseInt(ids);
//                    		int step = (int)Math.pow(10, level-1);
//                    		tree.generateLayer(parent, id, id+10*step, 1, 0, item);
//                    		tview.invalidate();
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
                    		tview.invalidate();
                		} else {
                			// level == 0 --> this is an already expanded item if ref != 0;
                			Object ref = parent.get("ref");
                        	int id = Integer.parseInt(ids);
                    		if(ref == null){
                    			tree.generateLayer(parent, id, id, 1, -1, item);
                    		}
                		}
                	}
                }
        		} 
        		 
        		@Override 
        		public void itemReleased(VisualItem item, MouseEvent e) { 
        		// Your stuff to do when released. 
        		} 
        });
        
        Box box = new Box(BoxLayout.X_AXIS);
        box.add(Box.createHorizontalStrut(10));
        box.add(title);
        box.add(Box.createHorizontalGlue());
        box.add(search);
        box.add(Box.createHorizontalStrut(3));
        box.setBackground(BACKGROUND);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        panel.setForeground(FOREGROUND);
        panel.add(tview, BorderLayout.CENTER);
        panel.add(box, BorderLayout.SOUTH);
        return panel;
    }
    
    // ------------------------------------------------------------------------
   
    public class OrientAction extends AbstractAction {
        private int orientation;
        
        public OrientAction(int orientation) {
            this.orientation = orientation;
        }
        public void actionPerformed(ActionEvent evt) {
            setOrientation(orientation);
            getVisualization().cancel("orient");
            getVisualization().run("treeLayout");
            getVisualization().run("orient");
        }
    }
    
    public class AutoPanAction extends Action {
        private Point2D m_start = new Point2D.Double();
        private Point2D m_end   = new Point2D.Double();
        private Point2D m_cur   = new Point2D.Double();
        private int     m_bias  = 150;
        
        @Override
				public void run(double frac) {
            TupleSet ts = m_vis.getFocusGroup(Visualization.FOCUS_ITEMS);
            if ( ts.getTupleCount() == 0 )
                return;
            
            if ( frac == 0.0 ) {
                int xbias=0, ybias=0;
                switch ( m_orientation ) {
                case Constants.ORIENT_LEFT_RIGHT:
                    xbias = m_bias;
                    break;
                case Constants.ORIENT_RIGHT_LEFT:
                    xbias = -m_bias;
                    break;
                case Constants.ORIENT_TOP_BOTTOM:
                    ybias = m_bias;
                    break;
                case Constants.ORIENT_BOTTOM_TOP:
                    ybias = -m_bias;
                    break;
                }

                VisualItem vi = (VisualItem)ts.tuples().next();
                m_cur.setLocation(getWidth()/2, getHeight()/2);
                getAbsoluteCoordinate(m_cur, m_start);
                m_end.setLocation(vi.getX()+xbias, vi.getY()+ybias);
            } else {
                m_cur.setLocation(m_start.getX() + frac*(m_end.getX()-m_start.getX()),
                                  m_start.getY() + frac*(m_end.getY()-m_start.getY()));
                panToAbs(m_cur);
            }
        }
    }
    
    public static class NodeColorAction extends ColorAction {
        
        public NodeColorAction(String group) {
            super(group, VisualItem.FILLCOLOR);
        }
        
        @Override
				public int getColor(VisualItem item) {
            if ( m_vis.isInGroup(item, Visualization.SEARCH_ITEMS) )
                return ColorLib.rgb(255,190,190);
            else if ( m_vis.isInGroup(item, Visualization.FOCUS_ITEMS) )
                return ColorLib.rgb(198,229,229);
            else if ( item.getDOI() > -1 )
                return ColorLib.rgb(164,193,193);
            else if ( item.canGetInt("level") ) 
            {
            	int level = item.getInt("level")!= 0 ? 1: 0;
                return ColorLib.rgb(150,100 + level*50 ,193);
            }
            else
                return ColorLib.rgba(255,255,255,0);
        }
        
    } // end of inner class TreeMapColorAction
    
    
} // end of class TreeMap
