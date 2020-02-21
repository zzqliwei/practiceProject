package com.westar.dao.impl;

import com.westar.dao.ShoppingCartDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class ShoppingCartDaoImpl implements ShoppingCartDao {

    private Logger logger = LoggerFactory.getLogger(ShoppingCartDaoImpl.class);

    @Override
    public void addProduct2Cart() {
        //将客户加的商品放到数据库中的购物车表中
        logger.info("add product in db shopping cart");
    }
}
