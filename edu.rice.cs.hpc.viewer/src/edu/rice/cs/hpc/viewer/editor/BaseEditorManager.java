package edu.rice.cs.hpc.viewer.editor;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import edu.rice.cs.hpc.data.experiment.Experiment;

public abstract class BaseEditorManager {

	
	/***
	 * prepare to split the editor  pane
	 * 
	 * @param page
	 * @param experiment
	 * @return
	 */
	static public boolean splitBegin(IWorkbenchPage page, Experiment experiment) {
		//sanity checks
		if (page == null) {
			throw new IllegalArgumentException();
		}

		// If there is already a editor area partition for this database, give one of its editors focus 
		// so that this new editor will display in the same partition.  If not we will create a new partition
		// and put the new editor in it after we open the editor.
		IEditorReference[] editorReferences = page.getEditorReferences();
		
		// assume there are no editors open and that we will not need a new partition
		boolean needNewPartition = false;
		// if at lease one editor already open, we may need new partition (see below)
		if (editorReferences.length > 0) {
			needNewPartition = true;
		}
		
		// check if there is already an editor opened for this database
		final String expFile = experiment.getXMLExperimentFile().getPath();
		for(IEditorReference editorRef: editorReferences) {
			IEditorPart editor = editorRef.getEditor(false);
			
			if (editor instanceof IViewerEditor) {
				Experiment expEditor = ((IViewerEditor)editor).getExperiment();
				// if there is no experiment associated with this editor, just ignore it
				if (expEditor != null) {
					final String expEditorFile = expEditor.getXMLExperimentFile().getPath();
					if (expFile.equals(expEditorFile)) {
						// An editor has already opened, do not split the pane
						needNewPartition = false;
						// make this editor active so the new editor will end up in its pane
						page.activate(editor.getSite().getPart());
						break;
					}
				}
			}
		}
		
		return needNewPartition;
	}

	/***
	 * finalize the split if necessary
	 * 
	 * @param needNewPartition
	 * @param iep
	 */
	static public void splitEnd(boolean needNewPartition, IEditorPart iep) {
		// if this is the first editor from this database, create new partition to put it in
		if (needNewPartition) {
			splitEditorArea(iep);
		}

	}
	
	
	//=====================================================================================================
	// Split editor
	// Attention: this split part uses Eclipse internal methods which are not portable across revisions
	//			  it has to be replaced by other approach if possible
	//=====================================================================================================
	
	/**
	 * Split the editor area for this new editor.
	 */
//	@SuppressWarnings("restriction")
	private static void splitEditorArea(IEditorPart iep) {
	}
	/**
	 * A method to create a part stack container (a new workbook)
	 * 
	 * @param editorSashContainer the <code>EditorSashContainer</code> to set for the returned <code>PartStack</code>
	 * @return a new part stack container
	 */
}
