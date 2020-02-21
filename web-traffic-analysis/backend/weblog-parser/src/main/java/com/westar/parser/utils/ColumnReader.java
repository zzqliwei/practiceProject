package com.westar.parser.utils;

import java.util.HashMap;
import java.util.Map;

/**
 *  按照一定的规则解析query_string，将所有的kv值放到内存中
 *  便于根据key获取相对应的value
 */
public class ColumnReader {

    public Map<String,String> keyvalues = new HashMap<String,String>();

    public ColumnReader(String line){
        String[] temps = line.split("&");
        for (String kvStr : temps) {
            String[] kv = kvStr.split("=");
            if (kv.length == 2) {
                keyvalues.put(kv[0], kv[1]);
            }
        }

    }

    public String getStringValue(String key) {
        return UrlParseUtils.decode(keyvalues.getOrDefault(key, "-"));
    }
}
