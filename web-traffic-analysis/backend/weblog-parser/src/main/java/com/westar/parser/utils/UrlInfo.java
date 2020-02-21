package com.westar.parser.utils;

import static com.westar.parser.utils.ParserUtils.isNullOrEmptyOrDash;
import static com.westar.parser.utils.ParserUtils.notNull;

/*
rawUrl -> https://www.underarmour.cn/s-HOVR?qf=11-149&pf=&sortStr=&nav=640#NewLaunch
schema -> https
hostport(domain) -> www.underarmour.cn
path -> /s-HOVR
query -> qf=11-149&pf=&sortStr=&nav=640
fragment -> NewLaunch
 */
public class UrlInfo {

    private String rawUrl;
    private String scheme;
    private String hostport;
    private String path;
    private String query;
    private String fragment;

    public UrlInfo(String rawUrl, String scheme, String hostport, String path, String query, String fragment) {
        this.rawUrl = rawUrl;
        this.scheme = scheme;
        this.hostport = hostport;
        this.path = path;
        this.query = query;
        this.fragment = fragment;
    }

    public String getRawUrl() {
        return rawUrl;
    }

    public void setRawUrl(String rawUrl) {
        this.rawUrl = rawUrl;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getHostport() {
        return hostport;
    }

    public void setHostport(String hostport) {
        this.hostport = hostport;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    public String getPathQueryFragment() {
        if (isNullOrEmptyOrDash(query) && isNullOrEmptyOrDash(fragment)) {
            return notNull(path);
        } else if (isNullOrEmptyOrDash(query) && !isNullOrEmptyOrDash(fragment)) {
            return path + "#" + fragment;
        } else if (!isNullOrEmptyOrDash(query) && isNullOrEmptyOrDash(fragment)) {
            return path + "?" + query;
        } else {
            return path + "?" + query + "#" + fragment;
        }
    }

    public String getUrlWithoutQuery() {
        if (isNullOrEmptyOrDash(path)) {
            return scheme + "://" + hostport;
        } else {
            return scheme + "://" + hostport + path;
        }
    }

    public String getFullUrl() {
        if (isNullOrEmptyOrDash(rawUrl)) {
            return "-";
        } else {
            return rawUrl;
        }
    }

    public String getDomain() {
        if (isNullOrEmptyOrDash(hostport)) {
            return "-";
        } else {
            return hostport;
        }
    }

}
