package org.tit.injection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class FirstInjection extends JFrame {

    public FirstInjection() {
       initUI();
    }
    
    public final void initUI() {

        JPanel panel = new JPanel();
        getContentPane().add(panel);
        List<JButton> buttons = new ArrayList<JButton>();
        panel.setLayout(null);
        int N = 10;
        for(int i=0;i<N;i++){
        	JButton btn = new JButton("Button"+i);
        	
            btn.setBounds(50, 30 + 30*(i-1), 80, 30);
            buttons.add(btn);
            panel.add(btn);
        }
        
        JButton quitButton = new JButton("Quit");
        quitButton.setBounds(50, 60, 80, 30);
        
        quitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
           }
        });
        

        setTitle("Quit button");
        setSize(200, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
     }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	FirstInjection ex = new FirstInjection();
                ex.setVisible(true);
            }
        });
    }
}