package com.heterodain.solar.monitor.task;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import com.heterodain.solar.monitor.config.DeviceConfig;
import com.heterodain.solar.monitor.device.PvControllerDevice;
import com.heterodain.solar.monitor.device.PvControllerDevice.RealtimeData;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * PVコントローラー関連の非同期タスク
 */
@Component
@Slf4j
public class PvControllerTasks {
    @Autowired
    private DeviceConfig deviceConfig;

    @Autowired
    private PvControllerDevice pvControllerDevice;

    private boolean initialized = false;
    private boolean enabled = false;

    private SerialConnection conn;

    /** 計測データ */
    @Getter
    private List<Pair<LocalDateTime, RealtimeData>> datas = new ArrayList<>();

    /**
     * 初期化
     */
    @PostConstruct
    public void init() throws IOException {
        var pvcSetting = deviceConfig.getPvController();
        log.info("PVコントローラーに接続します。unitId={}", pvcSetting.getUnitId());

        var serialParam = new SerialParameters();
        serialParam.setPortName(pvcSetting.getComPort());
        serialParam.setBaudRate(115200);
        serialParam.setDatabits(8);
        serialParam.setParity("None");
        serialParam.setStopbits(1);
        serialParam.setEncoding("rtu");
        serialParam.setEcho(false);
        conn = new SerialConnection(serialParam);
        conn.open();

        initialized = true;
    }

    public void start() {
        synchronized (datas) {
            datas.clear();
        }
        enabled = true;
    }

    public void stop() {
        enabled = false;
    }

    /**
     * 1秒毎にPVコントローラーからデータ取得
     */
    @Scheduled(initialDelay = 1 * 1000, fixedDelay = 1 * 1000)
    public void realtime() {
        if (!initialized || !enabled) {
            return;
        }

        try {
            var data = pvControllerDevice.readCurrent(deviceConfig.getPvController(), conn);
            synchronized (datas) {
                datas.add(Pair.of(LocalDateTime.now(), data));
            }
        } catch (Exception e) {
            log.warn("PVコントローラーへのアクセスに失敗しました。", e);
        }
    }

    /**
     * 終了処理
     */
    @PreDestroy
    public void destroy() {
        if (conn != null) {
            log.debug("PVコントローラーを切断します。unitId={}", deviceConfig.getPvController().getUnitId());
            conn.close();
        }

        initialized = false;
    }
}
