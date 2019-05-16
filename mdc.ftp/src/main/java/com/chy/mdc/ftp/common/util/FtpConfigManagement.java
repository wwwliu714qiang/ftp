package com.chy.mdc.ftp.common.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.chy.mdc.common.util.FileUtil;
import com.chy.mdc.common.util.IniReader;
import com.chy.mdc.common.util.Log;
import com.chy.mdc.common.util.MapUtil;
import com.chy.mdc.ftp.model.FtpConfig;

public class FtpConfigManagement {
	
	private static Map<String, FtpConfig> ftpConfigMap = null;

	public static Map<String, FtpConfig> get(){
		if(ftpConfigMap == null || ftpConfigMap.size() <= 0) {
			ftpConfigMap = new HashMap<String, FtpConfig>();
			new FtpConfigManagement();
		}
		return ftpConfigMap;
	}
	
	private FtpConfigManagement() {
		IniReader ir = null;
		InputStream is = null;
		try {
			// 读取判识配置文件
			is = new FileInputStream(FileUtil.validFilePath(System.getProperty("user.dir")) + "config/ftp_config.ini");
			ir = new IniReader(is);
		} catch (IOException e) {
			Log.error("加载配置文件ftp_config.ini失败", e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		Map<String, Properties> map = ir.get();

		Iterator<Entry<String, Properties>> it = map.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, Properties> next = it.next();
			String key = next.getKey(); // 配置section
			// 每一组配置信息
			Properties value = next.getValue();
			// 将配置信息转换为相应的javabean实例
			FtpConfig ftpConfig = MapUtil.map2Bean(value, new FtpConfig());
			ftpConfigMap.put(key, ftpConfig);
		}
	}
}
