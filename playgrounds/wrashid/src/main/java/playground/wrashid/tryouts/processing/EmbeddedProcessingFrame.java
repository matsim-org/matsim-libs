package playground.wrashid.tryouts.processing;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import processing.core.PApplet;

public class EmbeddedProcessingFrame extends Frame {

	NetworkWithHeatMap networkWithHeatMap;
	TextField tf1 ;
	Scrollbar sb;
	TextField tf2 ;
	public EmbeddedProcessingFrame() {
        super("Embedded PApplet");
        setSize(1200,1100);   
        setLayout(new FlowLayout());
        networkWithHeatMap=new NetworkWithHeatMap();
        add(networkWithHeatMap);
        tf1 = new TextField();
        tf1.setText(Double.toString(networkWithHeatMap.getMaxDistanceInMeters()));
        add(tf1);
        Button btn1 = new Button("Redraw");
        
        btn1.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				networkWithHeatMap.setMaxDistanceInMeters(Double.parseDouble(tf1.getText()));
				
			}
		});
        add(btn1);
        
        Button btn2 = new Button("High Quality");
        
        btn2.addActionListener(new ActionListener() {
        	
        	boolean isHighQualityRenderingOn=false;
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!isHighQualityRenderingOn){
					isHighQualityRenderingOn=true;
					networkWithHeatMap.smooth();
					((Button) e.getSource()).setLabel("Low Quality");
				} else {
					isHighQualityRenderingOn=false;
					networkWithHeatMap.noSmooth();
					((Button) e.getSource()).setLabel("High Quality");
				}
				
				
				
			}
		});
        add(btn2);
        
        Button btn3 = new Button("Animate Heat Wave");
        
        btn3.addActionListener(new ActionListener() {
        	
			@Override
			public void actionPerformed(ActionEvent e) {
				networkWithHeatMap.frameRate(24);
				networkWithHeatMap.animateHeatWave=true;
				
				
				
			}
		});
        add(btn3);
        
        
        sb=new  Scrollbar(Scrollbar.HORIZONTAL, 1, 100, 1, 1000);
        //sb.setSize(500, 40);
        add(sb);
        sb.addAdjustmentListener(new AdjustmentListener() {
			
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				int value = sb.getValue();
				networkWithHeatMap.setScaler(value);
				tf2.setText(String.valueOf(value));
			}
		});
        
        tf2=new TextField("1");
        add(tf2);
        
        // important to call this whenever embedding a PApplet.
        // It ensures that the animation thread is started and
        // that other internal variables are properly set.
        networkWithHeatMap.init();
        setVisible(true);
    }
	
	class TestWindowListener extends WindowAdapter
	  {
	    public void windowClosing(WindowEvent e)
	    {
	      e.getWindow().dispose();                   // Fenster "killen"
	      System.exit(0);                            // VM "killen" 
	    }           
	  }
	
	public static void main (String args[]) 
	  {
	    new EmbeddedProcessingFrame ();
	  }
	
}
