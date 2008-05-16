package org.matsim.utils.vis.otfivs.interfaces;

import java.rmi.RemoteException;

public interface OTFLiveServerRemote extends OTFServerRemote {
	public OTFQuery answerQuery(org.matsim.utils.vis.otfivs.interfaces.OTFQuery query) throws RemoteException;
}
