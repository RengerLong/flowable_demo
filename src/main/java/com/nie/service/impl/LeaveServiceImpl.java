package com.nie.service.impl;

import com.nie.dao.LeaveDao;
import com.nie.entity.Leave;
import com.nie.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * Created by Administrator on 2019/1/23.
 */
@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveDao leaveDao;

    @Override
    public int saveLeave(Leave leave) {
        return leaveDao.saveLeave(leave);
    }

}
