package com.chy.mdc.ftp.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import com.chy.mdc.common.util.DateFormatPatterns;
import com.chy.mdc.common.util.ICalendar;
import com.chy.mdc.common.util.Log;
import com.chy.mdc.common.util.StringUtil;
import com.chy.mdc.ftp.common.util.FtpConfigManagement;
import com.chy.mdc.ftp.common.util.FtpUtil;
import com.chy.mdc.ftp.common.util.ftppool.FTPConnector;
import com.chy.mdc.ftp.model.FileInfoModel;
import com.chy.mdc.ftp.model.FtpConfig;
import com.google.protobuf.TextFormat.ParseException;

public class FtpDownload {

	/**
	 * @ClassName: downloadThread
	 * @Description: ftp下载线程类
	 * @author: wangzunpeng
	 * @date: 2018年12月6日 下午3:26:00
	 * 
	 */
	class DownloadThread extends Thread {
		FTPClient ftpClient;
		String ftpFilename;
		String ftpWorkPath;
		String localWorkPath;
		boolean override;
		String localFilename;

		/**
		 * @Title: downloadThread
		 * @Description: ftp下载线程构造方法
		 * @param ftpFilename   FTP服务器上的文件名
		 * @param ftpWorkPath   FTP服务器目录
		 * @param localWorkPath 本地目录
		 * @param override      是否覆盖重名文件（true表示覆盖）
		 * @param localFilename 本地存储时的文件名（可与服务器上文件名的不同）
		 * @throws:
		 */
		public DownloadThread(FTPClient ftpClient, String ftpFilename, String ftpWorkPath, String localWorkPath,
				boolean override, String localFilename) {
			this.ftpClient = ftpClient;
			this.ftpFilename = ftpFilename;
			this.ftpWorkPath = ftpWorkPath;
			this.localWorkPath = localWorkPath;
			this.override = override;
			this.localFilename = localFilename;
		}

		@Override
		public void run() {

//			try {
				this.downloadFile();
				ftpConnector.returnClient(ftpClient);
//			} finally {
//				count--;
//			}
			/*if (count <= 0) {
				ftpConnector.close();
				System.exit(0);
			}*/
		}

