package com.vf.common;

public class Data {

	private String encoding;
	private String inputData;
	private byte[] outputData;
	private long offset;

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getInputData() {
		return inputData;
	}

	public void setInputData(String inputData) {
		this.inputData = inputData;
	}

	public byte[] getOutputData() {
		return outputData;
	}

	public void setOutputData(byte[] outputData) {
		this.outputData = outputData;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

}
