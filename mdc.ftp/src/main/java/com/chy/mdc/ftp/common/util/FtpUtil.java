package com.chy.mdc.ftp.common.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import com.chy.mdc.common.util.Log;
import com.chy.mdc.ftp.model.FileInfoModel;
import com.chy.mdc.ftp.model.FtpConfig;

/**
 * @ClassName: FtpUtil
 * @Description: ftp工具类
 * @author: wangzunpeng
 * @date: 2018年11月1日 上午9:07:33
 * 
 */
public class FtpUtil {
	
	public final static String FILE_NAME_TMP = ".tmp";

	private FTPClient ftpClient;

	public FtpUtil(FtpConfig ftpConfig) {
		// 创建ftp连接
		boolean connected = this.connect(ftpConfig);
		if(connected) {
			Log.info("connect ftp succeed:\r\n  ftp://" + ftpConfig.getUser() + ":" + ftpConfig.getPassword() + "@" + ftpConfig.getHost() + ":" + ftpConfig.getPort());
		}else {
			Log.error("connect ftp failed:\r\n  ftp://" + ftpConfig.getUser() + ":" + ftpConfig.getPassword() + "@" + ftpConfig.getHost() + ":" + ftpConfig.getPort());
		}
	}

	/**
	 * @Title: connect
	 * @Description: 连接ftp
	 * @param ftpConfig
	 * @return boolean
	 */
	private boolean connect(FtpConfig ftpConfig) {
		if (ftpClient == null) {
			ftpClient = new FTPClient();
		}
		if (ftpClient.isConnected()) {
			try {
				ftpClient.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			ftpClient.setConnectTimeout(60000); // 设置连接超时时间
			if (ftpConfig.getPort() >= 0) {
				ftpClient.connect(ftpConfig.getHost(), ftpConfig.getPort());
				Log.info(ftpClient.getReplyString());
			} else {
				ftpClient.connect(ftpConfig.getHost());
				Log.info(ftpClient.getReplyString());
			}
			ftpClient.login(ftpConfig.getUser(), ftpConfig.getPassword());
			Log.info(ftpClient.getReplyString());
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE); // 设置数据传输方式,二进制
			int timeout = ftpConfig.getTimeout();
			ftpClient.setControlKeepAliveReplyTimeout(timeout);
			ftpClient.setControlKeepAliveTimeout(timeout);
			ftpClient.setDataTimeout(timeout);
			ftpClient.enterLocalPassiveMode();
			return true;
		} catch (SocketException e1) {
			ftpClient = null;
			e1.printStackTrace();
		} catch (IOException e1) {
			ftpClient = null;
			e1.printStackTrace();
		}
		return false;
	}

