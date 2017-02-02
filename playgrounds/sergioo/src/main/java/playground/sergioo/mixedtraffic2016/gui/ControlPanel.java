package playground.sergioo.mixedtraffic2016.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ControlPanel extends JPanel implements ActionListener, ChangeListener {
	
	private JSlider fps;
	private JSlider time;
	private JButton pause;
	private JButton play;
	private Animation animator;
	
	public ControlPanel(Animation animator, Road road) {
		super();
		this.animator = animator;
		this.setLayout(new BorderLayout());
		fps = new JSlider(JSlider.HORIZONTAL, 0, 30, 1);
		fps.addChangeListener(this);
		Hashtable labelTable = new Hashtable();
		labelTable.put( new Integer( 0 ), new JLabel("0x") );
		labelTable.put( new Integer( 10 ), new JLabel("1x") );
		labelTable.put( new Integer( 20 ), new JLabel("2x") );
		fps.setLabelTable( labelTable );
		fps.setPaintLabels(true);
		this.add(fps, BorderLayout.WEST);
		time = new JSlider(JSlider.HORIZONTAL, (int)road.startTime, (int)road.endTime, (int)road.startTime);
		time.addChangeListener(this);
		time.setMajorTickSpacing(300);
		time.setPaintLabels(true);
		this.add(time, BorderLayout.CENTER);
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 2));
		pause = new JButton("||");
		pause.addActionListener(this);
		pause.setActionCommand("pause");
		panel.add(pause);
		play = new JButton("|>");
		play.addActionListener(this);
		play.setActionCommand("play");
		panel.add(play);
		this.add(panel, BorderLayout.EAST);
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("pause")) {
			animator.pause();
			fps.setValue(0);
		}
		else if(e.getActionCommand().equals("play")) {
			animator.play();
			fps.setValue(10);
		}
	}


	@Override
	public void stateChanged(ChangeEvent e) {
		if(e.getSource()==fps)
			animator.changeFPS(fps.getValue());
		else if(e.getSource()==time)
			animator.changeTime(time.getValue());
	}


	public void moveTime(int time2) {
		time.setValue(time2);
	}

}
