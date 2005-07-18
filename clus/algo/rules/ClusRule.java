/*
 * Created on May 1, 2005
 */
package clus.algo.rules;

import java.io.*;
import java.util.*;

import jeans.util.*;
import clus.data.rows.*;
import clus.main.*;
import clus.statistic.*;
import clus.model.test.*;
import clus.util.*;
import clus.data.type.*;
import clus.error.*;

public class ClusRule implements ClusModel, Serializable {
  
  public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

  protected int m_ID;
	protected Object m_Visitor;
	protected ClusStatistic m_Default;	
	protected ArrayList m_Tests = new ArrayList();
  /* Array of tuples covered by this rule */
  protected ArrayList m_Data = new ArrayList();
  protected ClusStatManager m_StatManager;
  /* Combined statistics for training and testing data */
  protected CombStat[] m_CombStat = new CombStat[2];
  /* Number of examples covered by this rule */
  protected double[] m_Coverage = new double[2];
  protected ClusErrorParent[] m_Errors;
	
  public ClusRule(ClusStatManager statManager) {
    m_StatManager = statManager;
  }
  
  	public int getID() {
  		return m_ID;
  	}
  	
  	public void setID(int id) {
  		m_ID = id;
  	}
  
	public ClusStatistic predictWeighted(DataTuple tuple) {
		return m_Default;
	}
	
	public void computePrediction() {
		m_Default.calcMean();
	}
	
	public ClusRule cloneRule() {
		ClusRule new_rule = new ClusRule(m_StatManager);
		for (int i = 0; i < getModelSize(); i++) {
			new_rule.addTest(getTest(i));
		}		
		return new_rule;
	}
	
	public boolean equals(Object other) {
		ClusRule o = (ClusRule)other;
		if (o.getModelSize() != getModelSize()) return false;
		for (int i = 0; i < getModelSize(); i++) {
			boolean has_test = false;
			for (int j = 0; j < getModelSize() && !has_test; j++) {
				if (getTest(i).equals(o.getTest(j))) has_test = true;
			}
			if (!has_test) return false;
		}
		return true;
	}
	
	public int hashCode() {
		int hashCode = 1234;
		for (int i = 0; i < getModelSize(); i++) {
			hashCode += getTest(i).hashCode();
		}
		return hashCode;
	}
	
	public boolean covers(DataTuple tuple) {
		for (int i = 0; i < getModelSize(); i++) {
			NodeTest test = getTest(i);
			int res = test.predictWeighted(tuple);
			if (res != ClusNode.YES) return false;
		}
		return true;
	}
	
	public void simplify() {
		for (int i = getModelSize()-1; i >= 0; i--) {
			boolean found = false;
			NodeTest test_i = getTest(i);
			for (int j = 0; j < i && !found; j++) {
				NodeTest test_j = getTest(j);
				NodeTest simplify = test_j.simplifyConjunction(test_i);
				if (simplify != null) {
					setTest(j, simplify);
					found = true;
				}				
			}
			if (found) removeTest(i);
		}
	}
		
  public RowData removeCovered(RowData data) {
    int covered = 0;
    for (int i = 0; i < data.getNbRows(); i++) {
      DataTuple tuple = data.getTuple(i);
      if (covers(tuple)) covered++;
    }
    int idx = 0;
    RowData res = new RowData(data.getSchema(), data.getNbRows()-covered);
    for (int i = 0; i < data.getNbRows(); i++) {
      DataTuple tuple = data.getTuple(i);
      if (!covers(tuple)) res.setTuple(tuple, idx++);
    }
    return res;
  }

