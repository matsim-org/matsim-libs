/* *********************************************************************** *
 * project: org.matsim.*
 * DgRuns
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.util.List;

import playground.dgrether.koehlerstrehlersignal.analysis.DgAnalyseCottbusKS2010.RunInfo;

/**
 * @author dgrether
 * 
 */
public class CottbusRuns {

	static void add1712BaseCaseAnalysis(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1712";
		ri.remark = "base case";
		ri.iteration = 0;
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1712";
		ri.remark = "base case";
		ri.baseCase = true;
		ri.iteration = 1000;
		l.add(ri);
	}

	static void add1722BaseCaseAnalysis(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1722";
		ri.remark = "base case it 0";
		ri.iteration = 0;
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1722";
		ri.remark = "base case it 1000";
		ri.iteration = 1000;
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1740";
		ri.remark = "base case it 2000";
		ri.baseCase = true;
		ri.iteration = 2000;
		l.add(ri);
	}

	static void add1712BaseCaseRoutesTimesRuns(List<RunInfo> l) {
		RunInfo ri = null;
		// ri = new RunInfo();
		// ri.runId = "1712";
		// ri.remark = "base case";
		// ri.baseCase = true;
		// ri.iteration = 1000;
		// l.add(ri);

		ri = new RunInfo();
		ri.runId = "1745";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark = "base case 1712 it 2000";
		ri.remark = "base case";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1746";
		ri.iteration = 2000;
		ri.remark = "continue 1712, com > 10";
		ri.remark = "optimization, commodities > 10";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1747";
		ri.iteration = 2000;
		ri.remark = "continue 1712, com > 50";
		ri.remark = "optimization, commodities > 50";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1748";
		ri.iteration = 2000;
		ri.remark = "continue 1712, sylvia";
		ri.remark = "traffic-actuated control";
		l.add(ri);
	}

	static void add1712BaseCaseRoutesOnlyHighStorageCapRuns(List<RunInfo> l) {
		RunInfo ri = null;

		ri = new RunInfo();
		ri.runId = "1926";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark = "base case 1712 it 2000, routes only";
		ri.remark = "base case high storage cap";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1927";
		ri.iteration = 2000;
		ri.remark = "continue 1712, com > 10, routes only";
		ri.remark = "optimization, commodities > 10 high storage cap";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1928";
		ri.iteration = 2000;
		ri.remark = "continue 1712, com > 50, routes only";
		ri.remark = "optimization, commodities > 50 high storage cap";
		l.add(ri);
	}

	static void add1712BaseCaseRoutesOnlyHighStorageCapRunsOnSimplifiedNetwork(
			List<RunInfo> l) {
		RunInfo ri = null;

		ri = new RunInfo();
		ri.runId = "1933";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark = "base case 1712 it 2000, routes only, btu network and population";
		ri.remark = "base case high storage cap, simplified network and population";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1934";
		ri.iteration = 2000;
		ri.remark = "continue 1712, com > 10, routes only, btu network and population";
		ri.remark = "optimization, commodities > 10 high storage cap, simplified network and population";
		l.add(ri);
	}

	static void add1712BaseCaseRoutesTimesHighStorageCapRunsOnSimplifiedNetwork(
			List<RunInfo> l) {
		RunInfo ri = null;

		ri = new RunInfo();
		ri.runId = "1935";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark = "base case 1712 it 2000, route and time choice, btu network and population";
		ri.remark = "base case high storage cap, routes and times, simplified network and population";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1936";
		ri.iteration = 2000;
		ri.remark = "continue 1712, com > 10, route and time choice, btu network and population";
		ri.remark = "optimization, commodities > 10 high storage cap, routes and times, simplified network and population";
		l.add(ri);
	}

	static void add1712BaseCaseRoutesOnlyRuns(List<RunInfo> l) {
		RunInfo ri = null;
		// ri = new RunInfo();
		// ri.runId = "1712";
		// ri.remark = "base case";
		// // ri.baseCase = true;
		// ri.iteration = 1000;
		// l.add(ri);

		ri = new RunInfo();
		ri.runId = "1910";
		ri.iteration = 1900;
		ri.baseCase = true;
		ri.remark = "base case 1712 it 2000, routes only";
		ri.remark = "base case";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1911";
		ri.iteration = 1900;
		ri.remark = "continue 1712, com > 10, routes only";
		ri.remark = "optimization, commodities > 10";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1912";
		ri.iteration = 1900;
		ri.remark = "continue 1712, com > 50, routes only";
		ri.remark = "optimization, commodities > 50";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1913";
		ri.iteration = 1900;
		ri.remark = "continue 1712, sylvia, routes only";
		ri.remark = "traffic-actuated control";
		l.add(ri);
	}

