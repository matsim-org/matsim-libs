package org.matsim.vis.otfvis.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface OTFQueryRemote extends Remote {

	public OTFQueryResult query() throws RemoteException;
		
}