		/**
		 * @Title: downloadFile
		 * @Description: 下载单个文件
		 * @return 下载代码 0:成功 -1:ftp服务器不存在所需文件 -2:文件为临时文件(.tmp) -3:下载抛异常 -4:断点续传下载出错(本地文件大小大于ftp服务器上的文件或获取ftp文件大小失败) -5:其他线程正在下载此文件
		 */
		@SuppressWarnings("resource")
		public int downloadFile() {
			
			long start = System.currentTimeMillis();
			Log.info("切换工作目录，开始下载 ... ... " + ftpWorkPath);
			cwd(ftpClient, ftpWorkPath);
			// 判断ftp下的文件是否有临时文件，临时文件不下载
			if (ftpFilename.toLowerCase().endsWith(".tmp")) {
				return -2;
			}
			String filePath = null;
			if (ftpWorkPath == null || ftpWorkPath.trim().equals("")) {
				filePath = ftpFilename;
			} else {
				filePath = ftpWorkPath + "/" + ftpFilename;
			}
			// 是否进行断点续传
			boolean resume = false;
			boolean successed = false;
			String localFilePath = Paths.get(localWorkPath, localFilename).toString();
			File localTempFile = null;
			File localFinalFile = null;
			FileOutputStream fileOutputStream = null;
			FileChannel channel = null;
			FileLock fileLock = null;
			localTempFile = new File(localFilePath + ".tmp");
			localFinalFile = new File(localFilePath);
			if (!override) {
				if (localFinalFile.exists()) {
					// 本地文件存在同名文件则不下载
					System.out.println("file:" + localFilename + " already exists");
					return -1;
				} else if (localTempFile.exists()) {
					resume = true;
				}
			} else {
				// 需判断是否正在写入，如果正在写入则不删除，直接跳过

				if (localTempFile.exists()) {

					try {
						fileOutputStream = new FileOutputStream(localTempFile);
						channel = fileOutputStream.getChannel();
						try {
							fileLock = channel.tryLock();
							if(fileLock == null || !fileLock.isValid()) {
								Log.info("该文件正在被其他线程占用 --> " + localFilePath + ".tmp");
								return -5;
							}
						} catch (IOException e) {
							Log.info("该文件正在被其他线程占用 --> " + localFilePath + ".tmp");
							return -5;
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}

					localTempFile.delete();
				}
				if (localFinalFile.exists()) {
					localFinalFile.delete();
				}
			}
			File parentPath = new File(localWorkPath);
			if (!parentPath.exists()) {
				// 判断本地工作目录是否存在，不存在则创建
				parentPath.mkdirs();
			}

			try {
				// 断点续传
				if (resume) {
					// 需判断临时文件是否有进程正在写入，如果正在写入，则直接跳过

					fileOutputStream = new FileOutputStream(localTempFile, true);
					// ==========================================================
					channel = fileOutputStream.getChannel();
					try {
						fileLock = channel.tryLock();
						if(fileLock == null || !fileLock.isValid()) {
							Log.info("该文件正在被其他线程占用 --> " + localFilePath + ".tmp");
							return -5;
						}
					} catch (IOException e) {
						Log.info("该文件正在被其他线程占用 --> " + localFilePath + ".tmp");
						return -5;
					}
					// ==========================================================
					long ftpFileSize = getFtpFileSize(filePath); // 获取ftp上文件大小
					if (ftpFileSize == -1 || localTempFile.length() > ftpFileSize) {
						return -4;
					}
					ftpClient.setRestartOffset(localTempFile.length()); // 设置断点续传的节点
					Log.info("resume downloading file:" + ftpFilename + " set RestartOffset:" + localTempFile.length());
				} else {
					fileOutputStream = new FileOutputStream(localTempFile);
					// ==========================================================
					channel = fileOutputStream.getChannel();
					try {
						fileLock = channel.tryLock();
						if(fileLock == null || !fileLock.isValid()) {
							Log.info("该文件正在被其他线程占用 --> " + localFilePath + ".tmp");
							return -5;
						}
					} catch (IOException e) {
						Log.info("该文件正在被其他线程占用 --> " + localFilePath + ".tmp");
						return -5;
					}
					// ==========================================================
				}

				successed = ftpClient.retrieveFile(filePath, fileOutputStream);

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (fileLock != null)
						fileLock.release();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				try {
					if (channel != null)
						channel.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				if (fileOutputStream != null) {
					try {
						fileOutputStream.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						fileOutputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			if (successed) {
				localTempFile.renameTo(localFinalFile); // 下载完成，将临时文件名修改为真正的文件名
				long end = System.currentTimeMillis();
				Log.info("downLoad file: " + filePath + "\r\n  to " + localFilePath + " succeeded, use time: " + (end - start) + "ms");
				return 0;
			} else {
				return -3;
			}

		}

		/**
		 * @Title: getFtpFileSize
		 * @Description: 获取ftp下的文件大小
		 * @param filePath
		 * @return
		 * @return long
		 */
		public long getFtpFileSize(String filePath) {
			try {
				ftpClient.sendCommand("SIZE " + filePath);
			} catch (IOException e) {
				e.printStackTrace();
				return -1;
			}
			long fileSize = Long.parseLong(ftpClient.getReplyString().split("\\s+")[1]);
			return fileSize;
		}
	}

	public static void main(String[] args) {
		long s = System.currentTimeMillis();

		new FtpDownload("fy4a", 5, 10).run();
		long e = System.currentTimeMillis();
		System.err.println((e - s) + "ms");
	}

	private Date scanStartTime;
	FTPConnector ftpConnector;
	private final static String THREAD_NAME_HEAD = "FTP_THREAD_";

//	private int count = 0;
	String configType;
	int threadCount;
	int processRunTime = 0;
	/**
	 * @Title: FtpDownload
	 * @Description: ftp下载构造方法，用于初始化各配置信息
	 * @param configType  ftpconfig配置文件中的配置id（section）
	 * @param threadCount 下载ftp数据的线程数
	 */
	public FtpDownload(String configType, int threadCount, int processRunTime) {
		this.configType = configType;
		this.threadCount = threadCount;
		this.processRunTime = processRunTime;
	}
	
	public void run() {
		Map<String, FtpConfig> ftpConfigs = FtpConfigManagement.get(); // 获取ftp配置信息
		FtpConfig ftpConfig = ftpConfigs.get(configType); // 根据section获取某一个ftp配置

		List<String> workPathList = formatConfWorkPath(ftpConfig); // 获取ftp配置中带有日期配置的路径

//		ftpUtil = new FtpUtil(ftpConfig);	//创建ftp连接
		// 创建ftp连接池，池大小为下载ftp的线程大小再加1
		ftpConnector = new FTPConnector(ftpConfig, threadCount + 1);

		String fileNameRegex = ftpConfig.getFileNameRegex();
		String savePath = ftpConfig.getSavePath();
		String fileTimeRegex = ftpConfig.getFileTimeRegex();
		boolean override = ftpConfig.isOverride();
		String saveFileNameExp = ftpConfig.getSaveFileNameExp();
		int timeout = ftpConfig.getTimeout();

		// 创建ftp下载线程池
		ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);

		List<FileInfoModel> allFilesInfo = new ArrayList<FileInfoModel>();
		for (String ftpWorkPath : workPathList) {
			// 从池中获取一个ftp连接
			FTPClient ftpClient = ftpConnector.get();
			// 获取需要下载的文件信息
			List<FileInfoModel> fileInfoListNeeded = this.getFileInfoListNeeded(ftpClient, ftpWorkPath, fileNameRegex);
			// 获取文件信息后将连接放回池中
			ftpConnector.returnClient(ftpClient);

			allFilesInfo.addAll(fileInfoListNeeded);

		}
//		count = allFilesInfo.size();
//		if (count == 0) {
//			System.exit(0);
//		}
		for (int i = 0; i < allFilesInfo.size(); i++) {
			FileInfoModel fileInfoModel = allFilesInfo.get(i);
			String ftpFileName = fileInfoModel.getFilename();
//			String saveFileName = StringUtil.formatFileNameExp(ftpFileName, saveFileNameExp);
			String saveFileName = StringUtil
					.formatDateTimeAddExp(StringUtil.formatFileNameExp(ftpFileName, saveFileNameExp));
//			long fileSize = fileInfoModel.getFileSize();
			String parentPath = fileInfoModel.getParentPath();

			String localDataPath = StringUtil.formatFileNameExp(ftpFileName, StringUtil.pathFormatWithFileTimeExp(savePath, saveFileName, fileTimeRegex));
			DownloadThread downloadThread = new DownloadThread(ftpConnector.get(), ftpFileName, parentPath,
					localDataPath, override, saveFileName);
			downloadThread.setName(THREAD_NAME_HEAD + i);
			threadPool.execute(downloadThread);
		}
		
		if(processRunTime > 0) {
			
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
			executor.schedule(new Runnable() {
				@Override
				public void run() {
					Log.info("运行超时，自动退出程序......");
					System.exit(0);
				}
			}, processRunTime, TimeUnit.MINUTES);
		}
		
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(timeout, TimeUnit.MINUTES);
			ftpConnector.close();
			Log.info("ftp采集完成, dataStartModifyTime >> "
					+ ICalendar.date2String(scanStartTime, ICalendar.DEFAULT_DATETIME_FORMAT));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @Title: formatConfWorkPath
	 * @Description: 将带有日期表达式的路径转换成真正的路径
	 * @param ftpConfig ftp配置信息
	 * @return List<String> 转换后的路径集合
	 */
	private List<String> formatConfWorkPath(FtpConfig ftpConfig) {

		String workPath = ftpConfig.getWorkPath(); // ftp工作目录
		int timeZoneOffset = ftpConfig.getTimeZoneOffset(); // 服务器时间时区
		int scanSpan = ftpConfig.getScanSpan(); // 数据扫描时间范围，分钟

		//fy4a
		/*Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, timeZoneOffset - 8);
		Date time = cal.getTime(); // 当前时间

		cal.add(Calendar.MINUTE, scanSpan);

		scanStartTime = cal.getTime(); // 扫描开始时间
*/		 
		//hsd
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String startTime="2019-04-19 00:00:00";
		String endtime="2019-04-19 23:59:59";
		Date time = null;
		try {
			scanStartTime=format.parse(startTime);
			time = format.parse(endtime);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Matcher workPathMatcher = Pattern.compile(StringUtil.PATH_TIME_REG).matcher(workPath);
		int timeSerialNumber = -999;
		while (workPathMatcher.find()) {
			String timeExp = workPathMatcher.group(1);
			int tsn = DateFormatPatterns.getValueByPattern(timeExp.split(",")[0].trim()); // 取时间单位值，即在Calendar中的值
			// 精确到最小的时间单位，年月日时分秒毫秒的值逐渐增大
			if (timeSerialNumber < tsn) {
				timeSerialNumber = tsn;
			}
		}
		List<String> workPathList = new ArrayList<String>(); // 存储所有的ftp工作目录
		if (timeSerialNumber > -1) {
			List<Date> allTimes = ICalendar.getAllTimesOf2DateTimes(scanStartTime, time, timeSerialNumber); // 获取时间段内所有符合标准的时间
			for (Date date : allTimes) {
				String finalWorkPath = StringUtil.pathTimeRegexFormat(workPath, date);
				workPathList.add(finalWorkPath); // 如果savepath需要根据此处的时间确定目录，将workPathList换成map，finalworkPath为key,date为value返回
			}
		} else if (timeSerialNumber == -1) {
			throw new IllegalArgumentException("路径中存在非法的日期表达式，请检查!");
		} else {
			workPathList.add(workPath);
		}
		return new ArrayList<String>(new HashSet<String>(workPathList)); // 防止配置中表达式过长导致目录重复，此处对list作去复操作
	}

	/**
	 * 获取需要下载的文件信息(返回List<FileInfoModel>)
	 * 
	 * @return
	 */
	private List<FileInfoModel> getFileInfoListNeeded(FTPClient ftpClient, String ftpWorkPath, String fileNameRegex) {

		// 切换ftp工作目录
		Log.info("切换FTP工作目录:" + ftpWorkPath);
		this.cwd(ftpClient, ftpWorkPath);

		List<FileInfoModel> fileInfos = new ArrayList<FileInfoModel>();
		// 获取ftp下所有文件列表
		FTPFile[] listFiles = null;
		try {
			listFiles = ftpClient.listFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		List<FTPFile> ftpFiles = new ArrayList<FTPFile>();
		// 过滤文件
		for (FTPFile ftpFile : listFiles) {
			// 如果扫描开始时间晚于文件修改时间，则不下载
			if (scanStartTime.after(ftpFile.getTimestamp().getTime())) {
//				ftpFiles.add(ftpFile);
				continue;
			}
			String ftpFileName = ftpFile.getName(); // ftp下的文件名
			// 如果ftp下获取的文件是文件，且不是缓存文件，则继续下载
			if (ftpFile.isFile() && !ftpFileName.endsWith(FtpUtil.FILE_NAME_TMP)) {

				// 过滤文件名，根据正则表达式匹配，忽略大小写
				Matcher ftpFileMatcher = Pattern.compile(fileNameRegex, Pattern.CASE_INSENSITIVE).matcher(ftpFileName);
				if (ftpFileMatcher.find()) {
					FileInfoModel fileInfo = new FileInfoModel();
					fileInfo.setFilename(ftpFileName);
					fileInfo.setFileSize(ftpFile.getSize());
					fileInfo.setParentPath(ftpWorkPath);
					fileInfos.add(fileInfo);
				}
			}

		}

		return fileInfos;
	}

	/************************************************************/
	private int cwd(FTPClient ftpClient, String ftpWorkPath) {
		try {
			int replyCode = ftpClient.cwd(ftpWorkPath);
			Log.info(ftpClient.getReplyString());
			return replyCode;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

}
