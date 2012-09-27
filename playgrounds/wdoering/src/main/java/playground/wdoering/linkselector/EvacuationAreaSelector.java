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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import org.matsim.api.core.v01.Id;

public class EvacuationAreaSelector implements ActionListener{

	private static final Logger log = Logger.getLogger(EvacuationAreaSelector.class);
	private JFrame frame;
	private JPanel compositePanel;
	private MyMapViewer jMapViewer;
	private JButton saveButton;
	private JButton openBtn;
	private JTextField blockFieldLink1hh;
	private JTextField blockFieldLink1mm;
	private JTextField blockFieldLink2hh;
	private JTextField blockFieldLink2mm;
	
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
	private JPanel panelLink1;
	private JPanel panelLink2;
	private boolean saveLink1 = false;
	private boolean saveLink2 = false;
	private JPanel panelDescriptions;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		final String config = "";//args[0];
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run()
			{
				try
				{
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
		blockFieldLink1hh = new JTextField("--");
		blockFieldLink1mm = new JTextField("--");
		blockFieldLink2hh = new JTextField("--");
		blockFieldLink2mm = new JTextField("--");
		blockButtonOK = new JButton("ok");
		
		blockPanel.setSize(new Dimension(200, 200));
		
		//add hour / minute input check listeners
		blockFieldLink1hh.addKeyListener(new TypeHour());
		blockFieldLink1mm.addKeyListener(new TypeMinute());
		blockFieldLink2hh.addKeyListener(new TypeHour());
		blockFieldLink2mm.addKeyListener(new TypeMinute());
		blockFieldLink1hh.addFocusListener(new CheckHour());
		blockFieldLink1mm.addFocusListener(new CheckMinute());
		blockFieldLink2hh.addFocusListener(new CheckHour());
		blockFieldLink2mm.addFocusListener(new CheckMinute());
		
		cbLink1 = new JCheckBox("link 1");
		cbLink2 = new JCheckBox("link 2");
		
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
				{
					blockFieldLink1hh.setEnabled(true);
					blockFieldLink1mm.setEnabled(true);
					
				}
				else
				{
					blockFieldLink1hh.setEnabled(false);
					blockFieldLink1mm.setEnabled(false);
				}
			}
		});
		
		cbLink2.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveLink2 = !saveLink2;
				
				if (saveLink2)
				{
					blockFieldLink2hh.setEnabled(true);
					blockFieldLink2mm.setEnabled(true);
				}
				else
				{
					blockFieldLink2hh.setEnabled(false);
					blockFieldLink2mm.setEnabled(false);
				}

				
				
			}
		});
		
		cbLink1.setBackground(Color.red);
		cbLink2.setBackground(Color.green);
		
		cbLink1.setSelected(false);
		cbLink2.setSelected(false);
		
		
		panelDescriptions = new JPanel(new GridLayout(1, 3));
		panelLink1 = new JPanel(new GridLayout(1, 3));
		panelLink2 = new JPanel(new GridLayout(1, 3));
		
		panelDescriptions.add(new JLabel("Road ID"));
		panelDescriptions.add(new JLabel("HH"));
		panelDescriptions.add(new JLabel("MM"));
		
		panelLink1.add(cbLink1);
		panelLink1.add(blockFieldLink1hh);
		panelLink1.add(blockFieldLink1mm);
		
		panelLink2.add(cbLink2);
		panelLink2.add(blockFieldLink2hh);
		panelLink2.add(blockFieldLink2mm);
		
		blockPanel.add(panelDescriptions);
		blockPanel.add(panelLink1);
		blockPanel.add(panelLink2);
		blockPanel.add(blockButtonOK);		
		
		blockPanel.setPreferredSize(new Dimension(200,300));
		
		blockPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		
		
		cbLink1.setEnabled(false); cbLink2.setEnabled(false);
		
		blockFieldLink1hh.setEnabled(false); blockFieldLink1mm.setEnabled(false);
		blockFieldLink2hh.setEnabled(false); blockFieldLink2mm.setEnabled(false);
		
		blockButtonOK.setEnabled(false);
		
		blockFieldLink1hh.setSelectedTextColor(Color.red);
		blockFieldLink1mm.setSelectedTextColor(Color.green);
		
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
		
		frame.addComponentListener(new ComponentListener() 
		{  
		        @Override
				public void componentResized(ComponentEvent evt)
		        {
		            Component src = (Component)evt.getSource();
		            Dimension newSize = src.getSize();
		            updateMapViewerSize(newSize.width-200, newSize.height);
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

	/**
	 * save and open events
	 * 
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand() == "Save")
		{
			if (roadClosures.size()>0)
			{
				final JFileChooser fc = new JFileChooser();
				int retVal = fc.showSaveDialog(this.frame);
				if (retVal == JFileChooser.APPROVE_OPTION)
				{
					File f = fc.getSelectedFile();
		            log.info("Saving file to: " + f.getAbsolutePath() + ".");
					this.snapper.saveRoadClosures(f.getAbsolutePath(), roadClosures);
					
				} else {
					 log.info("Save command cancelled by user.");
				}
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
		        	this.saveButton.setEnabled(true);
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

	/**
	 * set edit. basically dis-/enabling text fields
	 * and setting labels. 
	 * @param b
	 */
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
			
			blockFieldLink1hh.setText("--");
			blockFieldLink1mm.setText("--");
			blockFieldLink1hh.setEnabled(false);
			blockFieldLink1mm.setEnabled(false);
			
			blockFieldLink2hh.setText("--");
			blockFieldLink2mm.setText("--");
			blockFieldLink2hh.setEnabled(false);
			blockFieldLink2mm.setEnabled(false);
			
			cbLink1.setSelected(false);
			cbLink2.setSelected(false);
			cbLink1.setText("-");
			cbLink2.setText("-");
			saveLink1 = false;
			saveLink2 = false;
			
			
		}
			
		
	}
	
	/**
	 * set id #1 of the first selected link (in gui and object data)
	 * checks if there is any data for the link prior to this selection 
	 * 
	 * @param id
	 */	
	public void setLink1Id(Id id)
	{
		if (id!=null)
		{
			this.cbLink1.setText(id.toString());
			this.currentLinkId1 = id;

			if(roadClosures.containsKey(id))	
			{
				cbLink1.setSelected(true);
				blockFieldLink1hh.setEnabled(true); blockFieldLink1mm.setEnabled(true);
				
				blockFieldLink1hh.setText(roadClosures.get(id).substring(0,2));
				blockFieldLink1mm.setText(roadClosures.get(id).substring(3,5));
				
				saveLink1 = true;
			}
		}
		else
		{
			blockFieldLink1hh.setText("--"); blockFieldLink1mm.setText("--");
			blockFieldLink1hh.setEnabled(false); blockFieldLink2hh.setEnabled(false);
			cbLink1.setEnabled(false);

		}
		
	}
	
	/**
	 * set id #2 of the second selected link (in gui and object data)
	 * checks if there is any data for the link prior to this selection 
	 * 
	 * @param id
	 */
	public void setLink2Id(Id id)
	{
		if (id!=null)
		{
			this.cbLink2.setText(id.toString());
			this.currentLinkId2 = id;
			
			if(roadClosures.containsKey(id))	
			{
				cbLink2.setSelected(true);
				blockFieldLink2hh.setEnabled(true); blockFieldLink2mm.setEnabled(true);
				
				blockFieldLink2hh.setText(roadClosures.get(id).substring(0,2));
				blockFieldLink2mm.setText(roadClosures.get(id).substring(3,5));
				
				saveLink2 = true;
			}
		}
		else
		{
			blockFieldLink2hh.setText("--"); blockFieldLink2mm.setText("--");
			blockFieldLink2hh.setEnabled(false); blockFieldLink2mm.setEnabled(false);
			cbLink2.setEnabled(false);
		}
		
	}
	
	public void updateRoadClosure()
	{
		if ((currentLinkId1!=null))
		{
			if (cbLink1.isSelected())
				roadClosures.put(currentLinkId1, blockFieldLink1hh.getText() + ":" + blockFieldLink1mm.getText());
			else
				roadClosures.remove(currentLinkId1);
			
		}
		
		if ((currentLinkId2!=null))
		{
			if (cbLink2.isSelected())
				roadClosures.put(currentLinkId2, blockFieldLink2hh.getText() + ":" + blockFieldLink2mm.getText());
			else
				roadClosures.remove(currentLinkId2);
		}
		
		
//		if (roadClosures.size()>0)
//		{
//			Iterator it = roadClosures.entrySet().iterator();
//			
//		    while (it.hasNext())
//		    {
//		        Map.Entry pairs = (Map.Entry)it.next();
////		        System.out.println(pairs.getKey() + " = " + pairs.getValue());
//		    }
//		}

		
	}
	
	public synchronized HashMap<Id, String> getRoadClosures()
	{
		return roadClosures;
	}
	
	class TypeHour implements KeyListener 
	{
		
		@Override
		public void keyTyped(KeyEvent e)
		{
			if (!Character.toString(e.getKeyChar()).matches("[0-9]"))
				e.consume();
		}
		@Override
		public void keyReleased(KeyEvent e)
		{
			
			JTextField src = (JTextField)e.getSource();
			
			String text = src.getText();
			
			
			if (!text.matches("([01]?[0-9]|2[0-3])"))
					src.setText("00");
			
		}
		@Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}
	}	
	
	class CheckHour implements FocusListener
	{

		@Override
		public void focusGained(FocusEvent e)
		{
			JTextField src = (JTextField)e.getSource();
			src.setSelectionStart(0);
			src.setSelectionEnd(src.getText().length());			
			
		}

		@Override
		public void focusLost(FocusEvent e)
		{
			JTextField src = (JTextField)e.getSource();
			String text = src.getText();
			
			if (!text.matches("([01]?[0-9]|2[0-3])"))
				src.setText("00");
			else if (text.matches("[0-9]"))
				src.setText("0"+text);
			
		}
		
	}

	class CheckMinute implements FocusListener
	{

		@Override
		public void focusGained(FocusEvent e)
		{
			JTextField src = (JTextField)e.getSource();
			src.setSelectionStart(0);
			src.setSelectionEnd(src.getText().length());			
			
		}

		@Override
		public void focusLost(FocusEvent e)
		{
			JTextField src = (JTextField)e.getSource();
			
			String text = src.getText();
			
			if ((!text.matches("[0-5][0-9]")) && (!text.matches("[0-9]")))
				src.setText("00");
			else if (text.matches("[0-9]"))
				src.setText("0" + text);


			
		}
		
	}
	
	
	class TypeMinute implements KeyListener 
	{
		
		@Override
		public void keyTyped(KeyEvent e)
		{
			if (!Character.toString(e.getKeyChar()).matches("[0-9]"))
				e.consume();
		}
		@Override
		public void keyReleased(KeyEvent e)
		{
			
			JTextField src = (JTextField)e.getSource();
			
			String text = src.getText();
			

		}
		@Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}
	}


	public boolean hasLink(Id id)
	{
		
		
		if (roadClosures.containsKey(id))
			return true;
		else
			return false;
	}	
	
	
}
