package com.example.legendfive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service @Slf4j @RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService{

    private final StringRedisTemplate redisTemplate;

    public String makeVerificationCode() {
        String verificationCode = "";
        for (int i = 0; i < 4; i++) {
            verificationCode += (int)(Math.random() * 10);
        }
        return verificationCode;
    }
    //5분의 인증코드
    public void saveVerificationCode(String key, String verificationCode) {
        log.info("key: " + key + " verificationCode: " + verificationCode);
        log.info("redisTemplate: " + redisTemplate);
        redisTemplate.opsForValue().set(key, verificationCode, 5, TimeUnit.MINUTES);
    }

    //인증 성공 시 reids false -> true 덮어씌우기
    public void saveCompletionCode(String key, boolean status) {
        //status to string
        redisTemplate.opsForValue().set("CompletionCode:" + key, String.valueOf(status), 5, TimeUnit.MINUTES);
    }

    public boolean isVerified(String key){
        //or catch "error": "Cannot invoke \"String.equals(Object)\" because \"status\" is null"
        String status = redisTemplate.opsForValue().get("CompletionCode:"+key);
        if(status == null) return false;
        return status.equals("true");
    }
    public boolean verifyCode(String key, String verificationCode) {
        String code = redisTemplate.opsForValue().get(key);
        if(code == null) return false;
        return code.equals(verificationCode);
    }

    public void deleteVerificationCode(String key) {
        redisTemplate.delete(key);
    }

}
