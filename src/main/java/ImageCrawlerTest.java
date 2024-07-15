/**
 * @version 1.0
 * @Auther CC
 * @Date 2024/07/12 17:03
 */

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageCrawlerTest {

	public static void main(String[] args) {
		String url = "https://699pic.com/tupian/ai.html"; // 替换为你想爬取的网页URL
		crawlImages(url);
	}

	public static void crawlImages(String urlString) {
		try {
			// 创建HttpClient实例
			CloseableHttpClient httpClient = HttpClients.createDefault();

			// 构建URI
			URIBuilder uriBuilder = new URIBuilder(urlString);

			// 发送GET请求
			HttpGet request = new HttpGet(uriBuilder.build());
			// 执行请求
			CloseableHttpResponse response = httpClient.execute(request);
			// 获取响应内容
			String html = EntityUtils.toString(response.getEntity(), "UTF-8");
			// 使用Jsoup解析HTML
			Document doc = Jsoup.parse(html);
			// 选择所有的<img>标签
			Elements images = doc.select("img");
//			System.out.println(images);

			// 遍历所有图片标签
			for (Element img : images) {
				String src1 = img.attr("abs:src"); // 获取绝对路径的src属性
				String src2 = img.attr("src"); // 获取绝对路径的src属性
				// 如果src属性值以//开头，手动添加https协议头
//				String src = src2.startsWith("//") ? "https:" + src2 : src1;
				String src = !src1.isEmpty() ? src1 : src2;

				if(!src.isEmpty()){
//				if(!src.isEmpty() && src.startsWith("http")){
					System.out.println("获取到图片信息，开始下载……");
					System.out.println("src是："+src);
					downloadImage("https:"+src);
				} else {
					System.out.println("获取到图片url非法，跳过……");
				}
			}

			// 关闭HttpClient
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public static void downloadImage(String imageUrl) {
		try {
			// 创建HttpClient实例
			CloseableHttpClient httpClient = HttpClients.createDefault();
			// 发送GET请求
			HttpGet request = new HttpGet(imageUrl);
			// 执行请求
			CloseableHttpResponse response = httpClient.execute(request);
			// 检查响应状态
			if (response.getStatusLine().getStatusCode() == 200) {
				// 获取图片内容
				byte[] imageData = EntityUtils.toByteArray(response.getEntity());
				if(imageData.length>=(25*1024)){

					// 保存图片
					Path path = Paths.get("D:/tmp/logs", new String(imageData).hashCode() + ".jpg");
					Files.write(path, imageData);
					System.out.println("已下载图片到: " + path);
				}else {
					System.out.println("获取到的图片太小，忽略……");
				}
			}

			// 关闭HttpClient
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
