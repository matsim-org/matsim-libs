package playground.kai.otfvis;

import org.matsim.utils.vis.otfvis.opengl.OnTheFlyClientFileQuad;

public class OnTheFlyClientFileMvi 
{
	public static void main(String[] args) 
	{
//		String mviFile = "/home/nagel/vsp-cvs/runs/run476/99.T.mvi" ;
//		String mviFile = "/home/nagel/vsp-cvs/runs/run469/200.mvi.gz" ;
		String mviFile = "/home/nagel/vsp-cvs/runs/run495/100.T.mvi" ;
		new OnTheFlyClientFileQuad(mviFile).run();
	}
}
