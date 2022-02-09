package burp.Http;

import burp.IBurpExtenderCallbacks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class HttpRequest {

    public static String sendGet(String url, Map headers, IBurpExtenderCallbacks callbacks){
        String result = "";
        BufferedReader in = null;
        try {
            URL realUrl = new URL(url);
            URLConnection connection = realUrl.openConnection();
            for(Object header : headers.keySet()){
                //给GET请求的参数设置header参数
                if(header.toString().equals("X-Forwarded-For")){
                    connection.setRequestProperty(header.toString(),""+headers.get(header.toString()).toString());
                }
                connection.setRequestProperty(header.toString(),headers.get(header.toString()).toString());
            }
            connection.connect();
            Map<String, List<String>> map = connection.getHeaderFields();
            for(String key : map.keySet()){
                System.out.println(key + "--->" + map.get(key));
            }
            //获取到输入流读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while((line = in.readLine()) != null){
                result += line;
            }
            callbacks.printOutput(result);
        } catch (Exception e) {
            callbacks.printError("Get请求出现错误："+e.toString());
            e.printStackTrace();
        }finally {
            //关闭输入流
            try{
                if(in != null){
                    in.close();
                }
            }catch (Exception e2){
                e2.printStackTrace();
            }
        }
        return result;
    }
    //发送POST请求
    public static String sendPost(String url,String param,Map headers,IBurpExtenderCallbacks callbacks){
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            URLConnection connection = realUrl.openConnection();
            for (Object o : headers.keySet()) {
                connection.setRequestProperty(o.toString(),headers.get(o.toString()).toString());
            }
            //设置POST请求
            connection.setDoInput(true);
            connection.setDoOutput(true);
            //获取到connection的输出流
            out = new PrintWriter(connection.getOutputStream());
            //写入参数到请求中
            out.print(param);
            //刷新流就可以发送请求
            out.flush();
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while((line = in.readLine()) != null){
                result += line;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            callbacks.printError("POSTError："+e.toString()+"参数为："+param);
            e.printStackTrace();
        }finally {
            if(out != null){
                out.close();
            }
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    //重载没有header的get方法
    public static String sendGet(String url, IBurpExtenderCallbacks callbacks){
        String result = "";
        try {
            URL realUrl = new URL(url);
            URLConnection connection = realUrl.openConnection();
            connection.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
