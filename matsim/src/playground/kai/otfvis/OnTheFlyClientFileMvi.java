package playground.kai.otfvis;

import org.matsim.utils.vis.otfvis.opengl.OnTheFlyClientFileQuad;

public class OnTheFlyClientFileMvi 
{
	public static void main(String[] args) 
	{
		String mviFile = "/home/nagel/vsp-cvs/runs/run476/99.T.mvi" ;
//		String mviFile = "/home/nagel/vsp-cvs/runs/run469/200.mvi.gz" ;
//		String mviFile = "/home/nagel/vsp-cvs/runs/run465/it.550/550.T.mvi" ;
//		String mviFile = "/home/nagel/vsp-cvs/runs/run465/it.550/550.T.mvi" ;
//		String mviFile = "/home/nagel/vsp-cvs/runs/run582/it.500/500.events.mvi" ;

		new OnTheFlyClientFileQuad(mviFile).run();
	}
}

