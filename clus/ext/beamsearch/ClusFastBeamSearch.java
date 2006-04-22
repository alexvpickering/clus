/*
 * Created on Apr 25, 2005
 */
package clus.ext.beamsearch;

import clus.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.main.*;
import clus.model.test.*;
import clus.util.*;

import java.io.*;
import java.util.ArrayList;

public class ClusFastBeamSearch extends ClusBeamSearch {

	ClusBeamSizeConstraints m_Constr;
	
	public ClusFastBeamSearch(Clus clus) throws IOException, ClusException {
		super(clus);
		m_Constr = new ClusBeamSizeConstraints(); 
	}
	
	public ClusBeam initializeBeam(ClusRun run) throws ClusException {
		ClusBeam beam = super.initializeBeam(run);
		ClusBeamModel model = beam.getBestModel();
		initModelRecursive((ClusNode)model.getModel(), (RowData)run.getTrainingSet());
		return beam;
	}
	
	public void initModelRecursive(ClusNode node, RowData data) {
		if (node.atBottomLevel()) {
			ClusBeamAttrSelector attrsel = new ClusBeamAttrSelector();
			attrsel.setData(data);
			node.setVisitor(attrsel);
		} else {
			NodeTest test = node.getTest();
			for (int j = 0; j < node.getNbChildren(); j++) {
				ClusNode child = (ClusNode)node.getChild(j);
				RowData subset = data.applyWeighted(test, j);				
				initModelRecursive(child, subset);
			}			
		}
	}
	
	public void computeGlobalHeuristic(NodeTest test, RowData data, TestSelector sel) {
		sel.reset(2);
		data.calcPosAndMissStat(test, ClusNode.YES, sel.getPosStat(), sel.getMissStat());
		double global_heur = m_Heuristic.calcHeuristic(sel.getTotStat(), sel.getPosStat(), sel.getMissStat());
		test.setHeuristicValue(global_heur);
	}
	
	public void refineGivenLeaf(ClusNode leaf, ClusBeamModel root, ClusBeam beam, ClusAttrType[] attrs) {
		ClusBeamAttrSelector attrsel = (ClusBeamAttrSelector)leaf.getVisitor();
		if (attrsel.isStopCrit()) {
			/* stopping criterion already succeeded for this node */
			if (m_Verbose) System.out.print("[S:"+leaf.getClusteringStat()+"]");
			return;
		}
		RowData data = attrsel.getData();
		if (!attrsel.hasEvaluations()) {
			if (m_Induce.initSelectorAndStopCrit(leaf, data)) {
				/* stopping criterion succeeds */
				attrsel.setStopCrit(true);
				return;
			}
			TestSelector sel = m_Induce.getSelector();
			m_Heuristic.setTreeOffset(0.0);
			attrsel.newEvaluations(attrs.length);
			for (int i = 0; i < attrs.length; i++) {
				sel.resetBestTest();
				ClusAttrType at = attrs[i];
				if (at instanceof NominalAttrType) m_Induce.findNominal((NominalAttrType)at, data);
				else m_Induce.findNumeric((NumericAttrType)at, data);
				// found good test for attribute ?
				if (sel.hasBestTest()) {
					NodeTest test = sel.updateTest();
					if (hasAttrHeuristic()) {
						// has attribute heuristic -> recompute global heuristic
						computeGlobalHeuristic(test, data, sel);
					}
					attrsel.setBestTest(i, test);
				}
			}		
		}
		double offset = root.getValue() - m_Heuristic.computeLeafAdd(leaf);			
		NodeTest[] besttests = attrsel.getBestTests();
		if (m_Verbose) System.out.println("[M:"+beam.getMinValue()+"]");
		for (int i = 0; i < besttests.length; i++) {
			NodeTest test = besttests[i];
			if (test != null) {
				double beam_min_value = beam.getMinValue();
				double heuristic = test.getHeuristicValue() + offset;
				if (heuristic >= beam_min_value) {
					if (m_Verbose) System.out.print("[+]");
					ClusNode ref_leaf = (ClusNode)leaf.cloneNode();
					ref_leaf.setTest(test);
					// visitor is removed in updateModelRefinement() !
					ref_leaf.setVisitor(leaf.getVisitor());
					if (Settings.VERBOSE > 0) System.out.println("Test: "+ref_leaf.getTestString()+" -> "+ref_leaf.getTest().getHeuristicValue()+" ("+ref_leaf.getTest().getPosFreq()+")");
					int arity = ref_leaf.updateArity();
					for (int j = 0; j < arity; j++) {
						ClusNode child = new ClusNode();
						ref_leaf.setChild(child, j);				
					}
					ClusNode root_model = (ClusNode)root.getModel();
					ClusNode ref_tree = root_model.cloneTreeWithVisitors(leaf, ref_leaf);
					ClusBeamModel new_model = new ClusBeamModel(heuristic, ref_tree);
					new_model.setRefinement(ref_leaf);
					new_model.setParentModelIndex(getCurrentModel());
					beam.addModel(new_model);
					setBeamChanged(true);
				} else {
					if (m_Verbose) System.out.print("[-:"+heuristic+"]");
				}
			}
		}
	}
		