	static void add1712BaseCaseRoutesOnlyRandomRuns(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1910";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark = "base case";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1912";
		ri.iteration = 1900;
		ri.remark = "continue 1712, com > 50, routes only";
		ri.remark = "optimization, commodities > 50";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1956";
		ri.iteration = 2000;
		ri.remark = "best random, commodities > 50";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1958";
		ri.iteration = 2000;
		ri.remark = "worst random, commodities > 50";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1960";
		ri.iteration = 2000;
		ri.remark = "median random, commodities > 50";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1962";
		ri.iteration = 2000;
		ri.remark = "mean random, commodities > 50";
		l.add(ri);
	}

	static void add1712BaseCaseRoutesOnlyRuns5Percent(List<RunInfo> l) {
		RunInfo ri = null;
		// ri = new RunInfo();
		// ri.runId = "1712";
		// ri.remark = "base case";
		// // ri.baseCase = true;
		// ri.iteration = 1000;
		// l.add(ri);

		ri = new RunInfo();
		ri.runId = "1918";
		ri.iteration = 1900;
		ri.baseCase = true;
		ri.remark = "base case 1712 it 2000, routes only";
		ri.remark = "no change";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1919";
		ri.iteration = 1900;
		ri.remark = "continue 1712, com > 10, routes only";
		ri.remark = "optimization, commodities $\\geq$ 10";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1920";
		ri.iteration = 1900;
		ri.remark = "continue 1712, com > 50, routes only";
		ri.remark = "optimization, commodities $\\geq$ 50";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1921";
		ri.iteration = 1900;
		ri.remark = "continue 1712, sylvia, routes only";
		ri.remark = "traffic-actuated control";
		l.add(ri);
	}

	static void add1930BaseCase(List<RunInfo> l) {
		RunInfo ri = null;

		ri = new RunInfo();
		ri.runId = "1722";
		ri.iteration = 1000;
		ri.baseCase = true;
		ri.remark = "base case 1722 it 2000, routes only";
		ri.remark = "no change";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1930";
		ri.iteration = 1000;
		ri.remark = "random offsets";
		l.add(ri);
	}

	static void add1712BaseCaseRoutesOnlyRunsBeta20(List<RunInfo> l) {
		RunInfo ri = null;
		// ri = new RunInfo();
		// ri.runId = "1712";
		// ri.remark = "base case";
		// // ri.baseCase = true;
		// ri.iteration = 1000;
		// l.add(ri);

		ri = new RunInfo();
		ri.runId = "1914";
		ri.iteration = 1500;
		ri.baseCase = true;
		ri.remark = "base case 1712 it 2000, routes only";
		ri.remark = "base case";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1915";
		ri.iteration = 1500;
		ri.remark = "continue 1712, com > 10, routes only";
		ri.remark = "optimization, commodities $\\geq$ 10";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1916";
		ri.iteration = 1500;
		ri.remark = "continue 1712, com > 50, routes only";
		ri.remark = "optimization, commodities $\\geq$ 50";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1917";
		ri.iteration = 1500;
		ri.remark = "continue 1712, sylvia, routes only";
		ri.remark = "traffic-actuated control";
		l.add(ri);
	}

	static void add1712BaseCaseNoChoice(List<RunInfo> l) {
		RunInfo ri = null;
		// ri = new RunInfo();
		// ri.runId = "1712";
		// ri.remark = "base case";
		// // ri.baseCase = true;
		// ri.iteration = 1000;
		// l.add(ri);

		ri = new RunInfo();
		ri.runId = "1910";
		ri.iteration = 1000;
		ri.baseCase = true;
		ri.remark = "base case 1712 it 1000, no choice";
		ri.remark = "base case";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1911";
		ri.iteration = 1000;
		ri.remark = "continue 1712, com > 10, no choice";
		ri.remark = "optimization, commodities > 10";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1912";
		ri.iteration = 1000;
		ri.remark = "continue 1712, com > 50, no choice";
		ri.remark = "optimization, commodities > 50";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1913";
		ri.iteration = 1000;
		ri.remark = "continue 1712, sylvia, no choice";
		ri.remark = "traffic-actuated control";
		l.add(ri);
	}

