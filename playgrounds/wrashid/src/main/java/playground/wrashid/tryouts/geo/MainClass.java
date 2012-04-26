package playground.wrashid.tryouts.geo;

import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;

import playground.wrashid.thelma.y2030.psl.FilteroutAllEventsAbroad;

public class MainClass extends JPanel {

  public void paint(Graphics g) {
   /*
	  int xpoints[] = {25, 145, 25, 145, 25};
    int ypoints[] = {25, 25, 145, 145, 25};
    int npoints = 5;
    
    g.drawPolygon(xpoints, ypoints, npoints);
    
    */
	  
	  g.drawPolygon(FilteroutAllEventsAbroad.getSwissBorders(0.001));
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame();
    frame.getContentPane().add(new MainClass());

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1000,1000);
    frame.setVisible(true);
  }
}
       