	public void refineModel(ClusBeamModel model, ClusBeam beam, ClusRun run) throws IOException {
		ClusNode tree = (ClusNode)model.getModel();
		ClusBeamModel new_model = model.cloneModel();
		/* Create new model because value can be different */
		new_model.setValue(sanityCheck(model.getValue(), tree));
		if (isBeamPostPrune()) {			
			ClusNode clone = tree.cloneTreeWithVisitors();			
			m_Constr.enforce(clone, m_MaxTreeSize);
/*			if (m_Constr.isModified()) {
				System.out.println();
				System.out.println("Previous:");
				tree.printTree();
				System.out.println("Modified:");
				clone.printTree();				
				ClusNode clone2 = tree.cloneTreeWithVisitors();
				m_Constr.setDebug(true);
				m_Constr.enforce(clone2, m_MaxTreeSize);
				System.exit(0);
			} */
			if (m_Constr.isFinished()) {
				model.setFinished(true);
				return;
			}
			if (m_Constr.isModified()) {
				new_model.setModel(clone);
				new_model.setValue(estimateBeamMeasure(clone));
			}
		} else {
			if (m_MaxTreeSize >= 0) {
				int size = tree.getNbNodes();
				if (size + 2 > m_MaxTreeSize) {
					model.setFinished(true);
					return;
				}
			}
		}
		RowData train = (RowData)run.getTrainingSet();		
		ClusAttrType[] attrs = train.getSchema().getDescriptiveAttributes();		
		refineEachLeaf((ClusNode)new_model.getModel(), new_model, beam, attrs);
	}
	
	public void updateModelRefinement(ClusBeamModel model) {
		/* Get data into children of last refinement */
		ClusNode leaf = (ClusNode)model.getRefinement();
		if (leaf == null) return;
		ClusBeamAttrSelector attrsel = (ClusBeamAttrSelector)leaf.getVisitor();
		RowData data = attrsel.getData();
		ClusStatManager mgr = m_Induce.getStatManager();
		for (int j = 0; j < leaf.getNbChildren(); j++) {
			ClusNode child = (ClusNode)leaf.getChild(j);
			ClusBeamAttrSelector casel = new ClusBeamAttrSelector();					
			RowData subset = data.applyWeighted(leaf.getTest(), j);				
			child.initTargetStat(mgr, subset);
			child.initClusteringStat(mgr, subset);
			casel.setData(subset);
			child.setVisitor(casel);					
		}
		leaf.setVisitor(null);
		model.setRefinement(null);
	}
		
	public void refineBeam(ClusBeam beam, ClusRun run) throws IOException {
		super.refineBeam(beam, run);
		ArrayList models = beam.toArray();
		for (int i = 0; i < models.size(); i++) {
			ClusBeamModel model = (ClusBeamModel)models.get(i);
			updateModelRefinement(model);
		}
	}
}