	static void add1722BaseCaseRoutesTimesRuns(List<RunInfo> l) {
		RunInfo ri = null;
		// ri = new RunInfo();
		// ri.runId = "1722";
		// ri.iteration = 1000;
		// ri.remark = "base case, 0.7 cap";
		// l.add(ri);

		ri = new RunInfo();
		ri.runId = "1740";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark = "base case 1722 it 2000";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1737";
		ri.iteration = 2000;
		ri.remark = "continue 1722, com > 10, new";
		ri.remark = "optimization, commodities > 10";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1741";
		ri.iteration = 2000;
		ri.runId = "1741";
		ri.remark = "sylvia: continue base case 1722 for 1000 iterations";
		ri.remark = "traffic-actuated control";
		l.add(ri);

		// ri = new RunInfo();
		// ri.runId = "1742";
		// ri.iteration = 1000;
		// ri.remark =
		// "start it 0: continue base case 1722 for 1000 iterations";
		// l.add(ri);
	}

	static void add1722BaseCaseRoutesOnlyRuns(List<RunInfo> l) {
		RunInfo ri = null;
		// ri = new RunInfo();
		// ri.runId = "1722";
		// ri.iteration = 1000;
		// ri.remark = "base case";
		// l.add(ri);

		ri = new RunInfo();
		ri.runId = "1900";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark = "base case 1722 it 2000, no time choice";
		ri.remark = "base case";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1901";
		ri.iteration = 2000;
		ri.remark = "continue 1722, com > 10, new, no time choice";
		ri.remark = "optimization, commodities > 10";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1902";
		ri.iteration = 2000;
		ri.remark = "sylvia: continue base case 1722 for 1000 iterations, no time choice";
		ri.remark = "traffic-actuated control";
		l.add(ri);
	}

	static void addReduceFlowCapacityRunsNoIterations(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1940";
		ri.iteration = 1000;
		ri.baseCase = true;
		ri.remark = "one it, base case, 0.5 cap";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1941";
		ri.iteration = 1000;
		ri.remark = "one it, base case, 0.3 cap";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1942";
		ri.iteration = 1000;
		ri.remark = "one it, base case, 0.1 cap";
		l.add(ri);
	}

	static void addStorageCapacityRuns(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1722";
		ri.iteration = 1000;
		ri.baseCase = true;
		ri.remark = "one it, base case, 0.7 storage cap";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1945";
		ri.iteration = 1000;
		ri.remark = "one it, base case, 0.5 storage cap";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1946";
		ri.iteration = 1000;
		ri.remark = "one it, base case, 0.3 storage cap";
		l.add(ri);

	}

	static void addFlowStorageCapacityRuns(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1722";
		ri.iteration = 1000;
		ri.baseCase = true;
		ri.remark = "one it, base case, 0.7 flow storage cap";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1947";
		ri.iteration = 1000;
		ri.remark = "one it, base case, 0.3 flow storage cap";
		// l.add(ri);

		ri = new RunInfo();
		ri.runId = "1950";
		ri.iteration = 1000;
		ri.remark = "iterated base case, 0.5 flow storage cap";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1951";
		ri.iteration = 1000;
		ri.remark = "iterated base case, 0.4 flow storage cap";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1952";
		ri.iteration = 1000;
		ri.remark = "iterated base case, 0.3 flow storage cap";
		l.add(ri);

	}

	static void addReduceFlowCapacityRunsIterationsRoutesOnlyFromBaseCase(
			List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1900";
		ri.baseCase = true;
		ri.iteration = 2000;
		ri.remark = "0.7 flow cap, routes only, base case, it 2000";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1943";
		ri.iteration = 2000;
		ri.remark = "0.5 flow cap, routes only, base case, it 2000";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1944";
		ri.iteration = 2000;
		ri.remark = "0.3 flow cap, routes only, base case, it 2000";
		l.add(ri);
	}

	static void add1726BaseCaseLongRerouteRuns(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1726";
		ri.remark = "base_case_more_learning";
		ri.iteration = 1000;
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1743";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark = "continue base case 1726 for 1000 iterations";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1744";
		ri.iteration = 1000;
		ri.remark = "start it 0: continue base case 1726 for 1000 iterations";
		l.add(ri);

	}

