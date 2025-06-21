package com.alex.paper.Controller;

import com.alex.paper.DTO.CrawlerRequest;
import com.alex.paper.Model.Paper;
import com.alex.paper.Service.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/crawler")
@CrossOrigin(origins = "*")
public class CrawlerController {

    @Autowired
    private CrawlerService crawlerService;

    @PostMapping("/crawl")
    public ResponseEntity<Map<String, Object>> crawlPapers(@RequestBody CrawlerRequest request) {
        try {
            List<Paper> papers = crawlerService.crawlGoogleScholar(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("papers", papers);
            response.put("count", papers.size());
            response.put("keyword", request.getKeyword());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/crawl-and-save")
    public ResponseEntity<Map<String, Object>> crawlAndSavePapers(@RequestBody CrawlerRequest request) {
        try {
            List<Paper> savedPapers = crawlerService.crawlAndSave(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("papers", savedPapers);
            response.put("count", savedPapers.size());
            response.put("keyword", request.getKeyword());
            response.put("message", "論文已成功爬取並儲存到資料庫");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/crawl-batch")
    public ResponseEntity<Map<String, Object>> crawlBatchPapers(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> keywords = (List<String>) request.get("keywords");
            Integer maxResultsPerKeyword = (Integer) request.get("maxResultsPerKeyword");
            
            if (keywords == null || keywords.isEmpty()) {
                throw new IllegalArgumentException("關鍵字列表不能為空");
            }
            
            if (maxResultsPerKeyword == null) {
                maxResultsPerKeyword = 5; // 預設值
            }
            
            List<Paper> papers = crawlerService.crawlBatchGoogleScholar(keywords, maxResultsPerKeyword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("papers", papers);
            response.put("count", papers.size());
            response.put("keywords", keywords);
            response.put("maxResultsPerKeyword", maxResultsPerKeyword);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/crawl-batch-and-save")
    public ResponseEntity<Map<String, Object>> crawlBatchAndSavePapers(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> keywords = (List<String>) request.get("keywords");
            Integer maxResultsPerKeyword = (Integer) request.get("maxResultsPerKeyword");
            
            if (keywords == null || keywords.isEmpty()) {
                throw new IllegalArgumentException("關鍵字列表不能為空");
            }
            
            if (maxResultsPerKeyword == null) {
                maxResultsPerKeyword = 5; // 預設值
            }
            
            List<Paper> savedPapers = crawlerService.crawlBatchAndSave(keywords, maxResultsPerKeyword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("papers", savedPapers);
            response.put("count", savedPapers.size());
            response.put("keywords", keywords);
            response.put("maxResultsPerKeyword", maxResultsPerKeyword);
            response.put("message", "批量論文已成功爬取並儲存到資料庫");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/save-selected")
    public ResponseEntity<Map<String, Object>> saveSelectedPapers(@RequestBody List<Paper> papers) {
        try {
            List<Paper> savedPapers = crawlerService.saveSelectedPapers(papers);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("papers", savedPapers);
            response.put("count", savedPapers.size());
            response.put("message", "選定的論文已成功儲存到資料庫");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        try {
            boolean pythonCrawlerHealthy = crawlerService.isPythonCrawlerHealthy();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("pythonCrawlerHealthy", pythonCrawlerHealthy);
            response.put("message", "爬蟲服務健康檢查完成");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
} 