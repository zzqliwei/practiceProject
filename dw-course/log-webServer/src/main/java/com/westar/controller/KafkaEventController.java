package com.westar.controller;

import com.westar.model.UserActionJson;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;


@Controller
public class KafkaEventController implements InitializingBean {

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

    private Producer<String, String> producer;

    @Autowired
    private HttpServletRequest request;

    @Override
    public void afterPropertiesSet() throws Exception {
        Properties props = new Properties();
        props.put("bootstrap.servers", "master:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("batch.size", "10");
//        producer = new KafkaProducer<>(props);
    }

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

    @RequestMapping(path = "/user/action/json", method = RequestMethod.POST)
    @ResponseBody
    public String sendKafka(@RequestBody UserActionJson userAction) {
        userAction.setClientIp(getClientIp());
        userAction.setClickDate(dateFormat.format(new Date()));

        producer.send(new ProducerRecord<>("user-action", userAction.toJson()));
        return "";
    }
}