	static void add1722BaseCaseButKsModelBasedOn1712Runs(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1735";
		ri.iteration = 2000;
		ri.remark = "continue 1722, com > 50";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1736";
		ri.iteration = 2000;
		ri.remark = "continue 1722, com > 10";
		l.add(ri);
		ri = new RunInfo();
		ri.runId = "1740";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark = "base case 1722 it 2000";
		l.add(ri);
	}

	static void addReduceFlowCapacityRunsIterationsFromScratch(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1722";
		ri.remark = "base case, it 1000, 0.7 flow cap";
		ri.baseCase = true;
		ri.iteration = 1000;
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1724";
		ri.remark = "base case, it 1000, 0.5 flow cap";
		ri.iteration = 1000;
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1725";
		ri.remark = "base case, it 1000, 0.3 flow cap";
		ri.iteration = 1000;
		l.add(ri);
	}

	static void add1740vs1745BaseCaseAnalysis(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1745";
		ri.remark = "1712 base case, it 2000";
		ri.iteration = 2000;
		ri.baseCase = true;
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1740";
		ri.remark = "1722 base case, it 2000";
		ri.iteration = 2000;
		l.add(ri);
	}

	public static void add1933BaseCaseRoutesOnlyRandomRuns(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1933";
		ri.iteration = 2000;
		ri.baseCase = true;
		ri.remark = "base case";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1934";
		ri.iteration = 2000;
		ri.remark = "optimization scenario, com > 10, routes only";
		ri.remark = "optimization, commodities > 10";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1963";
		ri.iteration = 2000;
		ri.remark = "best random, commodities > 10";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1966";
		ri.iteration = 2000;
		ri.remark = "worst random, commodities > 10";
		l.add(ri);
		//
		ri = new RunInfo();
		ri.runId = "1965";
		ri.iteration = 2000;
		ri.remark = "median random, commodities > 10";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1964";
		ri.iteration = 2000;
		ri.remark = "mean random, commodities > 10";
		l.add(ri);

	}

	public static void add1972BaseCaseRoutesOnlyRandomRuns(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1972";
		ri.iteration = 1400;
		ri.baseCase = true;
		ri.remark = "base case";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1968";
		ri.iteration = 1400;
		ri.remark = "best random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1969";
		ri.iteration = 1400;
		ri.remark = "worst random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1976";
		ri.iteration = 1400;
		ri.remark = "avg random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1977";
		ri.iteration = 1400;
		ri.remark = "median random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1974";
		ri.iteration = 1400;
		ri.remark = "optimized"; // tt SP, min speed 15
		l.add(ri);
	}

	public static void add1973BaseCaseRoutesTimesRandomRuns(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1973";
		ri.iteration = 1400;
		ri.baseCase = true;
		ri.remark = "base case";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1970";
		ri.iteration = 1400;
		ri.remark = "best random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1971";
		ri.iteration = 1400;
		ri.remark = "worst random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1978";
		ri.iteration = 1400;
		ri.remark = "avg random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1979";
		ri.iteration = 1400;
		ri.remark = "median random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1975";
		ri.iteration = 1400;
		ri.remark = "optimized"; // tt SP, min speed 15
		l.add(ri);
	}

	public static void add1987BaseCaseRoutesOnlyRandomRuns(List<RunInfo> l) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = "1987";
		ri.iteration = 1400;
		ri.baseCase = true;
		ri.remark = "base case";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1983";
		ri.iteration = 1400;
		ri.remark = "best random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1984";
		ri.iteration = 1400;
		ri.remark = "worst random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1985";
		ri.iteration = 1400;
		ri.remark = "avg random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1986";
		ri.iteration = 1400;
		ri.remark = "median random"; // tt SP, min speed 15
		l.add(ri);

