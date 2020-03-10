package edu.rice.cs.hpc.remote.data;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import edu.rice.cs.hpc.data.experiment.extdata.IFilteredData;
import edu.rice.cs.hpc.data.trace.Filter;
import edu.rice.cs.hpc.data.trace.FilterSet;
import edu.rice.cs.hpc.data.trace.TraceName;




public class RemoteFilteredBaseData implements IFilteredData {

	private static final int FILTER = 0x464C5452;
	private final TraceName[] allNames;
	private int[] indexes;
	private final DataOutputStream server;
	FilterSet filter;
	
	public RemoteFilteredBaseData(TraceName[] names, int _headerSz, DataOutputStream server) {
		allNames = names;
		this.server = server;
		filter = new FilterSet();
		indexes = new int[names.length];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = i;
		}
	}
	@Override
	public void setFilter(FilterSet filter) {
		this.filter = filter;
		applyFilter();
	}


	private void applyFilter() {
		ArrayList<Integer> lindexes = new ArrayList<Integer>();

	
		for (int i = 0; i < allNames.length; i++) {
			if (filter.include(allNames[i]))
				lindexes.add(i);
		}

		indexes = new int[lindexes.size()];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = lindexes.get(i);
		}

		try {
			server.writeInt(FILTER);
			ArrayList<Filter> pat = filter.getPatterns();
			server.write(0);
			server.write(filter.isShownMode()? 0 : 1);
			server.writeShort(pat.size());
			for (Filter filter : pat) {
				filter.serializeSelfToStream(server);
			}
			server.flush();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	@Override
	public FilterSet getFilter() {
		return filter;
	}


	@Override
	public String[] getListOfRanks() {
		//This is already an O(n) operation so it's okay that we are recomputing the strings.
		String[] list = new String[getNumberOfRanks()];
		for (int i = 0; i < list.length; i++) {
			list[i] = allNames[indexes[i]].toString();
		}
		return list;
	}

	@Override
	public int getNumberOfRanks() {
		return indexes.length;
	}

	@Override
	public void dispose() {
		// Do nothing. The local version disposes the BaseDataFile. The rough
		// equivalent would be to dispose the RemoteDataRetriever, but that is
		// done elsewhere. Plus, because RemoteDataRetriever is in traceviewer,
		// we can't access it here.
	}

	@Override
	public boolean isGoodFilter() {
		return getNumberOfRanks() > 0;
	}
	@Override
	public int getFirstIncluded() {
		return indexes[0];
	}
	@Override
	public int getLastIncluded() {
		return indexes[indexes.length-1];
	}
	@Override
	public boolean isDenseBetweenFirstAndLast() {
		return indexes[indexes.length-1] == indexes.length-1;
	}
	@Override
	public boolean isHybridRank() {
		return allNames[0].toString().contains(".");
	}
	@Override
	public long getLong(long position) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getInt(long position) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getRecordSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getMinLoc(int rank) {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public long getMaxLoc(int rank) {
		// TODO Auto-generated method stub
		return 0;
	}
}
