package others.sergioo.visUtils;

import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

public class ExeWindow extends JFrame implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//Classes
	private class JExeButton extends JButton {
	
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	
		//Attributes
		private String path;
		
		//Constructors
		public JExeButton(String path) {
			super();
			this.path = path;
		}
	}
	
	//Constants
	private static final String S = ",";
	
	//Attributes
	private JExeButton[][] apps;

	//Constructors
	/**
	 * @throws HeadlessException
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public ExeWindow(String appsFile) throws HeadlessException, FileNotFoundException, IOException {
		super("Applications window");
		setUndecorated(true);
		Properties ps = new Properties();
		ps.load(new FileInputStream(new File(appsFile)));
		apps = new JExeButton[new Integer(ps.getProperty("height"))][new Integer(ps.getProperty("width"))];
		setLayout(new GridLayout(apps.length, apps[0].length));
		for(int f=0; f<apps.length; f++)
			for(int c=0; c<apps[0].length; c++) {
				apps[f][c] = new JExeButton(ps.getProperty("path"+S+f+S+c));
				apps[f][c].addActionListener(this);
				apps[f][c].setActionCommand("action"+S+f+S+c);
				String imageLocation = ps.getProperty("image"+S+f+S+c); 
				String text = ps.getProperty("text"+S+f+S+c); 
				if(imageLocation!=null)
					apps[f][c].setIcon(new ImageIcon(imageLocation));
				else if(text!=null)
					apps[f][c].setText(text);
				add(apps[f][c]);
			}
		setSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
	}
	
	//Methods
	@Override
	public void actionPerformed(ActionEvent e) {
		String[] comms = e.getActionCommand().split(S);
		try {
			Runtime.getRuntime().exec(apps[new Integer(comms[1])][new Integer(comms[2])].path);
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	//Main
	public static void main(String[] args) throws HeadlessException, FileNotFoundException, IOException {
		new ExeWindow(args[0]).setVisible(true);
	}
	
}
