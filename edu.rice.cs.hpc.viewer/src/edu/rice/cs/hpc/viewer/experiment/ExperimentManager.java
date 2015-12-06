/**
 * Experiment File to manage the database: open, edit, fusion, ...
 */
package edu.rice.cs.hpc.viewer.experiment;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.swt.widgets.Shell;

import java.io.File;

import edu.rice.cs.hpc.data.util.Constants;
import edu.rice.cs.hpc.data.util.Util.FileXMLFilter;

import edu.rice.cs.hpc.viewer.framework.Activator;
import edu.rice.cs.hpc.viewer.util.PreferenceConstants;
import edu.rice.cs.hpc.viewer.window.Database;
import edu.rice.cs.hpc.viewer.window.ViewerWindow;
import edu.rice.cs.hpc.viewer.window.ViewerWindowManager;

/**
 * This class manages to select, load and open a database directory
 * We assume that a database directory contains an XML file (i.e. extension .xml)
 * Warning: This class is not compatible with the old version of experiment file 
 *  (the old version has no xml extension)
 * @author laksono
 *
 */
public class ExperimentManager {
	final static public int FLAG_DEFAULT = 0;
	final static public int FLAG_WITH_CALLER_VIEW = 1;
	final static public int FLAG_WITHOUT_CALLER_VIEW = 2;

	/**
	 * Last path of the opened directory
	 */
	static public String sLastPath=null;
	/**
	 * pointer to the current active workbench window (supposed to be only one)
	 */
	private IWorkbenchWindow window;
	/**
	 * flag to indicate if a caller view needs to display or not
	 */
	private boolean flagCallerView = true;
	
	/**
	 * Constructor to instantiate experiment file
	 * @param win: the current workbench window
	 */
	public ExperimentManager(IWorkbenchWindow win) {
		this.window = win;
		ScopedPreferenceStore objPref = (ScopedPreferenceStore)Activator.getDefault().getPreferenceStore();
		if(ExperimentManager.sLastPath == null)
			ExperimentManager.sLastPath = objPref.getString(PreferenceConstants.P_PATH);
	}
	

	
	/**
	 * Get the list of database file name
	 * @param shell : the shell widget of the application
	 * @param sTitle : the title of the window open fike
	 * 
	 * @return the list of XML files in the selected directory
	 * null if the user click the "cancel" button
	 */
	private File[] getDatabaseFileList(Shell shell, String sTitle) {
		// preparing the dialog for selecting a directory
		Shell objShell = shell;
		DirectoryDialog dirDlg = new DirectoryDialog(objShell);
		dirDlg.setText("hpcviewer");
		dirDlg.setFilterPath(ExperimentManager.sLastPath);		// recover the last opened path
		dirDlg.setMessage(sTitle);
		String sDir = dirDlg.open();	// ask the user to select a directory
		if(sDir != null){
			return this.getListOfXMLFiles(sDir);
		}
		
		return null;
	}
	
	/**
	 * Open a database given a path to the database directory
	 * @param sPath : absolute path of the database directory
	 * @param flag : whether to show callers view or not
	 * 
	 * @return true if the database has been successfully opened
	 */
	public boolean openDatabaseFromDirectory(String sPath, int flag) {
		File []fileXML = this.getListOfXMLFiles(sPath);
		if(fileXML != null)
			return this.openFileExperimentFromFiles(fileXML, flag);
		return false;
	}
	/**
	 * Attempt to open an experiment database if valid then
	 * open the scope view  
	 * @return true if everything is OK. false otherwise
	 */
	public boolean openFileExperiment(int flag) {
		File []fileXML = this.getDatabaseFileList(this.window.getShell(), 
				"Select a directory containing a profiling database.");
		if(fileXML != null)
			return this.openFileExperimentFromFiles(fileXML, flag);
		return false;
	}

	//==================================================================
	// ---------- PRIVATE PART-----------------------------------------
	//==================================================================
	/**
	 * Open an experiment database based on given an array of java.lang.File
	 * @param filesXML: list of files
	 * @return true if the opening is successful
	 */
	private boolean openFileExperimentFromFiles(File []filesXML, int flag) {
		if((filesXML != null) && (filesXML.length>0)) {
			boolean bContinue = true;
			// let's make it complicated: assuming there are more than 1 XML file in this directory,
			// we need to test one by one if it is a valid database file.
			// Problem: if in the directory it has two XML files, then the second one will NEVER be opened !
			for(int i=0;i<(filesXML.length) && (bContinue);i++) 
			{
				File objFile = filesXML[i];
				String sFile=objFile.getAbsolutePath();

				// Since rel 5.x, the name of database is experiment.xml
				// there is no need to maintain compatibility with hpctoolkit prior 5.x 
				// 	where the name of database is config.xml
				if(objFile.getName().startsWith(Constants.DATABASE_FILENAME))  
				{
					// ------------------------------------------------------------------
					// we will continue to verify the content of the list of XML files
					// until we fine the good one.
					// ------------------------------------------------------------------
					
					// check if the database has been opened in this window
					ViewerWindow vw = ViewerWindowManager.getViewerWindow(window);
					if (vw != null)
					{
						Database db = vw.getDb(vw.getDatabasePath(sFile));
						
						if (db == null) {						
							// check if we can open the database successfully
							bContinue = (!setExperiment(sFile, flag));
						} else {
							MessageDialog.openError(window.getShell(), "Database is already opened", 
									"The database is already opened in this window.\n" +
									"There is no need to open the same database twice in the same window.");
							bContinue = false;
						}
					}
				}
			}
	   		if(bContinue) {
	   		} else
	   			return true;
		}
		MessageDialog.openError(window.getShell(), "Failed to open a database", 
			"Either the selected directory is not a database or the max number of databases allowed per window are already opened.\n"+
			"A database directory must contain at least one XML file which contains profiling information.");
		return false;
	}
	
	/**
	 * Set the experiment to be processed
	 * @param sFilename
	 * @return
	 */
	private boolean setExperiment(String sFilename, int flag) {
		IWorkbenchPage objPage = this.window.getActivePage();
		ExperimentView expViewer = new ExperimentView(objPage);

		// data looks OK
		boolean bResult;
		if (flag == FLAG_WITHOUT_CALLER_VIEW)  {
			flagCallerView = false;
			bResult = expViewer.loadExperimentAndProcess(sFilename, flagCallerView);
		} else if (flag == FLAG_DEFAULT && !flagCallerView )
			// use the initial flag (set by command line or preference page)
			bResult = expViewer.loadExperimentAndProcess(sFilename, flagCallerView );
		else
			bResult = expViewer.loadExperimentAndProcess(sFilename);

		return bResult; 
	}
	
	/**
	 * Return the list of .xml files in a directory
	 * @param sPath: the directory of the database
	 * @return
	 */
	private File[] getListOfXMLFiles(String sPath) {
		// find XML files in this directory
		File files = new File(sPath);
		// for debugging purpose, let have separate variable
		File filesXML[] = files.listFiles(new FileXMLFilter());
		// store it in the class variable for further usage
		ExperimentManager.sLastPath = sPath;
		// store the current path in the preference
		ScopedPreferenceStore objPref = (ScopedPreferenceStore)Activator.getDefault().getPreferenceStore();
		objPref.setValue(PreferenceConstants.P_PATH, sPath);
		return filesXML;
	}
	

}
