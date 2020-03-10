package edu.rice.cs.hpc.data.trace;

public class TraceName {
	int process;
	int thread;

	public TraceName(int _process, int _thread) {
		process = _process;
		thread = _thread;
	}

	@Override
	public String toString() {
		if (thread == -1) {
			return Integer.toString(process);
		} else {
			return process + "." + thread;
		}
	}
}
