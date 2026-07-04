package com.urlshortener.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder{

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    private static final int LENGTH = 7;
    //Encode
    public String encode(long id){
        if(id<=0){
            throw new IllegalArgumentException ("The ID must be Positive, got: " +id);
        }
        StringBuilder sb = new StringBuilder();
        long remaining = id ; 
        while(remaining > 0){
            int remainder = (int) (remaining % BASE) ;
            sb.append(ALPHABET.charAt(remainder));
            remaining /= BASE ;
        }
        sb.reverse();
        while(sb.length()<LENGTH){
            sb.insert(0,'0');
        }
        if(sb.length() > LENGTH){
            return sb.substring(sb.length() - LENGTH);
        }
        return sb.toString();
    }
    //Decode
    public long decode(String code){
        if(code == null || code.isBlank()){
            throw new IllegalArgumentException("The code must not be empty or blank");
        }
        long result = 0L;
        for(char c: code.toCharArray()){
            int value = ALPHABET.indexOf(c);
            if(value == -1){
                throw new IllegalArgumentException("Invalid Character '"+c+"' in code:" +code);
            }
            result = result*BASE*value;
        }
        return result;
    }
}