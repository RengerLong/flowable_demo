package com.nie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * Created by Administrator on 2019/1/11.
 */
@Controller
@RequestMapping()
public class MainController {

    @RequestMapping("/")
    public String index(){
        return "index";
    }

    @RequestMapping("/info")
    public String add(){
        return "info";
    }

    @RequestMapping("/document")
    public String document(){
        return "document";
    }

    @RequestMapping("/Approval")
    public String approval() {
        return "Approval";
    }

}
