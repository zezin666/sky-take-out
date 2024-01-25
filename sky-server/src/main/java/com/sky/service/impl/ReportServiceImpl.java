package com.sky.service.impl;

import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            Double temp = orderMapper.getTurnoverByDate(LocalDateTime.of(localDate, LocalTime.MIN),
                    LocalDateTime.of(localDate,LocalTime.MAX));
            if (temp == null){
                temp = 0.0;
            }
            turnoverList.add(temp);
        }

        TurnoverReportVO turnoverReportVO = new TurnoverReportVO();
        turnoverReportVO.setDateList(StringUtils.join(dateList,","));
        turnoverReportVO.setTurnoverList(StringUtils.join(turnoverList,","));
        return turnoverReportVO;
    }
}
