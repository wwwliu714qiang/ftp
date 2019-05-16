package com.chy.mdc.ftp.model;

public class FtpConfig {

	// 服务器IP
	private String host;
	// 用户名
	private String user;
	// 密码
	private String password;
	// 端口号
	private int port = 21;
	// 工作目录
	private String workPath;
	// 文件名匹配正则表达式
	private String fileNameRegex;
	// 文件名中时间正则表达式
	private String fileTimeRegex;
	// 超时时间ms
	private int timeout;
	// 服务器的时区(距离utc时区的小时偏移量,东八区则为8)
	private int timeZoneOffset;
	// 扫描时间间隔（获取ftp服务器多长时间内的文件,-30代表过去30分钟内的数据）
	private int scanSpan;
	// 从ftp下载的文件保存路径
	private String savePath;
	// 从ftp下载文件，如果本地存在同名文件是否覆盖
	private boolean override = false;
	// ftp下载文件保存到本地的文件名称表达式
	private String saveFileNameExp;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getWorkPath() {
		return workPath;
	}

	public void setWorkPath(String workPath) {
		this.workPath = workPath;
	}

	public String getFileNameRegex() {
		return fileNameRegex;
	}

	public void setFileNameRegex(String fileNameRegex) {
		this.fileNameRegex = fileNameRegex;
	}

	public String getFileTimeRegex() {
		return fileTimeRegex;
	}

	public void setFileTimeRegex(String fileTimeRegex) {
		this.fileTimeRegex = fileTimeRegex;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getTimeZoneOffset() {
		return timeZoneOffset;
	}

	public void setTimeZoneOffset(int timeZoneOffset) {
		this.timeZoneOffset = timeZoneOffset;
	}

	public int getScanSpan() {
		return scanSpan;
	}

	public void setScanSpan(int scanSpan) {
		this.scanSpan = scanSpan;
	}

	public String getSavePath() {
		return savePath;
	}

	public void setSavePath(String savePath) {
		this.savePath = savePath;
	}

	public boolean isOverride() {
		return override;
	}

	public void setOverride(boolean override) {
		this.override = override;
	}

	public String getSaveFileNameExp() {
		return saveFileNameExp;
	}

	public void setSaveFileNameExp(String saveFileNameExp) {
		this.saveFileNameExp = saveFileNameExp;
	}

}
