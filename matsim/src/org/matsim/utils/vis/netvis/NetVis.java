/* *********************************************************************** *
 * project: org.matsim.*
 * NetVis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.utils.vis.netvis;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.FileFilter;

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.vis.netvis.config.GeneralConfig;
import org.matsim.utils.vis.netvis.gui.ControlToolbar;
import org.matsim.utils.vis.netvis.gui.NetJComponent;
import org.matsim.utils.vis.netvis.gui.NetVisScrollPane;
import org.matsim.utils.vis.netvis.renderers.BackgroundRenderer;
import org.matsim.utils.vis.netvis.renderers.LabelRenderer;
import org.matsim.utils.vis.netvis.renderers.LinkSetRenderer;
import org.matsim.utils.vis.netvis.renderers.LinkSetRendererCOOPERSVehiclesOnly;
import org.matsim.utils.vis.netvis.renderers.LinkSetRendererLanes;
import org.matsim.utils.vis.netvis.renderers.LinkSetRendererRoutes;
import org.matsim.utils.vis.netvis.renderers.LinkSetRendererStuck;
import org.matsim.utils.vis.netvis.renderers.LinkSetRendererTRANSIMS;
import org.matsim.utils.vis.netvis.renderers.LinkSetRendererVolumes;
import org.matsim.utils.vis.netvis.renderers.NodeSetRenderer;
import org.matsim.utils.vis.netvis.renderers.RendererA;
import org.matsim.utils.vis.netvis.streaming.StreamConfig;
import org.matsim.utils.vis.netvis.visNet.DisplayNet;
import org.matsim.utils.vis.netvis.visNet.DisplayNetStateReader;


public class NetVis {

    // -------------------- CLASS VARIABLES --------------------

    public static final String FILE_SUFFIX = "vis";

    // -------------------- INSTANCE VARIABLES --------------------

    /*package*/ JFrame vizFrame;

    /*package*/ NetVisScrollPane networkScrollPane;

    private NetJComponent networkComponent;

    /*package*/ ControlToolbar buttonComponent;

    // -------------------- CONSTRUCTION --------------------

    protected NetVis(GeneralConfig generalConfig, VisConfig visConfig,
            String filePrefix) {

        // 1. create network

        System.out.println("loading network file");

        NetworkLayer networkLayer = new NetworkLayer();
        new MatsimNetworkReader(networkLayer).readFile(generalConfig.getNetFileName());

        System.out.println("composing network");

        DisplayNet network = new DisplayNet(networkLayer);

        System.out.println("starting visualizer");

        // 2. create renderers and (if required) a reader
        RendererA linkSetRenderer = null;
    	String rendererName = generalConfig.get("LinkSetRenderer");

    	if (rendererName == null) linkSetRenderer = new LinkSetRenderer(visConfig, network);
    	else if (rendererName.equals("LinkSetRendererTRANSIMS")) linkSetRenderer = new LinkSetRendererTRANSIMS(visConfig, network);
    	else if (rendererName.equals("LinkSetRendererVolumes")) linkSetRenderer = new LinkSetRendererVolumes(visConfig, network);
    	else if (rendererName.equals("LinkSetRendererLanes")) linkSetRenderer = new LinkSetRendererLanes(visConfig, network);
    	else if (rendererName.equals("LinkSetRendererStuck")) linkSetRenderer = new LinkSetRendererStuck(visConfig, network);
    	else if (rendererName.equals("LinkSetRendererRoutes")) linkSetRenderer = new LinkSetRendererRoutes(visConfig, network);
    	else if (rendererName.equals("LinkSetRendererCOOPERSVehiclesOnly")) linkSetRenderer = new LinkSetRendererCOOPERSVehiclesOnly(visConfig, network);
    	else linkSetRenderer = new LinkSetRenderer(visConfig, network);

       RendererA backgroundRenderer = new BackgroundRenderer(visConfig);
        NodeSetRenderer nodeSetRenderer = new NodeSetRenderer(visConfig,network);

        final RendererA mainRenderer = new LabelRenderer(visConfig, network);
        mainRenderer.append(linkSetRenderer);
        linkSetRenderer.append(nodeSetRenderer);
        nodeSetRenderer.append(backgroundRenderer);

       DisplayNetStateReader reader = openNetVisReader(network, filePrefix);

        // ----- 1. create frame -----

        vizFrame = new JFrame(filePrefix != null ? filePrefix : "");
        vizFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JFrame.setDefaultLookAndFeelDecorated(true);

        // ----- 2. create control bar -----

        buttonComponent = new ControlToolbar(this, network, reader, visConfig);
        vizFrame.getContentPane().add(buttonComponent, BorderLayout.NORTH);

        if (rendererName != null) {
        	if (rendererName.equals("LinkSetRendererVolumes")) ((LinkSetRendererVolumes)linkSetRenderer).setControlToolbar(buttonComponent);
        	else if (rendererName.equals("LinkSetRendererLanes")) ((LinkSetRendererLanes)linkSetRenderer).setControlToolbar(buttonComponent);
        	else if (rendererName.equals("LinkSetRendererStuck")) ((LinkSetRendererStuck)linkSetRenderer).setControlToolbar(buttonComponent);
        	else if (rendererName.equals("LinkSetRendererRoutes")) ((LinkSetRendererRoutes)linkSetRenderer).setControlToolbar(buttonComponent);
        	else if (rendererName.equals("LinkSetRendererCOOPERSVehiclesOnly")) ((LinkSetRendererCOOPERSVehiclesOnly)linkSetRenderer).setControlToolbar(buttonComponent);
        }

        // 4. ----- create component serving as drawing area -----

        networkComponent = new NetJComponent(network, mainRenderer, visConfig);
        mainRenderer.setTargetComponent(networkComponent);
        networkScrollPane = new NetVisScrollPane(networkComponent);
        // networkScrollPane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        vizFrame.getContentPane().add(networkScrollPane, BorderLayout.CENTER);
        VisMouseHandler handi = new VisMouseHandler();
        networkScrollPane.addMouseMotionListener(handi);
        networkScrollPane.addMouseListener(handi);
        networkScrollPane.getViewport().addChangeListener(handi);
    }

    protected NetVis(String generalConfigFile, String visConfigFile,
            String filePrefix) {
        // 0. create vis config singleton
        this(new GeneralConfig(generalConfigFile),
                new VisConfig(visConfigFile), filePrefix);
    }

    // -------------------- IMPLEMENTATION -------------------------
    protected DisplayNetStateReader openNetVisReader(DisplayNet network, String filePrefix) {
        DisplayNetStateReader reader = null;

        if (filePrefix != null)
       	 try {
       		 reader = new DisplayNetStateReader(network, filePrefix);
       		 reader.open();
       		 reader.toStart();
       	 } catch (Exception e) {
       		 System.err.println("There is a problem with the movie files: " + e);
       		 System.err.println("I'm not sure the movie will display correctly.");
       		 e.printStackTrace();
       		 // reader = null;
       	 }
       	 return reader;
    }



    public void paintNow() {
        if (vizFrame != null)
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    vizFrame.paint(vizFrame.getGraphics());
                }
            });
    }

    public void run() {
        vizFrame.pack();
        vizFrame.setVisible(true);
    }

    // -------------------- MAIN - FUNCTION --------------------

    protected static void tellParameters() {
        System.out
                .println("NetVis has three possible parameter configurations:");
        System.out.println("view only network:");
        System.out.println("\t<general config file>");
        System.out.println("\t<vis config file>");
        System.out.println("view movies:");
        System.out.println("\t<general config file>");
        System.out.println("\t<vis config file>");
        System.out.println("\t<prefix of movie files>");
        System.out.println("view movies (shortcut, all configs in one file):");
        System.out.println("\t<prefix of movie files>");
    }

    private static void chooseFileName() {
    	String userDir = System.getProperty("user.dir");
		JFileChooser fileDialog = new JFileChooser(userDir);
		String filename = "";
    	fileDialog.addChoosableFileFilter(
    		new FileFilter() {
    	    @Override
					public boolean accept(File f) {
    	      if (f.isDirectory()) return true;
    	      return f.getName().endsWith("CONFIG.vis");
    	    }
    	    @Override
					public String getDescription () { return "CONFIG.vis"; }
    	  });

    	  fileDialog.setMultiSelectionEnabled(false);

		int ret = fileDialog.showOpenDialog(null);
		while (ret != JFileChooser.CANCEL_OPTION) {
			filename = fileDialog.getSelectedFile().getAbsolutePath();
			if (filename.endsWith("CONFIG.vis")) {
				String nameauxsuffix = filename.substring(0, filename.length() - 10);
				(new NetVis(filename, filename, nameauxsuffix)).run();
				return;
			} else if (filename.endsWith(".xml")) {
				GeneralConfig gconf = new GeneralConfig(
						true,
						filename);
				(new NetVis(gconf, VisConfig.newDefaultConfig(), null)).run();
				return;
			} else {
				JOptionPane.showMessageDialog(null,
						"Das scheint kein CONFIG.vis oder XML-Netzwerk zu sein!\n Bitte erneut wählen!");
				ret = fileDialog.showOpenDialog(null);
			}
		}
    }

    public static void main(String[] args) {
    	Gbl.createConfig(null);
    	if (args.length == 1) {
   			String configFile = StreamConfig.getConfigFileName(args[0], FILE_SUFFIX);
   			(new NetVis(configFile, configFile, args[0])).run();
    	} else if (args.length == 2)
    		(new NetVis(args[0], args[1], null)).run();
    	else if (args.length == 3)
    		(new NetVis(args[0], args[1], args[2])).run();
    	else if (args.length == 0) {
    		chooseFileName();
    	} else {
    		tellParameters();
    	}
    }

    public static void start(String visFileName) {
 			String configFile = StreamConfig.getConfigFileName(visFileName, FILE_SUFFIX);
 			(new NetVis(configFile, configFile, visFileName)).run();
    }

    class VisMouseHandler extends MouseInputAdapter implements ChangeListener {
        public Point start = null;

        public Rectangle currentRect = null;

        public int button = 0;

        @Override
				public void mousePressed(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            button = e.getButton();
            start = new Point(x, y);
            // networkComponent.repaint();
        }

        @Override
				public void mouseDragged(MouseEvent e) {
            if (button == 1)
                updateSize(e);
            else if (button == 2) {
                int deltax = start.x - e.getX();
                int deltay = start.y - e.getY();
                start.x = e.getX();
                start.y = e.getY();
                networkScrollPane.moveNetwork(deltax, deltay);
            }
        }

        @Override
				public void mouseReleased(MouseEvent e) {
            if (button == 1) {
                updateSize(e);
                if ((currentRect.getHeight() > 10)
                   && (currentRect.getWidth() > 10)) {
                	float scale =  buttonComponent.getScale();
                	scale = networkScrollPane.scaleNetwork(currentRect,scale);
                	buttonComponent.setScale(scale);
                }
                currentRect = null;
            }
            button = 0;
        }

        void updateSize(MouseEvent e) {
            currentRect = new Rectangle(start);
            currentRect.add(e.getX(), e.getY());
            networkScrollPane.getGraphics().drawRect(currentRect.x,
                    currentRect.y, currentRect.width, currentRect.height);
            networkScrollPane.repaint();
        }

        public void stateChanged(ChangeEvent e) {
            networkScrollPane.updateViewClipRect();
        }
    }

	public void scaleNetwork(float scale) {
		networkScrollPane.scaleNetwork(scale);

	}
}
