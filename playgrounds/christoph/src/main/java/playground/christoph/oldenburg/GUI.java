package playground.christoph.oldenburg;

import javax.swing.JFrame;
import javax.swing.UIManager;

public class GUI extends JFrame {

	public GUI() {
		super();
		init();
	}
	
	private void init() {
		
//		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setTitle("Sioux Falls Evacuation - Summer School Oldenburg");
		this.setContentPane(new ConfigPanel());
		this.setVisible(true);
		this.setSize(1024, 768);
		
	    try {
	        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
	        //UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
	        //UIManager.setLookAndFeel("javax.swing.plaf.mac.MacLookAndFeel");
	        //UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");  
	        //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
	    } catch (Exception e) { }
	}
	
	public static void main(String args[]) {
		new GUI();
	}
}
