package com.vf.common;

public interface Progress {

	public void incrementProgress(int delta);

	public int getProgress();
	
	public int getTotalCount();

}
