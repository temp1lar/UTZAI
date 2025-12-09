package com.docstyler.backend.service;

import com.docstyler.backend.model.ProcessStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class DocumentProcessingService {

    // Храним статусы в памяти вместо MongoDB
    private final Map<String, ProcessStatus> statusCache = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Value("${python.agent.path:../python_agent}")
    private String pythonAgentPath;

    @Value("${python.executable:python3}")
    private String pythonExecutable;

    @Value("${temp.directory:/tmp/docstyler}")
    private String tempDirectory;

    public String startDocumentProcessing(String userId,
                                          MultipartFile template,
                                          MultipartFile draft) {

        String processId = UUID.randomUUID().toString();
        ProcessStatus status = new ProcessStatus(processId, userId);

        // Сохраняем в памяти
        statusCache.put(processId, status);

        log.info("🚀 Starting document processing: {}", processId);
        log.info("   User: {}, Template: {}, Draft: {}",
                userId, template.getOriginalFilename(), draft.getOriginalFilename());

        // Запускаем обработку асинхронно
        executorService.submit(() -> {
            try {
                processDocumentsAsync(processId, template, draft);
            } catch (Exception e) {
                log.error("❌ Error processing documents", e);
                status.setStatus("ERROR");
                status.setMessage("Ошибка: " + e.getMessage());
            }
        });

        return processId;
    }

    private void processDocumentsAsync(String processId,
                                       MultipartFile template,
                                       MultipartFile draft) {

        ProcessStatus status = statusCache.get(processId);

        try {
            // 1. Создаем временную директорию
            Path tempDir = Paths.get(tempDirectory, processId);
            Files.createDirectories(tempDir);

            // 2. Сохраняем файлы
            String templateName = template.getOriginalFilename();
            String draftName = draft.getOriginalFilename();

            if (templateName == null || templateName.isEmpty()) templateName = "template.txt";
            if (draftName == null || draftName.isEmpty()) draftName = "draft.txt";

            Path templatePath = tempDir.resolve(templateName);
            Path draftPath = tempDir.resolve(draftName);

            template.transferTo(templatePath);
            draft.transferTo(draftPath);

            // 3. Обновляем статус
            status.setProgress(10);
            status.setMessage("📁 Файлы сохранены, запускаем Python агент...");

            // 4. Находим Python агента
            File agentFile = findPythonAgentFile();
            log.info("✅ Python agent found at: {}", agentFile.getAbsolutePath());

            // 5. Запускаем Python агента
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable,
                    agentFile.getAbsolutePath(),
                    templatePath.toString(),
                    draftPath.toString(),
                    tempDir.toString()
            );

            Process process = pb.start();

            // 6. Читаем вывод Python в реальном времени
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Python: {}", line);

                    if (line.startsWith("PROGRESS:")) {
                        int progress = Integer.parseInt(line.split(":")[1]);
                        status.setProgress(progress);
                    } else if (line.startsWith("MESSAGE:")) {
                        status.setMessage(line.substring(8));
                    } else if (line.startsWith("RESULT:")) {
                        String resultPath = line.substring(7);
                        status.setResultFilePath(resultPath);
                        status.setStatus("COMPLETED");
                        status.setProgress(100);
                        status.setEndTime(java.time.LocalDateTime.now());
                        status.setMessage("✅ Обработка завершена!");
                    }
                }
            }

            // 7. Проверяем код завершения
            int exitCode = process.waitFor();

            if (exitCode != 0 && !"COMPLETED".equals(status.getStatus())) {
                status.setStatus("ERROR");
                status.setMessage("❌ Python агент завершился с ошибкой: " + exitCode);
            } else if ("COMPLETED".equals(status.getStatus())) {
                // Уже обновлено в цикле чтения
            } else {
                status.setStatus("COMPLETED");
                status.setProgress(100);
                status.setMessage("✅ Обработка завершена успешно!");
                status.setEndTime(java.time.LocalDateTime.now());
            }

            log.info("📊 Processing complete for {}: {}", processId, status.getStatus());

        } catch (Exception e) {
            log.error("❌ Processing error for {}", processId, e);
            status.setStatus("ERROR");
            status.setMessage("❌ Ошибка: " + e.getMessage());
        }
    }

    private File findPythonAgentFile() {
        // 1. Сначала пробуем путь из конфигурации
        File agentFile = new File(pythonAgentPath, "main.py");

        // 2. Если файл существует - используем его
        if (agentFile.exists()) {
            return agentFile;
        }

        // 3. Если это относительный путь, вычисляем абсолютный
        if (!agentFile.isAbsolute()) {
            // Получаем текущую директорию Spring Boot
            File backendDir = new File(".").getAbsoluteFile(); // backend/

            // Пробуем разные варианты
            File[] candidates = {
                    new File(backendDir, pythonAgentPath + File.separator + "main.py"),
                    new File(backendDir.getParentFile(), "python_agent/main.py"),
                    new File("../python_agent/main.py"),
                    new File("../../python_agent/main.py"),
                    new File("D:/Programming/UTZAI/python_agent/main.py")
            };

            for (File candidate : candidates) {
                log.debug("Checking path: {}", candidate.getAbsolutePath());
                if (candidate.exists()) {
                    log.info("✅ Found Python agent at: {}", candidate.getAbsolutePath());
                    return candidate;
                }
            }
        }

        // 4. Если ничего не нашли, бросаем исключение
        throw new RuntimeException("❌ Python agent not found. Searched paths:\n" +
                "1. " + agentFile.getAbsolutePath() + "\n" +
                "2. From backend dir: " + new File(".").getAbsolutePath());
    }

    public ProcessStatus getProcessStatus(String processId) {
        ProcessStatus status = statusCache.get(processId);
        if (status == null) {
            throw new RuntimeException("❌ Process not found: " + processId);
        }
        return status;
    }

    public byte[] getProcessResult(String processId) throws Exception {
        ProcessStatus status = getProcessStatus(processId);

        if (!"COMPLETED".equals(status.getStatus())) {
            throw new RuntimeException("❌ Process not completed yet. Status: " + status.getStatus());
        }

        if (status.getResultFilePath() == null) {
            throw new RuntimeException("❌ No result file found");
        }

        Path resultPath = Paths.get(status.getResultFilePath());
        if (!Files.exists(resultPath)) {
            throw new RuntimeException("❌ Result file not found: " + resultPath);
        }

        return Files.readAllBytes(resultPath);
    }

    public String testPythonAgent() {
        try {
            System.out.println("=== PYTHON AGENT DEBUG ===");

            // 1. Показываем текущую директорию Spring Boot
            File currentDir = new File(".").getAbsoluteFile();
            System.out.println("1. Spring Boot current dir: " + currentDir);

            // 2. Показываем конфигурационные значения
            System.out.println("2. Configuration:");
            System.out.println("   pythonAgentPath: " + pythonAgentPath);
            System.out.println("   pythonExecutable: " + pythonExecutable);

            // 3. Пробуем разные пути к агенту
            System.out.println("3. Trying different paths:");

            File[] possibleAgentFiles = {
                    // Относительный путь из конфигурации
                    new File(pythonAgentPath, "main.py"),
                    // Относительно backend папки
                    new File("../python_agent/main.py"),
                    // Абсолютный путь (для отладки)
                    new File("D:/Programming/UTZAI/python_agent/main.py"),
                    // Через parent directory
                    new File(currentDir.getParentFile(), "python_agent/main.py")
            };

            File agentFile = null;
            for (File file : possibleAgentFiles) {
                System.out.println("   - " + file.getAbsolutePath() + ": " +
                        (file.exists() ? "✅ FOUND" : "❌ NOT FOUND"));
                if (file.exists() && agentFile == null) {
                    agentFile = file;
                }
            }

            if (agentFile == null) {
                return "❌ Python agent not found in any location!";
            }

            System.out.println("4. Using agent file: " + agentFile.getAbsolutePath());

            // 4. Пробуем разные python команды
            System.out.println("5. Testing Python commands:");
            String[] pythonCommands = {"python", "python3", "py"};
            String pythonCmd = null;

            for (String cmd : pythonCommands) {
                try {
                    Process p = new ProcessBuilder(cmd, "--version").start();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    String version = reader.readLine();
                    p.waitFor();
                    System.out.println("   - " + cmd + ": " + (version != null ? version : "found"));
                    if (pythonCmd == null) {
                        pythonCmd = cmd;
                    }
                } catch (Exception e) {
                    System.out.println("   - " + cmd + ": ❌ " + e.getMessage());
                }
            }

            if (pythonCmd == null) {
                return "No Python command found! Tried: python, python3, py";
            }

            System.out.println("6. Using Python command: " + pythonCmd);

            // 5. Запускаем Python агента
            ProcessBuilder pb = new ProcessBuilder(
                    pythonCmd,
                    agentFile.getAbsolutePath(),
                    "--test"
            );

            System.out.println("7. Command: " + String.join(" ", pb.command()));

            Process process = pb.start();

            // 6. Читаем вывод
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("Python> " + line);
                }
            }

            // 7. Читаем ошибки если есть
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    System.out.println("Python ERROR> " + line);
                    output.append("ERROR: ").append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            System.out.println("8. Exit code: " + exitCode);

            return "Exit code: " + exitCode + "\n\n" + output.toString();

        } catch (Exception e) {
            System.err.println("Error in testPythonAgent: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}