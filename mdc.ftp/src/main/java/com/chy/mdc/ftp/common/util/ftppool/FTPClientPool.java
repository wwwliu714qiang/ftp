package com.chy.mdc.ftp.common.util.ftppool;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * 自定义实现ftp连接池
 * 
 * @author
 *
 */
public class FTPClientPool extends GenericObjectPool<FTPClient> {

	static GenericObjectPoolConfig cfg;
	static {
		cfg = new GenericObjectPoolConfig();
		// cfg.setMaxIdle(5);
		cfg.setMaxTotal(5);	//连接池中最大连接数
		cfg.setTestOnBorrow(true);	//获取连接时是否校验
		cfg.setMaxWaitMillis(10 * 1000);	//连接最大等待时间
	}

	public FTPClientPool(PooledObjectFactory<FTPClient> factory) {
		this(factory, cfg);
	}

	public FTPClientPool(PooledObjectFactory<FTPClient> factory, GenericObjectPoolConfig config) {
		super(factory, config);
	}
}
