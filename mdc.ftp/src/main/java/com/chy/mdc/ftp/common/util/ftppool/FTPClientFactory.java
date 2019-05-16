package com.chy.mdc.ftp.common.util.ftppool;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.chy.mdc.common.util.Log;


/**
 * 连接池工厂类
 * 
 * @author PYY
 *
 */
public class FTPClientFactory implements PooledObjectFactory<FTPClient> {

	private FTPClientConfig config;

	public FTPClientFactory(FTPClientConfig config) {
		this.config = config;
	}

	/**
	 * 销毁ftp连接
	 */
	@Override
	public void destroyObject(PooledObject<FTPClient> p) throws Exception {
		FTPClient ftpClient = p.getObject();
		try {
			if (ftpClient != null && ftpClient.isConnected()) {
				ftpClient.logout();
			}
		} catch (IOException io) {
			Log.info("destroyObject: ftpClient.logout() failed...");
		} finally {
			// 注意,一定要在finally代码中断开连接，否则会导致占用ftp连接情况
			try {
				ftpClient.disconnect();
			} catch (IOException io) {
				Log.info("destroyObject: ftpClient.disconnect() failed...");
			}
		}

	}

	/**
	 * 添加ftp连接实例
	 */
	@Override
	public PooledObject<FTPClient> makeObject() throws Exception {
		FTPClient ftpClient = new FTPClient();
		ftpClient.setConnectTimeout(config.getClientTimeout());	//超时时间
		ftpClient.setControlEncoding(config.getEncoding());	//编码
		try {
			ftpClient.connect(config.getHost(), config.getPort());	//建立ftp连接
			int reply = ftpClient.getReplyCode();	//ftp连接返回代码
			if (!FTPReply.isPositiveCompletion(reply)) {	//返回代码大于等于200小于300，表示连接是成功的，否则不成功断开连接
				ftpClient.disconnect();
				return null;
			}
			boolean result = ftpClient.login(config.getUsername(), config.getPassword());	//登录ftp
			if (!result) {	//登录异常，抛出异常信息
				throw new Exception("ftpClient登陆失败! userName:" + config.getUsername() + " ; password:" + config.getPassword());
			}
			ftpClient.setFileType(config.getTransferFileType());	//设置传输编码
			ftpClient.setFileTransferMode(FTPClient.COMPRESSED_TRANSFER_MODE);	//压缩形式传输
			ftpClient.setBufferSize(10240);	//传输大小
			if (config.getPassiveMode()) {	//被动传输
				ftpClient.enterLocalPassiveMode();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new DefaultPooledObject<FTPClient>(ftpClient);
	}

	/**
	 * 校验获取的ftp对象是否有效
	 */
	@Override
	public boolean validateObject(PooledObject<FTPClient> p) {
		FTPClient ftpClient = p.getObject();
		try {
			return ftpClient.sendNoOp();
		} catch (IOException e) {
			throw new RuntimeException("Failed to validate client: " + e, e);
		}
	}

	@Override
	public void activateObject(PooledObject<FTPClient> arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void passivateObject(PooledObject<FTPClient> arg0) throws Exception {
		// TODO Auto-generated method stub

	}
}
