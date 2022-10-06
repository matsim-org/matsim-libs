/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.cadyts.utils;

import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cadyts.utilities.io.tabularFileParser.TabularFileHandler;

/**Reader for the calibration-stat files produced after calibration.**/
public class CalibrationStatReader implements TabularFileHandler {

	private static final Logger log = LogManager.getLogger(CalibrationStatReader.class);

	private static final String[] HEADER = {"count-ll", "count-ll-pred-err", "p2p-ll", "total-ll", "link-lambda-avg", "link-lambda-stddev", "link-lambda-min", "link-lambda-max", "plan-lambda-avg", "plan-lambda-stddev", "plan-lambda-min", "plan-lambda-max", "replan-count"};
	private int rowNum =0;
	private final Map <Integer, StatisticsData> statDataMap = new TreeMap <Integer, StatisticsData>();

	public CalibrationStatReader(){
	}

	@Override
	public void startDocument() {
		log.info("reading calibration-stats file...");
	}

	@Override
	public void startRow(final String[] row)  {
		String colArray[] = row[0].split("\t");
		int col= 0;
		if (this.rowNum > 0) {
			StatisticsData sData = new StatisticsData();
			for (String s : colArray) {
				switch (col) {
		        	case 0:  sData.setCount_ll(s); break;
		            case 1:  sData.setCount_ll_pred_err(s); break;
		            case 2:  sData.setP2p_ll(s); break;
		            case 3:  sData.setTotal_ll(s); break;
		            case 4:  sData.setLink_lambda_avg(s); break;
		            case 5:  sData.setLink_lambda_stddev(s); break;
		            case 6:  sData.setLink_lambda_min(s); break;
		            case 7:  sData.setLink_lambda_max(s); break;
		            case 8:  sData.setPlan_lambda_avg(s); break;
		            case 9: sData.setPlan_lambda_stddev(s); break;
		            case 10: sData.setPlan_lambda_min(s); break;
		            case 11: sData.setPlan_lambda_max(s); break;
		            case 12: sData.setReplan_count(s); break;
				}//switch
				col++;
			}
			this.statDataMap.put(this.rowNum, sData);
		}else{
			boolean equalsHeader = true;
			for (String s : colArray) {
				if (!s.equalsIgnoreCase(HEADER[col])){
					equalsHeader = false;
					break;
				}
				col++;
			}
			if (!equalsHeader) {
				log.warn("the structure does not match. The header should be:  ");
				for (String g : HEADER) {
					log.warn(g + "\t");
				}
			}
		}
		this.rowNum++;
	}

	@Override
	public void endDocument() {
		log.info("done.");
	}

	public static class StatisticsData {
		private String count_ll;
		private String count_ll_pred_err;
		private String p2p_ll;
		private String total_ll;
		private String link_lambda_avg;
		private String link_lambda_stddev;
		private String link_lambda_min;
		private String link_lambda_max;
		private String plan_lambda_avg;
		private String plan_lambda_stddev;
		private String plan_lambda_min;
		private String plan_lambda_max;
		private String replan_count;

		protected void setCount_ll(final String countLl) {
			this.count_ll = countLl;
		}

		protected void setCount_ll_pred_err(final String countLlPredErr) {
			this.count_ll_pred_err = countLlPredErr;
		}

		protected void setP2p_ll(final String p2pLl) {
			this.p2p_ll = p2pLl;
		}

		protected void setTotal_ll(final String totalLl) {
			this.total_ll = totalLl;
		}

		protected void setLink_lambda_avg(final String linkLambdaAvg) {
			this.link_lambda_avg = linkLambdaAvg;
		}

		protected void setLink_lambda_stddev(final String linkLambdaStddev) {
			this.link_lambda_stddev = linkLambdaStddev;
		}

		protected void setLink_lambda_min(final String linkLambdaMin) {
			this.link_lambda_min = linkLambdaMin;
		}

		protected void setLink_lambda_max(final String linkLambdaMax) {
			this.link_lambda_max = linkLambdaMax;
		}

		protected void setPlan_lambda_avg(final String planLambdaAvg) {
			this.plan_lambda_avg = planLambdaAvg;
		}

		protected void setPlan_lambda_stddev(final String planLambdaStddev) {
			this.plan_lambda_stddev = planLambdaStddev;
		}

		protected void setPlan_lambda_min(final String planLambdaMin) {
			this.plan_lambda_min = planLambdaMin;
		}

		protected void setPlan_lambda_max(final String planLambdaMax) {
			this.plan_lambda_max = planLambdaMax;
		}

		protected void setReplan_count(final String replanCount) {
			this.replan_count = replanCount;
		}

		public String getCount_ll() {
			return this.count_ll;
		}

		public String getCount_ll_pred_err() {
			return this.count_ll_pred_err;
		}

		public String getP2p_ll() {
			return this.p2p_ll;
		}

		public String getTotal_ll() {
			return this.total_ll;
		}

		public String getLink_lambda_avg() {
			return this.link_lambda_avg;
		}

		public String getLink_lambda_stddev() {
			return this.link_lambda_stddev;
		}

		public String getLink_lambda_min() {
			return this.link_lambda_min;
		}

		public String getLink_lambda_max() {
			return this.link_lambda_max;
		}

		public String getPlan_lambda_avg() {
			return this.plan_lambda_avg;
		}

		public String getPlan_lambda_stddev() {
			return this.plan_lambda_stddev;
		}

		public String getPlan_lambda_min() {
			return this.plan_lambda_min;
		}

		public String getPlan_lambda_max() {
			return this.plan_lambda_max;
		}

		public String getReplan_count() {
			return this.replan_count;
		}
	}

	public Map <Integer, StatisticsData> getCalStatMap (){
		return this.statDataMap;
	}

	public String preprocess(final String arg0) {
		return arg0;
	}


}