package edu.rice.cs.hpc.data.trace;

import java.util.ArrayList;

public class FilterSet {
	private ArrayList<Filter> patterns;
	private boolean excludeMatched; //i.e. do we hide traces that match the pattern
	
	public FilterSet() {
		// default is to hide the matching processes
		excludeMatched = true;
		patterns = new ArrayList<Filter>();
	}
	public boolean hasAnyFilters() {
		return patterns != null && !patterns.isEmpty();
	}
	public void setPatterns(ArrayList<Filter> patterns) {
		this.patterns = patterns;
	}
	
	public void setShowMode(boolean toShow) {
		excludeMatched = !toShow;
	}
	
	public boolean isShownMode() {
		return !excludeMatched;
	}
	//TODO: We should probably use TraceName instead
	public boolean includes(String name){
		String[] split = name.split("\\.");
		int process = Integer.parseInt(split[0]);
		int thread = 0;
		if (split.length > 1) {
			thread = Integer.parseInt(split[1]);
		}
		return include(new TraceName(process, thread));
	}
	
	public ArrayList<Filter> getPatterns()
	{
		return patterns;
	}
	public boolean include(TraceName traceName) {
		
		if (excludeMatched) {
			boolean matchedSoFar = true;
			/*
			 * We think about it as applying each filter to the results of the
			 * filter before. This is logically the same as requiring a rank to
			 * match every filter (AND, though sometimes it is more intuitive as the
			 * OR equation provided by De Morgan's law). Also, if excludeMatched is
			 * true, we need to NOT the matches. This is the same as XORing with
			 * excludeMatched
			 */
			for (Filter filter : patterns) {
				matchedSoFar &= (filter.matches(traceName.process, traceName.thread)^excludeMatched);
			}
			return matchedSoFar;
		}
		// special case for "to show" pattern option
		
		boolean matchedSoFar = false;
		for (Filter filter : patterns) {
			matchedSoFar |= (filter.matches(traceName.process, traceName.thread));
		}
		return matchedSoFar;
	}
} 

