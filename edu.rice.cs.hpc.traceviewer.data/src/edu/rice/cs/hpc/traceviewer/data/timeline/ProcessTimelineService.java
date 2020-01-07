package edu.rice.cs.hpc.traceviewer.data.timeline;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;

import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimeline;

public class ProcessTimelineService extends AbstractSourceProvider {

	final static public String PROCESS_TIMELINE_PROVIDER = "edu.rice.cs.hpc.traceviewer.services.ProcessTimelineService.data";
	private ProcessTimeline []traces;


	@Override
	public void dispose() {	}

	@Override
	public Map getCurrentState() {
		Map<String, Object> map = new HashMap<String, Object>(1);
		map.put(PROCESS_TIMELINE_PROVIDER, traces);
		
		return map;
	}

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] {PROCESS_TIMELINE_PROVIDER};
	}

	public void setProcessTimeline(ProcessTimeline[] traces) {
		this.traces = traces;
	}
	
	
	public boolean setProcessTimeline(int index, ProcessTimeline trace) {
		boolean result = (traces != null && traces.length > index);
		if (result)
			traces[index] = trace;
		/*else
			System.err.println("PTS incorrect index: " + index + " out of " + (traces == null ? 0 : traces.length));*/
		return result;
	}
 	
	
	public ProcessTimeline getProcessTimeline(int proc) {
		
		if (traces != null && proc >= 0 && proc < traces.length)
			return traces[proc];
		
		return null;
	}
	
	public int getNumProcessTimeline() {
		if (traces == null)
			return 0;
		return traces.length;
	}
	
	public boolean isFilled() {
		if (traces != null) {
			for (ProcessTimeline trace: traces) {
				if (trace == null)
					return false;
			}
			return true;
		}
		return false;
	}
}
