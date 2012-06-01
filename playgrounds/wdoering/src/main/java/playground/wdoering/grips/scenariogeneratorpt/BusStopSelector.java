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

package playground.wdoering.grips.scenariogeneratorpt;

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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.JLabel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.matsim.api.core.v01.Id;



public class BusStopSelector implements ActionListener{

	private static final Logger log = Logger.getLogger(BusStopSelector.class);
	
	private JFrame frame;
	private JPanel compositePanel;
	private BusStopMapViewer jMapViewer;
	private JButton saveButton;
	private JButton openBtn;
	private JTextField blockField1Starthh;
	private JTextField blockField1Startmm;
	private JTextField blockField1Endhh;
	private JTextField blockField1Endmm;
	private JTextField blockField2Starthh;
	private JTextField blockField2Startmm;
	private JTextField blockField2Endhh;
	private JTextField blockField2Endmm;
	
	private HashMap<Id, String> busStops;
	private Id currentLinkId1 = null;
	private Id currentLinkId2 = null;
	
	private int activeLink = 0;
	private ShapeToStreetSnapperThreadWrapper snapper;
	private JPanel blockPanel;
	private JLabel blockLabel;
	private JButton blockButtonOK;
	private JCheckBox cbStart;
	private JCheckBox cbEnd;
	private JPanel panelStart;
	private JPanel panelEnd;
	private boolean saveLink1 = false;
	private boolean saveLink2 = false;

	private JPanel panelDescriptions;

	private JPanel panel1Start;

	private JPanel panel1End;

	private JPanel panel2Start;

	private JPanel panel2End;

	private JCheckBox cb1Start;

	private JCheckBox cb1End;

	private JCheckBox cb2Start;

	private JCheckBox cb2End;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		final String config = "";//args[0];
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					BusStopSelector window = new BusStopSelector();
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
	public BusStopSelector(){
		
		busStops = new HashMap<Id, String>();
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
		
		
		
		blockField1Starthh = new JTextField("--");
		blockField1Startmm = new JTextField("--");
		blockField1Endhh = new JTextField("--");
		blockField1Endmm = new JTextField("--");
		blockField2Starthh = new JTextField("--");
		blockField2Startmm = new JTextField("--");
		blockField2Endhh = new JTextField("--");
		blockField2Endmm = new JTextField("--");
		blockButtonOK = new JButton("ok");
		
		blockPanel.setSize(new Dimension(200, 200));
		
		//add hour / minute input check listeners
		blockField1Starthh.addKeyListener(new TypeHour());
		blockField1Startmm.addKeyListener(new TypeMinute());
		blockField1Endhh.addKeyListener(new TypeHour());
		blockField1Endmm.addKeyListener(new TypeMinute());
		
		blockField1Starthh.addFocusListener(new CheckHour());
		blockField1Startmm.addFocusListener(new CheckMinute());
		blockField1Endhh.addFocusListener(new CheckHour());
		blockField1Endmm.addFocusListener(new CheckMinute());
		
		blockField2Starthh.addKeyListener(new TypeHour());
		blockField2Startmm.addKeyListener(new TypeMinute());
		blockField2Endhh.addKeyListener(new TypeHour());
		blockField2Endmm.addKeyListener(new TypeMinute());
		
		blockField2Starthh.addFocusListener(new CheckHour());
		blockField2Startmm.addFocusListener(new CheckMinute());
		blockField2Endhh.addFocusListener(new CheckHour());
		blockField2Endmm.addFocusListener(new CheckMinute());
		
		cb1Start = new JCheckBox("from");
		cb1End = new JCheckBox("to");
		cb2Start = new JCheckBox("from");
		cb2End = new JCheckBox("to");
		
		blockButtonOK.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				updateRoadClosure();
				
			}
		});
		
		cbStart.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveLink1 = !saveLink1;
				
				if (saveLink1)
				{
					blockField1Starthh.setEnabled(true);
					blockField1Startmm.setEnabled(true);
					blockField1Endhh.setEnabled(true);
					blockField1Endmm.setEnabled(true);
					
				}
				else
				{
					blockField1Starthh.setEnabled(false);
					blockField1Startmm.setEnabled(false);
					blockField1Endhh.setEnabled(false);
					blockField1Endmm.setEnabled(false);
				}
			}
		});
		
		cbEnd.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveLink2 = !saveLink2;
				
				if (saveLink2)
				{
					blockField2Starthh.setEnabled(true);
					blockField2Startmm.setEnabled(true);
					blockField2Endhh.setEnabled(true);
					blockField2Endmm.setEnabled(true);
				}
				else
				{
					blockField2Starthh.setEnabled(false);
					blockField2Startmm.setEnabled(false);
					blockField2Endhh.setEnabled(false);
					blockField2Endmm.setEnabled(false);
				}

				
				
			}
		});
		
		cbStart.setBackground(Color.red);
		cbEnd.setBackground(Color.green);
		
		cbStart.setSelected(false);
		cbEnd.setSelected(false);
		
		
		panelDescriptions = new JPanel(new GridLayout(1, 3));
		
		panel1Start = new JPanel(new GridLayout(1, 3));
		panel1End = new JPanel(new GridLayout(1, 3));
		panel2Start = new JPanel(new GridLayout(1, 3));
		panel2End = new JPanel(new GridLayout(1, 3));
		
		panelDescriptions.add(new JLabel("Road ID"));
		panelDescriptions.add(new JLabel("HH"));
		panelDescriptions.add(new JLabel("MM"));
		
