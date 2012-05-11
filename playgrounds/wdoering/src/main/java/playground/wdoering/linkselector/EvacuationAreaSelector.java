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

package playground.wdoering.linkselector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.JLabel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;



public class EvacuationAreaSelector implements ActionListener{

	private static final Logger log = Logger.getLogger(EvacuationAreaSelector.class);
	
	private JFrame frame;
	private JPanel compositePanel;
	private MyMapViewer jMapViewer;
	private JButton saveButton;
	private JButton openBtn;
	private JTextField blockField;

	private ShapeToStreetSnapperThreadWrapper snapper;

	private JPanel blockPanel;

	private JLabel blockLabel;

	private JButton blockButton;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		final String config = "";//args[0];
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					EvacuationAreaSelector window = new EvacuationAreaSelector();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * @param config 
	 */
	public EvacuationAreaSelector(){
		initialize();
//		loadMapView(osmFile);

	}

	private void loadMapView(String osm) {
		
		addMapViewer(osmTileFactory());
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
		this.frame.setBounds(100, 100, 800, 800);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.getContentPane().setLayout(new BorderLayout(0, 0));
		this.frame.setResizable(false);
		
		JPanel panel = new JPanel();
		this.frame.getContentPane().add(panel, BorderLayout.SOUTH);
		
		blockPanel = new JPanel(new GridLayout(22, 1));
		
		JPanel innerBlockPanel = new JPanel(new BorderLayout());
		
		blockLabel = new JLabel(" Gesperrt ab ");
		blockField = new JTextField("12:00");
		blockButton = new JButton("ok");
		
		blockPanel.setSize(new Dimension(80, 200));
		
		blockPanel.add(blockLabel);
		blockPanel.add(blockField);
		blockPanel.add(blockButton);
		
		innerBlockPanel.add(blockPanel, BorderLayout.CENTER);
		
		this.frame.getContentPane().add(innerBlockPanel, BorderLayout.EAST);
		
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
	
	private static TileFactory osmTileFactory() {
		final int max=17;
		TileFactoryInfo info = new TileFactoryInfo(0, 17, 17,
				256, true, true,
				"http://tile.openstreetmap.org",
				"x","y","z") {
			@Override
			public String getTileUrl(int x, int y, int zoom) {
				zoom = max-zoom;
				String url = this.baseURL +"/"+zoom+"/"+x+"/"+y+".png";
				return url;
			}

		};
		TileFactory tf = new DefaultTileFactory(info);
		return tf;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand() == "Save")
		{
			final JFileChooser fc = new JFileChooser();
			int retVal = fc.showSaveDialog(this.frame);
			if (retVal == JFileChooser.APPROVE_OPTION)
			{
				File f = fc.getSelectedFile();
	            log.info("Saving file to: " + f.getAbsolutePath() + ".");
				this.snapper.savePolygon(f.getAbsolutePath());
				
			} else {
				 log.info("Save command cancelled by user.");
			}
		}
		
		if (e.getActionCommand() == "Open") {
			final JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileFilter() {
				
				@Override
				public String getDescription() {
					return "OSM XML File";
				}
				
				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					if (f.getName().endsWith("osm")){
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
		            loadMapView(file.getAbsolutePath());
		        } else {
		            log.info("Open command cancelled by user.");
		        }
		}
		
	}
	
	public void setSaveButtonEnabled(boolean enabled) {
		this.saveButton.setEnabled(enabled);
	}
}
