package playground.jjoubert.Utilities;

/**
 * A utility class to construct and maintain a progress bar. Somewhat of a nice thingy,
 * but only <i>looks good</i> if no other error or warning messages appear. In mundane 
 * tasks such as sorting through and processing vehicle files, though, it is nice to
 * know how many of the more than 30,000 files are done.
 * 
 * @author jwjoubert
 */
public class ProgressBar {
	private char progressCharacter; 
	private int dotsPrinted;
	private final int totalCount;
	
	/**
	 * Constructor for the progress bar class.
	 * @param c the character that will be used to print in the progress bar;
	 * @param totalCount the total against which progress is measured.
	 */
	public ProgressBar(char c, int totalCount){
		this.progressCharacter = c;
		this.dotsPrinted = 0;
		this.totalCount = totalCount;
	}
	
	/**
	 * A method to draw a basic progress bar form 0 to 100 percent with tick marks every 
	 * 10 percent.
	 */
	public void printProgressBar() {
		System.out.println("0%                 20%                 40%                 60%                 80%               100%");
		System.out.print("|");
		for(int i = 1; i <= 10; i++ ){
			for(int j = 1; j <= 9; j++ ){
				System.out.print("-");
			}
			System.out.print("|");
		}
		System.out.println();
		System.out.print( progressCharacter );
	}

	/**
	 * A method to calculate the new progress, and add the appropriate number of dots to 
	 * the progress bar, if required. 
	 * 
	 * @param current the current position in the sequence of activities for which progress 
	 * 		  is tracked.
	 */
	public void updateProgress(int current) {
		int percentage = (int) (( ((double) current) / ((double) this.totalCount ))*100);
		int dotsAdd = percentage - this.dotsPrinted;
		for(int i = 1; i <= dotsAdd; i++ ){
			System.out.print( progressCharacter );
		}
		this.dotsPrinted += dotsAdd;	
	}

	public char getProgressCharacter() {
		return progressCharacter;
	}
	
}