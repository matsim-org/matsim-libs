package playground.kai.otfvis;

import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.otfivs.opengl.OnTheFlyClientQuad;

public class OnTheFlyClientFileTveh {

	public static void main(String[] args) {
//		String netFileName = "../matsim-trunk/examples/equil/network.xml";
//		String vehFileName = "../matsim-trunk/output/equil/ITERS/it.200/200.T.veh.gz";
		
//		String netFileName = "/home/nagel/vsp-cvs/studies/schweiz/2network/ch.xml"; 
//		String vehFileName = "/home/nagel/vsp-cvs/runs/run198/100.T.veh.gz"; 
		
		String netFileName = "/home/nagel/vsp-cvs/studies/schweiz-ivtch/network/ivtch.xml" ;
//		String vehFileName = "/home/nagel/vsp-cvs/runs/run270/100.T.veh.gz" ;
//		String vehFileName = "/home/nagel/vsp-cvs/runs/run299/100.T.veh.gz" ;
//		String vehFileName = "/home/nagel/vsp-cvs/runs/run450/0.T.veh.gz" ;
		String vehFileName = "/home/nagel/vsp-cvs/runs/run465/500.T.veh.gz" ;
//		String vehFileName = "../matsim-trunk/output/myrun/ITERS/it.99/99.T.veh.gz" ;

//		String netFileName = "/home/nagel/vsp-cvs/studies/padang/dlr-network/network/dlr/padang_net.xml" ;
//		String vehFileName = "/home/nagel/vsp-cvs/runs/inundation/colorizedT.veh.txt.gz" ;

		if (Gbl.getConfig() == null) Gbl.createConfig(null);

//		String localDtdBase = "../matsim-trunk/dtd/";
//		Gbl.getConfig().global().setLocalDtdBase(localDtdBase);
		
		OnTheFlyClientQuad client = new OnTheFlyClientQuad("tveh:"+vehFileName + "@" + netFileName);
		client.run();
	}

 
}
