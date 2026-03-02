/**
 * se.vti.atap
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.atap.minimalframework.examples.parallel_links;

/**
 * 
 * @author GunnarF
 *
 */
public class Network {

	private static final double defaultEpsilon_veh = 1.0;
	private static final double defaultAlpha = 0.15;
	private static final double defaultBeta = 4.0;

	public final double epsilon_veh;

	public final double[] t0_s;
	public final double[] cap_veh;
	public final double[] alpha;
	public final double[] beta;

	public Network(int numberOfLinks, double epsilon_veh) {
		this.epsilon_veh = epsilon_veh;
		this.t0_s = new double[numberOfLinks];
		this.cap_veh = new double[numberOfLinks];
		this.alpha = new double[numberOfLinks];
		this.beta = new double[numberOfLinks];
	}

	public Network(int numberOfLinks) {
		this(numberOfLinks, defaultEpsilon_veh);
	}

	public Network setBPRParameters(int link, double t0_s, double cap_veh, double alpha, double beta) {
		this.t0_s[link] = t0_s;
		this.cap_veh[link] = cap_veh;
		this.alpha[link] = alpha;
		this.beta[link] = beta;
		return this;
	}
	
	public Network setBPRParameters(int link, double t0_s, double cap_veh) {
		this.setBPRParameters(link, t0_s, cap_veh, defaultAlpha, defaultBeta);
		return this;
	}
	
	public Network setAllBPRParameters(double t0_s, double cap_veh) {
		for (int link = 0; link < this.getNumberOfLinks(); link++) {
			this.setBPRParameters(link, t0_s, cap_veh);
		}
		return this;
	}	

	public int getNumberOfLinks() {
		return this.t0_s.length;
	}

	public double computeLinkTravelTime_s(int link, double flow_veh) {
		return this.t0_s[link] * (1.0
				+ this.alpha[link] * Math.pow((flow_veh + this.epsilon_veh) / this.cap_veh[link], this.beta[link]));
	}

	public double compute_dLinkTravelTime_dLinkFlow_s_veh(int link, double flow_veh) {
		return this.t0_s[link] * this.alpha[link] * this.beta[link]
				* Math.pow((flow_veh + this.epsilon_veh) / this.cap_veh[link], this.beta[link] - 1.0)
				/ this.cap_veh[link];
	}
}
