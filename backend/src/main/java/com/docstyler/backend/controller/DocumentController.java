package com.docstyler.backend.controller;

import com.docstyler.backend.model.ProcessStatus;
import com.docstyler.backend.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentProcessingService processingService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("DocStyler Backend работает! Версия 1.0.0 (без MongoDB)");
    }

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessStatus> processDocuments(
            @RequestParam("template") MultipartFile template,
            @RequestParam("draft") MultipartFile draft,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String user = userId != null ? userId : "anonymous";
        String processId = processingService.startDocumentProcessing(user, template, draft);
        ProcessStatus status = processingService.getProcessStatus(processId);

        return ResponseEntity.accepted().body(status);
    }

    @GetMapping("/status/{processId}")
    public ResponseEntity<ProcessStatus> getStatus(@PathVariable String processId) {
        ProcessStatus status = processingService.getProcessStatus(processId);

        if ("PROCESSING".equals(status.getStatus())) {
            List<String> funnyMessages = Arrays.asList(
                    "УткаИИ усердно перелопачивает ваш текст...",
                    "Ищем смысл между строк...",
                    "Нейросеть пьёт кофе и думает...",
                    "Ускоряем мыслительный процесс...",
                    "Магия стилизации в процессе...",
                    "Ищем потерянные запятые...",
                    "Добавляем немного волшебства..."
            );

            int index = (int) (System.currentTimeMillis() / 5000) % funnyMessages.size();
            status.setMessage(funnyMessages.get(index));
        }

        return ResponseEntity.ok(status);
    }

    @GetMapping("/result/{processId}")
    public ResponseEntity<Resource> getResult(@PathVariable String processId) {
        try {
            byte[] fileContent = processingService.getProcessResult(processId);

            ByteArrayResource resource = new ByteArrayResource(fileContent);

            // Определяем имя файла
            String filename = "styled_document.txt";
            ProcessStatus status = processingService.getProcessStatus(processId);
            if (status.getResultFilePath() != null) {
                String resultPath = status.getResultFilePath();
                filename = resultPath.substring(resultPath.lastIndexOf("/") + 1);
                if (filename.isEmpty()) filename = "result.txt";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileContent.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(404)
                    .body(new ByteArrayResource(("Error: " + e.getMessage()).getBytes()));
        }
    }

    @GetMapping("/test-python")
    public ResponseEntity<String> testPythonAgent() {
        String result = processingService.testPythonAgent();
        return ResponseEntity.ok("Результат теста Python агента:\n" + result);
    }

    @PostMapping("/test-upload")
    public ResponseEntity<String> testUpload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(String.format(
                "Файл получен: %s, размер: %d байт, тип: %s",
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
        ));
    }

    @GetMapping("/test-simple-process")
    public ResponseEntity<String> testSimpleProcess() {
        try {
            // Простой тест без реальных файлов
            return ResponseEntity.ok("Все endpoint'ы работают!\n\n" +
                    "1. GET /api/documents/health - проверка работы\n" +
                    "2. GET /api/documents/test-python - тест Python\n" +
                    "3. POST /api/documents/process - обработка документов\n" +
                    "4. GET /api/documents/status/{id} - статус обработки\n" +
                    "5. GET /api/documents/result/{id} - скачивание результата");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}