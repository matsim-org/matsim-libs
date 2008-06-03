package playground.kai.otfvis;

import org.matsim.utils.vis.otfivs.opengl.OnTheFlyClientFileQuad;

public class OnTheFlyClientFileMvi 
{
	public static void main(String[] args) 
	{
//		String mviFile = "/home/nagel/vsp-cvs/runs/run476/99.T.mvi" ;
		String mviFile = "/home/nagel/vsp-cvs/runs/run469/200.mvi.gz" ;
//		String mviFile = "/home/nagel/vsp-cvs/runs/run306/output/ITERS/it.100/100.movie.mvi" ;
		new OnTheFlyClientFileQuad(mviFile).run();
	}
}
