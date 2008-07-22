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

package clus.algo.split;

import java.util.Random;

import clus.data.rows.*;
import clus.data.type.*;
import clus.main.*;
import clus.statistic.*;
import clus.util.*;

public class FindBestTest {

	protected CurrentBestTestAndHeuristic m_BestTest = new CurrentBestTestAndHeuristic();
	protected ClusStatManager m_StatManager;	
	protected NominalSplit m_Split;
	protected int m_MaxStats;	
	
	public FindBestTest(ClusStatManager mgr) {
		m_StatManager = mgr;
		m_MaxStats = getSchema().getMaxNbStats();
	}	
		
	public FindBestTest(ClusStatManager mgr, NominalSplit split) {
		m_StatManager = mgr;
		m_Split = split;
		m_MaxStats = getSchema().getMaxNbStats();
	}
	
	public ClusSchema getSchema() {
		return getStatManager().getSchema();
	}
	
	public ClusStatManager getStatManager() {
		return m_StatManager;
	}
	
	public Settings getSettings() {
		return getStatManager().getSettings();
	}	
		
	public CurrentBestTestAndHeuristic getBestTest() {
		return m_BestTest;
	}
	
	public void cleanSplit() {
		m_Split = null;
	}
			
	public void findNominal(NominalAttrType at, RowData data) {
		// Reset positive statistic
		int nbvalues = at.getNbValues();
		m_BestTest.reset(nbvalues + 1);
		int nb_rows = data.getNbRows();
		// For each attribute value   
		for (int i = 0; i < nb_rows; i++) {
			DataTuple tuple = data.getTuple(i);
			int value = at.getNominal(tuple);     
			m_BestTest.m_TestStat[value].updateWeighted(tuple, i);      
		}
		// Find best split
		m_Split.findSplit(m_BestTest, at);
	}
  
	public void findNominalRandom(NominalAttrType at, RowData data, Random rn) {
		// Reset positive statistic
		int nbvalues = at.getNbValues();
		m_BestTest.reset(nbvalues + 1);
		// For each attribute value   
		int nb_rows = data.getNbRows();
		for (int i = 0; i < nb_rows; i++) {
			DataTuple tuple = data.getTuple(i);
			int value = at.getNominal(tuple);     
			m_BestTest.m_TestStat[value].updateWeighted(tuple, i);      
		}
		// Find the split
		m_Split.findRandomSplit(m_BestTest, at, rn);
	}

	public void findNumeric(NumericAttrType at, RowData data) { 
		DataTuple tuple;
		if (at.isSparse()) {
			data.sortSparse(at);
		} else {
			data.sort(at);
		}
		m_BestTest.reset(2);    
		// Missing values
		int first = 0;        
		int nb_rows = data.getNbRows();
		// Copy total statistic into corrected total
		m_BestTest.copyTotal();
		if (at.hasMissing()) {
			// Because of sorting, all missing values are in the front :-)
			while (first < nb_rows && at.isMissing(tuple = data.getTuple(first))) {
				m_BestTest.m_MissingStat.updateWeighted(tuple, first);
				first++;
			}
			m_BestTest.subtractMissing();
		}   
		double prev = Double.NaN;
		for (int i = first; i < nb_rows; i++) {
			tuple = data.getTuple(i);
			double value = at.getNumeric(tuple);
			if (value != prev) {
				if (value != Double.NaN) {
					// System.err.println("Value (>): " + value);
					m_BestTest.updateNumeric(value, at);
				}
				prev = value;
			}       
			m_BestTest.m_PosStat.updateWeighted(tuple, i);
		}
	}

	public void findNumericRandom(NumericAttrType at, RowData data, RowData orig_data, Random rn) { 
		DataTuple tuple;
		int idx = at.getArrayIndex();
		// Sort values from large to small
		if (at.isSparse()) {
			data.sortSparse(at);
		} else {
			data.sort(at);
		}
		m_BestTest.reset(2);    
		// Missing values
		int first = 0;        
		int nb_rows = data.getNbRows();
		// Copy total statistic into corrected total
		m_BestTest.copyTotal();
		if (at.hasMissing()) {
			// Because of sorting, all missing values are in the front :-)
			while (first < nb_rows && (tuple = data.getTuple(first)).hasNumMissing(idx)) {
				m_BestTest.m_MissingStat.updateWeighted(tuple, first);
				first++;
			}
			m_BestTest.subtractMissing();
		}   
		// Do the same for original data, except updating the statistics:
		// Sort values from large to small
		if (at.isSparse()) {
			orig_data.sortSparse(at);
		} else {
			orig_data.sort(at);
		}
		// Missing values
		int orig_first = 0;        
		int orig_nb_rows = orig_data.getNbRows();
		if (at.hasMissing()) {
			// Because of sorting, all missing values are in the front :-)
			while (orig_first < orig_nb_rows && 
					(tuple = orig_data.getTuple(orig_first)).hasNumMissing(idx)) {
				orig_first++;
			}
		}   
		// Generate the random split value based on the original data
		double min_value = orig_data.getTuple(orig_nb_rows-1).getDoubleVal(idx);
		double max_value = orig_data.getTuple(orig_first).getDoubleVal(idx);
		double split_value = (max_value - min_value) * rn.nextDouble() + min_value;
		for (int i = first; i < nb_rows; i++) {
			tuple = data.getTuple(i);
			if (tuple.getDoubleVal(idx) <= split_value) break;
			m_BestTest.m_PosStat.updateWeighted(tuple, i);        
		}
		m_BestTest.updateNumeric(split_value, at);
		System.err.println("Inverse splits not yet included!");
		// TODO: m_Selector.updateInverseNumeric(split_value, at);
	}
  
	public void initSelectorAndSplit(ClusStatistic totstat) throws ClusException {
		m_BestTest.create(m_StatManager, m_MaxStats);
		m_BestTest.setRootStatistic(totstat);
		if (getSettings().isBinarySplit()) m_Split = new SubsetSplit();
		else m_Split = new NArySplit();
		m_Split.initialize(m_StatManager);	
	}
	
	public boolean initSelectorAndStopCrit(ClusStatistic total, RowData data) {
		m_BestTest.initTestSelector(total, data);
		m_Split.setSDataSize(data.getNbRows());
		return m_BestTest.stopCrit();
	}	
}
