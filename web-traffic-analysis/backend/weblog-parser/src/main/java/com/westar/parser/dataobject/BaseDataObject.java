package com.westar.parser.dataobject;

import com.westar.iplocation.IpLocation;
import com.westar.parser.utils.DateUtils;
import eu.bitwalker.useragentutils.UserAgent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 存放公共信息的
 */
public class BaseDataObject implements ParsedDataObject {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private int profileId;
    private String trackerVersion;
    private String command;
    private String userId;
    private String pvId;
    private String serverTimeString;
    private Date serverTime;
    private Calendar calendar;
    private String userAgent;
    private UserAgent userAgentInfo;
    private String clientIp;
    private IpLocation ipLocation;

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getTrackerVersion() {
        return trackerVersion;
    }

    public void setTrackerVersion(String trackerVersion) {
        this.trackerVersion = trackerVersion;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPvId() {
        return pvId;
    }

    public void setPvId(String pvId) {
        this.pvId = pvId;
    }

    public String getServerTimeString() {
        return serverTimeString;
    }

    public void setServerTimeString(String serverTimeString) {
        this.serverTimeString = serverTimeString;
        this.calendar = Calendar.getInstance(Locale.ENGLISH);

        try {
            this.serverTime = dateFormat.parse(serverTimeString);
            calendar.setTime(this.serverTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Date getServerTime() {
        return serverTime;
    }

    public int getHourOfDay() {
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public String getDayOfWeek() {
        return DateUtils.getChineseWeekStr(calendar.get(Calendar.DAY_OF_WEEK));
    }

    public int getMonthOfYear() {
        return calendar.get(Calendar.MONTH);
    }

    public int getWeekOfYear() {
        calendar.setMinimalDaysInFirstWeek(7);
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }




    public void setServerTime(Date serverTime) {
        this.serverTime = serverTime;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public UserAgent getUserAgentInfo() {
        return userAgentInfo;
    }

    public void setUserAgentInfo(UserAgent userAgentInfo) {
        this.userAgentInfo = userAgentInfo;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public IpLocation getIpLocation() {
        return ipLocation;
    }

    public void setIpLocation(IpLocation ipLocation) {
        this.ipLocation = ipLocation;
    }
}
