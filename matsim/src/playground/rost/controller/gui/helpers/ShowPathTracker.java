package playground.rost.controller.gui.helpers;

import java.awt.GridLayout;

import javax.swing.JInternalFrame;
import javax.swing.JLabel;

import playground.rost.util.PathTracker;

public class ShowPathTracker extends JInternalFrame {
	public ShowPathTracker()
	{
		super("Current Paths:", true, true, true, true);
		this.setLayout(new GridLayout(0,2));
		String[][] files = PathTracker.getFiles();
		
		for(int i = 0; i < files.length; ++i)
		{
			JLabel key = new JLabel(files[i][0]);
			JLabel value = new JLabel(files[i][1]);
			this.add(key);
			this.add(value);
		}
		
		this.setSize(300, 400);
		this.setVisible(true);
	}
}
