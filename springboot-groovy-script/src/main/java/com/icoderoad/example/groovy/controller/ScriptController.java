package com.icoderoad.example.groovy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.icoderoad.example.groovy.util.GroovyScriptExecutor;

@Controller
public class ScriptController {

    @GetMapping("/")
    public String index(Model model) throws Exception {
        // 此处调用Groovy脚本来获取默认时间和星期几
        String currentTime = GroovyScriptExecutor.executeScript("DefaultScript.groovy", "getCurrentTime");
        String currentDayOfWeek = GroovyScriptExecutor.executeScript("DefaultScript.groovy", "getCurrentDayOfWeek");

        model.addAttribute("currentTime", currentTime);
        model.addAttribute("currentDayOfWeek", currentDayOfWeek);

        return "index";
    }
}