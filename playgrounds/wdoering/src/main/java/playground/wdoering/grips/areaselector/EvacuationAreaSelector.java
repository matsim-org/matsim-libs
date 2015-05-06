/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wdoering.grips.areaselector;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.matsim.contrib.evacuation.io.GripsConfigReader;
import org.matsim.contrib.evacuation.jxmapviewerhelper.TileFactoryBuilder;
import org.matsim.contrib.evacuation.model.config.GripsConfigModule;

public class EvacuationAreaSelector implements ActionListener{

	private static final Logger log = Logger.getLogger(EvacuationAreaSelector.class);
	
	private JFrame frame;
	private JPanel compositePanel;
	private MyMapViewer jMapViewer;
	private JButton saveButton;
	private JButton openBtn;

	private ShapeToStreetSnapperThreadWrapper snapper;

	private final String wms;

	private final String layer;

	private GripsConfigModule gcm;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		String wms = null;
		String layer = null;
		if (args.length == 4) {
			for (int i = 0; i < 4; i += 2) {
				if (args[i].equalsIgnoreCase("-wms")) {
					wms = args[i+1];
				}
				if (args[i].equalsIgnoreCase("-layer")) {
					layer = args[i+1];
				}
			}
			
		} else if (args.length != 0) {
			printUsage();
			System.exit(-1);
		}
		
		
		
		
		EventQueue.invokeLater(new Runner(wms,layer));
	}

	
	private static void printUsage() {
		System.out.println();
		System.out.println(EvacuationAreaSelector.class.getSimpleName());
		System.out.println("Starts the GRIPS evacuation area editor.");
		System.out.println();
		System.out.println("usage 1: " + EvacuationAreaSelector.class.getSimpleName() +"\n" +
						   "         starts the editor and uses openstreetmap as backround layer\n" +
						   "         requires a working internet connection");
		System.out.println("usage 2: " + EvacuationAreaSelector.class.getSimpleName() + " -wms <url> -layer <layer name>\n" +
				           "         starts the editor and uses the given wms server to load a backgorund layer");
		
	}
	
	/**
	 * Create the application.
	 * @param layer 
	 * @param wms 
	 * @param config 
	 */
	public EvacuationAreaSelector(String wms, String layer){
		this.wms = wms;
		this.layer = layer;
		initialize();
//		loadMapView(osmFile);

	}

	private void loadMapView(String osm) {
		
		if (this.wms == null) {
//			addMapViewer(null);
			addMapViewer(TileFactoryBuilder.getOsmTileFactory());
		} else {
			addMapViewer(TileFactoryBuilder.getWMSTileFactory(this.wms, this.layer));
		}
		this.snapper = new ShapeToStreetSnapperThreadWrapper(osm,this);
		this.jMapViewer.setSnapper(this.snapper);
		this.jMapViewer.setCenterPosition(this.snapper.getNetworkCenter());
		this.jMapViewer.setZoom(2);
		this.compositePanel.repaint();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		this.frame = new JFrame();
		this.frame.setSize(640, 640);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.getContentPane().setLayout(new BorderLayout(0, 0));
		this.frame.setResizable(true);
		
		JPanel panel = new JPanel();
		this.frame.getContentPane().add(panel, BorderLayout.SOUTH);
		
		this.openBtn = new JButton("Open");
		panel.add(this.openBtn);
		
		
		this.saveButton = new JButton("Save");
		this.saveButton.setEnabled(false);
		this.saveButton.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(this.saveButton);
		this.compositePanel = new JPanel();
		this.compositePanel.setBounds(new Rectangle(0, 0, 800, 800));
		this.frame.getContentPane().add(this.compositePanel, BorderLayout.CENTER);
		this.compositePanel.setLayout(new BorderLayout(0, 0));

		this.openBtn.addActionListener(this);
		this.saveButton.addActionListener(this);
		
		this.frame.setTitle("Evacuation Area Selector");
		
		this.frame.setLocationRelativeTo(null);
		
		this.frame.addComponentListener(new ComponentListener() 
		{  
		        @Override
				public void componentResized(ComponentEvent evt)
		        {
		            Component src = (Component)evt.getSource();
		            Dimension newSize = src.getSize();
		            updateMapViewerSize(newSize.width, newSize.height);
		        }
				@Override
				public void componentMoved(ComponentEvent e) {}
				@Override
				public void componentShown(ComponentEvent e) {}
				@Override
				public void componentHidden(ComponentEvent e) {}
		});
		
	}
	
	public void updateMapViewerSize(int width, int height)
	{
		if (this.jMapViewer!=null)
			this.jMapViewer.setBounds(0, 0, width, height);
	}

	
	public void addMapViewer(TileFactory tf) {
		this.compositePanel.setLayout(null);
		this.jMapViewer = new MyMapViewer();
		this.jMapViewer.setBounds(0, 0, 800, 800);
		this.jMapViewer.setTileFactory(tf);
		this.jMapViewer.setPanEnabled(true);
		this.jMapViewer.setZoomEnabled(true);
		this.compositePanel.add(this.jMapViewer);
	}	

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "Save") {
			log.info("Saving file to: " + this.gcm.getEvacuationAreaFileName() + ".");
			this.snapper.savePolygon(this.gcm.getEvacuationAreaFileName());
		}
		
		if (e.getActionCommand() == "Open") {
			final JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileFilter() {
				
				@Override
				public String getDescription() {
					return "GRIPS config file";
				}
				
				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					if (f.getName().endsWith("xml")){
						return true;
					}
					return false;
				}
			});
			 int returnVal = fc.showOpenDialog(this.frame);
		        if (returnVal == JFileChooser.APPROVE_OPTION) {
		        	this.openBtn.setEnabled(false);
		        	File file = fc.getSelectedFile();
		            log.info("Opening: " + file.getAbsolutePath() + ".");
		            
		            
					this.gcm = null;
					try
					{
						this.gcm = new GripsConfigModule("grips");
						GripsConfigReader reader = new GripsConfigReader(this.gcm);
						reader.parse(file.getAbsolutePath());
					}
					catch(Exception ee)
					{
						throw new RuntimeException("File is not a  grips config file! Exiting!");
					}
		            loadMapView(this.gcm.getNetworkFileName());
		        } else {
		            log.info("Open command cancelled by user.");
		        }
		}
		
	}
	
	public void setSaveButtonEnabled(boolean enabled) {
		this.saveButton.setEnabled(enabled);
	}
	
	private static final class Runner implements Runnable {
		
		private final String wms;
		private final String layer;
		public Runner(String wms,String layer) {
			this.wms = wms;
			this.layer = layer;
		}
		@Override
		public void run() {
			try {
				EvacuationAreaSelector window = new EvacuationAreaSelector(this.wms,this.layer);
				window.frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
