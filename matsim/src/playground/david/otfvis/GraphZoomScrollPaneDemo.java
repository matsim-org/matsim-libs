/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 */
package playground.david.otfvis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.DefaultToolTipFunction;
import edu.uci.ics.jung.graph.decorators.EdgeShape;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.visualization.FRLayout;
import edu.uci.ics.jung.visualization.GraphMouseListener;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.ShapePickSupport;
import edu.uci.ics.jung.visualization.SpringLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.VisualizationViewer.GraphMouse;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;

/**
 * Demonstrates the use of <code>GraphZoomScrollPane</code>.
 * This class shows off the <code>VisualizationViewer</code> zooming
 * and panning capabilities, using horizontal and
 * vertical scrollbars.
 *
 * <p>This demo also shows ToolTips on graph vertices.</p>
 * 
 * @author Tom Nelson - RABA Technologies
 * 
 */
public class GraphZoomScrollPaneDemo {

    /**
     * the graph
     */
    Graph graph;

    /**
     * the visual component and renderer for the graph
     */
    VisualizationViewer vv;
    
    /**
     * create an instance of a simple graph with controls to
     * demo the zoom features.
     * 
     */
    public GraphZoomScrollPaneDemo() {
        
        // create a simple graph for the demo
        graph = new DirectedSparseGraph();
        Vertex[] v = createVertices(30);
        createEdges(v);
        
        ImageIcon sandstoneIcon = null;
        String imageLocation = "res/matsim_logo_transparent.png";
        try {
            	sandstoneIcon = 
            	    new ImageIcon(imageLocation);
        } catch(Exception ex) {
            System.err.println("Can't load \""+imageLocation+"\"");
        }
        final ImageIcon icon = sandstoneIcon;
        PluggableRenderer pr = new PluggableRenderer();

        vv =  new VisualizationViewer(new SpringLayout(graph), pr);
        vv.setPickSupport(new ShapePickSupport());
        pr.setEdgeShapeFunction(new EdgeShape.QuadCurve());
        
        if(icon != null) {
            vv.addPreRenderPaintable(new VisualizationViewer.Paintable(){
                public void paint(Graphics g) {
                    Dimension d = vv.getSize();
                    g.drawImage(icon.getImage(),0,0,d.width,d.height,vv);
                }
                public boolean useTransform() { return false; }
            });
        }
        vv.addPostRenderPaintable(new VisualizationViewer.Paintable(){
            int x;
            int y;
            Font font;
            FontMetrics metrics;
            int swidth;
            int sheight;
            String str = "GraphZoomScrollPane Demo";
            
            public void paint(Graphics g) {
                Dimension d = vv.getSize();
                if(font == null) {
                    font = new Font(g.getFont().getName(), Font.BOLD, 30);
                    metrics = g.getFontMetrics(font);
                    swidth = metrics.stringWidth(str);
                    sheight = metrics.getMaxAscent()+metrics.getMaxDescent();
                    x = (d.width-swidth)/2;
                    y = (int)(d.height-sheight*1.5);
                }
                g.setFont(font);
                Color oldColor = g.getColor();
                g.setColor(Color.lightGray);
                g.drawString(str, x, y);
                g.setColor(oldColor);
            }
            public boolean useTransform() {
                return false;
            }
        });

        vv.addGraphMouseListener(new TestGraphMouseListener());
        
        // add my listener for ToolTips
        vv.setToolTipFunction(new DefaultToolTipFunction());
        
        // create a frome to hold the graph
        final JFrame frame = new JFrame();
        Container content = frame.getContentPane();
        final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
        content.add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final GraphMouse graphMouse = new DefaultModalGraphMouse();
        vv.setGraphMouse(graphMouse);
        JMenuBar menu = new JMenuBar();
        menu.add(((DefaultModalGraphMouse)graphMouse).getModeMenu());
        panel.setCorner(menu);
        final ScalingControl scaler = new CrossoverScalingControl();

        JButton plus = new JButton("+");
        plus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1.1f, vv.getCenter());
            }
        });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1/1.1f, vv.getCenter());
            }
        });
        JButton reset = new JButton("reset");
        reset.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				vv.getLayoutTransformer().setToIdentity();
				vv.getViewTransformer().setToIdentity();
				
			}});

        JPanel controls = new JPanel();
        controls.add(plus);
        controls.add(minus);
        controls.add(reset);
        content.add(controls, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);
    }
    
    /**
     * create some vertices
     * @param count how many to create
     * @return the Vertices in an array
     */
    private Vertex[] createVertices(int count) {
        Vertex[] v = new Vertex[count];
        for (int i = 0; i < count; i++) {
            v[i] = graph.addVertex(new DirectedSparseVertex());
        }
        return v;
    }

    /**
     * create edges for this demo graph
     * @param v an array of Vertices to connect
     */
    void createEdges(Vertex[] v) {
        graph.addEdge(new DirectedSparseEdge(v[0], v[1]));
        graph.addEdge(new DirectedSparseEdge(v[0], v[3]));
        graph.addEdge(new DirectedSparseEdge(v[0], v[4]));
        graph.addEdge(new DirectedSparseEdge(v[4], v[5]));
        graph.addEdge(new DirectedSparseEdge(v[3], v[5]));
        graph.addEdge(new DirectedSparseEdge(v[1], v[2]));
        graph.addEdge(new DirectedSparseEdge(v[1], v[4]));
        graph.addEdge(new DirectedSparseEdge(v[8], v[2]));
        graph.addEdge(new DirectedSparseEdge(v[3], v[8]));
        graph.addEdge(new DirectedSparseEdge(v[6], v[7]));
        graph.addEdge(new DirectedSparseEdge(v[7], v[5]));
        graph.addEdge(new DirectedSparseEdge(v[0], v[9]));
        graph.addEdge(new DirectedSparseEdge(v[9], v[8]));
        graph.addEdge(new DirectedSparseEdge(v[7], v[6]));
        graph.addEdge(new DirectedSparseEdge(v[6], v[5]));
        graph.addEdge(new DirectedSparseEdge(v[4], v[2]));
        graph.addEdge(new DirectedSparseEdge(v[5], v[4]));
    }

    /**
     * A nested class to demo the GraphMouseListener finding the
     * right vertices after zoom/pan
     */
    static class TestGraphMouseListener implements GraphMouseListener {
        
    		public void graphClicked(Vertex v, MouseEvent me) {
    		    System.err.println("Vertex "+v+" was clicked at ("+me.getX()+","+me.getY()+")");
    		}
    		public void graphPressed(Vertex v, MouseEvent me) {
    		    System.err.println("Vertex "+v+" was pressed at ("+me.getX()+","+me.getY()+")");
    		}
    		public void graphReleased(Vertex v, MouseEvent me) {
    		    System.err.println("Vertex "+v+" was released at ("+me.getX()+","+me.getY()+")");
    		}
    }

    /**
     * a driver for this demo
     */
    public static void main(String[] args) 
    {
        new GraphZoomScrollPaneDemo();
    }
}
