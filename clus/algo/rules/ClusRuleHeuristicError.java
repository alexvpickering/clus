/*
 * Created on May 2, 2005
 */
package clus.algo.rules;

import clus.heuristic.*;
import clus.main.Settings;
import clus.statistic.ClusStatistic;
import clus.data.attweights.*;

public class ClusRuleHeuristicError extends ClusHeuristic {

	private ClusAttributeWeights m_TargetWeights;
	
	public ClusRuleHeuristicError(ClusAttributeWeights prod) {
		m_TargetWeights = prod;
	}
	
	public double calcHeuristic(ClusStatistic c_tstat, ClusStatistic c_pstat, ClusStatistic missing) {
		double n_pos = c_pstat.m_SumWeight;
		// Acceptable?
		if (n_pos < Settings.MINIMAL_WEIGHT) {
			return Double.NEGATIVE_INFINITY;
		}
		double pos_error = c_pstat.getError(m_TargetWeights);
		return -pos_error;
	}
		
	public String getName() {
		return "Rule Heuristic (Reduced Error)";
	}
}
