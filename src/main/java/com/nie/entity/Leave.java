package com.nie.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 *
 * Created by Administrator on 2019/1/23.
 */
@Data
public class Leave {
    private int id;     //
    private String name;    //请假人名称
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDate;     //开始时间
    private Integer numDays;    //申请天数
    private String description;      //请假原因
    private String isok;    //是否审批通过
}
