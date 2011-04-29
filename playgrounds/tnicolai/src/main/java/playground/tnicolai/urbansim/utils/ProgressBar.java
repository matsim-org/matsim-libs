package playground.tnicolai.urbansim.utils;

/**
 * 
 * @author thomas
 *
 */

public class ProgressBar {
	
	private long percent = 0;
	private long percentDone = 0;
	private long stepSize = 0;
	private boolean isFinished = false;
	
	/**
	 * constructor 
	 * @param stepSize <code>long</code>
	 */
	public ProgressBar(long stepSize){
		this.stepSize = stepSize;
		System.out.println("|--------------------------------------------------------------------------------------------------|");
	}
	/**
	 * constructor
	 * @param stepSize <code>double</code>
	 */
	public ProgressBar(double stepSize){
		this.stepSize = Math.round( stepSize );
		System.out.println("|--------------------------------------------------------------------------------------------------|");
	}
	
	/**
	 * updates the progress bar
	 */
	public void update(){
		
		percent++;
		
		int newState = (int) (100.*percent/stepSize);
		if(newState > 100)
			newState = 100;
		
		while ( newState  > percentDone )
			percentDone++;  System.out.print('|');

		if(newState == 100 && !isFinished){
			System.out.println("\r\n");
			isFinished = true; // once this is true the progress bar can't be updated anymore
		}
	}

}

