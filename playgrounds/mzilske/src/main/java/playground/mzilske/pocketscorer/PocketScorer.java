package playground.mzilske.pocketscorer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;


// Abgebrochen, weil man keinen Plan einzeln parsen kann.

public class PocketScorer implements ActionListener {
	
	public static void main(String[] args) {
		PocketScorer pocketScorer = new PocketScorer();
		pocketScorer.run();
	}

	private void run() {
		
		JTextArea textArea = new JTextArea(80, 25);
		
		JButton button1 = new JButton("Calculate");
		button1.addActionListener(this);
		button1.setActionCommand("calculate");
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		
		frame.getContentPane().add(textArea, BorderLayout.CENTER);
		frame.getContentPane().add(button1, BorderLayout.SOUTH);
		frame.setVisible(true);
		
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
}
