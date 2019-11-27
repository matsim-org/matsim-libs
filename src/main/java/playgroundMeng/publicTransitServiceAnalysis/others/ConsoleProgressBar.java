package playgroundMeng.publicTransitServiceAnalysis.others;


import org.apache.log4j.Logger;

public class ConsoleProgressBar {
	public static void progressPercentage(int remain, int total, String string, Logger logger) {
	    if (remain > total) {
	        throw new IllegalArgumentException();
	    }
	    int maxBareSize = 10; // 10unit for 100%
	    int remainProcent = ((100 * remain) / total) / maxBareSize;
	    char defaultChar = '-';
	    String icon = "*";
	    String bare = new String(new char[maxBareSize]).replace('\0', defaultChar) + "]";
	    StringBuilder bareDone = new StringBuilder();
	    bareDone.append("[");
	    for (int i = 0; i < remainProcent; i++) {
	        bareDone.append(icon);
	    }
	    String bareRemain = bare.substring(remainProcent, bare.length());
	    logger.info(string +" "+ bareDone + bareRemain + " " + remainProcent * 10 + "%");
	    if (remain == total) {
	       logger.info("\n");
	    }
	}
}

