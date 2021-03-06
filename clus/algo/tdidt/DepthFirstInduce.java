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

package clus.algo.tdidt;

import clus.main.*;
import clus.util.*;
import clus.algo.*;
import clus.algo.split.*;
import clus.data.rows.*;
import clus.data.type.*;
import clus.model.*;
import clus.model.test.*;
import clus.statistic.*;
import clus.ext.ensembles.*;
import clus.heuristic.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import com.rits.cloning.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class DepthFirstInduce extends ClusInductionAlgorithm {

	protected FindBestTest m_FindBestTest;
	protected ClusNode m_Root;
  protected PrintWriter m_Writer;

  // multi thread
  int nCores = Runtime.getRuntime().availableProcessors() - 1;

	public DepthFirstInduce(ClusSchema schema, Settings sett) throws ClusException, IOException {
		super(schema, sett);
		m_FindBestTest = new FindBestTest(getStatManager());
	}

	public DepthFirstInduce(ClusInductionAlgorithm other) {
		super(other);
		m_FindBestTest = new FindBestTest(getStatManager());
	}

	public DepthFirstInduce(ClusInductionAlgorithm other, NominalSplit split) {
		super(other);
		m_FindBestTest = new FindBestTest(getStatManager(), split);
	}

	public void initialize() throws ClusException, IOException {
		super.initialize();
	}

	public FindBestTest getFindBestTest() {
		return m_FindBestTest;
	}

	public CurrentBestTestAndHeuristic getBestTest() {
		return m_FindBestTest.getBestTest();
	}

	public boolean initSelectorAndStopCrit(ClusNode node, RowData data) {
		int max = getSettings().getTreeMaxDepth();
		if (max != -1 && node.getLevel() >= max) {
			return true;
		}
		return m_FindBestTest.initSelectorAndStopCrit(node.getClusteringStat(), data);
	}

	public ClusAttrType[] getDescriptiveAttributes() {
		ClusSchema schema = getSchema();
		Settings sett = getSettings();
		if (!sett.isEnsembleMode()) {
			return schema.getDescriptiveAttributes();
		} else {
			switch (sett.getEnsembleMethod()) {
			case Settings.ENSEMBLE_BAGGING:
				return schema.getDescriptiveAttributes();
			case Settings.ENSEMBLE_RFOREST:
				ClusAttrType[] attrsAll = schema.getDescriptiveAttributes();
				ClusEnsembleInduce.setRandomSubspaces(attrsAll, schema.getSettings().getNbRandomAttrSelected());
				return ClusEnsembleInduce.getRandomSubspaces();
      case Settings.ENSEMBLE_BOOSTING:
        ClusAttrType[] attrsAll1 = schema.getDescriptiveAttributes();
				ClusEnsembleInduce.setRandomSubspaces(attrsAll1, schema.getSettings().getNbRandomAttrSelected());
				return ClusEnsembleInduce.getRandomSubspaces();
			case Settings.ENSEMBLE_RSUBSPACES:
				return ClusEnsembleInduce.getRandomSubspaces();
			case Settings.ENSEMBLE_BAGSUBSPACES:
				return ClusEnsembleInduce.getRandomSubspaces();
			case Settings.ENSEMBLE_NOBAGRFOREST:
				ClusAttrType[] attrsAll2 = schema.getDescriptiveAttributes();
				ClusEnsembleInduce.setRandomSubspaces(attrsAll2, schema.getSettings().getNbRandomAttrSelected());
				return ClusEnsembleInduce.getRandomSubspaces();
			default:
				return schema.getDescriptiveAttributes();
			}
		}
	}

	
	public void filterAlternativeSplits(ClusNode node, RowData data, RowData[] subsets) {
		boolean removed = false;
		CurrentBestTestAndHeuristic best = m_FindBestTest.getBestTest();
		int arity = node.getTest().updateArity();
		ArrayList v = best.getAlternativeBest(); // alternatives: all tests that result in same heuristic value
		for (int k = 0; k < v.size(); k++) {
			NodeTest nt = (NodeTest) v.get(k);
			int altarity = nt.updateArity();
			// remove alternatives that have different arity than besttest
			if (altarity != arity) {
				v.remove(k);
				k--;
				System.out.println("Alternative split with different arity: " + nt.getString());
				removed = true;
			} 
			else {
				// arity altijd 2 hier
				// exampleindices van subset[0] bijhouden, van alle alternatives zowel van subset[0] als subset[1] kijken of de indices gelijk zijn
				int nbsubset0 = subsets[0].getNbRows();
				int indices[] = new int[nbsubset0];
				for (int m=0; m<nbsubset0; m++) {
					indices[m] = subsets[0].getTuple(m).getIndex();
				}
				boolean same = false;
				for (int l = 0; l < altarity; l++) {
					RowData altrd = data.applyWeighted(nt, l);
					if (altrd.getNbRows() == nbsubset0) {
						int nbsame = 0;
						for (int m=0; m<nbsubset0; m++) {
							if (altrd.getTuple(m).getIndex() == indices[m]) {
								nbsame++;
							}
						}
						if (nbsame == nbsubset0) { 
							same = true;
							if (l!=0) {
								// we have the same subsets, but the opposite split, hence we change the test to not(test) 
								String test = v.get(k).toString();
								String newtest = "not(" + test + ")";
								v.set(k, new String(newtest));
							}
						}
					}
				}
				if (!same) {
					v.remove(k);
					k--;
					System.out.println("Alternative split with different ex in subsets: " + nt.getString());
					removed = true;
				}
				
				}
			}
		node.setAlternatives(v);
//		if (removed) System.out.println("Alternative splits were possible");
	}

	public void makeLeaf(ClusNode node) {
		node.makeLeaf();
		if (getSettings().hasTreeOptimize(Settings.TREE_OPTIMIZE_NO_CLUSTERING_STATS)) {
				node.setClusteringStat(null);
		}
	}
	
  // for python printing when tree is optimized
	public void induce(ClusNode node, RowData data, String prefix) {
		//System.out.println("nonsparse induce");
		// Initialize selector and perform various stopping criteria
		if (initSelectorAndStopCrit(node, data)) {
			makeLeaf(node);
			return;
		}
		// Find best test
		
    // descriptive column names
    ClusAttrType[] attrs = getDescriptiveAttributes();
		long start_time = System.currentTimeMillis();

    int needCores;
    if (getSettings().getEnsembleMethod() == Settings.ENSEMBLE_RFOREST) {
      // RForest faster if parallel trees
      needCores = 1;
    } else {
      // from empirical speed tests of 1 vs 1+ cores
      int ndata = data.getNbRows() * attrs.length;
      needCores = ndata >= 480 ? Math.min(nCores, 3) : 1;
    }

    // set differently if multithreading
    CurrentBestTestAndHeuristic best;
    if (needCores == 1) {
      
        // original code for induce
        for (int i = 0; i < attrs.length; i++) {
          ClusAttrType at = attrs[i];
          if (at instanceof NominalAttrType) m_FindBestTest.findNominal((NominalAttrType)at, data);
          else m_FindBestTest.findNumeric((NumericAttrType)at, data);
        }
        best = m_FindBestTest.getBestTest();

    } else {
        // Parallel induce
        // array to store results, 1 for each core
        // uses a lot less memory than 1 for each column
        Cloner cloner = new Cloner();
        FindBestTest[] subtests = new FindBestTest[needCores];
        for (int i = 0; i < needCores; i++) {
            subtests[i] = cloner.deepClone(m_FindBestTest);
        }

        ExecutorService executor = Executors.newFixedThreadPool(needCores);
        CompletableFuture[] futures = new CompletableFuture[needCores];


        //  assign new job to run on ith subtest until all columns done
        // this ensures that whenever a core finishes a column, it get's another (more efficient multithreading)
        int nextCol = 0;
        while (nextCol < attrs.length) {
            // check for completed futures and reassign jobs when complete
            for (int i = 0; i < needCores; i++) {
                if (futures[i] == null || futures[i].isDone()) {
                    // start next column on ith subtest
                    // since ith future is done, ith subtest is free to use (no race conditions)
                    futures[i] = CompletableFuture.runAsync(new InduceSubset(subtests[i], nextCol, attrs, data), executor);
                    nextCol = nextCol + 1;
                } 
                if (nextCol == attrs.length) break;
            }
        }

        // shutdown executor
        executor.shutdown();

        // wait for any remaining jobs to finish
        try {executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);} catch (Exception e) {}

        // get best from each subtests
        CurrentBestTestAndHeuristic[] bests = new CurrentBestTestAndHeuristic[needCores];

        int bestCore = 0;
        double bestHeur = 0;  
        double curHeur;
        for(int i = 0 ; i < needCores; i++) {
            bests[i] = subtests[i].getBestTest();
            if (bests[i].hasBestTest()) {
                curHeur = bests[i].getHeuristicValue();
                if (curHeur > bestHeur) {
                  bestHeur = curHeur;
                  bestCore = i;
                  // System.out.println("Current best heuristic from core "+i+": "+bestHeur);
                }
            }
        }
        best = subtests[bestCore].getBestTest();
    }
		
		long stop_time = System.currentTimeMillis();
		double elapsed = stop_time - start_time;
    String unit = "ms";
    if (elapsed > 60000) {
      elapsed = elapsed/60000;
      unit = "min";
    } else if (elapsed > 1000) {
      elapsed = elapsed/1000;
      unit = "s";
    }
		// m_Time += elapsed;
		
		// Partition data + recursive calls
		if (best.hasBestTest()) {

			// start_time = System.currentTimeMillis();
			
			node.testToNode(best);
			// Output best test
			if (Settings.VERBOSE > 0) System.out.println("Test: "+node.getTestString()+" -> "+best.getHeuristicValue()+" ("+elapsed+unit+")");
			// Create children
			int arity = node.updateArity();
			NodeTest test = node.getTest();
			RowData[] subsets = new RowData[arity];
			for (int j = 0; j < arity; j++) {
				subsets[j] = data.applyWeighted(test, j);
			}
			if (getSettings().showAlternativeSplits()) {
				filterAlternativeSplits(node, data, subsets);
			}

      // print to file
      m_Writer.println(prefix+"if " +node.getTestString()+":");

      // Don't remove statistics of root node; code below depends on them
      node.setClusteringStat(null);
      node.setTargetStat(null);
      node.setTest(null);

			for (int j = 0; j < arity; j++) {
				ClusNode child = new ClusNode();
				node.setChild(child, j);
				child.initClusteringStat(m_StatManager, m_Root.getClusteringStat(), subsets[j]);
				child.initTargetStat(m_StatManager, m_Root.getTargetStat(), subsets[j]);
        if (j == 1) m_Writer.println(prefix+"else: ");
				induce(child, subsets[j], prefix+"    ");
			}

    m_Writer.flush();
		} else {
			makeLeaf(node);
      node.postProc(null);
      m_Writer.println(prefix+"return "+node.getTargetStat().getArrayOfStatistic());
      m_Writer.flush();

      node.setTargetStat(null);
      node.setTest(null);

		}
	}

  public void induce(ClusNode node, RowData data) {
		//System.out.println("nonsparse induce");
		// Initialize selector and perform various stopping criteria
		if (initSelectorAndStopCrit(node, data)) {
			makeLeaf(node);
			return;
		}
		// Find best test
		
    // descriptive column names
    ClusAttrType[] attrs = getDescriptiveAttributes();
		long start_time = System.currentTimeMillis();

    int ndata = data.getNbRows() * attrs.length;
    int needCores = ndata >= 480 ? Math.min(nCores, 3) : 1;

    // set differently if multithreading
    CurrentBestTestAndHeuristic best;
    if (needCores == 1) {
      
        // original code for induce
        for (int i = 0; i < attrs.length; i++) {
          ClusAttrType at = attrs[i];
          if (at instanceof NominalAttrType) m_FindBestTest.findNominal((NominalAttrType)at, data);
          else m_FindBestTest.findNumeric((NumericAttrType)at, data);
        }
        best = m_FindBestTest.getBestTest();

    } else {
        // Parallel induce
        // array to store results, 1 for each core
        // uses a lot less memory than 1 for each column
        Cloner cloner = new Cloner();
        FindBestTest[] subtests = new FindBestTest[needCores];
        for (int i = 0; i < needCores; i++) {
            subtests[i] = cloner.deepClone(m_FindBestTest);
        }

        ExecutorService executor = Executors.newFixedThreadPool(needCores);
        CompletableFuture[] futures = new CompletableFuture[needCores];


        //  assign new job to run on ith subtest until all columns done
        // this ensures that whenever a core finishes a column, it get's another (more efficient multithreading)
        int nextCol = 0;
        while (nextCol < attrs.length) {
            // check for completed futures and reassign jobs when complete
            for (int i = 0; i < needCores; i++) {
                if (futures[i] == null || futures[i].isDone()) {
                    // start next column on ith subtest
                    // since ith future is done, ith subtest is free to use (no race conditions)
                    futures[i] = CompletableFuture.runAsync(new InduceSubset(subtests[i], nextCol, attrs, data), executor);
                    nextCol = nextCol + 1;
                } 
                if (nextCol == attrs.length) break;
            }
        }

        // shutdown executor
        executor.shutdown();

        // wait for any remaining jobs to finish
        try {executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);} catch (Exception e) {}

        // get best from each subtests
        CurrentBestTestAndHeuristic[] bests = new CurrentBestTestAndHeuristic[needCores];

        int bestCore = 0;
        double bestHeur = 0;  
        double curHeur;
        for(int i = 0 ; i < needCores; i++) {
            bests[i] = subtests[i].getBestTest();
            if (bests[i].hasBestTest()) {
                curHeur = bests[i].getHeuristicValue();
                if (curHeur > bestHeur) {
                  bestHeur = curHeur;
                  bestCore = i;
                  // System.out.println("Current best heuristic from core "+i+": "+bestHeur);
                }
            }
        }
        best = subtests[bestCore].getBestTest();
    }
		
		long stop_time = System.currentTimeMillis();
		long elapsed = stop_time - start_time;
    String unit = "ms";
    if (elapsed > 60000) {
      elapsed = elapsed/60000;
      unit = "min";
    } else if (elapsed > 1000) {
      elapsed = elapsed/1000;
      unit = "s";
    }
		// m_Time += elapsed;
		
		// Partition data + recursive calls
		if (best.hasBestTest()) {

			// start_time = System.currentTimeMillis();
			
			node.testToNode(best);
			// Output best test
			if (Settings.VERBOSE > 0) System.out.println("Test: "+node.getTestString()+" -> "+best.getHeuristicValue()+" ("+elapsed+unit+")");
			// Create children
			int arity = node.updateArity();
			NodeTest test = node.getTest();
			RowData[] subsets = new RowData[arity];
			for (int j = 0; j < arity; j++) {
				subsets[j] = data.applyWeighted(test, j);
			}
			if (getSettings().showAlternativeSplits()) {
				filterAlternativeSplits(node, data, subsets);
			}

			if (node != m_Root && getSettings().hasTreeOptimize(Settings.TREE_OPTIMIZE_NO_INODE_STATS)) {
				// Don't remove statistics of root node; code below depends on them
				node.setClusteringStat(null);
				node.setTargetStat(null);
			}

			for (int j = 0; j < arity; j++) {
				ClusNode child = new ClusNode();
				node.setChild(child, j);
				child.initClusteringStat(m_StatManager, m_Root.getClusteringStat(), subsets[j]);
				child.initTargetStat(m_StatManager, m_Root.getTargetStat(), subsets[j]);
				induce(child, subsets[j]);
			}
		} else {
			makeLeaf(node);
		}
	}

	public void rankFeatures(ClusNode node, RowData data) throws IOException {
		// Find best test
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream("ranking.csv")));
		ClusAttrType[] attrs = getDescriptiveAttributes();
		for (int i = 0; i < attrs.length; i++) {
			ClusAttrType at = attrs[i];
			initSelectorAndStopCrit(node, data);
			if (at instanceof NominalAttrType) m_FindBestTest.findNominal((NominalAttrType)at, data);
			else m_FindBestTest.findNumeric((NumericAttrType)at, data);			
			CurrentBestTestAndHeuristic cbt = m_FindBestTest.getBestTest();
			if (cbt.hasBestTest()) {
				NodeTest test = cbt.updateTest();
				wrt.print(cbt.m_BestHeur);
				wrt.print(",\""+at.getName()+"\"");
				wrt.println(",\""+test+"\"");
			}
		}
		wrt.close();
	}
	
	public void initSelectorAndSplit(ClusStatistic stat) throws ClusException {
		m_FindBestTest.initSelectorAndSplit(stat);
	}

	public void setInitialData(ClusStatistic stat, RowData data) throws ClusException {
		m_FindBestTest.setInitialData(stat,data);
	}

	public void cleanSplit() {
		m_FindBestTest.cleanSplit();
	}

	public ClusNode induceSingleUnpruned(RowData data) throws ClusException, IOException {

		// Begin of induction process
		m_Root = null;
		int nbr = 0;
		while (true) {
			nbr++;
			// Init root node
			m_Root = new ClusNode();
			m_Root.initClusteringStat(m_StatManager, data);
			m_Root.initTargetStat(m_StatManager, data);
			m_Root.getClusteringStat().showRootInfo();
			initSelectorAndSplit(m_Root.getClusteringStat());
			setInitialData(m_Root.getClusteringStat(),data);
			// Induce the tree
			induce(m_Root, data);
			// rankFeatures(m_Root, data);
			// Refinement finished
			if (Settings.EXACT_TIME == false) break;
		}
		m_Root.postProc(null);

		cleanSplit();
		return m_Root;
	}

  // for python printing when tree is optimized
  public ClusNode induceSingleUnpruned(RowData data, int i) throws ClusException, IOException {

    // init python model
    File pyscript = new File("model/"+m_StatManager.getSettings().getAppName()+"_bag"+i+".py");
    m_Writer = new PrintWriter(new FileOutputStream(pyscript));
     
    m_Writer.println("# Python code for bag "+i+" in the ensemble");
    m_Writer.println();

    m_Writer.println("#Model "+(i+1));
    m_Writer.println("def clus_tree_"+(i+1)+"():");


		// Begin of induction process
		m_Root = null;
		int nbr = 0;
		while (true) {
			nbr++;
			// Init root node
			m_Root = new ClusNode();
			m_Root.initClusteringStat(m_StatManager, data);
			m_Root.initTargetStat(m_StatManager, data);
			m_Root.getClusteringStat().showRootInfo();
			initSelectorAndSplit(m_Root.getClusteringStat());
			setInitialData(m_Root.getClusteringStat(),data);
			// Induce the tree
			induce(m_Root, data, "    ");
			// rankFeatures(m_Root, data);
			// Refinement finished
			if (Settings.EXACT_TIME == false) break;
		}

    // finish python model
    m_Writer.flush();
    m_Writer.close();
    System.out.println("Model to Python Code written to: "+pyscript.getName());

		cleanSplit();
		return m_Root;
	}

	public ClusModel induceSingleUnpruned(ClusRun cr) throws ClusException, IOException {
		return induceSingleUnpruned((RowData)cr.getTrainingSet());
	}

  // for python printing when tree is optimized
  public ClusModel induceSingleUnpruned(ClusRun cr, int i) throws ClusException, IOException {
		return induceSingleUnpruned((RowData)cr.getTrainingSet(), i);
	}
}
