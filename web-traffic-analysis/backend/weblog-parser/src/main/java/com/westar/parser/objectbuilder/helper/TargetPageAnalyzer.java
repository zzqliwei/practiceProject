package com.westar.parser.objectbuilder.helper;

import com.westar.parser.configuration.TargetConfigMatcher;
import com.westar.parser.configuration.loader.ProfileConfigLoader;
import com.westar.parser.configuration.service.TargetPageConfigService;

import java.util.ArrayList;
import java.util.List;

/**
 *  匹配并得到真正的目标页面的分析类
 */
public class TargetPageAnalyzer {

    private TargetPageConfigService targetPageConfigService;

    public TargetPageAnalyzer(ProfileConfigLoader profileConfigLoader) {
        this.targetPageConfigService = new TargetPageConfigService(profileConfigLoader.getTargetPages());
    }

    /**
     *  匹配指定的profileId的目标页面
     * @param profileId
     * @param pvUrl
     * @return
     */
    public List<TargetConfigMatcher> getTargetHits(int profileId, String pvUrl) {
        //获取指定profileId对应的所有目标页面匹配类
        List<TargetConfigMatcher> targetConfigMatchers = this.targetPageConfigService.getByProfileId(profileId);

        //将url与所有的目标页面匹配类进行匹配，得到所有可以匹配的目标页面匹配对象
        List<TargetConfigMatcher> targetConfigHits = new ArrayList<>();
        for (TargetConfigMatcher targetConfigMatcher : targetConfigMatchers) {
            if (targetConfigMatcher.match(pvUrl)) {
                targetConfigHits.add(targetConfigMatcher);
            }
        }
        return targetConfigHits;
    }

}
