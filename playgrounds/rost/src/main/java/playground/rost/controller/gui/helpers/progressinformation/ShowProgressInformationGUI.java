package playground.rost.controller.gui.helpers.progressinformation;

import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import playground.rost.controller.MainGUI;

public class ShowProgressInformationGUI extends JInternalFrame implements InternalFrameListener {
	
	protected ProgressInformationProvider infoProvider = null;
	protected Map<String, JLabel> mapProgressKeyToLabel = new HashMap<String, JLabel>();
	protected MainGUI mainGUI;

	protected class ProgressInformationWatcher implements Runnable
	{

		public void run() {
			if(infoProvider != null)
			{
				while(!infoProvider.isFinished())
				{
					updateUI();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}
				}
			}
			destroyFrame();
		}
		
		protected void updateUI()
		{
			for(String key : mapProgressKeyToLabel.keySet())
			{
				String value = infoProvider.getProgressInformation(key);
				JLabel label = mapProgressKeyToLabel.get(key);
				if(label != null)
				{
					label.setText(value);
				}
			}
		}
		
		
		
	}
	
	public ShowProgressInformationGUI(MainGUI mainGUI, ProgressInformationProvider infoProvider)
	{
		super("Progress of " + infoProvider.getTitle(),
				false, //resizable
		          false, //closable
		          false, //maximizable
		          false); //iconfiable
		this.infoProvider = infoProvider;
		this.mainGUI = mainGUI;
		this.createUI();
		this.pack();
		this.addInternalFrameListener(this);
		ProgressInformationWatcher watcher = new ProgressInformationWatcher();
		Thread thread = new Thread(watcher);
		thread.start();
	}
	
	protected void createUI()
	{
		this.setLayout(new GridLayout(0,2));
		List<String> infoNames = infoProvider.getListOfKeys();
		for(String key : infoNames)
		{
			JLabel lblKey = new JLabel(key);
			JLabel lblValue = new JLabel("");
			this.add(lblKey);
			this.add(lblValue);
			this.mapProgressKeyToLabel.put(key, lblValue);
		}
	}
	
	protected void destroyFrame()
	{
		this.setClosable(true);
		this.setDefaultCloseOperation(JInternalFrame.EXIT_ON_CLOSE);
		this.setClosable(true);
		this.doDefaultCloseAction();
		this.dispose();
	}
	
	public void internalFrameClosing(InternalFrameEvent e) {
		this.dispose();
	}
	
	public void internalFrameClosed(InternalFrameEvent e) {
	}
	
	public void internalFrameOpened(InternalFrameEvent e) {
	}
	
	public void internalFrameIconified(InternalFrameEvent e) {
	}
	
	public void internalFrameDeiconified(InternalFrameEvent e) {
	}
	
	public void internalFrameActivated(InternalFrameEvent e) {
	}
	
	public void internalFrameDeactivated(InternalFrameEvent e) {
	}

	
	
	
	
}
