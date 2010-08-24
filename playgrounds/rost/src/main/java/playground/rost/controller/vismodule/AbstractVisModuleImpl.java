/******************************************************************************
 *project: org.matsim.*
 * AbstractVisModuleImpl.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.controller.vismodule;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import playground.rost.controller.map.BasicMap;

public abstract class AbstractVisModuleImpl implements VisModule {
	
	protected String title;
	protected JPanel panel;
	
	protected Map<String, String> attributes = new HashMap<String, String>();
	protected Set<AttributeUI> aUIs = new HashSet<AttributeUI>();
	protected VisModuleContainer vMContainer;
	
	public AbstractVisModuleImpl(VisModuleContainer vMContainer, String title)
	{
		this.title = title;
		this.vMContainer = vMContainer;
	}
	
	public void paintGraphics(BasicMap map, Graphics g) {
		// TODO Auto-generated method stub

	}

	public Map<String, String> getAtrributes() {
		return attributes;
	}

	protected void createUI()
	{
		panel = new JPanel();
		createUIElementsForAttributes(panel);
	}
	
	public JPanel getUI() {
		if(panel == null)
			createUI();
		return panel;
	}
	
	protected void createUIElementsForAttributes(JPanel panel)
	{	
		JPanel attributePanel = new JPanel();
		attributePanel.setLayout(new BoxLayout(attributePanel, BoxLayout.Y_AXIS));
		for(String key : this.getAtrributes().keySet())
		{	
			AttributeUI aUI = new AttributeUI(this, key);
			aUIs.add(aUI);
			attributePanel.add(aUI.getPanel());
		}
		
		JPanel layerPanel = new JPanel();
		layerPanel.setLayout(new BoxLayout(layerPanel, BoxLayout.X_AXIS));
		ImageIcon up = new ImageIcon("./src/playground/rost/res/images/up25.png");
		JButton moveUp = new JButton(up);
		moveUp.setPreferredSize(new Dimension(20,20));
		ImageIcon down = new ImageIcon("./src/playground/rost/res/images/down25.png");
		JButton moveDown = new JButton(down);
		moveDown.setPreferredSize(new Dimension(20,20));
		moveUp.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				requestMoveLayer(MoveLayerDirection.Up);
			}
		});
		moveDown.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				requestMoveLayer(MoveLayerDirection.Down);
			}
		});
		layerPanel.add(moveUp);
		layerPanel.add(moveDown);
		
		
		
		this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.Y_AXIS));
		
		JLabel titleLabel = new JLabel(title);
		this.panel.add(titleLabel);
		
		JPanel attributeAndLayerPanel = new JPanel();
		attributeAndLayerPanel.setLayout(new BoxLayout(attributeAndLayerPanel, BoxLayout.X_AXIS));
		
		attributeAndLayerPanel.add(attributePanel);
		attributeAndLayerPanel.add(layerPanel);
	
		this.panel.add(attributeAndLayerPanel);
		
		
	}

	public void setAttribute(String key, String value) {
		attributes.put(key, value);
	}
	
	public void refreshAttributes()
	{
		for(AttributeUI aUI : aUIs)
		{
			this.setAttribute(aUI.key, aUI.text.getText());
		}
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public boolean parseBoolean(String s, boolean def)
	{
		Boolean result = Boolean.parseBoolean(attributes.get(s));
		if(result == null)
			result = def;
		return result;
	}
	
	public Color parseColor(String s, Color def)
	{
		Color c = Color.decode(attributes.get(s));
		if(c == null)
			c = def;
		return c;
	}
	
	public int parseInt(String s, int def)
	{
		Integer result = Integer.parseInt(attributes.get(s));
		if(result == null)
			result = def;
		return result;
	}
	
	public String[] parseList(String key)
	{
		String value = attributes.get(key);
		return value.split(",");
	}
	
	public void requestMoveLayer(MoveLayerDirection direction)
	{
		vMContainer.requestMoveLayer(this, direction);
	}
	
}
