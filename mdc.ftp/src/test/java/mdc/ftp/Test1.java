package mdc.ftp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.chy.mdc.common.util.Constants;
import com.chy.mdc.common.util.StringUtil;

public class Test1 {
	final static String DATE_TIME_ADD_EXP = "\\$\\{dateTimeAdd,(.*)?\\}";

	public static void main(String[] args) throws ParseException {
		String timeStr = "20190809";
		StringBuffer sb = new StringBuffer();
		sb.append(timeStr);
		for(int i = 0; i < 14 - timeStr.length(); i ++) {
			sb.append("0");
		}
		System.out.println(sb.toString());
	}
}
