package com.urlshortener.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SnowflakeIdGenerator{
    // Custom Epoch
    private static final long CUSTOM_EPOCH = 1704067200000L;
    // Each Section Bits
    private static final long SEQUENCE_BITS = 12L;
    private static final long MACHINE_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    //Maximum Values For Each Section
    private static final long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BITS);
    private static final long MAX_MACHINE_ID = -1L ^ (-1L << MACHINE_ID_BITS);
    private static final long MAX_DATACENTER_ID = -1L ^ (-1L << DATACENTER_ID_BITS);
    //Left Shift to Each Section
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATACENTER_ID_BITS;
    //Instance State
    private final long datacenterId;
    private final long machineId;
    private long sequence = 0L;
    private long lastTimeStamp = -1L;
    //Constructor
    public SnowflakeIdGenerator(
        @Value("${snowflake.datacenter-id}") long datacenterId,
        @Value("${snowflake.machine-id}") long machineId){
            if(datacenterId < 0 || datacenterId > MAX_DATACENTER_ID){
                throw new IllegalArgumentException("Datacenter ID must be between 0 and " + MAX_DATACENTER_ID);
            }
            if(machineId < 0 || machineId > MAX_MACHINE_ID){
                throw new IllegalArgumentException("Machine ID must be between 0 and " + MAX_MACHINE_ID);
            }
            this.datacenterId = datacenterId;
            this.machineId = machineId;
            log.info("Snowflake ready - datacenter = {} machine = {}",datacenterId,machineId);
        }
    public synchronized long nextId() {
        long currentTimeStamp = currentTimeMs();
        if(currentTimeStamp < lastTimeStamp) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate ID.");
        }
        if(currentTimeStamp == lastTimeStamp){
            sequence = (sequence+1) & MAX_SEQUENCE ; 
            if(sequence == 0L){
                currentTimeStamp = waitForNextMillis(lastTimeStamp);
            }
        }
        else{
            sequence = 0L ;
        }
        lastTimeStamp = currentTimeStamp;
        //Assemble the final 64 bit ID
        return  ((currentTimeStamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT) | (datacenterId << DATACENTER_ID_SHIFT) | (machineId << MACHINE_ID_SHIFT) | sequence;
    }
    private long waitForNextMillis(long lastTimeStamp){
        long timestamp = currentTimeMs();
       while(timestamp <= lastTimeStamp){
        timestamp = currentTimeMs();
       } 
       return timestamp;
    }
    private long currentTimeMs(){
        return System.currentTimeMillis();
    }
}