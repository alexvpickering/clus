/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.model.test;

import jeans.math.*;

import clus.main.*;
import clus.util.*;
import clus.statistic.*;
import clus.heuristic.*;
import clus.data.rows.*;
import clus.data.attweights.*;

public class TestSelector {
	
	public final static int TYPE_NONE = -1;
	public final static int TYPE_NUMERIC = 0;
	public final static int TYPE_TEST = 1;
	public final static int TYPE_INVERSE_NUMERIC = 2;

	// Statistics	
	public ClusStatistic m_TotStat;		// Points to total statistic of node
	public ClusStatistic m_TotCorrStat;	// Corrected total statistic	
	public ClusStatistic m_MissingStat;	// Points to last test statistic (see reset())
	public ClusStatistic m_PosStat;		// Points to m_TestStat[0] (see create())
	public ClusStatistic[] m_TestStat;	
	
	// Heuristic
	public ClusHeuristic m_Heuristic;
	public ClusAttributeWeights m_TargetWeights;

	// Best test information
	public NodeTest m_BestTest;	
	public int m_TestType;
	public double m_BestHeur;
	public double m_UnknownFreq;
	public ClusAttrProxy m_SplitAttr;
			
	// Cache for numeric attributes
	public double m_BestSplit;
	public double m_PosFreq;

	// Data set
	public RowData m_Subset;

/***************************************************************************
 * Reset
 ***************************************************************************/

	public String toString() {
		return m_PosStat.getString2();
	}

	public final boolean hasBestTest() {
		return m_TestType != TYPE_NONE;
	}
	
	public final String getTestString() {
		return m_BestTest.getString();
	}
	
	public final NodeTest updateTest() {
		if (m_TestType == TYPE_NUMERIC) {
			m_TestType = TYPE_TEST;
			m_BestTest = new NumericTest(m_SplitAttr.getType(), m_BestSplit, m_PosFreq);
		} else if (m_TestType == TYPE_INVERSE_NUMERIC) {
			m_TestType = TYPE_TEST;
			m_BestTest = new InverseNumericTest(m_SplitAttr.getType(), m_BestSplit, m_PosFreq);
		}
		m_BestTest.preprocess(ClusMode.DEPTH_FIRST);
		m_BestTest.setUnknownFreq(m_UnknownFreq);
		m_BestTest.setHeuristicValue(m_BestHeur);
		return m_BestTest;
	}
	
	public final void testToNode(ClusNode node) {
		node.setTest(updateTest());
	}

	public final void initTestSelector(ClusStatistic totstat, RowData subset) {
		initTestSelector(totstat);
		// Attach data set to heuristics and statistics
		for (int i = 0; i < m_TestStat.length; i++) {
			m_TestStat[i].setSDataSize(subset.getNbRows());
		}
		m_Heuristic.setData(subset);
		m_Subset = subset;
	}

	// Method for systems that do not support stats on data (like eff. xval.)	
	public final void initTestSelector(ClusStatistic totstat) {
		m_TotStat = totstat;
		resetBestTest();
	}
	
	public final void resetBestTest() {
		m_BestTest = null;
		m_TestType = TYPE_NONE;
		m_BestHeur = Double.NEGATIVE_INFINITY;
		m_UnknownFreq = 0.0;
		m_SplitAttr = null;		
	}

	public final void setBestHeur(double value) {
		m_BestHeur = value;
	}
	
	public final void reset(int nb) {
		for (int i = 0; i < nb; i++) {
			m_TestStat[i].reset();
		}
		m_MissingStat = m_TestStat[nb-1];
	}
	
	public final void reset() {
		m_PosStat.reset();
	}	
	
/***************************************************************************
 * Create statistics
 ***************************************************************************/

	public final void create(ClusStatManager smanager, int nbstat) throws ClusException {
		m_TotStat = null;	
		m_Heuristic = smanager.getHeuristic();
		m_TestStat = new ClusStatistic[nbstat];
		for (int i = 0; i < nbstat; i++) {
			m_TestStat[i] = smanager.createClusteringStat();
		}
		m_TargetWeights = smanager.createClusAttributeWeights();		
		m_TotCorrStat = smanager.createClusteringStat();		
		m_PosStat = m_TestStat[0];		
	}	
	
	public final void setHeuristic(ClusHeuristic heur) {
		m_Heuristic = heur;
	}
	
/***************************************************************************
 * Inspectors
 ***************************************************************************/
	
	public final double getPosWeight() {
		return m_PosStat.m_SumWeight;
	}
	
	public final double getTotWeight() {
		return m_TotStat.m_SumWeight;
	}

	public final double getTotNoUnkW() {
		return m_TotCorrStat.m_SumWeight;
	}
	
	public final void subtractMissing() {	
		m_TotCorrStat.subtractFromThis(m_MissingStat);
	}
	
	public final void copyTotal() {	
		m_TotCorrStat.copy(m_TotStat);	
	}	
	
	public final void calcPosFreq() {
		m_PosFreq = m_PosStat.m_SumWeight / m_TotStat.m_SumWeight;
	}
	
	public final ClusStatistic getStat(int i) {
		return m_TestStat[i];
	}
	
	public final ClusStatistic getPosStat() {
		return m_PosStat;
	}
	
