package playground.david.vis.interfaces;

import java.rmi.RemoteException;

public interface OTFLiveServerRemote extends OTFServerRemote {
	public OTFQuery answerQuery(playground.david.vis.interfaces.OTFQuery query) throws RemoteException;
}
