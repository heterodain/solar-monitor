package com.heterodain.solar.monitor.controller;

import java.util.List;
import java.util.stream.Collectors;

import com.heterodain.solar.monitor.task.PvControllerTasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/measure")
public class MeasureController {

    @Autowired
    PvControllerTasks tasks;

    @PostMapping("/start")
    public void start() {
        tasks.start();
    }

    @PostMapping("/stop")
    public void stop() {
        tasks.stop();
    }

    @GetMapping("/data")
    public List<Object[]> get() {
        var datas = tasks.getDatas();
        synchronized (datas) {
            return datas.stream().map(p -> new Object[] { p.getKey(), p.getValue().loadPower, p.getValue().pvPower,
                    p.getValue().battVolt, p.getValue().battSOC }).collect(Collectors.toList());
        }
    }

}
