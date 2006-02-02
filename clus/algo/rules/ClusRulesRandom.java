/*
 * Created on June 22, 2005
 */
package clus.algo.rules;

import java.io.*;

import clus.data.rows.RowData;
import clus.main.*;
import clus.util.*;

public class ClusRulesRandom {

  public ClusRulesRandom(ClusRun cr) {
   }

/**
  * Constructs the random rules.
  * 
  * @param cr ClusRun
  * @return rule set
  * @throws ClusException 
  * @throws IOException 
  */
  public ClusRuleSet constructRules(ClusRun run) throws IOException, ClusException {
 /*   RowData data = (RowData)run.getTrainingSet();
    ClusStatistic stat = m_Induce.createTotalClusteringStat(data);
    m_Induce.initSelectorAndSplit(stat);
    setHeuristic(m_Induce.getSelector().getHeuristic());
    ClusRuleSet rset = new ClusRuleSet(m_Induce.getStatManager());
    if (method == Settings.COVERING_METHOD_STANDARD) {
      separateAndConquor(rset, data);
    } else {
      separateAndConquorWeighted(rset, data);
    }
    rset.postProc();

   */ 
   /* 
    
    ClusRuleSet rset = new ClusRuleSet(cr.getStatManager());
    // ClusRule init = new ClusRule(cr.getStatManager());
    // constructRandomly(init, rset);
    constructRandomly(rset);
    rset.removeEmptyRules();
    rset.simplifyRules();
    // rset.setDefaultStat(node.getTotalStat());
    RowData data = (RowData)cr.getTrainingSet();
    RowData testdata;
    rset.addDataToRules(data);
    rset.computeCompactness(ClusModel.TRAIN);
    rset.removeDataFromRules();
    if (cr.getTestIter() != null) {
      testdata = (RowData)cr.getTestSet();
      rset.addDataToRules(testdata);
      rset.computeCompactness(ClusModel.TEST);
      rset.removeDataFromRules();
    }
    return rset;*/
    return new ClusRuleSet(run.getStatManager());
  }

  
  public void constructRandomly(ClusRuleSet rset, RowData data) {


/*    public void separateAndConquor(ClusRuleSet rset, RowData data) {
      while (data.getNbRows() > 0) {
        ClusRule rule = learnOneRule(data);
        if (rule.isEmpty()) {
          break;
        } else {
          rule.computePrediction();
          rule.printModel();
          System.out.println();
          rset.add(rule);
          data = rule.removeCovered(data);
        }
      }
      ClusStatistic left_over = m_Induce.createTotalTargetStat(data);
      left_over.calcMean();
      System.out.println("Left Over: "+left_over);
      rset.setTargetStat(left_over);
    }
*/
    
    /*		if (node.atBottomLevel()) {
			rule.setDefaultStat(node.getTotalStat());
			set.add(rule);
		} else {
			NodeTest test = node.getTest();
			for (int i = 0; i < node.getNbChildren(); i++) {
				ClusNode child = (ClusNode)node.getChild(i);
				NodeTest branchTest = test.getBranchTest(i);
				ClusRule child_rule = rule.cloneRule();
				child_rule.addTest(branchTest);
				constructRecursive(child, child_rule, set);
			}
		}	*/
	}	
}

