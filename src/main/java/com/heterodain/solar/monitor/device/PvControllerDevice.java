package com.heterodain.solar.monitor.device;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.heterodain.solar.monitor.config.DeviceConfig.PvController;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * PVコントローラーデバイス
 */
@Component
@Slf4j
public class PvControllerDevice {

    /**
     * リアルタイム情報取得
     * 
     * @param info 接続情報
     * @param conn シリアル接続
     * @return リアルタイム情報
     * @throws ModbusException
     */
    public synchronized RealtimeData readCurrent(PvController info, SerialConnection conn) throws ModbusException {
        ReadInputRegistersRequest req = new ReadInputRegistersRequest(0x3100, 17);
        req.setUnitID(info.getUnitId());
        ModbusSerialTransaction tr = new ModbusSerialTransaction(conn);
        tr.setRequest(req);
        tr.execute();

        ReadInputRegistersResponse res = (ReadInputRegistersResponse) tr.getResponse();

        RealtimeData data = new RealtimeData();
        data.pvVolt = ((double) res.getRegisterValue(0)) / 100;
        data.pvAmp = ((double) res.getRegisterValue(1)) / 100;
        data.pvPower = ((double) res.getRegisterValue(2) + res.getRegisterValue(3) * 0x10000) / 100;
        data.loadVolt = ((double) res.getRegisterValue(12)) / 100;
        data.loadAmp = ((double) res.getRegisterValue(13)) / 100;
        data.loadPower = ((double) res.getRegisterValue(14) + res.getRegisterValue(15) * 0x10000) / 100;
        data.battVolt = ((double) res.getRegisterValue(4)) / 100;
        // data.battAmp = ((double) res.getRegisterValue(5)) / 100;
        // data.battPower = ((double) res.getRegisterValue(6) + res.getRegisterValue(7)
        // * 0x10000) / 100;
        data.battPower = data.pvPower - data.loadPower;
        data.battAmp = data.battPower / data.battVolt;
        data.battTemp = ((double) res.getRegisterValue(16)) / 100;

        // MEMO： Tracerシリーズなど上位機種なら内蔵センサーの温度も取得可能
        // log.debug("Inside temp: {}℃", ((double) res.getRegisterValue(17)) / 100);
        // log.debug("Power temp: {}℃", ((double) res.getRegisterValue(18)) / 100);

        req = new ReadInputRegistersRequest(0x311A, 1);
        req.setUnitID(info.getUnitId());
        tr = new ModbusSerialTransaction(conn);
        tr.setRequest(req);
        tr.execute();

        res = (ReadInputRegistersResponse) tr.getResponse();
        data.battSOC = ((double) res.getRegisterValue(0)) / 100;

        req = new ReadInputRegistersRequest(0x3201, 1);
        req.setUnitID(info.getUnitId());
        tr = new ModbusSerialTransaction(conn);
        tr.setRequest(req);
        tr.execute();

        res = (ReadInputRegistersResponse) tr.getResponse();
        data.stage = STAGE.values()[(res.getRegisterValue(0) >> 2) & 0x0003];

        log.debug("{}", data);

        return data;
    }
    // public synchronized RealtimeData readCurrent(PvController info,
    // SerialConnection conn) throws ModbusException {
    // Random rand = new Random();
    // RealtimeData data = new RealtimeData();
    // data.loadPower = rand.nextDouble() * 100D;
    // data.pvPower = rand.nextDouble() * 100D;
    // data.battVolt = rand.nextDouble() * 14.8D;

    // log.debug("{}", data);

    // return data;
    // }

    /**
     * 集計値取得
     * 
     * @param info 接続情報
     * @param conn シリアル接続
     * @return 集計値
     * @throws ModbusException
     */
    public synchronized StaticalData readAggregate(PvController info, SerialConnection conn) throws ModbusException {
        ReadInputRegistersRequest req = new ReadInputRegistersRequest(0x3300, 20);
        req.setUnitID(info.getUnitId());
        ModbusSerialTransaction tr = new ModbusSerialTransaction(conn);
        tr.setRequest(req);
        tr.execute();

        ReadInputRegistersResponse res = (ReadInputRegistersResponse) tr.getResponse();

        StaticalData data = new StaticalData();
        data.dailyPvPower = ((double) res.getRegisterValue(4) + res.getRegisterValue(5) * 0x10000) / 100;
        data.monthlyPvPower = ((double) res.getRegisterValue(6) + res.getRegisterValue(7) * 0x10000) / 100;
        data.annualPvPower = ((double) res.getRegisterValue(8) + res.getRegisterValue(9) * 0x10000) / 100;
        data.totalPvPower = ((double) res.getRegisterValue(10) + res.getRegisterValue(11) * 0x10000) / 100;
        data.dailyLoadPower = ((double) res.getRegisterValue(12) + res.getRegisterValue(13) * 0x10000) / 100;
        data.monthlyLoadPower = ((double) res.getRegisterValue(14) + res.getRegisterValue(15) * 0x10000) / 100;
        data.annualLoadPower = ((double) res.getRegisterValue(16) + res.getRegisterValue(17) * 0x10000) / 100;
        data.totalLoadPower = ((double) res.getRegisterValue(18) + res.getRegisterValue(19) * 0x10000) / 100;

        log.debug("{}", data);

        return data;
    }

    @Getter
    @ToString
    public static class RealtimeData {
        public Double pvVolt;
        public Double pvAmp;
        public Double pvPower;

        public Double battVolt;
        public Double battAmp;
        public Double battPower;

        public Double loadVolt;
        public Double loadAmp;
        public Double loadPower;

        public Double battTemp;
        public Double battSOC;

        public STAGE stage;
    }

    @Getter
    @ToString
    public static class StaticalData {
        public Double dailyPvPower;
        public Double monthlyPvPower;
        public Double annualPvPower;
        public Double totalPvPower;

        public Double dailyLoadPower;
        public Double monthlyLoadPower;
        public Double annualLoadPower;
        public Double totalLoadPower;
    }

    @AllArgsConstructor
    @Getter
    public static enum STAGE {
        NO_CHARGING("No Charging"), FLOAT("Float"), BOOST("Boost"), EQULIZATION("Equlization");

        private String displayString;
    }
}