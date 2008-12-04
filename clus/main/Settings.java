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

package clus.main;

import jeans.io.ini.*;
import jeans.util.cmdline.*;
import jeans.util.*;
import jeans.resource.*;

import java.io.*;
import java.util.*;

import clus.statistic.*;
import clus.heuristic.*;
import clus.data.type.*;
import clus.ext.hierarchical.*;

/**
 * All the settings. Includes the command line parameters as boolean class attributes.
 * The settings file attributes are included by get* methods.
 * @author User
 *
 */
public class Settings implements Serializable {

	public final static long SERIAL_VERSION_ID = 1L;
	public final static long serialVersionUID = SERIAL_VERSION_ID;


/***********************************************************************
 * INI file structure                                                  *
 ***********************************************************************/

	protected INIFile m_Ini = new INIFile();

/***********************************************************************
 * Simple constants                                                    *
 ***********************************************************************/

	public final static String DEFAULT = "Default";
	public final static String NONE = "None";
	public final static String[] NONELIST = { "None" };
	public final static String[] EMPTY = {};
	public final static String[] INFINITY = { "Infinity" };
	public final static String INFINITY_STRING = "Infinity";
	public final static int INFINITY_VALUE = 0;
	public final static double[] FOUR_ONES = { 1.0, 1.0, 1.0, 1.0 };

/***********************************************************************
 * Generic information                                                 *
 ***********************************************************************/

	protected Date m_Date;
	protected String m_AppName;
	protected String m_DirName;

	public static int VERBOSE = 1;
	public static boolean EXACT_TIME = false;

	public Date getDate() {
		return m_Date;
	}

	public void setDate(Date date) {
		m_Date = date;
	}

	public String getAppName() {
		return m_AppName;
	}

	public void setAppName(String file) {
		file = StringUtils.removeSuffix(file, ".gz");
		file = StringUtils.removeSuffix(file, ".arff");
		file = StringUtils.removeSuffix(file, ".s");
		file = StringUtils.removeSuffix(file, ".");
		m_AppName = FileUtil.removePath(file);
		m_DirName = FileUtil.getPath(file);
	}

/***********************************************************************
 * Section: General                                                    *
 ***********************************************************************/

	protected INIFileInt m_Verbose;
	protected INIFileNominal m_Compatibility;
	protected INIFileString m_RandomSeed;
	protected INIFileNominal m_ResourceInfoLoaded;

	public static int enableVerbose(int talk) {
		int prev = VERBOSE;
		VERBOSE = talk;
		return prev;
	}

	public int getCompatibility() {
		return m_Compatibility.getValue();
	}

	public boolean hasRandomSeed() {
		//System.out.println(m_RandomSeed.getValue());
		return !StringUtils.unCaseCompare(m_RandomSeed.getValue(), NONE);
	}

	public int getRandomSeed() {
		return Integer.parseInt(m_RandomSeed.getValue());
	}

	public int getResourceInfoLoaded() {
		return m_ResourceInfoLoaded.getValue();
	}

/***********************************************************************
 * Section: General - Compatibility mode                               *
 ***********************************************************************/

	public final static String[] COMPATIBILITY = { "Latest", "CMB05" };
	public final static int COMPATIBILITY_LATEST = 0;
	public final static int COMPATIBILITY_CMB05 = 1;

/***********************************************************************
 * Section: General - ResourceInfo loaded                              *
 ***********************************************************************/

	public final static String[] RESOURCE_INFO_LOAD = {"Yes", "No", "Test"};

	public final static int RESOURCE_INFO_LOAD_YES = 0;
	public final static int RESOURCE_INFO_LOAD_NO = 1;
	public final static int RESOURCE_INFO_LOAD_TEST = 2;

/***********************************************************************
 * Section: Data                                                       *
 ***********************************************************************/

	protected INIFileString m_DataFile;
	protected INIFileStringOrDouble m_TestSet;
	protected INIFileStringOrDouble m_PruneSet;
	protected INIFileStringOrInt m_PruneSetMax;
	protected INIFileStringOrInt m_XValFolds;
	protected INIFileBool m_RemoveMissingTarget;

	public String getDataFile() {
		return m_DataFile.getValue();
	}

	public boolean isNullFile() {
		return StringUtils.unCaseCompare(m_DataFile.getValue(), NONE);
	}

	public void updateDataFile(String fname) {
		if (isNullFile()) m_DataFile.setValue(fname);
	}

	public String getTestFile() {
		return m_TestSet.getValue();
	}

	public boolean isNullTestFile() {
		return m_TestSet.isDoubleOrNull(NONE);
	}

	public String getPruneFile() {
		return m_PruneSet.getValue();
	}

	public boolean isNullPruneFile() {
		return m_PruneSet.isDoubleOrNull(NONE);
	}

	public double getTestProportion() {
		if (!m_TestSet.isDouble()) return 0.0;
		return m_TestSet.getDoubleValue();
	}

	public double getPruneProportion() {
		if (!m_PruneSet.isDouble())	return 0.0;
		return m_PruneSet.getDoubleValue();
	}

	public int getPruneSetMax() {
		if (m_PruneSetMax.isString(INFINITY_STRING)) return Integer.MAX_VALUE;
		else return m_PruneSetMax.getIntValue();
	}

	public boolean isNullXValFile() {
		return m_XValFolds.isIntOrNull(NONE);
	}

	public boolean isLOOXVal() {
		return m_XValFolds.isString("LOO");
	}

	public String getXValFile() {
		return m_XValFolds.getValue();
	}

	public int getXValFolds() {
		return m_XValFolds.getIntValue();
	}

	public void setXValFolds(int folds) {
		m_XValFolds.setIntValue(folds);
	}

	public boolean isRemoveMissingTarget() {
		return m_RemoveMissingTarget.getValue();
	}
	
/***********************************************************************
 * Section: Attribute                                                  *
 ***********************************************************************/

	protected INIFileString m_Target;
	protected INIFileString m_Clustering;
	protected INIFileString m_Descriptive;
	protected INIFileString m_Key;
	protected INIFileString m_Disabled;
	protected INIFileNominalOrDoubleOrVector m_Weights;
	protected INIFileNominalOrDoubleOrVector m_ClusteringWeights;
	protected INIFileBool m_ReduceMemoryNominal;

	public String getTarget() {
		return m_Target.getValue();
	}

	public void setTarget(String str) {
		m_Target.setValue(str);
	}

	public boolean isNullTarget() {
		return StringUtils.unCaseCompare(m_Target.getValue(), NONE);
	}

	public boolean isDefaultTarget() {
		return StringUtils.unCaseCompare(m_Target.getValue(), DEFAULT);
	}

	public String getClustering() {
		return m_Clustering.getValue();
	}

	public void setClustering(String str) {
		m_Clustering.setValue(str);
	}

	public String getDescriptive() {
		return m_Descriptive.getValue();
	}

	public void setDescriptive(String str) {
		m_Descriptive.setValue(str);
	}

	public String getKey() {
		return m_Key.getValue();
	}

	public String getDisabled() {
		return m_Disabled.getValue();
	}

	public void setDisabled(String str) {
		m_Disabled.setValue(str);
	}

	public INIFileNominalOrDoubleOrVector getNormalizationWeights() {
		return m_Weights;
	}

	public boolean hasNonTrivialWeights() {
		for (int i = 0; i < m_Weights.getVectorLength(); i++) {
			if (m_Weights.isNominal(i))
				return true;
			else if (m_Weights.getDouble(i) != 1.0)
				return true;
		}
		return false;
	}

	public INIFileNominalOrDoubleOrVector getClusteringWeights() {
		return m_ClusteringWeights;
	}

	public boolean getReduceMemoryNominalAttrs() {
		return m_ReduceMemoryNominal.getValue();
	}

/***********************************************************************
 * Section: Selective inductive transfer                               *
 ***********************************************************************/
	protected INIFileSection m_SectionSIT;
	protected INIFileString m_MainTarget;
	protected INIFileString m_Search;
	protected INIFileString m_Learner;
	protected INIFileBool m_Recursive;
	protected INIFileString m_Error;

	public String getError(){
		return m_Error.getValue();
	}

	public String getLearnerName(){
		return m_Learner.getValue();
	}

	//@deprecated
	public boolean getRecursive(){
		return m_Recursive.getValue();
	}

	public String getMainTarget() {
		return m_MainTarget.getValue();
	}

	public String getSearchName() {
		return m_Search.getValue();
	}

	public void setSearch(String b) {
		m_Search.setValue(b);
	}

	public void setMainTarget(String str) {
		m_MainTarget.setValue(str);
	}

/***********************************************************************
 * Section: Attribute - Normalization                                  *
 ***********************************************************************/

	public final static String[] NORMALIZATIONS = { "Normalize" };
	public final static int NORMALIZATION_DEFAULT = 0;

/***********************************************************************
 * Section: Attribute - Target weights                                 *
 ***********************************************************************/

