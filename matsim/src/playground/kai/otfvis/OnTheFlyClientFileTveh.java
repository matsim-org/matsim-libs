package playground.kai.otfvis;

import org.matsim.core.gbl.Gbl;
import org.matsim.run.OTFVis;

public class OnTheFlyClientFileTveh {

	public static void main(String[] args) {
//		String netFileName = "../matsim-trunk/examples/equil/network.xml";
//		String vehFileName = "../matsim-trunk/output/equil/ITERS/it.200/200.T.veh.gz";
		
//		String netFileName = "/home/nagel/vsp-cvs/studies/schweiz/2network/ch.xml"; 
//		String vehFileName = "/home/nagel/vsp-cvs/studies/schweiz/6-9SepFmaZurichOnly_rad=26000m-hwh/routes-and-times/it.150/150.T.veh.gz" ;
//		String vehFileName = "/home/nagel/vsp-cvs/runs/run198/100.T.veh.gz"; 
//		String vehFileName = "/home/nagel/vsp-cvs/runs/run475/fromEvent.mvi" ;
		
//		String netFileName = "/home/nagel/vsp-cvs/studies/big-zrh-hires/network/net.xml" ;
//		String vehFileName = "/home/nagel/vsp-cvs/studies/big-zrh-hires/metro-plans-IT2/it.0/T.0540-0550.1sec.veh.gz" ;
		
		String netFileName = "/Users/nagel/eclipse/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch.xml" ;
//		String vehFileName = "/home/nagel/vsp-cvs/runs/run465/0.T.veh.gz" ; 
//		String vehFileName = "/home/nagel/vsp-cvs/runs/run465/500.T.veh.gz" ;
//		String vehFileName = "/home/nagel/vsp-cvs/runs/run465/500.T.veh.gz" ; // current (may08) base case
		String vehFileName = "/home/nagel/vsp-cvs/runs/run457/0.T.veh.gz" ; 
//		String vehFileName = "/home/nagel/vsp-cvs/runs/run487/100.reduced.veh.gz" ; 
//		String vehFileName = "../matsim-trunk/output/myrun/ITERS/it.99/99.T.veh.gz" ;

//		String netFileName = "/home/nagel/vsp-cvs/studies/padang/dlr-network/network/dlr/padang_net.xml" ;
//		String vehFileName = "/home/nagel/vsp-cvs/runs/inundation/colorizedT.veh.txt.gz" ;
		
//		String netFileName = "/home/nagel/vsp-cvs/studies/seattle/network/net.xml"; 
//		String vehFileName = "/home/nagel/vsp-cvs/studies/empty/T.veh.gz" ;
//		String vehFileName = "/Users/nagel/vsp-cvs/studies/schweiz/evacuation/9jul07/reduced.veh.gz" ;
		
		if (Gbl.getConfig() == null) Gbl.createConfig(null);

//		String localDtdBase = "../matsim-trunk/dtd/";
//		Gbl.getConfig().global().setLocalDtdBase(localDtdBase);
		
		String[] a = {vehFileName, netFileName};
		
		OTFVis.main(a);
		
	}

 
}
