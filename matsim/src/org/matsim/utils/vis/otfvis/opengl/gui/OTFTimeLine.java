package org.matsim.utils.vis.otfvis.opengl.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.otfvis.data.OTFClientQuad;
import org.matsim.utils.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;

public class OTFTimeLine extends JToolBar implements OTFDrawer, ActionListener, ItemListener, ChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final OTFHostControlBar hostControl;
	private JSlider times;
    Hashtable<Integer, JLabel> labelTable = 
        new Hashtable<Integer, JLabel>();
	
	public OTFTimeLine(String string, OTFHostControlBar hostControl) {
		super(string);
		this.hostControl = hostControl;
		hostControl.addHandler("timeline", this);

		addSlider();
		
		JButton button = new JButton();
		button.setText("[");
		button.setActionCommand("setLoopStart");
		button.addActionListener(this);
	    button.setToolTipText("Sets the loop times");
	    add(button);
	    
		button = new JButton();
		button.setText("]");
		button.setActionCommand("setLoopEnd");
		button.addActionListener(this);
	    button.setToolTipText("Sets the loop times");

	    add(button);
	    this.setVisible(true);
	}

	void replaceLabel(String label, int newEnd) {
		for (Integer i : labelTable.keySet() ) {
			JLabel value = labelTable.get(i);
			if(value.getText().equals(label)) {
				labelTable.remove(i);
				break;
			}
		}
        labelTable.put(new Integer(newEnd), new JLabel(label));
        times.setLabelTable(labelTable);
    
        times.repaint();
	}
	
	public void actionPerformed(ActionEvent e) {
		// remove old label
		// get actual time
		int time = times.getValue();
		if(e.getActionCommand().equals("setLoopStart")){
			hostControl.setLoopBounds(time, -1);
			replaceLabel("[", time);
		}else if(e.getActionCommand().equals("setLoopEnd")){
			hostControl.setLoopBounds(-1, time);
			replaceLabel("]", time);
		}
		// insert new label
		
	}

	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		
	}

	/** Listen to the slider. */
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider)e.getSource();
        if (!source.getValueIsAdjusting()) {
            int newTime_s = source.getValue();
            hostControl.setNEWTime(newTime_s);
        }else hostControl.stopMovie();
    }
	   public void addSlider() {

	        //Create the slider.
		   Collection<Double> steps = hostControl.getTimeSteps();
		   Double[] dsteps = steps.toArray(new Double[0]);
	       
		   times = new JSlider(JSlider.HORIZONTAL,  dsteps[0].intValue(), dsteps[dsteps.length-1].intValue(), (int)hostControl.getTime());
	        
	        times.addChangeListener(this);
	        times.setMajorTickSpacing(dsteps[0].intValue()-dsteps[dsteps.length-1].intValue()/10);
	        times.setPaintTicks(true);
	        
	        //Create the label table.
	        //PENDING: could use images, but we don't have any good ones.
	        labelTable.put(new Integer( dsteps[0].intValue() ),
	                       new JLabel(Time.writeTime(dsteps[0])));
	                     //new JLabel(createImageIcon("images/stop.gif")) );
	        labelTable.put(new Integer( dsteps[dsteps.length-1].intValue() ),
	                       new JLabel(Time.writeTime(dsteps[dsteps.length-1])) );
	                     //new JLabel(createImageIcon("images/fast.gif")) );
	        
	        int n = dsteps.length/10;
	        
	        for(int i= n; i< dsteps.length-1; i+=n) {
		        labelTable.put(new Integer( dsteps[i].intValue() ),
	                       new JLabel(Time.writeTime(dsteps[i])) );
	        }
	        times.setLabelTable(labelTable);

	        times.setPaintLabels(true);
	        times.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
	        add(times);

	    }

	public void clearCache() {
		// TODO Auto-generated method stub
		
	}

	public Component getComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	public OTFClientQuad getQuad() {
		// TODO Auto-generated method stub
		return null;
	}

	public void handleClick(java.awt.geom.Point2D.Double point, int mouseButton) {
		// TODO Auto-generated method stub
		
	}

	public void invalidate(int time) throws RemoteException {
		times.setValue(time);
	}

	public void redraw() {
		// TODO Auto-generated method stub
		
	}
	   }