  /**
   * Removes the examples that have been covered by enough rules, i.e.,
   * have low enough weights.
   * @param data
   * @return data
   */
  public RowData removeCoveredEnough(RowData data) {
    int MAX_TIMES_COVERED = 3;
    int threshold = 1/(MAX_TIMES_COVERED+1+1);
    int covered = 0;
    for (int i = 0; i < data.getNbRows(); i++) {
      DataTuple tuple = data.getTuple(i);
      if (covers(tuple) && (tuple.m_Weight < threshold)) covered++;
    }
    int idx = 0;
    RowData res = new RowData(data.getSchema(), data.getNbRows()-covered);
    for (int i = 0; i < data.getNbRows(); i++) {
      DataTuple tuple = data.getTuple(i);
      if (!(covers(tuple) && (tuple.m_Weight < threshold))) res.setTuple(tuple, idx++);
    }
    return res;
  }
  
  /**
   * Reweighs all the examples covered by this rule. Reweighting
   * method can be additive (w=1/(1+i)) or multiplicative (w=gamma^i).
   * See Lavrac et al. 2004: Subgroup discovery with CN2-SD.
   * 
   * TODO: reweigh only positive examples, what to do for regression???
   * 
   * @param data all examples
   * @return data with reweighted examples
   * @throws ClusException 
   */
  public RowData reweighCovered(RowData data) throws ClusException {
    int method = getSettings().getCoveringMethod();
    double gamma = getSettings().getCoveringWeight();
    double newweight;
    RowData result = new RowData(data.getSchema(), data.getNbRows());
    for (int i = 0; i < data.getNbRows(); i++) {
      DataTuple tuple = data.getTuple(i);
      double oldweight = tuple.getWeight();
      if (oldweight > 0) {
        if (method == Settings.COVERING_METHOD_WEIGHTED_ADDITIVE) {
          double olditer = (oldweight != 1) ? ((1-oldweight)/oldweight) : 0;
          newweight = 1/(olditer+1+1);
        } else if (method == Settings.COVERING_METHOD_WEIGHTED_MULTIPLICATIVE) {
          newweight = oldweight*gamma;
        } else {
          throw new ClusException("Unsupported covering method!");
        }
        if (covers(tuple)) {
          result.setTuple(tuple.changeWeight(newweight), i);
        } else {
          result.setTuple(tuple, i);          
        }
      } else if (oldweight < 0){
        throw new ClusException("Negative example weights not supported!");
      }
    }
    return removeCoveredEnough(result);
  }
  
	public ClusStatistic getTotalStat() {
		return m_Default;
	}
	
	public void setVisitor(Object visitor) {
		m_Visitor = visitor;
	}
	
	public Object getVisitor() {
		return m_Visitor;
	}	
	
	public void applyModelProcessors(DataTuple tuple, MyArray mproc) throws IOException {
	}
	
	public void attachModel(Hashtable table) throws ClusException {
		for (int i = 0; i < m_Tests.size(); i++) {
			NodeTest test = (NodeTest)m_Tests.get(i);
			test.attachModel(table);
		}
	}
	
	public void printModel() {
		PrintWriter wrt = new PrintWriter(System.out);
		printModel(wrt);
		wrt.flush();
	}
	
