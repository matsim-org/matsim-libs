package org.matsim.utils.vis.otfivs.executables;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.swing.JFrame;

import org.matsim.utils.vis.otfivs.gui.OTFHostControlBar;


public class OTFVis   extends Thread {
	private final JFrame frame;
	private final OTFHostControlBar hostControl;
	
	public OTFVis(String address) throws RemoteException, InterruptedException, NotBoundException{
		frame = new JFrame("MATSIM NetVis" + address);
		hostControl = new OTFHostControlBar(address);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JFrame.setDefaultLookAndFeelDecorated(true);
		frame.add(hostControl);
		frame.setVisible(true);
	}
	
	
	
	public static void main(String[] args) {
		OTFVis client;
		try {
			client = new OTFVis("file:../MatsimJ/output/OTFQuadfile.mvi");
//			client = new OTFVis("rmi:127.0.0.1:4019");
			client.run();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
