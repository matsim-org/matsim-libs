/**
 * 
 */
package playground.yu.utils.math;


/**
 * just a naive method to judge whether a double array would probably be
 * convergent a foreseeable while
 * 
 * @author yu
 * 
 */
public class WouldBeSoonConvergent {
	/**
	 * @param amplitudeCriterion
	 *            criterion for the difference between the highest and lowest
	 *            value in each half of the array, the absolute value of the
	 *            second difference may be smaller than the first difference *
	 *            this amplitudeCriterion
	 * @param avgValueCriterion
	 *            the average value of the second half of the array may not
	 *            exceed a rang of +/- avgValueCriterion with the average value
	 *            of the first half as the center, should stand in the range of
	 *            (0,1)
	 * @param values
	 *            a double array, please had better ensure that the array length
	 *            is a even number, should be a positive value <0.2
	 * @return
	 */
	public static boolean wouldBe(double amplitudeCriterion,
			double avgValueCriterion, double[] values) {

		int size1 = values.length / 2;

		double min1 = SimpleStatistics.min(values, 0, size1 - 1)//
		, max1 = SimpleStatistics.max(values, 0, size1 - 1)//
		, min2 = SimpleStatistics.min(values, size1, values.length - 1)//
		, max2 = SimpleStatistics.max(values, size1, values.length - 1);

		boolean firstCondition = Math.abs(max1 - min1) <= amplitudeCriterion
				* Math.abs(max1 - min1);

		if (firstCondition) {
			return true;
		} else {
			double avg1 = SimpleStatistics.average(values, 0, size1 - 1)//
			, avg2 = SimpleStatistics.average(values, size1, values.length - 1);

			boolean secondCondition = false;
			if (avg1 != 0d) {
				secondCondition = Math.abs((avg2 - avg1) / avg1) <= avgValueCriterion;
			} else {
				secondCondition = Math.abs(avg2 - avg1) <= avgValueCriterion;
			}

			return secondCondition;
		}
	}
}
