package site.longint.utils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import site.longint.Longqbot;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class CrawlerUtil {
    public static final CrawlerUtil INSTANCE = new CrawlerUtil();

    CrawlerUtil(){
    }

    // 爬LOL英雄头像
    public LinkedHashMap<Integer, Map<String,String>> lolLengendsInfo(String folderPath) throws IOException, NullPointerException, InterruptedException {
        ChromeOptions options=new ChromeOptions();
        //设置 chrome 的无头模式
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);
        driver.get("https://lol.qq.com/cguide/code/demo/heroList.html");
        Thread.sleep(10000);
        Document doc = Jsoup.parse(driver.getPageSource());
        Element div = doc.body().getElementById("wp");
        Elements ul = div.getAllElements();
        if (ul == null || ul.size()==0){
            System.out.println("ul is null");
        }
        Elements liList = ul.get(0).getElementsByTag("li");
        LinkedHashMap<Integer, Map<String,String>>  heroList = new LinkedHashMap<>();
        int i = 0;
        for (Element li : liList){
            Element img = li.getElementsByTag("img").get(0);
            String imgURL = img.attr("src");
            if(!imgURL.contains("https:")){
                imgURL="https:"+imgURL;
            }
            String imgName = img.attr("alt");
            String imgpath = folderPath + "/" + imgName + ".png";
            LinkedHashMap<String,String> hero = new LinkedHashMap<>();
            hero.put(imgName, imgpath);
            heroList.put(i++, hero);

            saveFile(imgURL, imgpath);//保存文件的地址
        }
        driver.close();
        System.out.println("finish");
        return heroList;
    }

    // 保存网络URL图像到本地文件
    public void saveFile(String imgURL,String savePath) throws IOException {
        Connection connection = Jsoup.connect(imgURL);
        Connection.Response response = connection.method(Connection.Method.GET).ignoreContentType(true).timeout(3*1000).execute();
        BufferedInputStream bufferedInputStream = response.bodyStream();
        System.out.println(response.contentType());
        //一次最多读取1k
        byte[] buffer = new byte[1024];
        //实际读取的长度
        int readLenghth;
        //根据文件保存地址，创建文件输出流
        FileOutputStream fileOutputStream = new FileOutputStream(new File(savePath));
        //创建的一个写出的缓冲流
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        //文件逐步写入本地
        while ((readLenghth = bufferedInputStream.read(buffer,0,1024)) != -1){//先读出来，保存在buffer数组中
//            System.out.println(readLenghth);
            bufferedOutputStream.write(buffer,0,readLenghth);//再从buffer中取出来保存到本地
        }
        //关闭缓冲流
        bufferedOutputStream.close();
        fileOutputStream.close();
        bufferedInputStream.close();
    }
}
