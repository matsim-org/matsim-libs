Because I keep forgetting it:
* (Note: "measurement" is from reality, "feature" is from the simulation.)
* The correction per measurement item is
```java
public double SingleLinkMeasurement#getLambdaCoefficient(final L link) {
	double result = this.dll_dAvgLinkFeature() 
	     * this.loading.get_dLinkFeature_dDemand(link);
	return result;
}
```
* The second term, `dLinkFeature_dDemand`, is how the feature changes when the plan is used or not.  
This is often just "1", since selecting the plan increases the count by one.
* The first term is how the lambda (which is the cadyts correction for this measurement) changes when the 
feature value changes.  It is computed as
```java
protected double SingleLinkMeasurement#dll_dAvgLinkFeature() {
	return (this.getMeasValue() - this.avgLinkFeature.getSmoothedValue())
			/ this.getMeasVariance();
}
```
* `measValue` is just the measured value. 
* The smoothed value is somehow computed over several iterations.
* The variance can be controlled individually for each measurement by setting its stddev.  
* If that functionality is not used, it is computed in `Calibrator#addMeasurement(...)` as
```java
	final double stddev = max(this.getMinStddev(type), 
	    sqrt(this.getVarianceScale() * value));
}
```
_*(Note that the cadyts v1 documentation here writes `min` instead of `max`.)*_
That is, by default the stddev is propto the sqrt of the measurement value, except if the resulting value is too small.  
The `minStddev` default is 25 (I think); the default variance scale is 1. 


The overall consequence of this is that the correction is propto something like
```
(measValue - simValue) / sqrt[ max( measValue, 625 ) ] 
```  
That has, in particular, the consequence that this is totally asymmetric:
1. If `simValue` is much smaller than `measValue`, then the correction term is essentially `sqrt( measValue )`.
2. If `simValue` is much larger than `measValue`, then the correction term is `- simValue / sqrt( measValue )'.

In case 1, the result is roughly constant no matter if sim value is 10% or 1% of the measurement.

In case 2, the result is roughly proportional to the simValue.

So simValues much larger than meaValues get a much stronger pull towards the meaValue than simValues much 
smaller than the meaValue.

This is not good for modal distance calibration, since we start with not enough short walks, but because there is
not enough pull, this does not change.

What seems to help is to not use the default, but set the stddev to always the same value for every distance bin entry. 