	public final static String[] NUM_NOM_TAR_NTAR_WEIGHTS = { "TargetWeight",
		"NonTargetWeight", "NumericWeight", "NominalWeight" };

	public final static int TARGET_WEIGHT = 0;
	public final static int NON_TARGET_WEIGHT = 1;
	public final static int NUMERIC_WEIGHT = 2;
	public final static int NOMINAL_WEIGHT = 3;

/***********************************************************************
 * Section: Nominal - Should move?                                     *
 ***********************************************************************/

	protected INIFileDouble m_MEstimate;

	public double getMEstimate() {
		return m_MEstimate.getValue();
	}

/***********************************************************************
 * Section: Model                                                      *
 ***********************************************************************/

	protected INIFileDouble m_MinW;
	protected INIFileString m_TuneFolds;
	protected INIFileNominalOrDoubleOrVector m_ClassWeight;
	protected INIFileBool m_NominalSubsetTests;

	public double getMinimalWeight() {
		return m_MinW.getValue();
	}

	public void setMinimalWeight(double val) {
		m_MinW.setValue(val);
	}

	public String getTuneFolds() {
		return m_TuneFolds.getValue();
	}

	public double[] getClassWeight() {
		return m_ClassWeight.getDoubleVector();
	}
	
	public boolean isNominalSubsetTests() {
		return m_NominalSubsetTests.getValue();
	}

/***********************************************************************
 * Section: Constraints                                                *
 ***********************************************************************/

	protected INIFileString m_SyntacticConstrFile;
	protected INIFileNominalOrIntOrVector m_MaxSizeConstr;
	protected INIFileNominalOrDoubleOrVector m_MaxErrorConstr;

	public boolean hasConstraintFile() {
		return !StringUtils.unCaseCompare(m_SyntacticConstrFile.getValue(),	NONE);
	}

	public String getConstraintFile() {
		return m_SyntacticConstrFile.getValue();
	}

	public int getMaxSize() {
		return getSizeConstraintPruning(0);
	}

	public int getSizeConstraintPruning(int idx) {
		if (m_MaxSizeConstr.isNominal(idx)) {
			return -1;
		} else {
			return m_MaxSizeConstr.getInt(idx);
		}
	}

	public int getSizeConstraintPruningNumber() {
		int len = m_MaxSizeConstr.getVectorLength();
		if (len == 1 && m_MaxSizeConstr.getNominal() == INFINITY_VALUE)	return 0;
		else return len;
	}

	public int[] getSizeConstraintPruningVector() {
		int size_nb = getSizeConstraintPruningNumber();
		int[] sizes = new int[size_nb];
		for (int i = 0; i < size_nb; i++) {
			sizes[i] = getSizeConstraintPruning(i);
		}
		return sizes;
	}

	public void setSizeConstraintPruning(int size) {
		m_MaxSizeConstr.setInt(size);
	}

	public double getMaxErrorConstraint(int idx) {
		if (m_MaxErrorConstr.isNominal(idx)) {
			return Double.POSITIVE_INFINITY;
		} else {
			return m_MaxErrorConstr.getDouble(idx);
		}
	}

	public int getMaxErrorConstraintNumber() {
		int len = m_MaxErrorConstr.getVectorLength();
		if (len == 1 && m_MaxErrorConstr.getDouble(0) == 0.0) return 0;
		else return len;
	}

	public double[] getMaxErrorConstraintVector() {
		int error_nb = getMaxErrorConstraintNumber();
		double[] max_error = new double[error_nb];
		for (int i = 0; i < error_nb; i++) {
			max_error[i] = getMaxErrorConstraint(i);
		}
		return max_error;
	}

/***********************************************************************
 * Section: Output                                                     *
 ***********************************************************************/

	protected INIFileInt m_SetsData;
	protected INIFileBool m_OutFoldErr;
	protected INIFileBool m_OutFoldModels;
	protected INIFileBool m_OutTrainErr;
	protected INIFileBool m_ShowForest;
	protected INIFileBool m_ShowBrFreq;
	protected INIFileBool m_ShowUnknown;
	protected INIFileNominal m_ShowInfo;
	protected INIFileNominal m_ShowModels;
	protected INIFileBool m_PrintModelAndExamples;
	protected INIFileNominal m_WritePredictions;
	protected INIFileBool m_WriteErrorFile;
	protected INIFileBool m_ModelIDFiles;
	protected INIFileBool m_OutputPythonModel;
	protected INIFileBool m_OutputDatabaseQueries;

	public boolean isOutTrainError() {
		return m_OutTrainErr.getValue();
	}

	public boolean isShowBranchFreq() {
		return m_ShowBrFreq.getValue();
	}

	public boolean isShowUnknown() {
		return m_ShowUnknown.getValue();
	}

	public boolean isPrintModelAndExamples() {
		return m_PrintModelAndExamples.getValue();
	}

	public boolean isOutFoldError() {
		return m_OutFoldErr.getValue();
	}

	public boolean isOutputFoldModels() {
		return m_OutFoldModels.getValue();
	}

	public boolean isWriteTestSetPredictions() {
		return m_WritePredictions.contains(WRITE_PRED_TEST);
	}

	public boolean isWriteTrainSetPredictions() {
		return m_WritePredictions.contains(WRITE_PRED_TRAIN);
	}
	
	public boolean isWriteErrorFile() {
		return m_WriteErrorFile.getValue();
	}

	public boolean isWriteModelIDPredictions() {
		return m_ModelIDFiles.getValue();
	}

	public boolean isOutputPythonModel() {
		return m_OutputPythonModel.getValue();
	}

	public boolean isOutputDatabaseQueries() {
		return m_OutputDatabaseQueries.getValue();
	}

	public boolean isShowXValForest() {
		return m_ShowForest.getValue();
	}

	public boolean getShowModel(int i) {
		return m_ShowModels.contains(i);
	}

	public StatisticPrintInfo getStatisticPrintInfo() {
		StatisticPrintInfo info = new StatisticPrintInfo();
		info.SHOW_EXAMPLE_COUNT = m_ShowInfo.contains(0);
		info.SHOW_EXAMPLE_COUNT_BYTARGET = m_ShowInfo.contains(1);		
		info.SHOW_DISTRIBUTION = m_ShowInfo.contains(2);
		info.SHOW_INDEX = m_ShowInfo.contains(3);
		info.INTERNAL_DISTR = m_ShowInfo.contains(4);
		return info;
	}

	public int getBaggingSets() {
		return m_SetsData.getValue();
	}

/***********************************************************************
 * Section: Output - Show info in .out file                            *
 ***********************************************************************/

	public final static String[] SHOW_MODELS = {"Default", "Original", "Pruned", "Others"};

	public final static int[] SHOW_MODELS_VALUES = {0,2,3};
	public final static int SHOW_MODELS_DEFAULT = 0;
	public final static int SHOW_MODELS_ORIGINAL = 1;
	public final static int SHOW_MODELS_PRUNED = 2;
	public final static int SHOW_MODELS_OTHERS = 3;

	public final static String[] SHOW_INFO = {"Count", "CountByTarget", "Distribution", "Index", "NodePrototypes"};

	public final static int[] SHOW_INFO_VALUES = {0};

	public final static String[] CONVERT_RULES = { "No", "Pruned", "All" };

	public final static int CONVERT_RULES_NONE = 0;
	public final static int CONVERT_RULES_PRUNED = 1;
	public final static int CONVERT_RULES_ALL = 2;

	public static boolean SHOW_UNKNOWN_FREQ;
	public static boolean SHOW_BRANCH_FREQ;

/***********************************************************************
 * Section: Output - Write predictions to file                         *
 ***********************************************************************/

	public final static String[] WRITE_PRED = { "None", "Test", "Train" };

	public final static int[] WRITE_PRED_VALUES = {0};
	public final static int WRITE_PRED_NONE = 0;
	public final static int WRITE_PRED_TEST = 1;
	public final static int WRITE_PRED_TRAIN = 2;

/***********************************************************************
 * Section: Tree                                                       *
 ***********************************************************************/

	protected INIFileSection m_SectionTree;
	protected INIFileNominal m_Heuristic;
	protected INIFileInt m_TreeMaxDepth;
	protected INIFileBool m_BinarySplit;
	protected INIFileBool m_AlternativeSplits;
	protected INIFileDouble m_FTest;
	protected INIFileNominal m_PruningMethod;
	protected INIFileBool m_1SERule;
	protected INIFileBool m_MSENominal;
	protected INIFileDouble m_M5PruningMult;
	protected INIFileNominal m_RulesFromTree;

	public void setSectionTreeEnabled(boolean enable) {
		m_SectionTree.setEnabled(enable);
	}

	public int getHeuristic() {
		return m_Heuristic.getValue();
	}

