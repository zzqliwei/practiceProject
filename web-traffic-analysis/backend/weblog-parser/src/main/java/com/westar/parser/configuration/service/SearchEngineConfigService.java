package com.westar.parser.configuration.service;

import com.westar.parser.configuration.ReferUrlAndParams;
import com.westar.parser.configuration.SearchEngineConfig;
import com.westar.parser.configuration.loader.SearchEngineConfigLoader;
import com.westar.parser.configuration.loader.impl.FileSearchEngineConfigLoader;

import java.util.List;

/**
 *  搜索引擎配置服务类
 */
public class SearchEngineConfigService {
    private static SearchEngineConfigService searchEngineConfigManager = new SearchEngineConfigService();

    private SearchEngineConfigLoader loader = new FileSearchEngineConfigLoader();

    private List<SearchEngineConfig> searchEngineConfigs = loader.getSearchEngineConfigs();

    /**
     *  对构造子私有化
     *  从而达到利用单例模式
     */
    private SearchEngineConfigService() {

    }

    public static SearchEngineConfigService getInstance() {
        return searchEngineConfigManager;
    }

    /**
     *  根据来源url匹配已经配置好的所有的搜索引擎的配置
     *  找到第一个匹配到的搜索引擎并返回
     * @param referUrlAndParams
     * @return 匹配到的搜索引擎，如果没有匹配的搜索引擎的话则返回null
     */
    public SearchEngineConfig doMatch(ReferUrlAndParams referUrlAndParams) {
        for (SearchEngineConfig searchEngineConfig : searchEngineConfigs) {
            if (searchEngineConfig.match(referUrlAndParams)) {
                return searchEngineConfig;
            }
        }
        return null;
    }

}
