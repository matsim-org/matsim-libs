package playground.mmoyo.analysis.counts.chen;

public class GraphData implements Comparable<GraphData>{
	String coeffCombination;
	String filePath;
	double independentVariable;
		
	GraphData(String filePath, String coeffCombination, String PREFIX){
		this.filePath =  filePath;
		this.coeffCombination = coeffCombination;
	
		if (coeffCombination.startsWith(PREFIX)){ 
			independentVariable = Double.parseDouble(coeffCombination.substring(4));   //PREFIX must have 4 chars
		}
	}

	public String getCoeffCombination() {
		return coeffCombination;
	}

	public String getFilePath() {
		return filePath;
	}

	public double getTimePriority(){
		return this.independentVariable;	
	}
		
	@Override
	public int compareTo(GraphData otherGraphData) {
	    return Double.compare(independentVariable, otherGraphData.getTimePriority());
	}
}
	