	public void setHeuristic(int value) {
		m_Heuristic.setSingleValue(value);
	}

	public boolean checkHeuristic(String value) {
		return m_Heuristic.getStringSingle().equals(value);
	}

	public int getTreeMaxDepth() {
		return m_TreeMaxDepth.getValue();
	}

	public boolean isBinarySplit() {
		return m_BinarySplit.getValue();
	}

	public boolean showAlternativeSplits() {
		return m_AlternativeSplits.getValue();
	}

	public double getFTest() {
		return m_FTest.getValue();
	}

	public void setFTest(double ftest) {
		FTEST_VALUE = ftest;
		FTEST_LEVEL = FTest.getLevelAndComputeArray(ftest);
		m_FTest.setValue(ftest);
	}

	public int getPruningMethod() {
		return m_PruningMethod.getValue();
	}

	public void setPruningMethod(int method) {
		m_PruningMethod.setSingleValue(method);
	}

	public String getPruningMethodName() {
		return m_PruningMethod.getStringValue();
	}

	public boolean get1SERule() {
		return m_1SERule.getValue();
	}

	public boolean isMSENominal() {
		return m_MSENominal.getValue();
	}

	public double getM5PruningMult() {
		return m_M5PruningMult.getValue();
	}

	/**
	 * If we transform the induced trees to rules.
	 */
	public int rulesFromTree() {
		return m_RulesFromTree.getValue();
	}

/***********************************************************************
 * Section: Tree - Heuristic                                           *
 ***********************************************************************/

	public final static String[] HEURISTICS = { "Default", "ReducedError",
		"Gain", "GainRatio", "SSPD", "SSReduction", "MEstimate", "Morishita",
		"DispersionAdt", "DispersionMlt", "RDispersionAdt", "RDispersionMlt",
		"GeneticDistance", "SemiSupervised", "SSReductionMissing"};
	
	public final static int HEURISTIC_DEFAULT = 0;	
	public final static int HEURISTIC_REDUCED_ERROR = 1;	
	public final static int HEURISTIC_GAIN = 2;	
	public final static int HEURISTIC_GAIN_RATIO = 3;	

	public final static int HEURISTIC_SSPD = 4;
	/** Sum of Squared Distances, the default for ensemble tree regression learning */
	public final static int HEURISTIC_SS_REDUCTION = 5;
	public final static int HEURISTIC_MESTIMATE = 6;
	public final static int HEURISTIC_MORISHITA = 7;
	public final static int HEURISTIC_DISPERSION_ADT = 8;
	public final static int HEURISTIC_DISPERSION_MLT = 9;
	public final static int HEURISTIC_R_DISPERSION_ADT = 10;
	public final static int HEURISTIC_R_DISPERSION_MLT = 11;
	public final static int HEURISTIC_GENETIC_DISTANCE = 12;
	public final static int HEURISTIC_SEMI_SUPERVISED = 13;
	public final static int HEURISTIC_SS_REDUCTION_MISSING = 14;	

	public static int FTEST_LEVEL;
	public static double FTEST_VALUE;
	public static double MINIMAL_WEIGHT;
	public static boolean ONE_NOMINAL = true;

/***********************************************************************
 * Section: Tree - Pruning method                                      *
 ***********************************************************************/

	public final static String[] PRUNING_METHODS = { "Default", "None", "C4.5",
		"M5", "M5Multi", "ReducedErrorVSB", "Garofalakis", "GarofalakisVSB",
		"CartVSB", "CartMaxSize" };

	public final static int PRUNING_METHOD_DEFAULT = 0;
	public final static int PRUNING_METHOD_NONE = 1;
	public final static int PRUNING_METHOD_C45 = 2;
	public final static int PRUNING_METHOD_M5 = 3;
	public final static int PRUNING_METHOD_M5_MULTI = 4;
	public final static int PRUNING_METHOD_REDERR_VSB = 5;
	public final static int PRUNING_METHOD_GAROFALAKIS = 6;
	public final static int PRUNING_METHOD_GAROFALAKIS_VSB = 7;
	public final static int PRUNING_METHOD_CART_VSB = 8;
	public final static int PRUNING_METHOD_CART_MAXSIZE = 9;

/***********************************************************************
 * Section: Rules                                                      *
 ***********************************************************************/

	public final static String[] COVERING_METHODS =	{"Standard", "WeightedMultiplicative",
		"WeightedAdditive", "WeightedError", "Union", "BeamRuleDefSet",
		"RandomRuleSet", "StandardBootstrap", "HeurOnly", "RulesFromTree"};

	// Standard covering: ordered rules (decision list)
	public final static int COVERING_METHOD_STANDARD = 0;

	// 'Weighted' coverings: unordered rules
	public final static int COVERING_METHOD_WEIGHTED_MULTIPLICATIVE = 1;
	public final static int COVERING_METHOD_WEIGHTED_ADDITIVE = 2;
	public final static int COVERING_METHOD_WEIGHTED_ERROR = 3;

	/**
	 *  In multi-label classification: predicted set of classes is union
	 *  of predictions of individual rules
	 */
	public final static int COVERING_METHOD_UNION = 4;

	// Evaluates rules in the context of complete rule set: unordered rules
	// public final static int COVERING_METHOD_RULE_SET = 5;

	// Evaluates rules in the context of complete rule set, checks all rules
	// in the beam: unordered rules
	// public final static int COVERING_METHOD_BEAM_RULE_SET = 6;

	/** Evaluates rules in the context of complete rule set, builds default
	 *  rule first, checks all rules in the beam: unordered rules
	 *  FIXME Obsolete - should be deleted!
	 */
	public final static int COVERING_METHOD_BEAM_RULE_DEF_SET = 5;

	/**
	 *  Evaluates rules in the context of complete rule set, separate rules
	 *  are constructed randomly: unordered rules.
	 *  This is set only if the amount of RandomRules is greater than 0.
	 */ 
	public final static int COVERING_METHOD_RANDOM_RULE_SET = 6;

	/** 
	 * Repeated standard covering on bootstraped data
	 */
	public final static int COVERING_METHOD_STANDARD_BOOTSTRAP = 7;

	/**
	 *  No covering, only heuristic
	 */
	public final static int COVERING_METHOD_HEURISTIC_ONLY = 8;

	/**
	 *  No covering, rules transcribed from tree
	 */
	public final static int COVERING_METHOD_RULES_FROM_TREE = 9;

	public final static String[] RULE_PREDICTION_METHODS =
	{"DecisionList", "TotCoverageWeighted", "CoverageWeighted", "AccuracyWeighted",
		"AccCovWeighted", "EquallyWeighted", "Optimized", "Union"};

	public final static int RULE_PREDICTION_METHOD_DECISION_LIST = 0;
	
	/**
	 *  Each rule's prediction has a weight proportional to its coverage on the total learning set
	 */
	public final static int RULE_PREDICTION_METHOD_TOT_COVERAGE_WEIGHTED = 1;
	
	/**
	 *  Each rule's prediction has a weight proportional to its coverage on the current learning set
	 *  i.e., learning set on which the rule was learned
	 */
	public final static int RULE_PREDICTION_METHOD_COVERAGE_WEIGHTED = 2;
	
	/**
	 *  Each rule's prediction has a weight proportional to its accuracy on the total learning set
	 *  TODO Not yet implemented.
	 */
	public final static int RULE_PREDICTION_METHOD_ACCURACY_WEIGHTED = 3;

	/**
	 *  Each rule's prediction has a weight proportional a product of to its accuracy on
	 *  the total learning set and its coverage
	 */
	public final static int RULE_PREDICTION_METHOD_ACC_COV_WEIGHTED = 4;
	
	/**
	 *  TODO Not yet implemented.
	 */
	public final static int RULE_PREDICTION_METHOD_EQUALLY_WEIGHTED = 5;
	public final static int RULE_PREDICTION_METHOD_OPTIMIZED = 6;
	public final static int RULE_PREDICTION_METHOD_UNION = 7;

	public final static String[] RULE_ADDING_METHODS =	{"Always", "IfBetter", "IfBetterBeam"};

	// Always adds a rule to the rule set
	public final static int RULE_ADDING_METHOD_ALWAYS = 0;
	// Only adds a rule to the rule set if it improves the rule set performance
	public final static int RULE_ADDING_METHOD_IF_BETTER = 1;
	// Only adds a rule to the rule set if it improves the rule set performance.
	// If not, it checks other rules in the beam
	public final static int RULE_ADDING_METHOD_IF_BETTER_BEAM = 2;

	public static boolean IS_RULE_SIG_TESTING = false;

	/**
	 * Possible loss functions for evolutionary algorithm optimization
	 */
	public final static String[] DE_LOSS_FUNCTIONS = {"Squared", "01Error", "RRMSE", "Huber"};
	
