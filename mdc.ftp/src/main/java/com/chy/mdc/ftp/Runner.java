package com.chy.mdc.ftp;

import com.chy.mdc.common.util.ICalendar;
import com.chy.mdc.common.util.Log;
import com.chy.mdc.ftp.process.FtpDownload;

public class Runner {

	/**
	 * 启动ftp下载
	 * @param args
	 */
	public static void main(String[] args) {
		
		Log.info("========================================================");
		String sysTime = ICalendar.getSysTime();
		Log.info("download file from ftp start : " + sysTime);
		Log.info("========================================================");
		
		if(args.length < 1) {
			Log.error("请至少输入一个启动参数，参数为配置文件对应的配置信息的section!");
			System.exit(0);
		}
		int threadCount = 1;	//下载ftp数据线程数，如果第二个参数没有，默认为1
		if(args.length >= 2) {
			threadCount = Integer.parseInt(args[1].trim());
		}
		String configType = args[0];
		int processRunTime = 0;
		if(args.length == 3) {
			processRunTime = Integer.parseInt(args[2]);
		}
		
		//启动下载
		FtpDownload ftpDownload = new FtpDownload(configType, threadCount, processRunTime);
		ftpDownload.run();
		//第三个参数为运行超时时间，单位分钟
//		if(args.length == 3) {
//			String processRunTime = args[2].trim();
//			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
//			executor.schedule(new Runnable() {
//				@Override
//				public void run() {
//					Log.info("运行超时，自动退出程序......");
//					System.exit(0);
//				}
//			}, Integer.parseInt(processRunTime), TimeUnit.MINUTES);
//		}
	}
}