	/**
	 * 切换FTP工作目录
	 * 
	 * @param ftpWorkPath FTP目录
	 * @return
	 */
	public int cwd(String ftpWorkPath) {
		try {
			int replyCode = ftpClient.cwd(ftpWorkPath);
			setFtpWorkPath(ftpWorkPath);	//切换工作目录同时将切换后的目录设置在当前配置中
			Log.info(ftpClient.getReplyString());
			return replyCode;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
	 * 用于获取距离当前时间一定时间内的文件列表
	 * @param field 时间单位
	 * @param amount 时间长度
	 * @param timeZoneOffset ftp服务器的时区(距离utc时区的小时偏移量,东八区则为8)
	 * @return List<FTPFile>
	 */
	public List<FTPFile> getFtpFileListByTime(int field, int amount, int ftpTimeZoneOffset){
		List<FTPFile> ftpFiles = getAllFileNames();
		List<FTPFile> ftpFilesMatched = new ArrayList<FTPFile>();
		Calendar calendar = Calendar.getInstance();
		calendar.add(field, amount);
		if (ftpTimeZoneOffset != 8) {
			calendar.add(Calendar.HOUR, ftpTimeZoneOffset - 8);
		}
		Date date = calendar.getTime();
		
		for (FTPFile ftpFile : ftpFiles) {
			if (date.before(ftpFile.getTimestamp().getTime())) {
				ftpFilesMatched.add(ftpFile);
			}
		}
		return ftpFilesMatched;
	}
	
	/**
	 * @Title: getFtpFileListByTimeRange
	 * @Description: 获取时间点以后的ftp路径下所有文件
	 * @param scanStartTime 扫描开始时间
	 * @return List<FTPFile> 时间点以后到当前时间的所有文件集合
	 */
	public List<FTPFile> getFtpFileListByTimeRange(Date scanStartTime){
		List<FTPFile> ftpFiles = getAllFileNames();
		List<FTPFile> ftpFilesMatched = new ArrayList<FTPFile>();
		for (FTPFile ftpFile : ftpFiles) {
			//早于扫描开始时间的文件都放入list返回
			if (scanStartTime.before(ftpFile.getTimestamp().getTime())) {
				ftpFilesMatched.add(ftpFile);
			}
		}
		return ftpFilesMatched;
	}
	
	/**
	 * 
	 * @param field 时间单位
	 * @param amount 时间长度
	 * @param ftpTimeZoneOffset ftp服务器的时区(距离utc时区的小时偏移量,东八区则为8)
	 * @return List<FileInfoModel>
	 */
	public List<FileInfoModel> getFileInfoListByTime(int field, int amount, int ftpTimeZoneOffset){
		List<FTPFile> ftpFiles = getAllFileNames();
		List<FileInfoModel> ftpFilesMatched = new ArrayList<FileInfoModel>();
		Calendar calendar = Calendar.getInstance();
		calendar.add(field, amount);
		if (ftpTimeZoneOffset != 8) {
			calendar.add(Calendar.HOUR, ftpTimeZoneOffset - 8);
		}
		Date date = calendar.getTime();
		
		for (FTPFile ftpFile : ftpFiles) {
			if (date.before(ftpFile.getTimestamp().getTime())) {
				FileInfoModel fileInfo = new FileInfoModel();
				fileInfo.setFilename(ftpFile.getName());
				fileInfo.setFileSize(ftpFile.getSize());
				fileInfo.setFileTimestamp(ftpFile.getTimestamp().getTime());
				fileInfo.setParentPath(getFtpWorkPath());
				ftpFilesMatched.add(fileInfo);
			}
		}
		return ftpFilesMatched;
	}
	
	
	/**
	 * 获取ftpWorkPath中的所有文件名
	 * @return
	 */
	public List<FTPFile> getAllFileNames() {
		List<FTPFile> filenamesList;
		try {
			filenamesList = Arrays.asList(ftpClient.listFiles());
			return filenamesList;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public List<FileInfoModel> getAllFileInfo() {
		List<FTPFile> filenamesList;
		List<FileInfoModel> result = new ArrayList<FileInfoModel>();
		try {
			filenamesList = Arrays.asList(ftpClient.listFiles());
			for (FTPFile ftpFile : filenamesList) {
				FileInfoModel fileInfo = new FileInfoModel();
				fileInfo.setFilename(ftpFile.getName());
				fileInfo.setFileSize(ftpFile.getSize());
				fileInfo.setFileTimestamp(ftpFile.getTimestamp().getTime());
				fileInfo.setParentPath(getFtpWorkPath());
				result.add(fileInfo);
			}
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 下载单个文件
	 * @param ftpFilename FTP服务器上的文件名
	 * @param ftpWorkPath FTP服务器目录
	 * @param localWorkPath 本地目录
	 * @param override 是否覆盖重名文件（true表示覆盖）
	 * @param localFilename 本地存储时的文件名（可与服务器上文件名的不同）
	 * @return 下载代码
	 * 		   0:成功
	 *         -1:ftp服务器不存在所需文件
	 *         -2:文件为临时文件(.tmp)
	 *         -3:下载抛异常
	 *         -4:断点续传下载出错(本地文件大小大于ftp服务器上的文件或获取ftp文件大小失败)
	 * @throws IOException
	 */
	public int downloadFile(String ftpFilename, String ftpWorkPath, String localWorkPath, boolean override, String localFilename) throws IOException {
		//判断ftp下的文件是否有临时文件，临时文件不下载
		if (ftpFilename.toLowerCase().endsWith(FILE_NAME_TMP)){
			return -2;
		}
		String filePath = null;
		if(ftpWorkPath == null || ftpWorkPath.trim().equals("")){
			filePath = ftpFilename;
		}else {
			filePath = ftpWorkPath + "/" + ftpFilename;
		}
		//是否进行断点续传
		boolean resume = false;
		boolean successed = false;
		String localFilePath = Paths.get(localWorkPath, localFilename).toString();
		File localTempFile = null;
		File localFinalFile = null;
		FileOutputStream fileOutputStream = null;
		localTempFile = new File(localFilePath + FILE_NAME_TMP);
		localFinalFile = new File(localFilePath);
		if(!override){
			if(localFinalFile.exists()){
				//本地文件存在同名文件则不下载
				System.out.println("file:" + localFilename + " already exists");
				return -1;
			}else if (localTempFile.exists()) {
				resume = true;
			}
		}else{
			if (localTempFile.exists()) {
				localTempFile.delete();
			}
			if (localFinalFile.exists()) {
				localFinalFile.delete();
			}
		}
		File parentPath = new File(localWorkPath);
		if(!parentPath.exists()){
			//判断本地工作目录是否存在，不存在则创建
			parentPath.mkdirs();
		}
		try {
			//断点续传
			if (resume) {
				fileOutputStream = new FileOutputStream(localTempFile, true);
				long ftpFileSize = getFtpFileSize(filePath);
				if (ftpFileSize == -1 || localTempFile.length() > ftpFileSize){
					return -4;
				}
				ftpClient.setRestartOffset(localTempFile.length());	//设置断点续传的节点
				Log.info("resume downloading file:"+ ftpFilename +" set RestartOffset:" + localTempFile.length());
			}else {
				fileOutputStream = new FileOutputStream(localTempFile);
			}
			successed=ftpClient.retrieveFile(filePath, fileOutputStream);
		} finally {
			if (fileOutputStream != null) {
				fileOutputStream.flush(); 
				fileOutputStream.close();
			}
		}
		if(successed){
			localTempFile.renameTo(localFinalFile);	//下载完成，将临时文件名修改为真正的文件名
			Log.info("downLoad file: " + filePath + "\r\n  to " + localFilePath + " succeeded!");
			return 0;
		}else{
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

	private String ftpWorkPath;

	public String getFtpWorkPath() {
		return ftpWorkPath;
	}

	public void setFtpWorkPath(String ftpWorkPath) {
		this.ftpWorkPath = ftpWorkPath;
	}

}