	/**	DE Loss function type. Default for regression: Square of differences. */
	public final static int DE_LOSS_FUNCTIONS_SQUARED = 0;
	/**	DE Loss function type. 0/1 error for classification. Zenko 2007, p. 26*/
	public final static int DE_LOSS_FUNCTIONS_01ERROR = 1;
	/**	DE Loss function type. Relative root mean squared error */
	public final static int DE_LOSS_FUNCTIONS_RRMSE = 2;
	/**	DE Loss function type. Huber 1962 error. Like squared but robust for outliers. Friedman&Popescu 2005, p. 7*/
	public final static int DE_LOSS_FUNCTIONS_HUBER = 3;
	
	// Settings in the settings file.
	protected INIFileNominal m_CoveringMethod;
	protected INIFileNominal m_PredictionMethod;
	protected INIFileNominal m_RuleAddingMethod;
	protected INIFileDouble m_CoveringWeight;
	protected INIFileDouble m_InstCoveringWeightThreshold;
	protected INIFileInt m_MaxRulesNb;
	protected INIFileDouble m_HeurDispOffset;
	protected INIFileDouble m_HeurCoveragePar;
	protected INIFileDouble m_HeurRuleDistPar;
	protected INIFileDouble m_HeurPrototypeDistPar;
	protected INIFileDouble m_RuleSignificanceLevel;
	protected INIFileInt m_RuleNbSigAtts;
	protected INIFileBool m_ComputeDispersion;
	protected INIFileDouble m_VarBasedDispNormWeight;
	protected INIFileNominalOrDoubleOrVector m_DispersionWeights;
	/** How many random rules are wanted. If > 0 only random rules are generated */
	protected INIFileInt m_RandomRules;
	protected INIFileBool m_RuleWiseErrors;

	//	Differential evolution optimization
	/**	DE Number of individuals (population) during every iteration */
	protected INIFileInt m_OptDEPopSize;
	/**	Differential evolution, number of individual evaluations to be done. Divide this with m_OptDEPopSize 
	 * to get the number of 'iterations' */
	protected INIFileInt m_OptDENumEval;
	/** DE Crossover probability */
	protected INIFileDouble m_OptDECrossProb;
	protected INIFileDouble m_OptDEWeight;
	protected INIFileInt m_OptDESeed;
	/** DE Optimization regularization parameter */
	protected INIFileDouble m_OptRegPar;
	/** DE The treshold for rule weights. If weight < this, rule is removed. */
	protected INIFileDouble m_OptRuleWeightThreshold;
	/** DE The loss function. The default is squared loss. */
	protected INIFileNominal m_OptDELossFunction;
	/** DE The power of regularization function. The default is 1, i.e. l1 norm. */
	protected INIFileDouble m_OptDERegulPower;
	/** DE For Huber 1962 loss function an alpha value for outliers has to be given. */
	protected INIFileDouble m_OptDEHuberAlpha;
	/** DE A probability to mutate certain value to zero. Useful if zero weights are wanted */
	protected INIFileDouble m_DEProbMutationZero;
	/** DE A reverse for the zeroing. A probability to mutate certain value to nonzero random value.
	 * Could be used if zeroing is used. */
	protected INIFileDouble m_DEProbMutationNonZero;	
	
	

	
	
	public INIFileNominalOrDoubleOrVector getDispersionWeights() {
		return m_DispersionWeights;
	}

	/**
	 * Returns if random rules are wanted. That is RandomRules in settings file is above 0.
	 */
	public boolean isRandomRules() {
	    return (m_RandomRules.getValue() > 0);
	}

	/**
	 * How many random rules are wanted by RandomRules in the settings file.
	 */
	public int nbRandomRules() {
	    return m_RandomRules.getValue();
	}

	public boolean isRuleWiseErrors() {
	  	return m_RuleWiseErrors.getValue();
	}

	public int getCoveringMethod() {
	    return m_CoveringMethod.getValue();
	}

	public void setCoveringMethod(int method) {
	    m_CoveringMethod.setSingleValue(method);
	}

	public int getRulePredictionMethod() {
	    return m_PredictionMethod.getValue();
	}

	public void setRulePredictionMethod(int method) {
	    m_PredictionMethod.setSingleValue(method);
	}

	public int getRuleAddingMethod() {
	    return m_RuleAddingMethod.getValue();
	}

	public void setRuleAddingMethod(int method) {
	    m_RuleAddingMethod.setSingleValue(method);
	}

	public double getCoveringWeight() {
	    return m_CoveringWeight.getValue();
	}

	public void setCoveringWeight(double weight) {
	    m_CoveringWeight.setValue(weight);
	}

	public double getInstCoveringWeightThreshold() {
	    return m_InstCoveringWeightThreshold.getValue();
	}

	public void setInstCoveringWeightThreshold(double thresh) {
	  	m_InstCoveringWeightThreshold.setValue(thresh);
	}

	public int getMaxRulesNb() {
	    return m_MaxRulesNb.getValue();
	}

	public void setMaxRulesNb(int nb) {
	    m_MaxRulesNb.setValue(nb);
	}

	public double getRuleSignificanceLevel() {
	    return m_RuleSignificanceLevel.getValue();
	}

	public int getRuleNbSigAtt() {
	    return m_RuleNbSigAtts.getValue();
	}

	public boolean isRuleSignificanceTesting() {
		return m_RuleNbSigAtts.getValue() != 0;
	}

	public double getHeurDispOffset() {
		return m_HeurDispOffset.getValue();
	}

	public double getHeurCoveragePar() {
		return m_HeurCoveragePar.getValue();
	}

	public double getHeurRuleDistPar() {
		return m_HeurRuleDistPar.getValue();
	}

	public void setHeurRuleDistPar(double value) {
	    m_HeurRuleDistPar.setValue(value);
	}

	public boolean isHeurRuleDist() {
	    return m_HeurRuleDistPar.getValue() > 0;
	}

	public boolean isWeightedCovering() {
		if (m_CoveringMethod.getValue() == COVERING_METHOD_WEIGHTED_ADDITIVE ||
			m_CoveringMethod.getValue() == COVERING_METHOD_WEIGHTED_MULTIPLICATIVE ||
			m_CoveringMethod.getValue() == COVERING_METHOD_WEIGHTED_ERROR) {
			return true;
		} else {
			return false;
		}
	}

  	public double getHeurPrototypeDistPar() {
	  	return m_HeurPrototypeDistPar.getValue();
  	}

  	public void setHeurPrototypeDistPar(double value) {
		m_HeurPrototypeDistPar.setValue(value);
	}

	public boolean isHeurPrototypeDistPar() {
	 	return m_HeurPrototypeDistPar.getValue() > 0;
	}

	public void disableRuleInduceParams() {
		setHeurRuleDistPar(0.0);
		setRulePredictionMethod(RULE_PREDICTION_METHOD_DECISION_LIST);
		setCoveringMethod(COVERING_METHOD_RULES_FROM_TREE);
	}

	public boolean computeDispersion() {
		return m_ComputeDispersion.getValue();
  	}

  	public double getVarBasedDispNormWeight() {
  		return m_VarBasedDispNormWeight.getValue();
  	}

	public double getOptDECrossProb() {
		return m_OptDECrossProb.getValue();
	}

	public int getOptDENumEval() {
		return m_OptDENumEval.getValue();
	}

	public int getOptDEPopSize() {
		return m_OptDEPopSize.getValue();
	}

	public int getOptDESeed() {
		return m_OptDESeed.getValue();
	}

	public double getOptDEWeight() {
		return m_OptDEWeight.getValue();
	}

	/** Optimization regularization parameter */
	public double getOptRegPar() {
		return m_OptRegPar.getValue();
	}

	public double getOptRuleWeightThreshold() {
		return m_OptRuleWeightThreshold.getValue();
	}
	
	/** Type of Loss function for DE optimization */
	public int getOptDELossFunction() {
		return m_OptDELossFunction.getValue();
	}
	
	/** Power for regularization parameter */
	public double getOptDERegulPower() {
		return m_OptDERegulPower.getValue();
	}

	/** DE For Huber 1962 loss function an alpha value for outliers has to be given. */
	public double getOptDEHuberAlpha() {
		return m_OptDEHuberAlpha.getValue();
	}
	
	/** DE A probability to mutate certain value to zero. Useful if zero weights are wanted */
	public double getDEProbMutationZero()	{
		return m_DEProbMutationZero.getValue();
	}

	/** DE A reverse for the zeroing. A probability to mutate certain value to nonzero random value.
	 * Could be used if zeroing is used. */
	public double getDEProbMutationNonZero() {
		return m_DEProbMutationNonZero.getValue();
	}
	
//	protected INIFileNominal m_OptDELossFunction;
	/** DE The power of regularization function. The default is 1, i.e. l1 norm. */
//	protected INIFileDouble m_OptDERegulPower;

/***********************************************************************
 * Section: Hierarchical multi-label classification                    *
 ***********************************************************************/

