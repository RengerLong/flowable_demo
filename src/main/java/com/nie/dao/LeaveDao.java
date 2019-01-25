package com.nie.dao;

import com.nie.entity.Leave;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * Created by Administrator on 2019/1/23.
 */
@Mapper
@Repository
public interface LeaveDao {
    int saveLeave(Leave leave);
}