	public void printModel(PrintWriter wrt) {
		wrt.print("IF ");
		if (m_Tests.size() == 0) {
			wrt.print("true");
		} else {
			for (int i = 0; i < m_Tests.size(); i++) {
				NodeTest test = (NodeTest)m_Tests.get(i);
				if (i != 0) {
					wrt.println(" AND");
					wrt.print("   ");
				}
				wrt.print(test.getString());
			}
		}
		wrt.println();
		wrt.print("THEN "+m_Default.getString());
		if (getID() != 0) wrt.println(" ("+getID()+")");
		else wrt.println();		
		String extra = m_Default.getExtraInfo();
		if (extra != null) {
			// Used, e.g., in hierarchical multi-classification
			wrt.println();
			wrt.print(extra);
		}
    if (getSettings().computeCompactness() && (m_CombStat[ClusModel.TRAIN] != null)) {
      wrt.println("\n   Compactness (train): " + m_CombStat[ClusModel.TRAIN].getCompactnessString());
      wrt.println("   Coverage    (train): " + m_Coverage[ClusModel.TRAIN]);
      wrt.println("   Cover*Comp  (train): " + (m_CombStat[ClusModel.TRAIN].compactnessCalc()*m_Coverage[ClusModel.TRAIN]));
      if (m_CombStat[ClusModel.TEST] != null) {
        wrt.println("   Compactness (test):  " + m_CombStat[ClusModel.TEST].getCompactnessString());
        wrt.println("   Coverage    (test):  " + m_Coverage[ClusModel.TEST]);
        wrt.println("   Cover*Comp  (test):  " + (m_CombStat[ClusModel.TEST].compactnessCalc()*m_Coverage[ClusModel.TEST]));
      }
    }
    if (hasErrors()) {
    	  // Enable with setting PrintRuleWiseErrors = Yes
    		ClusErrorParent train_err = getError(ClusModel.TRAIN);
    		if (train_err != null) {
    			wrt.println();
    			wrt.println("Training error");
    			train_err.showError(wrt);    			
    		}
    		ClusErrorParent test_err = getError(ClusModel.TEST);
    		if (test_err != null) {
    			wrt.println();
    			wrt.println("Testing error");
    			test_err.showError(wrt);    			
    		}    		
    }
	}

  public Settings getSettings() {
    return m_StatManager.getSettings();
  }
  
	public boolean isEmpty() {
		return getModelSize() == 0;
	}
	
	public int getModelSize() {
		return m_Tests.size();
	}
	
	public NodeTest getTest(int i) {
		return (NodeTest)m_Tests.get(i);
	}
	
	public void setTest(int i, NodeTest test) {
		m_Tests.set(i, test);
	}
	
	public void addTest(NodeTest test) {
		m_Tests.add(test);
	}
	
	public void removeTest(int i) {
		m_Tests.remove(i);
	}
	
	public void setDefaultStat(ClusStatistic def) {
		m_Default = def;
	}
	
	public void postProc() {
		m_Default.calcMean();
	}
	
	public String getModelInfo() {
		return "Tests = "+getModelSize();
	}	
  
  /**
   * Computes the compactness of data tuples covered by this rule.
   * @param mode 0 for train set, 1 for test set
   */
  public void computeCompactness(int mode) {
    CombStat combStat = (CombStat)m_StatManager.createStatistic(ClusAttrType.ATTR_USE_ALL);
    for (int i = 0; i < m_Data.size(); i++) {
      combStat.updateWeighted((DataTuple)m_Data.get(i), 0); // second parameter does nothing!
    }
    combStat.calcMean();
    m_CombStat[mode] = combStat;
    // save the coverage
    m_Coverage[mode] = m_Data.size();
  }

  /**
   * Adds the tuple to the m_Data array
   * @param tuple
   */
  public void addDataTuple(DataTuple tuple) {
    m_Data.add(tuple);
  }

  /**
   * Removes the data tuples from the m_Data array
   */
  public void removeDataTuples() {
    m_Data.clear();
  }
  
  public ArrayList getData() {
  	return m_Data;
  }

  /**
   * For computation of rule-wise error measures
   */
  public void setError(ClusErrorParent error, int subset) {
  	if (m_Errors == null) m_Errors = new ClusErrorParent[2];
  	m_Errors[subset] = error;
  }
  
  public ClusErrorParent getError(int subset) {
  	if (m_Errors == null) return null;
  	return m_Errors[subset];
  }
  
  public boolean hasErrors() {
  	return m_Errors != null;
  }

  public void computeCoverStat(RowData data, ClusStatistic stat) {
  	int nb = data.getNbRows();
		stat.setSDataSize(nb);
		for (int i = 0; i < nb; i++) {
			DataTuple tuple = data.getTuple(i);
			if (covers(tuple)) {
				stat.updateWeighted(tuple, i);
			}
		}
		stat.optimizePreCalc(data);
  }
}