	// Hierarchical multi-classification now supports both trees and DAGS
	// This was required because Gene Ontology terms are organized in a partial order
	public final static String[] HIERTYPES = { "Tree", "DAG" };

	public final static int HIERTYPE_TREE = 0;
	public final static int HIERTYPE_DAG = 1;

    public final static String[] HIERWEIGHT = { "ExpSumParentWeight", "ExpAvgParentWeight", "ExpMinParentWeight", "ExpMaxParentWeight", "NoWeight" };

    public final static int HIERWEIGHT_EXP_SUM_PARENT_WEIGHT = 0;
    public final static int HIERWEIGHT_EXP_AVG_PARENT_WEIGHT = 1;
    public final static int HIERWEIGHT_EXP_MIN_PARENT_WEIGHT = 2;
    public final static int HIERWEIGHT_EXP_MAX_PARENT_WEIGHT = 3;
    public final static int HIERWEIGHT_NO_WEIGHT = 4;


	INIFileSection m_SectionHierarchical;
	protected INIFileNominal m_HierType;
	protected INIFileNominal m_HierWType;
	protected INIFileDouble m_HierWParam;
	protected INIFileString m_HierSep;
	protected INIFileString m_HierEmptySetIndicator;
	protected INIFileString m_DefinitionFile;
	protected INIFileBool m_HierNoRootPreds;
	protected INIFileDouble m_HierPruneInSig;
	protected INIFileBool m_HierUseBonferroni;
	protected INIFileNominalOrDoubleOrVector m_HierClassThreshold;
	protected INIFileString m_HierEvalClasses;

	public void setSectionHierarchicalEnabled(boolean enable) {
		m_SectionHierarchical.setEnabled(enable);
	}

	public int getHierType() {
		return m_HierType.getValue();
	}

	public int getHierWType() {
		return m_HierWType.getValue();
	}

	public double getHierWParam() {
		return m_HierWParam.getValue();
	}

	public INIFileNominalOrDoubleOrVector getClassificationThresholds() {
		return m_HierClassThreshold;
	}

	public boolean isHierNoRootPreds() {
		return m_HierNoRootPreds.getValue();
	}

	public boolean isUseBonferroni() {
		return m_HierUseBonferroni.getValue();
	}

	public double getHierPruneInSig() {
		return m_HierPruneInSig.getValue();
	}

	public boolean hasHierEvalClasses() {
		return !StringUtils.unCaseCompare(m_HierEvalClasses.getValue(), NONE);
	}

	public String getHierEvalClasses() {
		return m_HierEvalClasses.getValue();
	}

	public boolean hasDefinitionFile() {
		return !StringUtils.unCaseCompare(m_DefinitionFile.getValue(), NONE);
	}

	public String getDefinitionFile() {
		return m_DefinitionFile.getValue();
	}

	public void initHierarchical() {
		ClassesValue.setHSeparator(m_HierSep.getValue());
		ClassesValue.setEmptySetIndicator(m_HierEmptySetIndicator.getValue());
	}

/***********************************************************************
 * Section: Instance level constraints                                 *
 ***********************************************************************/

	protected INIFileSection m_SectionILevelC;
	protected INIFileString m_ILevelCFile;
	protected INIFileDouble m_ILevelCAlpha;
	protected INIFileInt m_ILevelNbRandomConstr;
	protected INIFileBool m_ILevelCCOPKMeans;
	protected INIFileBool m_ILevelCMPCKMeans;

	public boolean isSectionILevelCEnabled() {
		return m_SectionILevelC.isEnabled();
	}

	public boolean hasILevelCFile() {
		return !StringUtils.unCaseCompare(m_ILevelCFile.getValue(), NONE);
	}

	public String getILevelCFile() {
		return m_ILevelCFile.getValue();
	}

	public double getILevelCAlpha() {
		return m_ILevelCAlpha.getValue();
	}

	public int getILevelCNbRandomConstraints() {
		return m_ILevelNbRandomConstr.getValue();
	}

	public boolean isILevelCCOPKMeans() {
		return m_ILevelCCOPKMeans.getValue();
	}

	public boolean isILevelCMPCKMeans() {
		return m_ILevelCMPCKMeans.getValue();
	}

/***********************************************************************
 * Section: Beam search                                                *
 ***********************************************************************/

	public static int BEAM_WIDTH;
	public static double SIZE_PENALTY;
	public static double BEAM_SIMILARITY;
	public static boolean BEAM_SYNT_DIST_CONSTR;

	protected INIFileSection m_SectionBeam;
	protected INIFileDouble m_SizePenalty;
	protected INIFileInt m_BeamWidth;
	protected INIFileInt m_BeamBestN;
	protected INIFileInt m_TreeMaxSize;
	protected INIFileNominal m_BeamAttrHeuristic;
	protected INIFileBool m_FastBS;
	protected INIFileBool m_BeamPostPrune;
	protected INIFileBool m_BMRemoveEqualHeur;
	protected INIFileDouble m_BeamSimilarity;
	protected INIFileBool m_BSortTrainParameter;
	protected INIFileBool m_BeamToForest;
	protected INIFileString m_BeamSyntacticConstrFile;

	public void setSectionBeamEnabled(boolean enable) {
		m_SectionBeam.setEnabled(enable);
	}

	public int getBeamWidth() {
		return m_BeamWidth.getValue();
	}

	public double getSizePenalty() {
		return m_SizePenalty.getValue();
	}

	public int getBeamBestN() {
		return m_BeamBestN.getValue();
	}

	public int getBeamTreeMaxSize() {
		return m_TreeMaxSize.getValue();
	}

	public boolean getBeamRemoveEqualHeur() {
		return m_BMRemoveEqualHeur.getValue();
	}

	public boolean getBeamSortOnTrainParameter() {
		return m_BSortTrainParameter.getValue();
	}

	public double getBeamSimilarity(){
		return m_BeamSimilarity.getValue();
	}

	public boolean isBeamPostPrune() {
		return m_BeamPostPrune.getValue();
	}

	public int getBeamAttrHeuristic() {
		return m_BeamAttrHeuristic.getValue();
	}

	public boolean hasBeamConstraintFile() {
		return !StringUtils.unCaseCompare(m_BeamSyntacticConstrFile.getValue(),	NONE);
	}

	public String getBeamConstraintFile() {
		return m_BeamSyntacticConstrFile.getValue();
	}

	public boolean isBeamToForest(){
		return m_BeamToForest.getValue();
	}

	public boolean isFastBS() {
		return m_FastBS.getValue();
	}

/***********************************************************************
 * Section: Exhaustive search                                          *
 ***********************************************************************/

	protected INIFileSection m_SectionExhaustive;
	protected INIFileBool m_Exhaustive;
	protected INIFileInt m_StartTreeCpt;
	protected INIFileInt m_StartItemCpt;

	public void setSectionExhaustiveEnabled(boolean enable) {
		m_SectionExhaustive.setEnabled(enable);
	}

	public boolean isExhaustiveSearch() {
		return m_Exhaustive.getValue();
	}

	public int getStartTreeCpt() {
		return m_StartTreeCpt.getValue();
	}

	public int getStartItemCpt() {
		return m_StartItemCpt.getValue();
	}

/***********************************************************************
 * Section: Time series                                                *
 ***********************************************************************/

	public final static String[] TIME_SERIES_DISTANCE_MEASURE={"DTW","QDM","TSC"};

	public final static int TIME_SERIES_DISTANCE_MEASURE_DTW = 0;
	public final static int TIME_SERIES_DISTANCE_MEASURE_QDM = 1;
	public final static int TIME_SERIES_DISTANCE_MEASURE_TSC = 2;

	public final static String[] TIME_SERIES_PROTOTYPE_COMPLEXITY={"N2", "LOG", "LINEAR", "NPAIRS", "TEST"};

	INIFileSection m_SectionTimeSeries;
	public static INIFileNominal m_TimeSeriesDM;
	public static INIFileNominal m_TimeSeriesProtoComlexity;

	public boolean isSectionTimeSeriesEnabled() {
		return m_SectionTimeSeries.isEnabled();
	}

	public void setSectionTimeSeriesEnabled(boolean enable) {
		m_SectionTimeSeries.setEnabled(enable);
	}

	public static boolean isTimeSeriesProtoComlexityExact() {
		if (m_TimeSeriesProtoComlexity.getValue() == 0) {
			return true;
		} else {
			return false;
		}
	}

/***********************************************************************
 * Section: Ensemble methods                                           *
 ***********************************************************************/

	public final static String[] ENSEMBLE_TYPE = {"Bagging", "RForest", "RSubspaces", "BagSubspaces", "Boosting"};

