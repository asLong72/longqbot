package site.longint.utils;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.ImageType;
import net.mamoe.mirai.utils.ExternalResource;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import site.longint.DAO.ImageIndicator;
import site.longint.Longqbot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImgUtils {
    public static Image getImagefromImageIndicator(ImageIndicator ii, Contact aim, Bot bot) throws FileNotFoundException {
        String imgid = ii.getImgid();
        String nativeURI = ii.getNativeURI();
        Integer width = ii.getWidth();
        Integer height = ii.getHeight();
        if(!imgid.equals("") && !nativeURI.equals("")){
            Image.Builder builder = Image.Builder.newBuilder(imgid);
            builder.setWidth(width);
            builder.setHeight(height);
            builder.setType(ImageType.PNG);

            Image img = builder.build();
            if(Image.isUploaded(img, bot)){
                return img;
            }else{
                return uploadNativeImg(aim, new FileInputStream(new File(nativeURI)));
            }
        }
        return null;
    }

    public static Image uploadWebImg(Contact aim, String imgURL){
        try {
            Connection connection = Jsoup.connect(imgURL);
            Connection.Response response = connection.method(Connection.Method.GET).ignoreContentType(true).timeout(3*1000).execute();
            BufferedInputStream bufferedInputStream = response.bodyStream();
            System.out.println(response.contentType());
            //一次最多读取1k
            byte[] buffer = new byte[1024];
            //实际读取的长度
            int readLenghth;
            //根据文件保存地址，创建文件输出流
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            while ((readLenghth = bufferedInputStream.read(buffer,0,1024)) != -1){//先读出来，保存在buffer数组中
//            System.out.println(readLenghth);
                os.write(buffer,0,readLenghth);//再从buffer中取出来保存到本地
            }
            InputStream inputStream = new ByteArrayInputStream(os.toByteArray());//文件逐步写入本地
            os.close();
            ExternalResource temp = ExternalResource.create(inputStream);
            Image img = ExternalResource.uploadAsImage(temp, aim);
            temp.close();
            inputStream.close();
            return img;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    public static Image uploadNativeImg(Contact aim, InputStream imgStream){
        try {
//            System.out.println("uploadNativeImg");
            ExternalResource temp = ExternalResource.create(imgStream);
            Image img = ExternalResource.uploadAsImage(temp, aim);
            temp.close();
            imgStream.close();
            return img;
        }catch (Exception e){
            Longqbot.INSTANCE.getLogger().error(e);
            return null;
        }
    }

    /**
     * 横向拼接一组（多张）图像
     * @param picspath  将要拼接的图像
     * @param type 图像写入格式
     * @return
     */
    public static InputStream joinNativeImageListHorizontal(String[] picspath, String type) {
        try {
            int len = picspath.length;
            File[] src = new File[len];
            BufferedImage[] images = new BufferedImage[len];
            int[][] imageArrays = new int[len][];
            for (int i = 0; i < len; i++) {
                src[i] = new File(picspath[i]);
                images[i] = ImageIO.read(src[i]);
                int width = images[i].getWidth();
                int height = images[i].getHeight();
                imageArrays[i] = new int[width * height];// 从图片中读取RGB
                imageArrays[i] = images[i].getRGB(0, 0, width, height,  imageArrays[i], 0, width);
            }

            int dst_width = 0;
            int dst_height = images[0].getHeight();
            for (BufferedImage image : images) {
                dst_height = Math.max(dst_height, image.getHeight());
                dst_width += image.getWidth();
            }
//            System.out.println(dst_width);
//            System.out.println(dst_height);
            if (dst_height < 1) {
                System.out.println("dst_height < 1");
                return null;
            }
            /*
             * 生成新图片
             */
            BufferedImage ImageNew = new BufferedImage(dst_width, dst_height,  BufferedImage.TYPE_INT_RGB);
            int width_i = 0;
            for (int i = 0; i < images.length; i++) {
                ImageNew.setRGB(width_i, 0, images[i].getWidth(), dst_height,  imageArrays[i], 0, images[i].getWidth());
                width_i += images[i].getWidth();
            }
//            File outFile = new File(dst_pic);
//            ImageIO.write(ImageNew, type, outFile);// 写图片
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(ImageNew, "png", os);
            InputStream inputStream = new ByteArrayInputStream(os.toByteArray());

            return inputStream;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static InputStream joinWebImageListHorizontal(String[] picsURL, String type) {
        try {
            int len = picsURL.length;
            BufferedImage[] images = new BufferedImage[len];
            int[][] imageArrays = new int[len][];
            for (int i = 0;i<len;i++){
                Connection connection = Jsoup.connect(picsURL[i]);
                Connection.Response response = connection.method(Connection.Method.GET).ignoreContentType(true).timeout(3*1000).execute();
                BufferedInputStream bufferedInputStream = response.bodyStream();
                System.out.println(response.contentType());

                //一次最多读取1k
                byte[] buffer = new byte[1024];
                //实际读取的长度
                int readLenghth;
                //根据文件保存地址，创建文件输出流
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                while ((readLenghth = bufferedInputStream.read(buffer,0,1024)) != -1){//先读出来，保存在buffer数组中
//            System.out.println(readLenghth);
                    os.write(buffer,0,readLenghth);//再从buffer中取出来保存到本地
                }
                InputStream inputStream = new ByteArrayInputStream(os.toByteArray());//文件逐步写入本地
                os.close();
                images[i] = ImageIO.read(inputStream);
                int width = images[i].getWidth();
                int height = images[i].getHeight();
                imageArrays[i] = new int[width * height];// 从图片中读取RGB
                imageArrays[i] = images[i].getRGB(0, 0, width, height,  imageArrays[i], 0, width);
            }

            int dst_width = 0;
            int dst_height = images[0].getHeight();
            for (BufferedImage image : images) {
                dst_height = Math.max(dst_height, image.getHeight());
                dst_width += image.getWidth();
            }
//            System.out.println(dst_width);
//            System.out.println(dst_height);
            if (dst_height < 1) {
                System.out.println("dst_height < 1");
                return null;
            }
            /*
             * 生成新图片
             */
            BufferedImage ImageNew = new BufferedImage(dst_width, dst_height,  BufferedImage.TYPE_INT_RGB);
            int width_i = 0;
            for (int i = 0; i < images.length; i++) {
                ImageNew.setRGB(width_i, 0, images[i].getWidth(), dst_height,  imageArrays[i], 0, images[i].getWidth());
                width_i += images[i].getWidth();
            }
//            File outFile = new File(dst_pic);
//            ImageIO.write(ImageNew, type, outFile);// 写图片
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(ImageNew, "png", os);
            InputStream inputStream = new ByteArrayInputStream(os.toByteArray());

            return inputStream;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 纵向拼接一组（多张）图像
     * @param picspath		将要拼接的图像数组
     * @param type	写入图像类型
     * @param dst_pic	写入图像路径
     * @return
     */
    public static boolean joinImageListVertical(String[] picspath, String type, String dst_pic) {
        try {
            int len = picspath.length;
            if (len < 1) {
                System.out.println("pics len < 1");
                return false;
            }
            File[] src = new File[len];
            BufferedImage[] images = new BufferedImage[len];
            int[][] imageArrays = new int[len][];
            for (int i = 0; i < len; i++) {
                //System.out.println(i);
                src[i] = new File(picspath[i]);
                images[i] = ImageIO.read(src[i]);
                int width = images[i].getWidth();
                int height = images[i].getHeight();
                imageArrays[i] = new int[width * height];// 从图片中读取RGB
                imageArrays[i] = images[i].getRGB(0, 0, width, height,  imageArrays[i], 0, width);
            }

            int dst_height = 0;
            int dst_width = images[0].getWidth();
            for (int i = 0; i < images.length; i++) {
                dst_width = dst_width > images[i].getWidth() ? dst_width : images[i].getWidth();
                dst_height += images[i].getHeight();
            }
            //System.out.println(dst_width);
            //System.out.println(dst_height);
            if (dst_height < 1) {
                System.out.println("dst_height < 1");
                return false;
            }
            /*
             * 生成新图片
             */
            BufferedImage ImageNew = new BufferedImage(dst_width, dst_height,  BufferedImage.TYPE_INT_RGB);
            int height_i = 0;
            for (int i = 0; i < images.length; i++) {
                ImageNew.setRGB(0, height_i, dst_width, images[i].getHeight(),  imageArrays[i], 0, dst_width);
                height_i += images[i].getHeight();
            }
            File outFile = new File(dst_pic);
            ImageIO.write(ImageNew, type, outFile);// 写图片
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
