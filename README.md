# Spring Boot Paper Management
## Purpose
This is a Spring Boot-based academic paper management system, using MVC architecture and Controller-Service-DAO pattern. The system integrates Google Scholar crawler through a Python microservice, which can automatically search, extract and manage academic papers.

## Main Features
### Thesis Crawling Function
- **Google Scholar auto-crawler**: Support keyword search and paper information extraction through Python microservice.
- **Intelligent Year Filtering**: Filter papers according to a specified year range.
- **Duplicate Paper Detection**: Filter existing papers automatically(auto-check from your database).
- **Batch Crawling**: Support crawling multiple keywords simultaneously.

### Thesis Management Features
- **Complete paper information**: title, author, journal, publication year, abstract, citations, PDF URL.
- **Database management**: Complete CRUD operation.
- **Modernized interface**: Responsive design, supports various devices.

### Microservice Architecture
- **Python Crawler Microservice**: Independent Flask-based crawler service with BeautifulSoup.
- **Spring Boot Application**: Main application handling business logic and data management.
- **Service Communication**: RESTful API communication between services.

### Containerized Deployment
- **Docker Compose**: One-click deployment with consistent environment.
- **MySQL Database**: Persistent Data Storage (free to utilize other databases).
- **Multi-container Architecture**: Separate containers for each service.

## Architecture
```plaintext
paper-management-system
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.alex.paper
│   │   │       ├── SpringbootPaperApplication.java      // Spring Boot 主程式
│   │   │       ├── Controller
│   │   │       │   ├── PaperController.java             // 論文管理控制器
│   │   │       │   └── CrawlerController.java           // 爬蟲控制器
│   │   │       ├── Service
│   │   │       │   ├── PaperService.java                // 論文服務介面
│   │   │       │   ├── PaperServiceImpl.java            // 論文服務實作
│   │   │       │   ├── CrawlerService.java              // 爬蟲服務介面
│   │   │       │   └── PythonCrawlerService.java        // Python 爬蟲服務整合
│   │   │       ├── Dao
│   │   │       │   ├── PaperDao.java                    // DAO 介面
│   │   │       │   └── PaperDaoImpl.java                // DAO 實作 (Spring JDBC)
│   │   │       ├── Model
│   │   │       │   └── Paper.java                       // 論文資料模型
│   │   │       ├── DTO
│   │   │       │   └── CrawlerRequest.java              // 爬蟲請求 DTO
│   │   │       └── Crawler
│   │   │           └── GoogleScholarCrawler.java        // Java 爬蟲備用方案
│   │   └── resources
│   │       ├── application.properties                   // 配置檔
│   │       ├── schema.sql                               // 資料表建立腳本
│   │       └── static
│   │           ├── index.html                           // 主頁面
│   │           └── crawler.html                         // 爬蟲介面
├── python-crawler/                                      // Python 爬蟲微服務
│   ├── app.py                                           // Flask 應用程式
│   ├── requirements.txt                                 // Python 依賴
│   ├── Dockerfile                                       // Python 服務 Docker 建置檔
│   └── .dockerignore                                    // Docker 忽略檔案
├── Dockerfile                                           // Spring Boot Docker 建置檔
├── docker-compose.yml                                   // Docker Compose 配置
└── pom.xml
```

## System Architecture Diagram
```plaintext
┌─────────────────┐    HTTP/REST    ┌─────────────────┐
│   Spring Boot   │ ◄──────────────► │  Python Crawler │
│   Application   │                 │   Microservice  │
│   (Port 8080)   │                 │   (Port 5000)   │
└─────────────────┘                 └─────────────────┘
         │                                    │
         │                                    │
         ▼                                    ▼
┌─────────────────┐                 ┌─────────────────┐
│   MySQL DB      │                 │  BeautifulSoup  │
│   (Port 3306)   │                 │  + Requests     │
└─────────────────┘                 └─────────────────┘
```

## Configuration
- **Backend Language**: Java (Spring Boot)
- **Crawler Language**: Python (Flask + BeautifulSoup)
- **Database**: MySQL
- **Containerization**: Docker + Docker Compose

### DB structure
```sql
CREATE TABLE paper (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,           -- 論文標題
    author VARCHAR(500) NOT NULL,          -- 作者
    journal VARCHAR(500),                  -- 期刊名稱
    year INT,                              -- 出版年份
    abstractText TEXT                      -- 摘要
);
```

## Usage
1. **Clone the repo**: ```git clone https://github.com/Alles1212/Spring_boot_paper_management_bs.git```
2. **Start services**: ```docker-compose up --build```
3. **Access the system**: Navigate to ```http://localhost:8080```
4. **Check MySQL**: ```docker exec -it paper-management-mysql mysql -uroot -proot paper_management``` then ```show tables;``` (whether table named `paper` exist) then ```select * from paper``` (if no data in it, will be empty)

## Service Endpoints
### Spring Boot Application (Port 8080)
- `GET /` - Main application interface
- `GET /api/papers` - Get all papers
- `POST /api/papers` - Add new paper
- `PUT /api/papers/{id}` - Update paper
- `DELETE /api/papers/{id}` - Delete paper
- `POST /api/crawler/crawl` - Crawl papers using Python service

### Python Crawler Microservice (Port 5000)
- `GET /health` - Health check
- `POST /crawl` - Crawl papers by keyword
- `POST /crawl/batch` - Batch crawl multiple keywords

## Notice
### Law
- Follow Google Scholar usage terms
- Merely for academic research purposes

### Limit
- Google Scholar may adjust its architecture
- Avoid excessive crawling to prevent rate limiting

## Prerequisites
- Docker
- Docker Compose

## Current Status
- Python microservice crawler is fully functional
- Java crawler implementation is available as backup
- Selenium ChromeDriver integration is in progress

## Future Outlook
- Support for more academic libraries
- Precise year extraction improvements
- Advanced keyword search capabilities
- CSV export functionality (however due to paper management, filtering by yourself might work well according to your research topic into the database)
- Enhanced duplicate detection algorithms

## Contact Me
Email: alleszhe1212@gmail.com
LinkedIn: linkedin.com/in/竣哲陳alex