	public final static int ENSEMBLE_BAGGING = 0;
	public final static int ENSEMBLE_RFOREST = 1;
	public final static int ENSEMBLE_RSUBSPACES = 2;
	public final static int ENSEMBLE_BAGSUBSPACES = 3;
	public final static int ENSEMBLE_BOOSTING = 4;

	public final static String[] VOTING_TYPE={"Majority","ProbabilityDistribution"};

	public final static int VOTING_TYPE_MAJORITY = 0;
	public final static int VOTING_TYPE_PROBAB_DISTR = 1;

	INIFileSection m_SectionEnsembles;
	protected INIFileNominalOrIntOrVector m_NbBags;
	public static INIFileNominal m_EnsembleMethod;
	public static INIFileNominal m_ClassificationVoteType;
	protected INIFileInt m_RandomAttrSelected;
	public static INIFileBool m_PrintAllModels;
	public static boolean m_EnsembleMode = false;
	public static INIFileBool m_EnsembleShouldOpt;
	public static INIFileBool m_EnsembleOOBestimate;
	protected INIFileBool m_FeatureRanking;


	public boolean isEnsembleMode() {
		return m_EnsembleMode;
	}

	public void setEnsembleMode(boolean value) {
		m_EnsembleMode = value;
	}

	public int getEnsembleMethod() {
		return m_EnsembleMethod.getValue();
	}

	public void setEnsembleMethod(String value){
		m_EnsembleMethod.setValue(value);
	}
	
	public void setEnsembleMethod(int value){
		m_EnsembleMethod.setSingleValue(value);
	}
	
	public boolean shouldPerformRanking(){
		return m_FeatureRanking.getValue();
	}
	
	public void setFeatureRanking(boolean value){
		m_FeatureRanking.setValue(value);
	}
	
	public INIFileNominalOrIntOrVector getNbBaggingSets(){
		if (!m_NbBags.isVector()&&(m_NbBags.getInt() == 0))m_NbBags.setInt(10);
		return m_NbBags;
	}

	public void setNbBags(int value){
		m_NbBags.setInt(value);
	}
	
	public int getNbRandomAttrSelected() {
		return m_RandomAttrSelected.getValue();
	}

	public void setNbRandomAttrSelected(int value) {
		m_RandomAttrSelected.setValue(value);
	}

	public static boolean isPrintEnsembleModels( ){
		return m_PrintAllModels.getValue();
	}

	public static boolean shouldOptimizeEnsemble( ){
		return m_EnsembleShouldOpt.getValue();
	}

	public static boolean shouldEstimateOOB( ){
		return m_EnsembleOOBestimate.getValue();
	}

	public void setOOBestimate(boolean value){
		m_EnsembleOOBestimate.setValue(value);
	}

/***********************************************************************
 * Section: KNN                                                        *
 ***********************************************************************/

	INIFileSection m_SectionKNN;
	public static INIFileInt kNN_k;
	public static INIFileString kNN_vectDist;
	public static INIFileBool kNN_distWeighted;
	public static INIFileBool kNN_normalized;
	public static INIFileBool kNN_attrWeighted;

	public void setSectionKNNEnabled(boolean enable) {
		m_SectionKNN.setEnabled(enable);
	}

/***********************************************************************
 * Section: KNN Trees                                                  *
 ***********************************************************************/

	INIFileSection m_SectionKNNT;
	public static INIFileInt kNNT_k;
	public static INIFileString kNNT_vectDist;
	public static INIFileBool kNNT_distWeighted;
	public static INIFileBool kNNT_normalized;
	public static INIFileBool kNNT_attrWeighted;

	public void setSectionKNNTEnabled(boolean enable) {
		m_SectionKNNT.setEnabled(enable);
	}

/***********************************************************************
 * Cross-validaiton                                                    *
 ***********************************************************************/

	public static boolean SHOW_XVAL_FOREST;
	public static boolean XVAL_OVERLAP = true;
	public static boolean IS_XVAL = false;

/***********************************************************************
 * Create the settings structure                                       *
 ***********************************************************************/

