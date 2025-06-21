package com.alex.paper.Service;

import com.alex.paper.DTO.CrawlerRequest;
import com.alex.paper.Model.Paper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;

@Service
public class PythonCrawlerService {
    
    private static final Logger logger = LoggerFactory.getLogger(PythonCrawlerService.class);
    
    @Value("${python.crawler.url:http://localhost:5000}")
    private String pythonCrawlerUrl;
    
    @Value("${python.crawler.timeout:30000}")
    private int timeout;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public PythonCrawlerService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 使用 Python 爬蟲服務爬取論文
     */
    public List<Paper> crawlWithPython(CrawlerRequest request) {
        try {
            logger.info("開始使用 Python 爬蟲服務爬取論文: {}", request.getKeyword());
            
            // 準備請求資料
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("keyword", request.getKeyword());
            requestBody.put("maxResults", request.getMaxResults());
            requestBody.put("yearFrom", request.getYearFrom());
            requestBody.put("yearTo", request.getYearTo());
            
            // 設定請求標頭
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 發送請求到 Python 微服務
            ResponseEntity<Map> response = restTemplate.postForEntity(
                pythonCrawlerUrl + "/crawl",
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    List<Map<String, Object>> papersData = (List<Map<String, Object>>) responseBody.get("papers");
                    List<Paper> papers = convertToPapers(papersData);
                    
                    logger.info("Python 爬蟲服務成功爬取 {} 篇論文", papers.size());
                    return papers;
                } else {
                    String error = (String) responseBody.get("error");
                    logger.error("Python 爬蟲服務返回錯誤: {}", error);
                    throw new RuntimeException("Python 爬蟲服務錯誤: " + error);
                }
            } else {
                logger.error("Python 爬蟲服務請求失敗，狀態碼: {}", response.getStatusCode());
                throw new RuntimeException("Python 爬蟲服務請求失敗");
            }
            
        } catch (ResourceAccessException e) {
            logger.error("無法連接到 Python 爬蟲服務: {}", e.getMessage());
            throw new RuntimeException("Python 爬蟲服務無法連接", e);
        } catch (Exception e) {
            logger.error("Python 爬蟲服務調用失敗: {}", e.getMessage());
            throw new RuntimeException("Python 爬蟲服務調用失敗", e);
        }
    }
    
    /**
     * 批量爬取多個關鍵字
     */
    public List<Paper> crawlBatchWithPython(List<String> keywords, int maxResultsPerKeyword) {
        try {
            logger.info("開始批量爬取關鍵字: {}", keywords);
            
            // 準備請求資料
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("keywords", keywords);
            requestBody.put("maxResultsPerKeyword", maxResultsPerKeyword);
            
            // 設定請求標頭
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 發送請求到 Python 微服務
            ResponseEntity<Map> response = restTemplate.postForEntity(
                pythonCrawlerUrl + "/crawl/batch",
                entity,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    List<Map<String, Object>> papersData = (List<Map<String, Object>>) responseBody.get("papers");
                    List<Paper> papers = convertToPapers(papersData);
                    
                    logger.info("Python 爬蟲服務成功批量爬取 {} 篇論文", papers.size());
                    return papers;
                } else {
                    String error = (String) responseBody.get("error");
                    logger.error("Python 爬蟲服務批量爬取返回錯誤: {}", error);
                    throw new RuntimeException("Python 爬蟲服務批量爬取錯誤: " + error);
                }
            } else {
                logger.error("Python 爬蟲服務批量爬取請求失敗，狀態碼: {}", response.getStatusCode());
                throw new RuntimeException("Python 爬蟲服務批量爬取請求失敗");
            }
            
        } catch (Exception e) {
            logger.error("Python 爬蟲服務批量爬取失敗: {}", e.getMessage());
            throw new RuntimeException("Python 爬蟲服務批量爬取失敗", e);
        }
    }
    
    /**
     * 檢查 Python 爬蟲服務健康狀態
     */
    public boolean isHealthy() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                pythonCrawlerUrl + "/health",
                Map.class
            );
            
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.warn("Python 爬蟲服務健康檢查失敗: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 將 Python 服務返回的資料轉換為 Paper 物件
     */
    private List<Paper> convertToPapers(List<Map<String, Object>> papersData) {
        List<Paper> papers = new ArrayList<>();
        
        for (Map<String, Object> paperData : papersData) {
            try {
                Paper paper = new Paper();
                
                // 設定基本資訊
                paper.setTitle((String) paperData.get("title"));
                paper.setAuthor((String) paperData.get("author"));
                paper.setJournal((String) paperData.get("journal"));
                paper.setAbstractText((String) paperData.get("abstractText"));
                
                // 設定年份
                Object yearObj = paperData.get("year");
                if (yearObj != null) {
                    if (yearObj instanceof Integer) {
                        paper.setYear((Integer) yearObj);
                    } else if (yearObj instanceof String) {
                        try {
                            paper.setYear(Integer.parseInt((String) yearObj));
                        } catch (NumberFormatException e) {
                            logger.warn("無法解析年份: {}", yearObj);
                        }
                    }
                }
                
                // 設定引用數（如果 Paper 類別有這個欄位）
                Object citationsObj = paperData.get("citations");
                if (citationsObj != null && citationsObj instanceof Integer) {
                    // 如果 Paper 類別有 citations 欄位，可以在這裡設定
                    // paper.setCitations((Integer) citationsObj);
                }
                
                // 設定 PDF URL（如果 Paper 類別有這個欄位）
                String pdfUrl = (String) paperData.get("pdfUrl");
                if (pdfUrl != null && !pdfUrl.isEmpty()) {
                    // 如果 Paper 類別有 pdfUrl 欄位，可以在這裡設定
                    // paper.setPdfUrl(pdfUrl);
                }
                
                papers.add(paper);
                
            } catch (Exception e) {
                logger.error("轉換論文資料失敗: {}", e.getMessage());
            }
        }
        
        return papers;
    }
} 