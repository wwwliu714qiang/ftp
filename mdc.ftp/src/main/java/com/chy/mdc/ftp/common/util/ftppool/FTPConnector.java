package com.chy.mdc.ftp.common.util.ftppool;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.chy.mdc.common.util.Log;
import com.chy.mdc.ftp.model.FtpConfig;

public class FTPConnector {
	private static FTPClientPool ftpPool;

	public FTPConnector(FtpConfig ftpConfig, int maxTotal) {
		String host = ftpConfig.getHost();
		String user = ftpConfig.getUser();
		String password = ftpConfig.getPassword();
		int port = ftpConfig.getPort();
		int timeout = ftpConfig.getTimeout();

		FTPClientConfig ftpClientConfig = new FTPClientConfig();
		ftpClientConfig.setHost(host);
		ftpClientConfig.setUsername(user);
		ftpClientConfig.setPassword(password);
		ftpClientConfig.setPort(port);
		ftpClientConfig.setPassiveMode(true);
		ftpClientConfig.setEncoding("utf-8");
		ftpClientConfig.setClientTimeout(timeout);
		ftpClientConfig.setTransferFileType(FTP.BINARY_FILE_TYPE);

		GenericObjectPoolConfig cfg = new GenericObjectPoolConfig();
		cfg.setMaxTotal(maxTotal);
		cfg.setTestOnBorrow(true);
		cfg.setMaxWaitMillis(1200000);

		ftpPool = new FTPClientPool(new FTPClientFactory(ftpClientConfig), cfg);
	}

	public FTPClientPool GetFtpPool() {
		return ftpPool;
	}

	public FTPClient get() {
		FTPClient client = null;
		try {
			int numActive = ftpPool.getNumActive();
			int numIdle = ftpPool.getNumIdle();
			Log.debug("numActive: " + numActive + ",  numIdle: " + numIdle);
			client = ftpPool.borrowObject();
		} catch (Exception e) {
			Log.error("获取ftpClient连接失败", new Throwable(e));
		}
		return client;
	}

	public void returnClient(FTPClient client) {
		ftpPool.returnObject(client);
	}
	public void close() {
		ftpPool.close();
	}
}
