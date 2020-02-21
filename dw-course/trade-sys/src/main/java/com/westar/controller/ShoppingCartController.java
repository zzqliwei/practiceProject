package com.westar.controller;

import com.westar.service.HelloService;
import com.westar.service.ShoppingCartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * logback
 * log4j
 * log4j2
 */
@Controller
public class ShoppingCartController {

    private Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);

    @Autowired
    private HelloService helloService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @RequestMapping(path = "/cart/index")
    public String index(Model model) {
        String hello = helloService.getHello();
        model.addAttribute("hello", hello);
        return "shoppingcart/index";
    }

    @RequestMapping(path = "/cart/succ")
    public String succ() {
        return "shoppingcart/addcartsucc";
    }


    @RequestMapping(path = "/cart/add", method = RequestMethod.POST)
    public String addProduct2Cart() {
        logger.info("starting addProduct2Cart");

        shoppingCartService.addProduct2Cart();

        logger.info("end addProduct2Cart");

        return "shoppingcart/addcartsucc";
    }



}
