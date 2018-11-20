 package clus.algo.tdidt;

 import clus.algo.split.*;
 import clus.data.type.*;
 import clus.data.rows.*;
 
 public class InduceSubset implements Runnable {
     public FindBestTest core_FindBestTest;
     public int jstart;
     public int jstop;
     public ClusAttrType[] attrs;
     public RowData data;

     public InduceSubset(FindBestTest core_FindBestTest, int jstart, int jstop, ClusAttrType[] attrs, RowData data) {
       this.core_FindBestTest = core_FindBestTest;
       this.jstart = jstart;
       this.jstop = jstop;
       this.attrs = attrs;
       this.data = data;
     }

     @Override
    public void run() {
        try {
            for (int j = jstart; j < jstop; j++) {
              ClusAttrType at = attrs[j];
              if (at instanceof NominalAttrType) core_FindBestTest.findNominal((NominalAttrType)at, data);
              else core_FindBestTest.findNumeric((NumericAttrType)at, data);
            }
            // System.out.println("Thread finished attributes: " +jstart+ "-"+(jstop-1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 }