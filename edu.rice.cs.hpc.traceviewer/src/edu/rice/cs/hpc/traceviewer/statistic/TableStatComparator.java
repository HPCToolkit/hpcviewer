package edu.rice.cs.hpc.traceviewer.statistic;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;

import edu.rice.cs.hpc.traceviewer.statistic.HPCStatView.StatisticItem;

public class TableStatComparator extends ViewerComparator 
{
	private int propertyIndex;
    private static final int DESCENDING = 1;
    private int direction = DESCENDING;
    
    public TableStatComparator() {
    	this.propertyIndex = 1;
    	direction = DESCENDING;
    }
    
    
    public int getDirection() {
    	return direction == 1 ? SWT.DOWN : SWT.UP;
    }
    
    public void setColumn(int column) {
    	if (column == this.propertyIndex) {
    		direction = 1 - direction;
    	} else  {
    		this.propertyIndex = column;
    		direction = DESCENDING; 
    	}
    }
    
    
    public int compare(Viewer viewer, Object e1, Object e2) {
    	StatisticItem item1 = (StatisticItem) e1;
    	StatisticItem item2 = (StatisticItem) e2;
    	int rc = 0;
    	
    	switch (propertyIndex) {
    	case 0:
    		rc = item1.procedureName.compareTo(item2.procedureName);
    		break;
    		
    	case 1:
    		rc = (Float.compare(item1.percent, item2.percent));
    		break;
    		
   		default:
   			rc = 0;
    	}
    	if (direction == DESCENDING) {
    		rc = -rc;
    	}
    	return rc;
    }
}
