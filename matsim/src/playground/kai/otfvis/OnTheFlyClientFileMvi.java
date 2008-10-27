package playground.kai.otfvis;

import org.matsim.utils.vis.otfvis.opengl.OnTheFlyClientFileQuad;

public class OnTheFlyClientFileMvi 
{
	public static void main(String[] args) 
	{

//		String mviFile = "/home/nagel/vsp-cvs/runs/run465/it.500/500.T.mvi" ; // basecase 10% old pt
		String mviFile = "/home/nagel/vsp-cvs/runs/run487/it.500/500.events.mvi" ; // basecase 100% old pt
//		String mviFile = "/home/nagel/vsp-cvs/runs/run476/99.T.mvi" ;
//		String mviFile = "/home/nagel/vsp-cvs/runs/run469/200.mvi.gz" ;
//		String mviFile = "/home/nagel/vsp-cvs/runs/run628/it.500/500.events.mvi" ; // basecase new pt
//		String mviFile = "/home/nagel/vsp-cvs/runs/run651/it.100/100.events.mvi" ; //gauteng

		new OnTheFlyClientFileQuad(mviFile).run();
	}
}

