package com.westar.controller;

import com.westar.model.UserAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;


@Controller
public class LoggerController {

    private Logger logger = LoggerFactory.getLogger("userAction");

    @Autowired
    private HttpServletRequest request;

    private String getClientIp() {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }

    @RequestMapping(path = "/user/action", method = RequestMethod.POST)
    @ResponseBody
    public String logUserAction(@RequestBody UserAction userAction) {
        String finalUserActionStr = getClientIp() + "\t" + userAction.toString();
        logger.info(finalUserActionStr);
        return "";
    }

}
