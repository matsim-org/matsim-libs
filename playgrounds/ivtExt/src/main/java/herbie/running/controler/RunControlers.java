package herbie.running.controler;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * To start the HerbieControler.java parallel.
 * Dont forget to set the number of cores.
 * 
 * @author bvitins
 *
 */
public class RunControlers extends Thread {

	private String[] args;
	private int numberOfCores = 1;

	public RunControlers(String[] args) {
		this.args = args;
	}

	public RunControlers() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) 
	{
		RunControlers runInParallel = new RunControlers();
		runInParallel.prepare();
	}
	
	public void prepare() 
	{
		ArrayList<String> configs = new ArrayList<String>();
		
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110524/configCalibrationStandard.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110524/configCalibration1.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110524/configCalibration2.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110524/configCalibration3.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110524/configCalibration4.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110524/configCalibration5.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110524/configCalibration6.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110524/configCalibration7.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110524/configCalibration8.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110524/configCalibration9.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110524/configCalibration10.xml");
		
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110525/configCalibrationStandard.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110525/configCalibration5.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110525/configCalibration6.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110525/configCalibration7.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110525/configCalibration8.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110525/configCalibration1.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110525/configCalibration2.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110525/configCalibration3.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110525/configCalibration4.xml");
		
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110531/configCalibrationStandard.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110531/configCalibration1.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110531/configCalibration2.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110531/configCalibration3.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110531/configCalibration4.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110531/configCalibration5.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110531/configCalibration6.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110531/configCalibration7.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110531/configCalibration8.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110531/configCalibration9.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110531/configCalibration10.xml");
		
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110607/configCalibrationStandard.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110607/configCalibration1.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110607/configCalibration2.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110607/configCalibration3.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110607/configCalibration4.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110607/configCalibration5.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110607/configCalibration6.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110607/configCalibration7.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110607/configCalibration8.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110607/configCalibration9.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110607/configCalibration10.xml");
		
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110610/configCalibrationStandard.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110610/configCalibration1.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110610/configCalibration2.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110610/configCalibration3.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110610/configCalibration4.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110610/configCalibration5.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110610/configCalibration6.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110610/configCalibration7.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110610/configCalibration8.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110610/configCalibration9.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110610/configCalibration10.xml");		
		
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110614/configCalibrationStandard.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110614/configCalibration1.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110614/configCalibration2.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110614/configCalibration3.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110614/configCalibration4.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110614/configCalibration5.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110614/configCalibration6.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110614/configCalibration7.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110614/configCalibration8.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110614/configCalibration9.xml");
//		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110614/configCalibration10.xml");
		
		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110617/configCalibrationStandard.xml");
		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110617/configCalibration1.xml");
		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110617/configCalibration2.xml");
		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110617/configCalibration3.xml");
		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110617/configCalibration4.xml");
		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110617/configCalibration5.xml");
		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110617/configCalibration6.xml");
		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110617/configCalibration7.xml");
		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110617/configCalibration8.xml");
		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110617/configCalibration9.xml");
		configs.add("/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/Projekte/herbie/configs/20110617/configCalibration10.xml");
		
		for (int i = 0; i < configs.size(); i++) {
			
			String[]args = new String[1];
			args[0] = configs.get(i);
			RunControlers herbie  = new RunControlers(args);
			herbie.run();
		}
		
		
		
//		int currentRun = 0;
//		
//		while (currentRun < configs.size()) {
//			
//			ArrayList<RunControlersInParallel> threads = new ArrayList<RunControlersInParallel>();
//			
//			for (int i = 0; i < numberOfCores; i++) {
//				if (currentRun < configs.size()) {
//					String[]args = new String[1];
//					args[0] = configs.get(i);
//					threads.add(new RunControlersInParallel(args));
//				}
//				currentRun++;
//			}
//			
//			for (int i = 0; i < threads.size(); i++) {threads.get(i).start();}
//			
//			for (int i = 0; i < threads.size(); i++){
//				try {threads.get(i).join();} 
//				catch (InterruptedException e) {e.printStackTrace();}
//			}
//		}
		
		
		
//		ArrayList<RunControlersInParallel> runnable = new ArrayList<RunControlersInParallel>();
//		for (int i = 0; i < configs.size(); i++) {
//			String[]args = new String[1];
//			args[0] = configs.get(i);
//			runnable.add(new RunControlersInParallel(args));
//		}
//	    ExecutorService executorService = Executors.newFixedThreadPool(numberOfCores);
//	      
//		for (int i = 0; i < runnable.size(); i++) {
//			executorService.execute(runnable.get(i));
//		}
//		executorService.shutdown();
		
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		HerbieControler herbie = new HerbieControler(args);
		herbie.run();
	}
}
