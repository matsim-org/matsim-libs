package playground.jjoubert.CommercialTraffic;

/**
 * A class to construct and maintain a progress bar.
 * @author johanwjoubert
 */
public class ProgressBar {
	private char progressCharacter; 
	
	public ProgressBar(char c){
		this.setProgressCharacter(c);
	}
	
	/**
	 * A method to draw a basic progress bar form 0 to 100 percent 
	 * with tick marks every 10 percent.
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
	 * A method to calculate the new progress, and add the appropriate 
	 * number of dots to the progress bar.
	 * 
	 * @param dotsPrinted is the number of dots already on the bar.
	 * @param current the current position in the sequence of activities for which progress is tracked.
	 * @param total the length of the sequence for which progress is tracked.
	 * @return the updated number of dots 
	 */
	public int updateProgress(int dotsPrinted, int current, int total) {
		int percentage = (int) (( ((float) current) / ((float) total ))*100);
		int dotsAdd = percentage - dotsPrinted;
		for(int i = 1; i <= dotsAdd; i++ ){
			System.out.print( progressCharacter );
		}
		dotsPrinted += dotsAdd;	
		return dotsPrinted;
	}

	public char getProgressCharacter() {
		return progressCharacter;
	}
	
	private void setProgressCharacter(char c){
		this.progressCharacter = c;
	}
}