	public void create() {
		INIFileSection settings = new INIFileSection("General");
		settings.addNode(m_Verbose = new INIFileInt("Verbose", 1));
		settings.addNode(m_Compatibility = new INIFileNominal("Compatibility", COMPATIBILITY, 0));
		settings.addNode(m_RandomSeed = new INIFileString("RandomSeed", "0"));
		settings.addNode(m_ResourceInfoLoaded = new INIFileNominal("ResourceInfoLoaded", RESOURCE_INFO_LOAD, 1));

		INIFileSection data = new INIFileSection("Data");
		data.addNode(m_DataFile = new INIFileString("File", NONE));
		data.addNode(m_TestSet = new INIFileStringOrDouble("TestSet", NONE));
		data.addNode(m_PruneSet = new INIFileStringOrDouble("PruneSet", NONE));
		data.addNode(m_PruneSetMax = new INIFileStringOrInt("PruneSetMax", INFINITY_STRING));
		data.addNode(m_XValFolds = new INIFileStringOrInt("XVal"));
		m_XValFolds.setIntValue(10);
		data.addNode(m_RemoveMissingTarget = new INIFileBool("RemoveMissingTarget", false));

		INIFileSection attrs = new INIFileSection("Attributes");
		attrs.addNode(m_Target = new INIFileString("Target", DEFAULT));
		attrs.addNode(m_Clustering = new INIFileString("Clustering", DEFAULT));
		attrs.addNode(m_Descriptive = new INIFileString("Descriptive", DEFAULT));
		attrs.addNode(m_Key = new INIFileString("Key", NONE));
		attrs.addNode(m_Disabled = new INIFileString("Disable", NONE));
		attrs.addNode(m_Weights = new INIFileNominalOrDoubleOrVector("Weights",	NORMALIZATIONS));
		m_Weights.setNominal(NORMALIZATION_DEFAULT);
		attrs.addNode(m_ClusteringWeights = new INIFileNominalOrDoubleOrVector("ClusteringWeights", EMPTY));
		m_ClusteringWeights.setDouble(1.0);
		m_ClusteringWeights.setArrayIndexNames(NUM_NOM_TAR_NTAR_WEIGHTS);
		attrs.addNode(m_ReduceMemoryNominal = new INIFileBool("ReduceMemoryNominalAttrs", false));

		m_SectionSIT = new INIFileSection("SIT");
		m_SectionSIT.addNode(m_MainTarget = new INIFileString("Main_target", DEFAULT));
		m_SectionSIT.addNode(m_Recursive = new INIFileBool("Recursive",false));
		m_SectionSIT.addNode(m_Search = new INIFileString("Search","OneTarget"));
		m_SectionSIT.addNode(m_Learner = new INIFileString("Learner","ClusLearner"));
		m_SectionSIT.addNode(m_Error = new INIFileString("Error","MSE"));

    	INIFileSection constr = new INIFileSection("Constraints");
		constr.addNode(m_SyntacticConstrFile = new INIFileString("Syntactic",	NONE));
		constr.addNode(m_MaxSizeConstr = new INIFileNominalOrIntOrVector("MaxSize", INFINITY));
		constr.addNode(m_MaxErrorConstr = new INIFileNominalOrDoubleOrVector("MaxError", INFINITY));
		m_MaxSizeConstr.setNominal(0);
		m_MaxErrorConstr.setDouble(0);

		INIFileSection output = new INIFileSection("Output");
		output.addNode(m_ShowModels = new INIFileNominal("ShowModels", SHOW_MODELS, SHOW_MODELS_VALUES));
		output.addNode(m_OutTrainErr = new INIFileBool("TrainErrors", true));
		output.addNode(m_OutFoldModels = new INIFileBool("AllFoldModels", true));
		output.addNode(m_OutFoldErr = new INIFileBool("AllFoldErrors", false));
		output.addNode(m_WriteErrorFile = new INIFileBool("WriteErrorFile", false));
		output.addNode(m_ShowUnknown = new INIFileBool("UnknownFrequency", false));
		output.addNode(m_ShowBrFreq = new INIFileBool("BranchFrequency", false));
		output.addNode(m_ShowInfo = new INIFileNominal("ShowInfo", SHOW_INFO, SHOW_INFO_VALUES));
		output.addNode(m_PrintModelAndExamples = new INIFileBool("PrintModelAndExamples", false));
		output.addNode(m_WritePredictions = new INIFileNominal("WritePredictions", WRITE_PRED, WRITE_PRED_VALUES));
		output.addNode(m_ModelIDFiles = new INIFileBool("ModelIDFiles", false));
		output.addNode(m_OutputPythonModel = new INIFileBool("OutputPythonModel", false));
		output.addNode(m_OutputDatabaseQueries = new INIFileBool("OutputDatabaseQueries", false));

		INIFileSection nominal = new INIFileSection("Nominal");
		nominal.addNode(m_MEstimate = new INIFileDouble("MEstimate", 1.0));

		INIFileSection model = new INIFileSection("Model");
		model.addNode(m_MinW = new INIFileDouble("MinimalWeight", 2.0));
		model.addNode(m_TuneFolds = new INIFileString("ParamTuneNumberFolds", "10"));
		model.addNode(m_ClassWeight = new INIFileNominalOrDoubleOrVector("ClassWeights", EMPTY));
		model.addNode(m_NominalSubsetTests = new INIFileBool("NominalSubsetTests", true));

		m_SectionTree = new INIFileSection("Tree");
		m_SectionTree.addNode(m_Heuristic = new INIFileNominal("Heuristic", HEURISTICS, 0));
		m_SectionTree.addNode(m_FTest = new INIFileDouble("FTest", 1.0));
		m_SectionTree.addNode(m_TreeMaxDepth = new INIFileInt("MaxDepth", -1));
		m_SectionTree.addNode(m_BinarySplit = new INIFileBool("BinarySplit", true));
		m_SectionTree.addNode(m_PruningMethod = new INIFileNominal("PruningMethod", PRUNING_METHODS, 0));
		m_SectionTree.addNode(m_1SERule = new INIFileBool("1-SE-Rule", false));
		m_SectionTree.addNode(m_MSENominal = new INIFileBool("MSENominal", false));
		m_SectionTree.addNode(m_M5PruningMult = new INIFileDouble("M5PruningMult", 2.0));
		m_SectionTree.addNode(m_RulesFromTree = new INIFileNominal("ConvertToRules", CONVERT_RULES, 0));
		m_SectionTree.addNode(m_AlternativeSplits = new INIFileBool("AlternativeSplits", false));

		INIFileSection rules = new INIFileSection("Rules");
		rules.addNode(m_CoveringMethod = new INIFileNominal("CoveringMethod", COVERING_METHODS, 0));
		rules.addNode(m_PredictionMethod = new INIFileNominal("PredictionMethod", RULE_PREDICTION_METHODS, 0));
		rules.addNode(m_RuleAddingMethod = new INIFileNominal("RuleAddingMethod", RULE_ADDING_METHODS, 0));
		rules.addNode(m_CoveringWeight = new INIFileDouble("CoveringWeight", 0.9));
		rules.addNode(m_InstCoveringWeightThreshold = new INIFileDouble("InstCoveringWeightThreshold", 0.1));
		rules.addNode(m_MaxRulesNb = new INIFileInt("MaxRulesNb", 1000));
	    rules.addNode(m_HeurDispOffset = new INIFileDouble("HeurDispOffset", 0.0));
		rules.addNode(m_HeurCoveragePar = new INIFileDouble("HeurCoveragePar", 1.0));
		rules.addNode(m_HeurRuleDistPar = new INIFileDouble("HeurRuleDistPar", 0.0));
		rules.addNode(m_HeurPrototypeDistPar = new INIFileDouble("HeurPrototypeDistPar", 0.0));
		rules.addNode(m_RuleSignificanceLevel = new INIFileDouble("RuleSignificanceLevel", 0.05));
		rules.addNode(m_RuleNbSigAtts = new INIFileInt("RuleNbSigAtts", 0));
		rules.addNode(m_ComputeDispersion = new INIFileBool("ComputeDispersion", false));
		rules.addNode(m_VarBasedDispNormWeight = new INIFileDouble("VarBasedDispNormWeight", 4.0));
		rules.addNode(m_DispersionWeights = new INIFileNominalOrDoubleOrVector("DispersionWeights", EMPTY));
		m_DispersionWeights.setArrayIndexNames(NUM_NOM_TAR_NTAR_WEIGHTS);
		m_DispersionWeights.setDoubleArray(FOUR_ONES);
		m_DispersionWeights.setArrayIndexNames(true);
		rules.addNode(m_RandomRules = new INIFileInt("RandomRules", 0));
		rules.addNode(m_RuleWiseErrors = new INIFileBool("PrintRuleWiseErrors", false));
		rules.addNode(m_OptDEPopSize = new INIFileInt("OptDEPopSize", 500));
		rules.addNode(m_OptDENumEval = new INIFileInt("OptDENumEval", 10000));
		rules.addNode(m_OptDECrossProb = new INIFileDouble("OptDECrossProb", 0.3));
		rules.addNode(m_OptDEWeight = new INIFileDouble("OptDEWeight", 0.5));
		rules.addNode(m_OptDESeed = new INIFileInt("OptDESeed", 0));
		rules.addNode(m_OptRegPar = new INIFileDouble("OptRegPar", 0.0));
		rules.addNode(m_OptRuleWeightThreshold = new INIFileDouble("OptRuleWeightThreshold", 0.1));
		rules.addNode(m_OptDELossFunction = new INIFileNominal("OptDELossFunction",DE_LOSS_FUNCTIONS, 0));
		rules.addNode(m_OptDERegulPower = new INIFileDouble("OptDERegulPower", 1.0));
		rules.addNode(m_OptDEHuberAlpha = new INIFileDouble("OptDEHuberAlpha", 0.9));
		rules.addNode(m_DEProbMutationZero = new INIFileDouble("DEProbMutationZero", 0.0));
		rules.addNode(m_DEProbMutationNonZero = new INIFileDouble("DEProbMutationNonZero", 0.0));
		
		rules.setEnabled(false);

		m_SectionHierarchical = new INIFileSection("Hierarchical");
		m_SectionHierarchical.addNode(m_HierType = new INIFileNominal("Type", HIERTYPES, 0));
		m_SectionHierarchical.addNode(m_HierWType = new INIFileNominal("WType", HIERWEIGHT, 0));
		m_SectionHierarchical.addNode(m_HierWParam = new INIFileDouble("WParam", 0.75));
		m_SectionHierarchical.addNode(m_HierSep = new INIFileString("HSeparator", "."));
		m_SectionHierarchical.addNode(m_HierEmptySetIndicator = new INIFileString("EmptySetIndicator", "n"));
		m_SectionHierarchical.addNode(m_DefinitionFile = new INIFileString("DefinitionFile", NONE));
		m_SectionHierarchical.addNode(m_HierNoRootPreds = new INIFileBool("NoRootPredictions", false));
		m_SectionHierarchical.addNode(m_HierPruneInSig = new INIFileDouble("PruneInSig", 0.0));
		m_SectionHierarchical.addNode(m_HierUseBonferroni = new INIFileBool("Bonferroni", false));
		m_SectionHierarchical.addNode(m_HierClassThreshold = new INIFileNominalOrDoubleOrVector("ClassificationTreshold", NONELIST));
		m_HierClassThreshold.setNominal(0);
		m_SectionHierarchical.addNode(m_HierEvalClasses = new INIFileString("EvalClasses", NONE));
		m_SectionHierarchical.setEnabled(false);

		m_SectionILevelC = new INIFileSection("ILevelC");
		m_SectionILevelC.addNode(m_ILevelCAlpha = new INIFileDouble("Alpha", 0.5));
		m_SectionILevelC.addNode(m_ILevelCFile = new INIFileString("File", NONE));
		m_SectionILevelC.addNode(m_ILevelNbRandomConstr = new INIFileInt("NbRandomConstraints", 0));
		m_SectionILevelC.addNode(m_ILevelCCOPKMeans = new INIFileBool("RunCOPKMeans", false));
		m_SectionILevelC.addNode(m_ILevelCMPCKMeans = new INIFileBool("RunMPCKMeans", false));
		m_SectionILevelC.setEnabled(false);

		m_SectionBeam = new INIFileSection("Beam");
		m_SectionBeam.addNode(m_SizePenalty = new INIFileDouble("SizePenalty", 0.1));
		m_SectionBeam.addNode(m_BeamWidth = new INIFileInt("BeamWidth", 10));
		m_SectionBeam.addNode(m_BeamBestN = new INIFileInt("BeamBestN", 5));
		m_SectionBeam.addNode(m_TreeMaxSize = new INIFileInt("MaxSize", -1));
		m_SectionBeam.addNode(m_BeamAttrHeuristic = new INIFileNominal("AttributeHeuristic", HEURISTICS, 0));
		m_SectionBeam.addNode(m_FastBS = new INIFileBool("FastSearch", true));
		m_SectionBeam.addNode(m_BeamPostPrune = new INIFileBool("PostPrune", false));
		m_SectionBeam.addNode(m_BMRemoveEqualHeur = new INIFileBool("RemoveEqualHeur", false));
		m_SectionBeam.addNode(m_BeamSimilarity = new INIFileDouble("BeamSimilarity", 0.0));
		m_SectionBeam.addNode(m_BSortTrainParameter = new INIFileBool("BeamSortOnTrainParameteres", false));
		m_SectionBeam.addNode(m_BeamSyntacticConstrFile = new INIFileString("DistSyntacticConstr",NONE));
		m_SectionBeam.addNode(m_BeamToForest = new INIFileBool("BeamToForest", false));
		m_SectionBeam.setEnabled(false);

		//added by elisa 1/08/2006
		m_SectionExhaustive = new INIFileSection("Exhaustive");
		m_SectionExhaustive.addNode(m_Exhaustive = new INIFileBool("Exhaustive", true));
		m_SectionExhaustive.addNode(m_StartTreeCpt = new INIFileInt("StartTreeCpt", 0));
		m_SectionExhaustive.addNode(m_StartItemCpt = new INIFileInt("StartItemCpt", 0));
		m_SectionExhaustive.setEnabled(false);

		m_SectionTimeSeries = new INIFileSection("TimeSeries");
		m_SectionTimeSeries.addNode(m_TimeSeriesDM=new INIFileNominal("DistanceMeasure", TIME_SERIES_DISTANCE_MEASURE,0));
		m_SectionTimeSeries.addNode(m_TimeSeriesProtoComlexity=new INIFileNominal("PrototypeComlexity", TIME_SERIES_PROTOTYPE_COMPLEXITY,0));
		m_SectionTimeSeries.setEnabled(false);

		m_SectionEnsembles = new INIFileSection("Ensemble");
		m_SectionEnsembles.addNode(m_NbBags = new INIFileNominalOrIntOrVector("Iterations", NONELIST));
		m_SectionEnsembles.addNode(m_EnsembleMethod =new INIFileNominal("EnsembleMethod", ENSEMBLE_TYPE,0));
		m_SectionEnsembles.addNode(m_ClassificationVoteType =new INIFileNominal("VotingType", VOTING_TYPE,0));
		m_SectionEnsembles.addNode(m_RandomAttrSelected = new INIFileInt("SelectRandomSubspaces", 1));
		m_SectionEnsembles.addNode(m_PrintAllModels = new INIFileBool("PrintAllModels", false));
		m_SectionEnsembles.addNode(m_EnsembleShouldOpt = new INIFileBool("Optimize", false));
		m_SectionEnsembles.addNode(m_EnsembleOOBestimate = new INIFileBool("OOBestimate", false));
		m_SectionEnsembles.addNode(m_FeatureRanking = new INIFileBool("FeatureRanking", false));
		m_SectionEnsembles.setEnabled(false);

		m_SectionKNN = new INIFileSection("kNN");
		m_SectionKNN.addNode(kNN_k = new INIFileInt("k", 3));
		m_SectionKNN.addNode(kNN_vectDist = new INIFileString("VectorDistance", "Euclidian"));
		m_SectionKNN.addNode(kNN_distWeighted = new INIFileBool("DistanceWeighted", false));
		m_SectionKNN.addNode(kNN_normalized = new INIFileBool("Normalizing", true));
		m_SectionKNN.addNode(kNN_attrWeighted = new INIFileBool("AttributeWeighted", false));
		m_SectionKNN.setEnabled(false);

		m_SectionKNNT = new INIFileSection("kNNTree");
		m_SectionKNNT.addNode(kNNT_k = new INIFileInt("k", 3));
		m_SectionKNNT.addNode(kNNT_vectDist = new INIFileString("VectorDistance", "Euclidian"));
		m_SectionKNNT.addNode(kNNT_distWeighted = new INIFileBool("DistanceWeighted", false));
		m_SectionKNNT.addNode(kNNT_normalized = new INIFileBool("Normalizing", true));
		m_SectionKNNT.addNode(kNNT_attrWeighted = new INIFileBool("AttributeWeighted", false));
		m_SectionKNNT.setEnabled(false);

		INIFileSection exper = new INIFileSection("Experimental");
		exper.addNode(m_SetsData = new INIFileInt("NumberBags", 25));
		exper.addNode(m_ShowForest = new INIFileBool("XValForest", false));
		exper.setEnabled(false);

		m_Ini.addNode(settings);
		m_Ini.addNode(data);
		m_Ini.addNode(attrs);
		m_Ini.addNode(constr);
		m_Ini.addNode(output);
		m_Ini.addNode(nominal);
		m_Ini.addNode(model);
		m_Ini.addNode(m_SectionTree);
		m_Ini.addNode(rules);
		m_Ini.addNode(m_SectionHierarchical);
		m_Ini.addNode(m_SectionILevelC);
		m_Ini.addNode(m_SectionBeam);
		m_Ini.addNode(m_SectionExhaustive);
		m_Ini.addNode(m_SectionTimeSeries);
		m_Ini.addNode(m_SectionEnsembles);
		m_Ini.addNode(m_SectionKNN);
		m_Ini.addNode(m_SectionKNNT);
		m_Ini.addNode(exper);
		m_Ini.addNode(m_SectionSIT);
	}

