import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class FFmpegMergeM4sToMp4 {

	public static void main(String[] args) {
//设置文件编码格式
		System.setProperty("file.encoding", "UTF-8");
//		设置需要遍历合并视频的目录
		File rootDir = new File("D:/tmp/test/download");
//		File rootDir = new File("D:/tmp/test/download");

//		开始进行目录下文件查找,从第一层开始找
		traverseDirectory(rootDir, 1);
	}

/*	public static void main(String[] args) {
//设置文件编码格式
		System.setProperty("file.encoding", "UTF-8");
//		设置需要遍历合并视频的目录
//		Scanner scanner = new Scanner(System.in);
//
//		System.out.print("需要遍历合并视频的目录: ");
//		String listVideoDir = scanner.nextLine(); // 读取一行文本
		File rootDir;
//		if (!listVideoDir.isEmpty()){
//			rootDir = new File(listVideoDir);
//		}else {
////			指定默认需要遍历合并视频的目录
//			rootDir = new File("D:\\tmp\\test\\download");
//		}
		rootDir = new File("D:\\tmp\\test\\download");
//		开始进行目录下文件查找,从第一层开始找
		traverseDirectory(rootDir, 1);
	}*/

	private static void traverseDirectory(File dir, int level) {
//		判断是否为第5层目录,且是否为有效目录
		if (level == 4 && dir.isDirectory()) {
//			对目录内m4s文件进行合并处理
			mergeM4SFilesInDirectory(dir);
			return;
		}

//递归遍历每一个子文件夹进行查找对应的第4层目录
		File[] subDirs = dir.listFiles(File::isDirectory);
		if (subDirs != null) {
			for (File subDir : subDirs) {
				traverseDirectory(subDir, level + 1);
			}
		}
	}

	private static void mergeM4SFilesInDirectory(File directory) {
//		开启日志记录
		Logger logs = loggerInfo(true);
		File videoFile = new File(directory, "video.m4s");
		File audioFile = new File(directory, "audio.m4s");
		String entryJson = directory.getParent() + "\\entry.json";
		// 解析 JSON 字符串到 JSONObject
		Map<String, String> outProperties = getOutProperties(entryJson);
		String title = outProperties.get("title").replaceAll("[\\\\\\|\\s\\.&</]", "-");
		String part = outProperties.get("part").trim().replaceAll("[\\\\\\|\\s\\.&</]", "-").replaceAll("(?i)\\.mp4", "");
//		System.out.println("------------------->>>开始进行视频合并<<<-------------------");
//		System.out.println("视频文件路径: " + videoFile);
//		System.out.println("音频文件路径: " + audioFile);
//		System.out.println("配置文件路径: " + entryFile);
//		System.out.println("获取到的标题是: " + outProperties.get("title"));
//		System.out.println("获取到的文件名是: " + outProperties.get("part"));


		System.out.println("视频合并的全路径目录是: " + directory.toString());
		/*Scanner scanner = new Scanner(System.in);

		System.out.print("合并视频输出的根目录: ");
		String outputFileRootPath = scanner.nextLine(); // 读取一行文本
		// 定义默认的输出文件目录和视频名称
		String outputDir = !outputFileRootPath.isEmpty() ? outputFileRootPath : "D:\\tmp\\test\\download";*/
		String outputDir = "D:\\tmp\\test";
		String videoName = "output.mp4";
		if (title != null && !title.isEmpty()) {
			// 使用 title 作为输出文件目录
			outputDir = new File(outputDir, title).toString();
		}
		if (part != null && !part.isEmpty()) {
			// 使用 part 作为输出视频名称
			videoName = part + ".mp4";
		}

		// 确保输出目录存在
		File outputDirFile = new File(outputDir);
		if (!outputDirFile.exists()) {
			outputDirFile.mkdirs();
		}
		// 定义输出文件的完整路径
		String outputFilePath = new File(outputDir, videoName).getAbsolutePath();

		File videoFileTmp = new File(outputFilePath);
//		控制台打印相关日志
//		outputFilePath = Pattern.quote(outputFilePath);
//		outputFilePath = outputFilePath.replaceAll("\\s+", "");
//		System.out.println("输出目录路径: " + outputDir);
//		System.out.println("输出文件名称: " + videoName);
		System.out.println("最终输出文件是: " + outputFilePath);
		if (videoFile.exists() && audioFile.exists() && !videoFileTmp.exists()) {
//组装合并视频的操作命令
			String ffmpeg = "D: & cd D:\\personSoftware\\ffmpeg-6.1.1-full_build\\bin & ffmpeg -i " + videoFile + " -i " + audioFile + "  -codec copy " + outputFilePath;
			List<String> cmdList = new ArrayList<>();
			cmdList.add("cmd");
			cmdList.add("/c");
			cmdList.add(ffmpeg);
			try {
				ProcessBuilder processBuilder = new ProcessBuilder(cmdList);
				processBuilder.directory(directory);
				processBuilder.redirectErrorStream(true);

				Process process = processBuilder.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")));

//				 ffmpag的日志输出,排查问题时开启
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
				int exitCode = process.waitFor();
				if (exitCode == 0) {
					System.out.println("最终执行的状态码（0为成功）是: " + exitCode);
				} else {
					logs.info("异常：合并过程出现异常。当前目录是:" + directory);
//					System.out.println("异常：合并过程出现异常,最终执行的状态码（0为成功）: " + exitCode);
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		} else if (videoFileTmp.exists()) {
			logs.info("视频已合并,跳过执行: " + videoFileTmp);

//			System.out.println("视频已合并,跳过执行: " + videoFileTmp.toString());
		} else {
			logs.info("异常：合并时发现video.m4s或audio.m4s不在目标路径: " + directory.getAbsolutePath());

//			System.out.println("异常：合并时发现video.m4s或audio.m4s不在目标路径: " + directory.getAbsolutePath());
		}
	}

	public static Map<String, String> getOutProperties(String entryJson) {

		Map<String, String> outProperties = new HashMap<>();

		// 读取 JSON 文件并进行解析
		try (BufferedReader reader = new BufferedReader(new FileReader(entryJson))) {
			StringBuilder jsonContent = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				jsonContent.append(line);
			}

			String jsonString = jsonContent.toString();
			JSONObject jsonObject = JSON.parseObject(jsonString);


			// 获取 标题 文件名
//			String title = jsonObject.containsKey("title") ? jsonObject.getString("title"): "未知标题";
			String title;
			if (jsonObject.containsKey("title")) {
				title = jsonObject.getString("title");
			} else {
				title = "未知标题";
			}
			outProperties.put("title", title);

			String part;
			if (jsonObject.containsKey("page_data") && !jsonObject.getJSONObject("page_data").getString("part").isEmpty()) {
				part = jsonObject.getJSONObject("page_data").getString("part");
			} else if (jsonObject.containsKey("ep") && !jsonObject.getJSONObject("ep").getString("index_title").isEmpty()) {
				part = jsonObject.getJSONObject("ep").getString("index_title");
			} else if (jsonObject.containsKey("ep") && !jsonObject.getJSONObject("ep").getString("index").isEmpty()) {
				part = jsonObject.getJSONObject("ep").getString("index");
			} else {
				part = "未知文件名";
			}
			outProperties.put("part", part);

			return outProperties;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return outProperties;
	}

	public static Logger loggerInfo(boolean appendLogs) {
		try {
			// 获取日志记录器
			Logger logger = Logger.getLogger("LoggingToFile");

			// 设置日志级别
			logger.setLevel(Level.ALL);

			// 创建文件处理器
			FileHandler fileHandler = new FileHandler("app.log", 10 * 1024 * 1024, 1, appendLogs);

			// 创建日志格式
			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);

			// 将文件处理器添加到日志记录器
			logger.addHandler(fileHandler);
			// 返回日志记录器
			return logger;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
