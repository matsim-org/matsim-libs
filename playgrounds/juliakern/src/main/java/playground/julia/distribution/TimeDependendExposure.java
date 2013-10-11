package playground.julia.distribution;

import org.apache.log4j.Logger;

public class TimeDependendExposure {
	
	Double startTime;
	Double endTime;
	Double exposureValue;
	String actType;
	private static final Logger logger = Logger.getLogger(TimeDependendExposure.class.getSuperclass());
	
	public TimeDependendExposure(Double startTime, Double endTime, Double exposureValue, String actType){
		this.startTime=startTime;
		this.endTime=endTime;
		this.exposureValue=exposureValue;
		this.actType=actType;
//		if(exposureValue==null){
//			logger.warn("Exposure value not set, will be handled as zero.");
//			this.exposureValue=0.0;
//		}else{
//			if(exposureValue<0.0){
//				logger.warn("Exposure value is smaller than 0.0, will be handled as 0.0.");
//				this.exposureValue=0.0;
//			}
//		}
		if(endTime!=null && startTime!=null && endTime-startTime<=0.0){
			logger.warn("Start time is later than end time!");
		}
	}
	
	public Double getDuration(){
		return (endTime-startTime);
	}
	
	public Double getAverageExposure(){
		return(exposureValue/(this.getDuration()));
	}
	
	public Double getStartTime() {
		return startTime;
	}
	public void setStartTime(Double startTime) {
		this.startTime = startTime;
	}
	public Double getEndTime() {
		return endTime;
	}
	public void setEndTime(Double endTime) {
		this.endTime = endTime;
	}
	public Double getExposureValue() {
		return exposureValue;
	}
	public void setExposureValue(Double exposureValue) {
		this.exposureValue = exposureValue;
	}

	public String getType() {
		return actType;
	}

}
