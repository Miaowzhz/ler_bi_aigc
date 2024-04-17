package com.wu.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wu.springbootinit.mapper.ChartMapper;
import com.wu.springbootinit.model.entity.Chart;
import com.wu.springbootinit.service.ChartService;
import org.springframework.stereotype.Service;

/**
* @author 仵明雨
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-03-11 20:47:09
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService {

}




