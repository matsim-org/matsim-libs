package playground.tnicolai.urbansim.utils;

/**
 * 
 * @author thomas
 *
 */

public class ProgressBar {
	
	private long percent = 0;
	private long stepsDone = 0;
	private long numberOfSteps = 0;
	private boolean isFinished = false;
	
	/**
	 * constructor 
	 * @param stepSize <code>long</code>
	 */
	public ProgressBar(long stepSize){
		this.numberOfSteps = stepSize;
		System.out.println("|--------------------------------------------------------------------------------------------------|");
	}
	/**
	 * constructor
	 * @param stepSize <code>double</code>
	 */
	public ProgressBar(double stepSize){
		this.numberOfSteps = Math.round( stepSize );
		System.out.println("|--------------------------------------------------------------------------------------------------|");
	}
	
	/**
	 * updates the progress bar
	 */
	public void update(){
		
		stepsDone++;
		
		int newState = (int) (100*stepsDone/numberOfSteps);
		if(newState > 100)
			newState = 100;
		
		while ( newState  > percent ){
			percent++;  
			System.out.print('|');
		}

		if(newState == 100 && !isFinished){
			System.out.println("\r\n");
			isFinished = true; // once this is true the progress bar can't be updated anymore
		}
	}

}

