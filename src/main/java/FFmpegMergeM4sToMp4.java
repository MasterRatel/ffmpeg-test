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

	//	开启日志，并使用追加模式记录信息
	private static final Logger logger = setupLogger(true);

	// 定义一个正则表达式，匹配Windows文件名中不允许的字符
	private static final String INVALID_CHARACTERS_REGEX = "[<>:\"/\\\\\\s\\.|?*]";


	public static void main(String[] args) {
		// 设置文件编码格式
		System.setProperty("file.encoding", "UTF-8");
		// 设置需要遍历合并视频的目录
		File rootDir = new File("D:/tmp/test/download");
		// 开始进行目录下文件查找,从第一层开始找
		traverseDirectory(rootDir, 1);
		System.out.println("------------->>>"+rootDir+"文件夹下的视频合并任务，全部运行完毕，记得检查日志噢<<<-------------");
	}

	private static void traverseDirectory(File dir, int level) {
		// 判断是否为第4层目录,且是否为有效目录
		if (level == 4 && dir.isDirectory()) {
			// 对目录内m4s文件进行合并处理
			mergeM4SFilesInDirectory(dir);
			return;
		}

		// 递归遍历每一个子文件夹进行查找对应的第4层目录
		File[] subDirs = dir.listFiles(File::isDirectory);
		if (subDirs != null) {
			for (File subDir : subDirs) {
				traverseDirectory(subDir, level + 1);
			}
		}
	}

	private static void mergeM4SFilesInDirectory(File directory) {
		File videoFile = new File(directory, "video.m4s");
		File audioFile = new File(directory, "audio.m4s");
		String entryJson = directory.getParent() + "\\entry.json";

		// 解析 JSON 字符串到 JSONObject
		Map<String, String> outProperties = getOutProperties(entryJson);
		String title = sanitizeFileName(outProperties.get("title"));
		String part = sanitizeFileName(outProperties.get("part").replaceAll("(?i)\\.mp4", ""));

		logger.info("视频合并的全路径目录是: " + directory);

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
		logger.info("最终输出文件是: " + outputFilePath);

		if (videoFile.exists() && audioFile.exists() && !videoFileTmp.exists()) {
			// 组装合并视频的操作命令
			String ffmpeg = "D: & cd D:\\personSoftware\\ffmpeg-6.1.1-full_build\\bin & ffmpeg -i " + videoFile + " -i " + audioFile + " -codec copy " + outputFilePath;
			List<String> cmdList = new ArrayList<>();
			cmdList.add("cmd");
			cmdList.add("/c");
			cmdList.add(ffmpeg);
			executeCommand(cmdList, directory);
		} else if (videoFileTmp.exists()) {
			logger.info("视频已合并,跳过执行: " + videoFileTmp);
		} else {
			logger.info("异常：合并时发现video.m4s或audio.m4s不在目标路径: " + directory.getAbsolutePath());
		}
	}

	private static void executeCommand(List<String> cmdList, File directory) {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(cmdList);
			processBuilder.directory(directory);
			processBuilder.redirectErrorStream(true);

			Process process = processBuilder.start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")))) {
				String line;
				while ((line = reader.readLine()) != null) {
					logger.info(line);
				}
			}
			int exitCode = process.waitFor();
			if (exitCode == 0) {
				logger.info("最终执行的状态码（0为成功）是: " + exitCode);
			} else {
				logger.severe("异常：合并过程出现异常。异常目录是:" + directory + "，状态码: " + exitCode);
			}
		} catch (IOException | InterruptedException e) {
			logger.log(Level.SEVERE, "执行命令时发生异常", e);
		}
	}

	private static String sanitizeFileName(String inputString) {
		if (inputString == null || inputString.isEmpty()) {
			return inputString;
		}
		// 使用正则表达式替换非法字符
		return inputString.replaceAll(INVALID_CHARACTERS_REGEX, "-");
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
			String title;
			if (jsonObject.containsKey("title")) {
				title = jsonObject.getString("title");
			} else {
				logger.info("异常：解析标题异常："+entryJson);
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
				logger.info("异常：解析文件名异常："+entryJson);
				part = "未知文件名";
			}
			outProperties.put("part", part);

			return outProperties;
		} catch (IOException e) {
			logger.log(Level.SEVERE, "读取 JSON 文件时发生异常", e);
		}
		return outProperties;
	}

	private static Logger setupLogger(boolean appendLogs) {
		try {
			// 获取日志记录器
			Logger logger = Logger.getLogger("LoggingToFile");

			// 设置日志级别
			logger.setLevel(Level.ALL);

			// 创建文件处理器
			FileHandler fileHandler = new FileHandler("app.log", 20 * 1024 * 1024, 1, appendLogs);

			// 创建日志格式
			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);

			// 将文件处理器添加到日志记录器
			logger.addHandler(fileHandler);
			return logger;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Logger.getLogger("LoggingToFile");
	}
}