	public final ClusStatistic getMissStat() {
		return m_MissingStat;
	}
	
	public final ClusStatistic getTotStat() {
		return m_TotStat;
	}	
	
/***************************************************************************
 * Stopping criterion
 ***************************************************************************/	

	public final boolean stopCrit() {
		// Similar to J48, stop if num instances smaller than 2*MIN_WT
		// or if subset if pure -> we test this based on the error
		boolean stop = false;
		if (m_TotStat.m_SumWeight < 2.0*Settings.MINIMAL_WEIGHT) stop = true;
		if (m_TotStat.getError(m_TargetWeights, m_Subset) < MathUtil.C1E_9) stop = true;
//		if (Settings.VERBOSE >= 2) {
//			System.out.print("Stop criterion (weight = "+m_TotStat.m_SumWeight);
//			System.out.print(", error = "+m_TotStat.getError(m_TargetWeights));
//			System.out.println(") result: "+stop);
//		}
		
		// Is it normal that the stopping criterion is completely handled by the heuristics?
		// I had a look in the cvs history and this appears to have been always the case
		// Note: this is also how Clus is presented in most papers, so it might be ok
		return false;
	}
	
/***************************************************************************
 * Nummeric splits
 ***************************************************************************/	

	// Where is this used?
	public final void updateNumeric(double val, ClusStatistic pos, ClusAttrProxy at) {
		double heur = m_Heuristic.calcHeuristic(m_TotCorrStat, pos, m_MissingStat);
		if (heur > m_BestHeur + ClusHeuristic.DELTA) {
			double tot_w = getTotWeight();
			double tot_no_unk = getTotNoUnkW();
			m_UnknownFreq = (tot_w - tot_no_unk) / tot_w;
			m_TestType = TYPE_NUMERIC;						
			m_PosFreq = pos.m_SumWeight / tot_no_unk;
			m_BestSplit = val;
			m_BestHeur = heur;
			m_SplitAttr = at;
		}
	}
	
	public final void updateNumeric(double val, ClusAttrProxy at) {
		double heur = m_Heuristic.calcHeuristic(m_TotCorrStat, m_PosStat, m_MissingStat);
		if (Settings.VERBOSE >= 2) System.err.println("Heur: " + heur + " nb: " + m_PosStat.m_SumWeight);
		if (heur > m_BestHeur + ClusHeuristic.DELTA) {
			if (Settings.VERBOSE >= 2) System.err.println("Better.");
			double tot_w = getTotWeight();
			double tot_no_unk = getTotNoUnkW();
			m_UnknownFreq = (tot_w - tot_no_unk) / tot_w;
			m_TestType = TYPE_NUMERIC;						
			m_PosFreq = getPosWeight() / tot_no_unk;
			m_BestSplit = val;			
			m_BestHeur = heur;
			m_SplitAttr = at;			
		}
//		System.out.println("Try: "+at+">"+ClusFormat.TWO_AFTER_DOT.format(val)+" -> "+heur);
//		DebugFile.log(""+at.getType().getName()+">"+ClusFormat.TWO_AFTER_DOT.format(val)+","+heur);
	}	

	public final void updateInverseNumeric(double val, ClusAttrProxy at) {
		double heur = m_Heuristic.calcHeuristic(m_TotCorrStat, m_PosStat, m_MissingStat);
		if (Settings.VERBOSE >= 2) System.err.println("Heur: " + heur + " nb: " + m_PosStat.m_SumWeight);
		if (heur > m_BestHeur + ClusHeuristic.DELTA) {
			if (Settings.VERBOSE >= 2) System.err.println("Better.");
			double tot_w = getTotWeight();
			double tot_no_unk = getTotNoUnkW();
			m_UnknownFreq = (tot_w - tot_no_unk) / tot_w;
			m_TestType = TYPE_INVERSE_NUMERIC;						
			m_PosFreq = getPosWeight() / tot_no_unk;
			m_BestSplit = val;			
			m_BestHeur = heur;
			m_SplitAttr = at;			
		}
	}	
		
/***************************************************************************
 * Heuristics
 ***************************************************************************/		
	
	public final double calcHeuristic(ClusStatistic stat) {
		return m_Heuristic.calcHeuristic(m_TotStat, stat, m_MissingStat);
	}	
	
	public final double calcHeuristic(ClusStatistic tot, ClusStatistic pos) {
		return m_Heuristic.calcHeuristic(tot, pos, m_MissingStat);
	}	
	
	public final double calcHeuristic(ClusStatistic tot, ClusStatistic[] set, int arity) {
		return m_Heuristic.calcHeuristic(tot, set, arity);
	}
	
	public final ClusHeuristic getHeuristic() {
		return m_Heuristic;
	}

/***************************************************************************
 * Statistics on data
 ***************************************************************************/	

	public final void setRootStatistic(ClusStatistic stat) {
		m_Heuristic.setRootStatistic(stat);
	}
	
	public final void statOnData(RowData data) {
		setSDataSize(data.getNbRows());
		m_Heuristic.setData(data);
	}
	
	private final void setSDataSize(int nbex) {
		m_TotStat.setSDataSize(nbex);
		int nbstat = m_TestStat.length;
		for (int i = 0; i < nbstat; i++) 
			m_TestStat[i].setSDataSize(nbex);
	}	
}
