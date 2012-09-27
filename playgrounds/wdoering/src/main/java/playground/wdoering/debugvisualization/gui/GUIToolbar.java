package playground.wdoering.debugvisualization.gui;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import playground.wdoering.debugvisualization.controller.Controller;


public class GUIToolbar extends JPanel {

	JLabel labelTitle;
	JButton buttonRewind;
	JButton buttonPlay;
	JButton buttonPause;
	JButton buttonTool;
	JButton buttonSetOffset;
	
	JTextField textXPos;
	JTextField textYPos;
	
	JLabel xPos;
	JLabel yPos;
	
	JSlider sliderSpeed;
	
	Controller controller;
	
	int x = 0;
	
	boolean isPaused = false;
	
	public GUIToolbar(Controller controller)
	{
		this.controller = controller;
		
        setLayout(new BorderLayout());
        //Container content = this.getContentPane();
        labelTitle = new JLabel("Debug Visualization Toolbar");
        
        buttonRewind  = new JButton("rewind");
        buttonPause = new JButton("pause");
        buttonPlay = new JButton("play");
        buttonTool = new JButton("test");
        buttonSetOffset = new JButton("update");
         
        xPos = new JLabel(" X offset: ");
        yPos = new JLabel(" Y offset: ");
        
        textXPos = new JTextField("0", 10);
        textYPos = new JTextField("0", 10);
        
        
        
        
        buttonPlay.addActionListener(new ActionPlay());
        buttonPause.addActionListener(new ActionPause());
        buttonRewind.addActionListener(new ActionRewind());
        buttonTool.addActionListener(new ActionTool());
        buttonSetOffset.addActionListener(new ActionSetOffset());
        
        JPanel panelButtons;
        
    	panelButtons = new JPanel(new GridLayout(0,12));
    	
        if (!controller.isLiveMode())
        {
	        panelButtons.add(buttonPlay);
	        panelButtons.add(buttonRewind);
        }
        
        panelButtons.add(buttonPause);
        panelButtons.add(xPos);
        panelButtons.add(textXPos);
        panelButtons.add(yPos);
        panelButtons.add(textYPos);
        panelButtons.add(buttonSetOffset);
        //panelButtons.add(buttonTool);
        	
        
        add(panelButtons, BorderLayout.NORTH);
        add(labelTitle, BorderLayout.CENTER);
        
	}
	
	class ActionSetOffset implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e)
		{
			controller.setOffset(Integer.valueOf(textXPos.getText()), Integer.valueOf(textYPos.getText()));
		}
	
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
	
	class ActionTool implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			x += 10;
			controller.setOffset(x, 0);
			
		}
		
	}
	
	class ActionPause implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
		
			isPaused = !isPaused;
			
			if (isPaused)
				buttonPause.setText("play");
			else
				buttonPause.setText("pause");
				
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

	public void setOffsetText(int x, int y)
	{
		if ((textXPos != null) || (textYPos != null))
		{
			textXPos.setText(""+ x);
			textYPos.setText(""+ y);
		}
		
	}
	
	
}