		ri = new RunInfo();
		ri.runId = "1980";
		ri.iteration = 1400;
		ri.remark = "optimized"; // tt SP, min speed 15
		l.add(ri);
	}

	public static void addBaseCaseAndOptIt1400Runs(List<RunInfo> l,
			int baseCaseRunId, int optRunId) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = Integer.toString(baseCaseRunId);
		ri.iteration = 1400;
		ri.baseCase = true;
		ri.remark = "base case";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = Integer.toString(optRunId);
		ri.iteration = 1400;
		ri.remark = "optimized";
		l.add(ri);
	}

	public static void addBaseCaseOptAndRandomIt1400Runs(List<RunInfo> l,
			int baseCaseRunId, int optRunId, int bestRandomId,
			int worstRandomId, int avgRandomId, int medRandomId) {
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = Integer.toString(baseCaseRunId);
		ri.iteration = 1400;
		ri.baseCase = true;
		ri.remark = "base case";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = Integer.toString(optRunId);
		ri.iteration = 1400;
		ri.remark = "optimized";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = Integer.toString(bestRandomId);
		ri.iteration = 1400;
		ri.remark = "best random";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = Integer.toString(worstRandomId);
		ri.iteration = 1400;
		ri.remark = "worst random";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = Integer.toString(avgRandomId);
		ri.iteration = 1400;
		ri.remark = "avg random";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = Integer.toString(medRandomId);
		ri.iteration = 1400;
		ri.remark = "median random";
		l.add(ri);
	}

	public static void addBaseCaseOptAndRandomAndAnotherIt1400Runs(
			List<RunInfo> l, int baseCaseRunId, int optRunId, int bestRandomId,
			int worstRandomId, int avgRandomId, int medRandomId,
			int anotherRunId) {

		addBaseCaseOptAndRandomIt1400Runs(l, baseCaseRunId, optRunId,
				bestRandomId, worstRandomId, avgRandomId, medRandomId);

		RunInfo ri = new RunInfo();
		ri.runId = Integer.toString(anotherRunId);
		ri.iteration = 1400;
		ri.remark = "another opt";
		l.add(ri);
	}

	public static void addBaseCaseOptAndOptFixed(List<RunInfo> l,
			int baseCaseRunId, int optRunId, int optFixedRoutesAndTimeId,
			int optFixedRoutesId) {

		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = Integer.toString(baseCaseRunId);
		ri.iteration = 1400;
		ri.baseCase = true;
		ri.remark = "base case";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = Integer.toString(optRunId);
		ri.iteration = 1400;
		ri.remark = "optimized";
		l.add(ri);
		
		ri = new RunInfo();
		ri.runId = Integer.toString(optFixedRoutesAndTimeId);
		ri.iteration = 1001;
		ri.remark = "fixed opt routes and time";
		l.add(ri);
		
		ri = new RunInfo();
		ri.runId = Integer.toString(optFixedRoutesId);
		ri.iteration = 1400;
		ri.remark = "fixed opt routes";
		l.add(ri);
	}
	
	public static void addBaseCaseOptAndOptFixedAndOptChoice(List<RunInfo> l,
			int baseCaseRunId, int optRunId, int optFixedRoutesAndTimeId,
			int optFixedRoutesId, int optChoiceRoutesFixedTimeId, int optChoiceRoutesId) {

		CottbusRuns.addBaseCaseOptAndOptFixed(l, baseCaseRunId, optRunId, optFixedRoutesAndTimeId, optFixedRoutesId);
		
		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = Integer.toString(optChoiceRoutesFixedTimeId);
		ri.iteration = 1400;
		ri.remark = "choice set opt routes. fixed time";
		l.add(ri);
		
		ri = new RunInfo();
		ri.runId = Integer.toString(optChoiceRoutesId);
		ri.iteration = 1400;
		ri.remark = "choice set opt routes";
		l.add(ri);
	}
	
	public static void addBaseCaseOptAndRouteChoice1400(List<RunInfo> l,
			int baseCaseRunId, int optRunId, int optRouteChoice) {

		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = Integer.toString(baseCaseRunId);
		ri.iteration = 1400;
		ri.baseCase = true;
		ri.remark = "base case";
		l.add(ri);

		ri = new RunInfo();
		ri.runId = Integer.toString(optRunId);
		ri.iteration = 1400;
		ri.remark = "optimized";
		l.add(ri);
		
		ri = new RunInfo();
		ri.runId = Integer.toString(optRouteChoice);
		ri.iteration = 1400;
		ri.remark = "opt routes";
		l.add(ri);
	}
	
	public static void addOptAndRouteChoice1400(List<RunInfo> l,
			int optRunId, int optRouteChoice) {

		RunInfo ri = null;
		ri = new RunInfo();
		ri.runId = Integer.toString(optRunId);
		ri.iteration = 1400;
		ri.baseCase = true;
		ri.remark = "optimized";
		l.add(ri);
		
		ri = new RunInfo();
		ri.runId = Integer.toString(optRouteChoice);
		ri.iteration = 1400;
		ri.remark = "opt routes";
		l.add(ri);
	}

}
