package com.chy.mdc.ftp.model;

import java.util.Date;

public class FileInfoModel {

	private String filename;
	private long fileSize;
	private String parentPath;
	private Date fileTimestamp;

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(String parentPath) {
		this.parentPath = parentPath;
	}

	public Date getFileTimestamp() {
		return fileTimestamp;
	}

	public void setFileTimestamp(Date fileTimestamp) {
		this.fileTimestamp = fileTimestamp;
	}

}
