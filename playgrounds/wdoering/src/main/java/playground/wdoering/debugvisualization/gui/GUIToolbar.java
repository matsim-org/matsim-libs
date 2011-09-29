package playground.wdoering.debugvisualization.gui;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import playground.wdoering.debugvisualization.controller.Controller;

import processing.core.PApplet;


public class GUIToolbar extends JPanel {

	JLabel labelTitle;
	JButton buttonRewind;
	JButton buttonPlay;
	JButton buttonPause;
	
	JSlider sliderSpeed;
	
	Controller controller;
	
	public GUIToolbar(Controller controller)
	{
		this.controller = controller;
		
        setLayout(new BorderLayout());
        //Container content = this.getContentPane();
        labelTitle = new JLabel("Debug Visualization Toolbar");
        
        buttonRewind  = new JButton("rewind");
        buttonPause = new JButton("pause");
        buttonPlay = new JButton("play");
        
        JPanel panelButtons = new JPanel(new GridLayout(0,3));
        
        
        buttonPlay.addActionListener(new ActionPlay());
        buttonPause.addActionListener(new ActionPause());
        buttonRewind.addActionListener(new ActionRewind());
        
        panelButtons.add(buttonRewind);
        panelButtons.add(buttonPause);
        panelButtons.add(buttonPlay);
        
        add(panelButtons, BorderLayout.NORTH);
        add(labelTitle, BorderLayout.CENTER);
        
	}
	
	class ActionPlay implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
		
			labelTitle.setText("test");
			//controller.setAnimationSpeed(22f);
			controller.play();
		}
		
	}
	
	class ActionPause implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
		
			controller.pause();
		}
		
	}

	class ActionRewind implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
		
			controller.rewind();
		}
		
	}
	
	public void setTimeRange(Double from, Double to)
	{
		
	}
	
	public void setPositionRange(Point min, Point max)
	{
		
	}
	
	
}
