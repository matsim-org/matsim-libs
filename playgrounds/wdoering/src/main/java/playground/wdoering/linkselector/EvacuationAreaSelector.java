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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.JLabel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.matsim.api.core.v01.Id;



public class EvacuationAreaSelector implements ActionListener{

	private static final Logger log = Logger.getLogger(EvacuationAreaSelector.class);
	
	private JFrame frame;
	private JPanel compositePanel;
	private MyMapViewer jMapViewer;
	private JButton saveButton;
	private JButton openBtn;
	private JTextField blockFieldLink1;
	
	private HashMap<Id, String> roadClosures;
	private Id currentLinkId1 = null;
	private Id currentLinkId2 = null;
	
	private int activeLink = 0;

	private ShapeToStreetSnapperThreadWrapper snapper;

	private JPanel blockPanel;

	private JLabel blockLabel;

	private JButton blockButtonOK;

	private JCheckBox cbLink1;

	private JCheckBox cbLink2;

	private JTextField blockFieldLink2;

	private JPanel panelLink1;

	private JPanel panelLink2;
	
	private boolean saveLink1 = false;
	private boolean saveLink2 = false;

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
		
		roadClosures = new HashMap<Id, String>();
		
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
	private void initialize()
	{
		this.frame = new JFrame();
		this.frame.setBounds(100, 100, 1000, 800);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.getContentPane().setLayout(new BorderLayout(0, 0));
		this.frame.setResizable(true);
		
		JPanel panel = new JPanel();
		this.frame.getContentPane().add(panel, BorderLayout.SOUTH);
		
		blockPanel = new JPanel(new GridLayout(18, 2));
		
		
		
		blockLabel = new JLabel(" Gesperrt ab ");
		blockFieldLink1 = new JTextField("--:--");
		blockFieldLink2 = new JTextField("--:--");
		blockButtonOK = new JButton("ok");
		
		blockPanel.setSize(new Dimension(200, 200));
		
		
		cbLink1 = new JCheckBox("link 1");
		cbLink2 = new JCheckBox("link 2");
		
//		JRadioButton rbLink3 = new JRadioButton("link 3");
//		JRadioButton rbLink4 = new JRadioButton("link 4");
		
		blockButtonOK.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				updateRoadClosure();
				
			}
		});
		
		cbLink1.addActionListener(new ActionListener()
		{
			

			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveLink1 = !saveLink1;
				
				if (saveLink1)
					blockFieldLink1.setEnabled(true);
				else
					blockFieldLink1.setEnabled(false);
			}
		});
		
		cbLink2.addActionListener(new ActionListener()
		{
			

			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveLink2= !saveLink2;
				if (saveLink2)
					blockFieldLink2.setEnabled(true);
				else
					blockFieldLink2.setEnabled(false);
				
				
			}
		});
		
		cbLink1.setBackground(Color.red);
		cbLink2.setBackground(Color.green);
//		rbLink3.setBackground(Color.blue);
//		rbLink4.setBackground(Color.orange);
//		rbLink3.setForeground(Color.white);
		
		
		cbLink1.setSelected(false);
		cbLink2.setSelected(false);
//		rbLink3.setSelected(false);
//		rbLink4.setSelected(false);
		
		
		panelLink1 = new JPanel(new GridLayout(1, 2));
		panelLink2 = new JPanel(new GridLayout(1, 2));
		panelLink1.add(cbLink1);
		panelLink1.add(blockFieldLink1);
		panelLink2.add(cbLink2);
		panelLink2.add(blockFieldLink2);
		
		blockPanel.add(panelLink1);
		blockPanel.add(panelLink2);
		blockPanel.add(blockButtonOK);		
		
		blockPanel.setPreferredSize(new Dimension(200,300));
		
		
		cbLink1.setEnabled(false);
		cbLink2.setEnabled(false);
		blockFieldLink1.setEnabled(false);
		blockFieldLink2.setEnabled(false);
		blockButtonOK.setEnabled(false);
		
		blockFieldLink1.setSelectedTextColor(Color.red);
		blockFieldLink2.setSelectedTextColor(Color.green);
		
		this.frame.getContentPane().add(blockPanel, BorderLayout.EAST);
		
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
		this.jMapViewer = new MyMapViewer(this);
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

	public void setEditMode(boolean b)
	{
		if (b)
		{
			cbLink1.setEnabled(true);
			cbLink2.setEnabled(true);
			blockButtonOK.setEnabled(true);
		}
		else
		{
			cbLink1.setEnabled(false);
			cbLink2.setEnabled(false);
			blockButtonOK.setEnabled(false);
			blockFieldLink1.setText("--.--");
			blockFieldLink2.setText("--.--");
			
		}
			
		
	}
	
	public void setLink1Id(Id id)
	{
		if (id!=null)
		{
			this.cbLink1.setText(id.toString());
			this.currentLinkId1 = id;
			
			if(roadClosures.containsKey(id))	
				blockFieldLink1.setText(roadClosures.get(id));
		}
		else
		{
			blockFieldLink1.setText("--.--");
			blockFieldLink1.setEnabled(false);
			cbLink1.setEnabled(false);

		}
		
		
		
		
	}
	
	public void setLink2Id(Id id)
	{
		if (id!=null)
		{
			this.cbLink2.setText(id.toString());
			this.currentLinkId2 = id;
			
			if(roadClosures.containsKey(id))	
				blockFieldLink2.setText(roadClosures.get(id));
		}
		else
		{
			blockFieldLink2.setText("--.--");
			blockFieldLink2.setEnabled(false);
			cbLink2.setEnabled(false);
		}
		
	}
	
	public void updateRoadClosure()
	{
		if ((currentLinkId1!=null) && (cbLink1.isSelected()))
			roadClosures.put(currentLinkId1, blockFieldLink1.getText());
		
		if ((currentLinkId2!=null) && (cbLink2.isSelected()))
			roadClosures.put(currentLinkId2, blockFieldLink2.getText());
		
		if (roadClosures.size()>0)
		{
			Iterator it = roadClosures.entrySet().iterator();
			
		    while (it.hasNext())
		    {
		        Map.Entry pairs = (Map.Entry)it.next();
		        System.out.println(pairs.getKey() + " = " + pairs.getValue());
		        it.remove(); // avoids a ConcurrentModificationException
		    }
		}

		
	}
	
	
	
}
