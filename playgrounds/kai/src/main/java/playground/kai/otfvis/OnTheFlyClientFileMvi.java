package playground.kai.otfvis;

import org.matsim.vis.otfvis.OTFClientFile;

public class OnTheFlyClientFileMvi {
	public static void main(String[] args) {

		// String mviFile = "/home/nagel/vsp-cvs/runs/run465/it.500/500.T.mvi" ; // basecase 10% old pt
		// String mviFile = "/home/nagel/vsp-cvs/runs/run487/it.500/500.events.mvi" ; // basecase 100% old pt
		// String mviFile = "/home/nagel/vsp-cvs/runs/run476/99.T.mvi" ;
		// String mviFile = "/home/nagel/vsp-cvs/runs/run469/200.mvi.gz" ;
		// String mviFile = "/home/nagel/vsp-cvs/runs/run628/it.500/500.events.mvi" ; // basecase new pt
		// String mviFile = "/home/nagel/vsp-cvs/runs/run651/it.100/100.events.mvi" ; //gauteng
		// String mviFile = "/home/nagel/vsp-cvs/runs/run650/it.600/600.events.mvi" ; // "same" as 628
//		 String mviFile = "/home/nagel/vsp-cvs/runs/run652/it.500/500.events.mvi" ; // "same" as 628
//		 String mviFile = "/home/nagel/vsp-cvs/runs/run657/it.1000/1000.events.mvi" ; // best w/ new op.times

//		 String mviFile = "/home/nagel/eclipse/matsim-trunk/myoutput/test1/ITERS/it.0/0.otfvis.mvi";
//		 String mviFile = "/home/nagel/eclipse/matsim-trunk/psrc-parcel-output/ITERS/it.100/100.otfvis.mvi" ;
//		String mviFile = "/home/nagel/eclipse/opus/opus_matsim/runs/25nov08-w-viaduct/ITERS/it.200/200.otfvis.mvi" ;
		
		String mviFile = "/Users/nagel/runs-svn/evac-no-risk-inc-moves/output/ITERS/it.0/0.movie.mvi" ;

		new OTFClientFile(mviFile).run();
	}
}
