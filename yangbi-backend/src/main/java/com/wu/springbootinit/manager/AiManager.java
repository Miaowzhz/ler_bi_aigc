package com.wu.springbootinit.manager;

import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AiManager {


    @Resource
    private YuCongMingClient yuCongMingClient;


    public String doChat(long modelId ,String message){
        DevChatRequest devChatRequest = new DevChatRequest();
        //devChatRequest.setModelId(1651468516836098050L);
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(message);
        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);

        return response.getData().getContent();
    }

}
