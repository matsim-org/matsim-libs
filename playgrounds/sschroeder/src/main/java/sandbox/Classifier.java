package sandbox;

import java.util.List;

import org.apache.commons.math.stat.Frequency;
import org.apache.log4j.Logger;

public class Classifier {
	
	public static class FrequencyClass {
		private int id;
		
		private Double start;
		
		private Double end;

		public FrequencyClass(int id, Double start, Double end) {
			super();
			this.start = start;
			this.end = end;
			this.id = id;
		}

		
		public int getId() {
			return id;
		}


		public Double getStart() {
			return start;
		}

		public Double getEnd() {
			return end;
		}


		public boolean hasValue(double d) {
			if(end == null){
				if(d >= start){
					return true;
				}
			}
			else if(d >= start && d < end){
				return true;
			}
			return false;
		}
		
		
	}
	
	private static Logger logger = Logger.getLogger(Classifier.class);
	
	private double[] values;
	
	public Classifier(double[] values) {
		super();
		this.values = values;
	}
	
	public static Frequency makeConditionalFrequency(FrequencyClass frequencyClass, double[] referenceDistribution, double[] distributionConditionalToReference){
		Frequency frequency = new Frequency();
		for(int i=0;i<referenceDistribution.length;i++){
			if(frequencyClass.hasValue(referenceDistribution[i])){
				frequency.addValue(distributionConditionalToReference[i]);
			}
		}
		return frequency;
	}
	
	public Frequency makeFrequency(List<FrequencyClass> classes){
		Frequency frequency = new Frequency();
		int countNullValues = 0;
		for(int i=0;i<values.length;i++){
			FrequencyClass c = getClass(values[i],classes);
			if(c == null){
				countNullValues++;
				continue;
			}
			frequency.addValue(c.getId());
		}
		logger.info(countNullValues + " items could not be assigned");
		return frequency;
	}
	
	public double[] classify(List<FrequencyClass> classes){
		double[] classArr = new double[classes.size()];
		int countNullValues = 0;
		for(int i=0;i<values.length;i++){
			FrequencyClass c = getClass(values[i],classes);
			if(c == null){
				countNullValues++;
				continue;
			}
			int index = classes.indexOf(c);
			classArr[index] += 1.0;
		}
		logger.info(countNullValues + " items could not be assigned");
		return classArr;
	}
	
	private FrequencyClass getClass(double d, List<FrequencyClass> classes) {
		for(FrequencyClass c : classes){
			if(c.hasValue(d)){
				return c;
			}
		}
		return null;
	}

	public double[] classify(int nOfClasses, int classLength){
		double[] classifiedValues = new double[10];
		for(int i=0;i<values.length;i++){
			double rest = values[i]/(double)classLength;
			int classNumber = (int) Math.floor(rest);
			if(classNumber < 0){
				logger.warn("nOfVehicles < 0");
				continue;
			}
			if(classNumber < nOfClasses-1){
				classifiedValues[classNumber] += 1.0;
			}
			else{
				classifiedValues[nOfClasses-1] += 1.0;
			}
		}
		return classifiedValues;
	}
	
	
	
	

}
