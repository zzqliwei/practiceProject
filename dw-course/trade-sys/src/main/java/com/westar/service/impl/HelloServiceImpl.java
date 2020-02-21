package com.westar.service.impl;

import com.westar.dao.HelloDao;
import com.westar.service.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HelloServiceImpl implements HelloService {

    @Autowired
    private HelloDao helloDao;

    @Override
    public String getHello() {
        return helloDao.findHello();
    }
}
