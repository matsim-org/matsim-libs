package playground.sergioo.mixedtraffic2016.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Observable;

import javax.swing.JFrame;

import others.sergioo.visUtils.PanelPointLines;
import others.sergioo.visUtils.PointLines;

public class Window extends JFrame {
	
	
	private PanelPointLines panel;
	private ControlPanel controlPanel;

	public Window(Road road, Animation animation) {
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		//this.setUndecorated(true);
		panel=new PanelPointLines(road, this);
		this.add(panel, BorderLayout.CENTER);
		controlPanel = new ControlPanel(animation, road);
		this.add(controlPanel, BorderLayout.NORTH);
	}

	public void moveTime(int time) {
		controlPanel.moveTime(time);
	}
}
