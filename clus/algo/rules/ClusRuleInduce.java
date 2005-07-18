/* Created on May 1, 2005
 */
package clus.algo.rules;

import java.io.*;
import java.util.*;

import clus.algo.induce.*;
import clus.main.*;
import clus.model.test.*;
import clus.heuristic.*;
import clus.statistic.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.ext.beamsearch.*;
import clus.util.*;

public class ClusRuleInduce {

	protected boolean m_BeamChanged;
	protected DepthFirstInduce m_Induce;
	protected ClusHeuristic m_Heuristic;
	
	public ClusRuleInduce(DepthFirstInduce induce) {
		m_Induce = induce;
	}
	
	public void setHeuristic(ClusHeuristic heur) {
		m_Heuristic = heur;
	}
	
	public Settings getSettings() {
		return m_Induce.getSettings();
	}
	
	public double estimateBeamMeasure(ClusRule rule) {
			return m_Heuristic.calcHeuristic(null, rule.getClusteringStat(), null);
	}
	
	public boolean isBeamChanged() {
		return m_BeamChanged;
	}
	
	public void setBeamChanged(boolean change) {
		m_BeamChanged = change;
	}
	
	ClusBeam initializeBeam(RowData data) {
		Settings sett = getSettings();
		ClusBeam beam = new ClusBeam(sett.getBeamWidth(), sett.getBeamRemoveEqualHeur());
		ClusStatistic stat = m_Induce.createTotalClusteringStat(data);
		ClusRule rule = new ClusRule(m_Induce.getStatManager());
		rule.setClusteringStat(stat);
		rule.setVisitor(data);
		double value = estimateBeamMeasure(rule);
		beam.addModel(new ClusBeamModel(value, rule));		
		return beam;
	}
	
	public void refineModel(ClusBeamModel model, ClusBeam beam, int model_idx) {
		ClusRule rule = (ClusRule)model.getModel();
		RowData data = (RowData)rule.getVisitor();
		if (m_Induce.initSelectorAndStopCrit(rule.getClusteringStat(), data)) {
			model.setFinished(true);
			return;			
		}
		TestSelector sel = m_Induce.getSelector();
		ClusStatManager mgr = m_Induce.getStatManager();		
		ClusAttrType[] attrs = data.getSchema().getDescriptiveAttributes();
		for (int i = 0; i < attrs.length; i++) {
			sel.resetBestTest();
			double beam_min_value = beam.getMinValue();
			sel.setBestHeur(beam_min_value);
			ClusAttrType at = attrs[i];
			if (at instanceof NominalAttrType) m_Induce.findNominal((NominalAttrType)at, data);
			else m_Induce.findNumeric((NumericAttrType)at, data);
			if (sel.hasBestTest()) {
				NodeTest test = sel.updateTest();
				if (Settings.VERBOSE > 0) System.out.println("  Test: "+test.getString()+" -> "+sel.m_BestHeur);
				RowData subset = data.applyWeighted(test, ClusNode.YES);				
				ClusRule ref_rule = rule.cloneRule();
				ref_rule.addTest(test);
				ref_rule.setVisitor(subset);
				ref_rule.setClusteringStat(m_Induce.createTotalClusteringStat(subset));
				double new_heur = sanityCheck(sel.m_BestHeur, ref_rule);
				// Check for sure if _strictly_ better!
				if (new_heur > beam_min_value) {
					ClusBeamModel new_model = new ClusBeamModel(new_heur, ref_rule);
					new_model.setParentModelIndex(model_idx);
					beam.addModel(new_model);
					setBeamChanged(true);
				}
			}
		}
	}
	
	public void refineBeam(ClusBeam beam) {
		setBeamChanged(false);
		ArrayList models = beam.toArray();
		for (int i = 0; i < models.size(); i++) {
			ClusBeamModel model = (ClusBeamModel)models.get(i);
			if (!(model.isRefined() || model.isFinished())) {
				refineModel(model, beam, i);
				model.setRefined(true);
				model.setParentModelIndex(-1);
			}
		}	
	}
	
	public ClusRule learnOneRule(RowData data) {
		ClusBeam beam = initializeBeam(data);
		int i = 0;
		System.out.print("Step: ");
		while (true) {
      if (Settings.VERBOSE > 0) {
        System.out.println("Step: " + i);
      } else {
        if (i != 0) {
          System.out.print(",");
        }
        System.out.print(i);
      }
			System.out.flush();
			refineBeam(beam);
			if (!isBeamChanged()) {
				break;
			}
			i++;
		}
		System.out.println();
		double best = beam.getBestModel().getValue();
		double worst = beam.getWorstModel().getValue();		
		System.out.println("Worst = "+worst+" Best = "+best);
		ClusRule result = (ClusRule)beam.getBestAndSmallestModel().getModel();
		// Create target statistic for rule
		RowData rule_data = (RowData)result.getVisitor();
		result.setTargetStat(m_Induce.createTotalTargetStat(rule_data));
		result.setVisitor(null);
		return result;
	}
	
  public void separateAndConquor(ClusRuleSet rset, RowData data) {
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

  /**
   * separateAndConquor method which uses reweighting
   * @param rset
   * @param data
   * @throws ClusException 
   */
  public void separateAndConquorWeighted(ClusRuleSet rset, RowData data) throws ClusException {
    int MAXLOOPS = 50;
    int i = 0;
    while ((data.getNbRows() > 0) && (i < MAXLOOPS)) {
      ClusRule rule = learnOneRule(data);
      if (rule.isEmpty()) {
        break;
      } else {
        rule.computePrediction();
        rule.printModel();
        System.out.println();
        rset.add(rule);
        data = rule.reweighCovered(data);
        i++;
      }
    }
    ClusStatistic left_over = m_Induce.createTotalClusteringStat(data);
    left_over.calcMean();
    System.out.println("Left Over: "+left_over);
    rset.setTargetStat(left_over);
  }

	public double sanityCheck(double value, ClusRule rule) {
		double expected = estimateBeamMeasure(rule);
		if (Math.abs(value-expected) > 1e-6) {
			System.out.println("Bug in heurisitc: "+value+" <> "+expected);
			PrintWriter wrt = new PrintWriter(System.out);
			rule.printModel(wrt);
			wrt.close();
			System.out.flush();
			System.exit(1);
		}
		return expected;
	}
	
	public ClusModel induce(ClusRun run) throws ClusException, IOException {
		int method = getSettings().getCoveringMethod();
		RowData data = (RowData)run.getTrainingSet();
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
    // Computing compactness
    if (getSettings().computeCompactness()) {
      rset.addDataToRules(data);
      rset.computeCompactness(ClusModel.TRAIN);
      rset.removeDataFromRules();
      if (run.getTestIter() != null) {
        RowData testdata = (RowData)run.getTestSet();
        rset.addDataToRules(testdata);
        rset.computeCompactness(ClusModel.TEST);
        rset.removeDataFromRules();
      }
    }
    return rset;
	}
}
