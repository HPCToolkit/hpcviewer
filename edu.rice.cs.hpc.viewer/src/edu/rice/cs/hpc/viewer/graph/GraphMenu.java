package edu.rice.cs.hpc.viewer.graph;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.extdata.IThreadDataCollection;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.editor.BaseEditorManager;
import edu.rice.cs.hpc.viewer.window.Database;

/****
 * 
 * Class to handle metric graph menus (plot, sorted and histo)
 *
 */
public class GraphMenu 
{
	
	static public void createAdditionalContextMenu(IWorkbenchWindow window, IMenuManager mgr, 
			Database database, Scope scope) {
		if (scope != null) {
			IThreadDataCollection threadData = database.getThreadDataCollection();
			if (threadData == null || !threadData.isAvailable())
				// no menus if there is no thread-level data
				return;
			
			final BaseMetric []metrics = database.getExperiment().getMetricRaw();
			if (metrics == null)
				return;
			
			mgr.add( new Separator() );
			
			final int num_metrics = metrics.length;
			for (int i=0; i<num_metrics; i++) {
				
				// do not display empty metric 
				// this is important to keep consistency with the table
				// which doesn't display empty metrics
				
				RootScope root = scope.getRootScope();
				MetricValue mv = root.getMetricValue(metrics[i]);
				if (mv == MetricValue.NONE)
					continue;
				
				// display the menu
				
				MenuManager subMenu = new MenuManager("Graph "+ metrics[i].getDisplayName() );
				createGraphMenus(window, database, subMenu, scope, metrics[i]);
				mgr.add(subMenu);
			}
		}		
	} 

	/***
	 * Create 3 submenus for plotting graph: plot, sorted and histo
	 * @param menu
	 * @param scope
	 * @param m
	 * @param index
	 */
	static private void createGraphMenus(IWorkbenchWindow window, Database database, IMenuManager menu, Scope scope, BaseMetric m) {
		menu.add( createGraphMenu(window, database, scope, m, GraphType.PlotType.PLOT) );
		menu.add( createGraphMenu(window, database, scope, m, GraphType.PlotType.SORTED) );
		menu.add( createGraphMenu(window, database, scope, m, GraphType.PlotType.HISTO) );
	}
	
	/***
	 * Create a menu action for graph
	 * @param scope
	 * @param m
	 * @param index
	 * @param t
	 * @return
	 */
	static private ScopeGraphAction createGraphMenu(IWorkbenchWindow window, Database database, Scope scope, BaseMetric m, GraphType.PlotType t) {
		final String sTitle = GraphType.toString(t);
		return new ScopeGraphAction( window, database, sTitle, scope, m, t);
	}
	

    /********************************************************************************
     * class to initialize an action for displaying a graph
     ********************************************************************************/
    static private class ScopeGraphAction extends Action {
    	final private GraphType.PlotType graph_type;
    	final private BaseMetric metric;	
    	final private Scope scope;
    	final private IWorkbenchWindow window;
    	final private Database database;
    	
		public ScopeGraphAction(IWorkbenchWindow window, Database database, String sTitle, Scope scopeCurrent, 
				BaseMetric m, GraphType.PlotType type) {
			
			super(sTitle);
			this.metric 	= m;
			this.graph_type = type;
			scope 		 	= scopeCurrent;
			this.window 	= window;
			this.database 	= database;
		}
    	
		public void run() {
			IWorkbenchPage objPage = window.getActivePage();
        	
			try {
				final Experiment experiment = (Experiment) this.scope.getExperiment();
				
				// prepare to split the editor pane
				boolean needNewPartition = BaseEditorManager.splitBegin(objPage, experiment);
				
				String id = GraphEditorInput.getID(scope, metric, graph_type, database);
	        	GraphEditorInput objInput = getGraphEditorInput(window, id);
	        	
	        	if (objInput == null) {
	        		objInput = new GraphEditorInput(database, scope, metric, graph_type, window);
	        	}
	        	IEditorPart editor = null;
	        	switch (graph_type) {
	        	case PLOT:
	        		editor = objPage.openEditor(objInput, GraphEditorPlot.ID);
		        	break;
	        	case SORTED:
	        		editor = objPage.openEditor(objInput, GraphEditorPlotSort.ID);
	        		break;
	        	case HISTO:
	        		editor = objPage.openEditor(objInput, GraphEditorHisto.ID);
	        		break;
	        	}
	        	
	        	if (editor instanceof GraphEditorBase) {
	        		((GraphEditorBase)editor).editorFinalize();
	        	}
	        	
	        	// finalize the pane splitting if needed
	        	BaseEditorManager.splitEnd(needNewPartition, editor);
				
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		}
    }

	/****
	 * If the editor has been displayed, we need to activate it
	 * If not, return null and let the caller to create a new editor
	 * @param id
	 * @return
	 */
	static private GraphEditorInput getGraphEditorInput(IWorkbenchWindow window, String id) {
		IEditorReference editors[] = window.getActivePage().getEditorReferences();
		if (editors == null)
			return null;
		
		//-------------------------------------------------------------------
		// look at all active editors if our editor has been there or not
		//-------------------------------------------------------------------
		for (int i = 0; i<editors.length; i++) {
			String name = editors[i].getName();
			
			// check if it is a graph editor (started with [....])
			if (name != null && name.charAt(0)=='[') {
				try {
					IEditorInput input = editors[i].getEditorInput();
					if (input instanceof GraphEditorInput) {
						String editor_id = ((GraphEditorInput)input).getID();
						if (editor_id.equals(id)) {
							// we found out editor !
							return (GraphEditorInput) input;
						}
					}
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return null;
	}
	

}
