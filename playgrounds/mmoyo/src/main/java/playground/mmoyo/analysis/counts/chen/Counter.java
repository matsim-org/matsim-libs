package playground.mmoyo.analysis.counts.chen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import playground.yu.run.TrCtl;

/**uses Yu transit controler to have counts results**/
public class Counter {

	public static void main(String[] args) throws IOException {
		//It makes sure that "res" folder exists otherwise it won't write anything at the end
		File resFile = new File("./res/"); 
		if (!resFile.exists()){
			throw new FileNotFoundException("the resource folder -res- does not exist");
		}
		
		/*
		String configFile;
		if(args.length>0){ 
			configFile = args[0];
		}else{	
			configFile ="../playgrounds/mmoyo/output/sixth/configs";
		}
		TrCtl.main(new String[]{configFile});
		*/
		
		//read many configs:  
		String configsDir = "../playgrounds/mmoyo/output/sixth/configs"; 
		File dir = new File(configsDir);
		for (String configName : dir.list()){
			String completePath= configsDir + "/" + configName;
			System.out.println("\n\n  procesing: " + completePath);
			TrCtl.main(new String[]{completePath});
		}
		
		 
		//shut down ms-windows
		Runtime runtime = Runtime.getRuntime();
		runtime.exec("shutdown -s -t 300 -f");
	}
}