	public void initNamedValues() {
		m_TreeMaxDepth.setNamedValue(-1, "Infinity");
		m_TreeMaxSize.setNamedValue(-1, "Infinity");
	}

	public void updateTarget(ClusSchema schema) {
		if (checkHeuristic("SSPD")) {
			schema.addAttrType(new IntegerAttrType("SSPD"));
			int nb = schema.getNbAttributes();
			m_Target.setValue(String.valueOf(nb));
		}
	}

	public void initialize(CMDLineArgs cargs, boolean loads) throws IOException {
		create();
		initNamedValues();
		if (cargs != null) preprocess(cargs);
		if (loads) {
			try {
				String fname = getFileAbsolute(getAppName() + ".s");
				m_Ini.load(fname, '%');
			} catch (FileNotFoundException e) {
				System.out.println("No settings file found");
			}
		}
		if (cargs != null) process(cargs);
		updateDataFile(getAppName() + ".arff");
		initHierarchical();
	}

	public void preprocess(CMDLineArgs cargs) {
		if (cargs.hasOption("xval") || cargs.hasOption("fold")) {
			m_OutTrainErr.setValue(false);
		}
	}

	public void process(CMDLineArgs cargs) {
		if (cargs.hasOption("target")) {
			m_Target.setValue(cargs.getOptionValue("target"));
		}
		if (cargs.hasOption("disable")) {
			String disarg = cargs.getOptionValue("disable");
			String orig = m_Disabled.getValue();
			if (StringUtils.unCaseCompare(orig, NONE)) {
				m_Disabled.setValue(disarg);
			} else {
				m_Disabled.setValue(orig + "," + disarg);
			}
		}
		if (cargs.hasOption("silent")) {
			VERBOSE = 0;
		}
	}

	public void update(ClusSchema schema) {
		setFTest(getFTest());
		MINIMAL_WEIGHT = getMinimalWeight();
		SHOW_UNKNOWN_FREQ = isShowUnknown();
		SHOW_XVAL_FOREST = isShowXValForest();
		SHOW_BRANCH_FREQ = isShowBranchFreq();
		ONE_NOMINAL = (schema.getNbNominalTargetAttributes() == 1 && schema.getNbNumericTargetAttributes() == 0);
		SIZE_PENALTY = getSizePenalty();
		BEAM_WIDTH = m_BeamWidth.getValue();
		BEAM_SIMILARITY = getBeamSimilarity();
		BEAM_SYNT_DIST_CONSTR = hasBeamConstraintFile();
		VERBOSE = m_Verbose.getValue();
	}

	public int getVerbose() {
		return m_Verbose.getValue();
	}

	public void updateDisabledSettings() {
		int pruning = getPruningMethod();
		m_M5PruningMult.setEnabled(pruning == PRUNING_METHOD_M5 || pruning == PRUNING_METHOD_M5_MULTI);
		m_PruneSetMax.setEnabled(!m_PruneSet.isString(NONE));
		m_1SERule.setEnabled(pruning == PRUNING_METHOD_GAROFALAKIS_VSB);
		int heur = getHeuristic();
		m_FTest.setEnabled(heur == HEURISTIC_SSPD || heur == HEURISTIC_SS_REDUCTION);
		if (ResourceInfo.isLibLoaded()) m_ResourceInfoLoaded.setSingleValue(RESOURCE_INFO_LOAD_YES);
		else m_ResourceInfoLoaded.setSingleValue(RESOURCE_INFO_LOAD_NO);
	}

	public void show(PrintWriter where) throws IOException {
		updateDisabledSettings();
		m_Ini.save(where);
	}

	public String getFileAbsolute(String fname) {
		// System.out.println("Dir name: '"+m_DirName+"'");
		// System.out.println("File name: '"+fname+"'");
		if (m_DirName == null) {
			return fname;
		} else {
			if (FileUtil.isAbsolutePath(fname)) {
				return fname;
			} else {
				return m_DirName + File.separator + fname;
			}
		}
	}

	public PrintWriter getFileAbsoluteWriter(String fname) throws FileNotFoundException {
		String path = getFileAbsolute(fname);
		return new PrintWriter(new OutputStreamWriter(new FileOutputStream(path)));
	}

}