//		panel1Start.add(cbStart);
		panel1Start.add(blockField1Starthh);
		panel1Start.add(blockField1Startmm);
		panel2Start.add(blockField2Starthh);
		panel2Start.add(blockField2Startmm);
		
//		panelEnd.add(cbEnd);
		panel1End.add(blockField1Endhh);
		panel1End.add(blockField1Endmm);
		panel2End.add(blockField2Endhh);
		panel2End.add(blockField2Endmm);
		
		blockPanel.add(panelDescriptions);
		blockPanel.add(panel1Start);
		blockPanel.add(panel1End);
		blockPanel.add(panel2Start);
		blockPanel.add(panel2End);
		blockPanel.add(blockButtonOK);		
		
		blockPanel.setPreferredSize(new Dimension(200,300));
		
		blockPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		
		
		cbStart.setEnabled(false); cbEnd.setEnabled(false);
		
		blockField1Starthh.setEnabled(false); blockField1Startmm.setEnabled(false);
		blockField1Endhh.setEnabled(false); blockField1Endmm.setEnabled(false);
		
		blockField2Starthh.setEnabled(false); blockField2Startmm.setEnabled(false);
		blockField2Endhh.setEnabled(false); blockField2Endmm.setEnabled(false);
		
		blockButtonOK.setEnabled(false);
		
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
		this.jMapViewer = new BusStopMapViewer(this);
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
			if (busStops.size()>0)
			{
				final JFileChooser fc = new JFileChooser();
				int retVal = fc.showSaveDialog(this.frame);
				if (retVal == JFileChooser.APPROVE_OPTION)
				{
					File f = fc.getSelectedFile();
		            log.info("Saving file to: " + f.getAbsolutePath() + ".");
					this.snapper.saveRoadClosures(f.getAbsolutePath(), busStops);
					
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
			cbStart.setEnabled(true);
			cbEnd.setEnabled(true);
			blockButtonOK.setEnabled(true);
		}
		else
		{
			cbStart.setEnabled(false);
			cbEnd.setEnabled(false);
			blockButtonOK.setEnabled(false);
			
			blockField1Starthh.setText("--");
			blockField1Startmm.setText("--");
			blockField1Starthh.setEnabled(false);
			blockField1Startmm.setEnabled(false);
			
			blockField1Endhh.setText("--");
			blockField1Endmm.setText("--");
			blockField1Endhh.setEnabled(false);
			blockField1Endmm.setEnabled(false);
			
			blockField2Starthh.setText("--");
			blockField2Startmm.setText("--");
			blockField2Starthh.setEnabled(false);
			blockField2Startmm.setEnabled(false);
			
			blockField2Endhh.setText("--");
			blockField2Endmm.setText("--");
			blockField2Endhh.setEnabled(false);
			blockField2Endmm.setEnabled(false);
			
			cbStart.setSelected(false);
			cbEnd.setSelected(false);
			cbStart.setText("-");
			cbEnd.setText("-");
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
			this.cbStart.setText(id.toString());
			this.currentLinkId1 = id;

			if(busStops.containsKey(id))	
			{
				cbStart.setSelected(true);
				blockField1Starthh.setEnabled(true); blockField1Startmm.setEnabled(true);
				blockField1Endhh.setEnabled(true); blockField1Endmm.setEnabled(true);
				
				blockField1Starthh.setText(busStops.get(id).substring(0,2));
				blockField1Startmm.setText(busStops.get(id).substring(3,5));
				blockField1Endhh.setText(busStops.get(id).substring(0,2));
				blockField1Endmm.setText(busStops.get(id).substring(3,5));
				
				saveLink1 = true;
			}
		}
		else
		{
//			blockField1Starthh.setText("--"); blockField1Startmm.setText("--");
//			blockField1Starthh.setEnabled(false); blockField1Endhh.setEnabled(false);
//			blockField1Starthh.setText("--"); blockField1Startmm.setText("--");
//			blockField1Starthh.setEnabled(false); blockField1Endhh.setEnabled(false);
			cbStart.setEnabled(false);

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
			this.cbEnd.setText(id.toString());
			this.currentLinkId2 = id;
			
			if(busStops.containsKey(id))	
			{
				cbEnd.setSelected(true);
				blockField2Starthh.setEnabled(true); blockField2Startmm.setEnabled(true);
				blockField2Endhh.setEnabled(true); blockField2Endmm.setEnabled(true);
				
				blockField2Starthh.setText(busStops.get(id).substring(0,2));
				blockField2Startmm.setText(busStops.get(id).substring(3,5));
				blockField2Endhh.setText(busStops.get(id).substring(0,2));
				blockField2Endmm.setText(busStops.get(id).substring(3,5));
				
				saveLink2 = true;
			}
		}
		else
		{
//			blockFieldEndhh.setText("--"); blockFieldEndmm.setText("--");
//			blockFieldEndhh.setEnabled(false); blockFieldEndmm.setEnabled(false);
			cbEnd.setEnabled(false);
		}
		
	}
	
	public void updateRoadClosure()
	{
		if ((currentLinkId1!=null))
		{
//			if (cbStart.isSelected())
//				busStops.put(currentLinkId1, blockFieldStarthh.getText() + ":" + blockFieldStartmm.getText());
//			else
//				busStops.remove(currentLinkId1);
			
		}
		
		if ((currentLinkId2!=null))
		{
//			if (cbEnd.isSelected())
//				busStops.put(currentLinkId2, blockFieldEndhh.getText() + ":" + blockFieldEndmm.getText());
//			else
//				busStops.remove(currentLinkId2);
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
		return busStops;
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
		
		
		if (busStops.containsKey(id))
			return true;
		else
			return false;
	}	
	
	
}
