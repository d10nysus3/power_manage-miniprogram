package com.powermanage.controller;

import com.powermanage.entity.ElectricityData;
import com.powermanage.entity.User;
import com.powermanage.service.ElectricityDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Font;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.pdf.PdfWriter;

@RestController
@RequestMapping("/api/electricity-data")
public class ElectricityDataController {

    @Autowired
    private ElectricityDataService electricityDataService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    
    @PostMapping
    public ResponseEntity<ElectricityData> saveElectricityData(@RequestBody ElectricityData electricityData) {
        try {
            
            if (electricityData.getUser() == null && electricityData.getUserId() != null) {
                
                User user = new User();
                user.setId(electricityData.getUserId());
                electricityData.setUser(user);
            }
            
            
            if (electricityData.getUser() == null) {
                User defaultUser = new User();
                defaultUser.setId(1L); 
                electricityData.setUser(defaultUser);
            }
            
            ElectricityData savedData = electricityDataService.saveElectricityData(electricityData);
            return new ResponseEntity<>(savedData, HttpStatus.CREATED);
        } catch (Exception e) {
            
            System.err.println("保存用电数据失败: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
    @PostMapping("/batch")
    public ResponseEntity<List<ElectricityData>> batchSaveElectricityData(@RequestBody List<ElectricityData> electricityDataList) {
        List<ElectricityData> savedDataList = electricityDataService.batchSaveElectricityData(electricityDataList);
        return new ResponseEntity<>(savedDataList, HttpStatus.CREATED);
    }

    
    @GetMapping("/user/{userId}")
    public List<ElectricityData> getElectricityDataByUserId(@PathVariable Long userId) {
        return electricityDataService.getElectricityDataByUserId(userId);
    }

    
    @GetMapping("/time-range")
    public ResponseEntity<List<ElectricityData>> getElectricityDataByTimeRange(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
            
            List<ElectricityData> dataList = electricityDataService.getElectricityDataByTimeRange(start, end);
            return ResponseEntity.ok(dataList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    
    @GetMapping("/user/{userId}/time-range")
    public ResponseEntity<List<ElectricityData>> getElectricityDataByUserIdAndTimeRange(
            @PathVariable Long userId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
            
            List<ElectricityData> dataList = electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, start, end);
            return ResponseEntity.ok(dataList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    
    @GetMapping("/user/{userId}/recent/{days}")
    public List<ElectricityData> getRecentElectricityDataByUserId(@PathVariable Long userId, @PathVariable int days) {
        return electricityDataService.getRecentElectricityDataByUserId(userId, days);
    }

    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllElectricityData(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String community,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            // 获取所有用电数据
            List<ElectricityData> allData = electricityDataService.getAllElectricityData();
            System.out.println("获取到的原始数据总数: " + allData.size());
            System.out.println("社区筛选参数: " + (community != null ? community : "无"));
            
            // 过滤数据
            List<ElectricityData> filteredData = allData.stream()
                    .filter(data -> {
                        // 社区筛选
                        if (community != null && !community.isEmpty()) {
                            if (data.getUser() == null || data.getUser().getCommunity() == null) {
                                return false;
                            }
                            if (!community.equals(data.getUser().getCommunity())) {
                                return false;
                            }
                        }
                        
                        // 关键字搜索
                        if (keyword == null || keyword.isEmpty()) {
                            return true;
                        }
                        String lowerKeyword = keyword.toLowerCase();
                        return (data.getId() != null && String.valueOf(data.getId()).toLowerCase().contains(lowerKeyword)) ||
                               (data.getUserId() != null && String.valueOf(data.getUserId()).toLowerCase().contains(lowerKeyword)) ||
                               (data.getElectricityConsumption() != null && String.valueOf(data.getElectricityConsumption()).toLowerCase().contains(lowerKeyword)) ||
                               (data.getPower() != null && String.valueOf(data.getPower()).toLowerCase().contains(lowerKeyword)) ||
                               (data.getRecordTime() != null && data.getRecordTime().toString().toLowerCase().contains(lowerKeyword)) ||
                               (data.getUser() != null && data.getUser().getName() != null && data.getUser().getName().toLowerCase().contains(lowerKeyword));
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            System.out.println("过滤后的数据总数: " + filteredData.size());
            
            // 计算分页
            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, filteredData.size());
            List<ElectricityData> pageData;
            if (startIndex >= filteredData.size()) {
                pageData = new ArrayList<>();
            } else {
                pageData = filteredData.subList(startIndex, endIndex);
            }
            
            System.out.println("当前页数据数: " + pageData.size());
            
            // 构建标准格式的返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("records", pageData);
            response.put("total", filteredData.size());
            response.put("page", page);
            response.put("pageSize", pageSize);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("获取用电数据失败: " + e.getMessage());
            e.printStackTrace();
            
            // 返回错误情况下的标准格式响应
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("records", new ArrayList<>());
            errorResponse.put("total", 0);
            errorResponse.put("page", page);
            errorResponse.put("pageSize", pageSize);
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<ElectricityData> getElectricityDataById(@PathVariable Long id) {
        ElectricityData electricityData = electricityDataService.getElectricityDataById(id);
        return electricityData != null ? ResponseEntity.ok(electricityData) : ResponseEntity.notFound().build();
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<ElectricityData> updateElectricityData(@PathVariable Long id, @RequestBody ElectricityData electricityData) {
        electricityData.setId(id);
        ElectricityData updatedData = electricityDataService.updateElectricityData(electricityData);
        return ResponseEntity.ok(updatedData);
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteElectricityData(@PathVariable Long id) {
        electricityDataService.deleteElectricityData(id);
        return ResponseEntity.noContent().build();
    }

    
    @GetMapping("/statistics")
    public ResponseEntity<Object> getElectricityStatistics(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String period) {
        try {
            List<ElectricityData> dataList;
            
            
            if (userId != null && startTime != null && endTime != null) {
                LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
                LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
                dataList = electricityDataService.getElectricityDataByUserIdAndTimeRange(userId, start, end);
            } else if (userId != null) {
                
                int days = parsePeriodToDays(period);
                dataList = electricityDataService.getRecentElectricityDataByUserId(userId, days);
            } else if (startTime != null && endTime != null) {
                LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
                LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
                dataList = electricityDataService.getElectricityDataByTimeRange(start, end);
            } else {
                
                int days = parsePeriodToDays(period);
                LocalDateTime startDate = LocalDateTime.now().minusDays(days);
                dataList = electricityDataService.getElectricityDataByTimeRange(startDate, LocalDateTime.now());
            }
            
            
            if (dataList.isEmpty()) {
                return ResponseEntity.ok(java.util.Map.of(
                    "months", new String[0],
                    "usageData", new Double[0],
                    "message", "暂无数据"
                ));
            }
            
            
            java.util.Map<String, Double> dailyStats = dataList.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    data -> data.getRecordTime().toLocalDate().toString(),
                    java.util.stream.Collectors.summingDouble(ElectricityData::getElectricityConsumption)
                ));
            
            
            java.util.List<String> sortedDates = dailyStats.keySet().stream()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            
            java.util.List<Double> sortedUsageData = sortedDates.stream()
                .map(dailyStats::get)
                .collect(java.util.stream.Collectors.toList());
            
            
            String[] monthsArray = sortedDates.toArray(new String[0]);
            Double[] usageDataArray = sortedUsageData.toArray(new Double[0]);
            
            return ResponseEntity.ok(java.util.Map.of(
                "months", monthsArray,
                "usageData", usageDataArray
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    
    private int parsePeriodToDays(String period) {
        if (period == null) return 30;
        try {
            if (period.endsWith("d")) {
                return Integer.parseInt(period.substring(0, period.length() - 1));
            } else if (period.endsWith("m")) {
                return Integer.parseInt(period.substring(0, period.length() - 1)) * 30;
            } else {
                return Integer.parseInt(period);
            }
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    
    @GetMapping("/export-report/{userId}")
    public ResponseEntity<byte[]> exportUserReport(@PathVariable Long userId) {
        try {
            byte[] reportData = electricityDataService.generateUserReport(userId);
            
            String filename = "electricity-report-user-" + userId + "-" + 
                java.time.LocalDate.now().toString() + ".pdf";
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(reportData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    @PostMapping("/generate-report")
    public ResponseEntity<Map<String, Object>> generateReport(@RequestBody java.util.Map<String, Object> reportParams) {
        try {
            Long userId = null;
            if (reportParams.containsKey("userId") && reportParams.get("userId") != null) {
                Object userIdObj = reportParams.get("userId");
                if (userIdObj instanceof String) {
                    String userIdStr = (String) userIdObj;
                    if (!userIdStr.isEmpty()) {
                        userId = Long.parseLong(userIdStr);
                    }
                } else if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                }
            }
            
            String startTime = (String) reportParams.get("startTime");
            String endTime = (String) reportParams.get("endTime");
            String reportType = (String) reportParams.getOrDefault("reportType", "usage_analysis");
            
            LocalDateTime start = startTime != null ? 
                LocalDateTime.parse(startTime, DATE_TIME_FORMATTER) : 
                LocalDateTime.now().minusDays(30);
            LocalDateTime end = endTime != null ? 
                LocalDateTime.parse(endTime, DATE_TIME_FORMATTER) : 
                LocalDateTime.now();
            
            String reportId = electricityDataService.generateReport(userId, start, end, reportType);
            
            
            String format = (String) reportParams.getOrDefault("format", "PDF");
            String reportUrl = "/api/electricity-data/download-report/" + reportId + "?format=" + format;
            
            Map<String, Object> result = new HashMap<>();
            result.put("reportId", reportId);
            result.put("reportUrl", reportUrl);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result,
                "message", "报告生成成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "报告生成失败: " + e.getMessage()
                ));
        }
    }

    
    @GetMapping("/download-report/{reportId}")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable String reportId, 
            @RequestParam(required = false, defaultValue = "TEXT") String format) {
        try {
            byte[] reportData = electricityDataService.downloadReport(reportId);
            
            String filename;
            org.springframework.http.MediaType mediaType;
            
            switch (format.toUpperCase()) {
                case "PDF":
                    filename = "用电规律分析报告_" + reportId + ".pdf";
                    mediaType = org.springframework.http.MediaType.APPLICATION_PDF;
                    
                    reportData = convertTextToPdf(reportData);
                    break;
                case "HTML":
                    filename = "用电规律分析报告_" + reportId + ".html";
                    mediaType = org.springframework.http.MediaType.TEXT_HTML;
                    
                    reportData = convertTextToHtml(reportData);
                    break;
                default:
                    filename = "用电规律分析报告_" + reportId + ".txt";
                    mediaType = org.springframework.http.MediaType.TEXT_PLAIN;
                    break;
            }
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(reportData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    private byte[] convertTextToPdf(byte[] textData) {
        try {
            String text = new String(textData, "UTF-8");
            
            
            com.itextpdf.text.Document document = new com.itextpdf.text.Document(
                com.itextpdf.text.PageSize.A4, 50, 50, 50, 50);
            java.io.ByteArrayOutputStream pdfOutput = new java.io.ByteArrayOutputStream();
            com.itextpdf.text.pdf.PdfWriter writer = com.itextpdf.text.pdf.PdfWriter.getInstance(document, pdfOutput);
            
            
            document.addTitle("用电规律分析报告");
            document.addSubject("电力管理系统用电分析报告");
            document.addAuthor("电力管理系统");
            document.addCreator("PowerManage System");
            
            
            writer.setPageEvent(new com.itextpdf.text.pdf.PdfPageEventHelper() {
                @Override
                public void onEndPage(com.itextpdf.text.pdf.PdfWriter writer, com.itextpdf.text.Document document) {
                    try {
                        
                        com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(
                            com.itextpdf.text.Font.FontFamily.HELVETICA, 8, 
                            com.itextpdf.text.Font.NORMAL, 
                            new com.itextpdf.text.BaseColor(128, 128, 128));
                        com.itextpdf.text.pdf.ColumnText.showTextAligned(
                            writer.getDirectContent(), 
                            com.itextpdf.text.Element.ALIGN_CENTER,
                            new com.itextpdf.text.Phrase("第 " + document.getPageNumber() + " 页", footerFont),
                            (document.right() + document.left()) / 2, 
                            document.bottom() - 10, 0);
                    } catch (Exception e) {
                        
                    }
                }
                
                @Override
                public void onStartPage(com.itextpdf.text.pdf.PdfWriter writer, com.itextpdf.text.Document document) {
                    try {
                        
                        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                            com.itextpdf.text.Font.FontFamily.HELVETICA, 10, 
                            com.itextpdf.text.Font.ITALIC, 
                            new com.itextpdf.text.BaseColor(128, 128, 128));
                        com.itextpdf.text.Paragraph header = new com.itextpdf.text.Paragraph(
                            "用电规律分析报告 - 电力管理系统", headerFont);
                        header.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                        document.add(header);
                        document.add(new com.itextpdf.text.Paragraph(" ")); 
                    } catch (Exception e) {
                        
                    }
                }
            });
            
            document.open();
            
            
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 18, 
                com.itextpdf.text.Font.BOLD, 
                new com.itextpdf.text.BaseColor(0, 51, 102));
            
            com.itextpdf.text.Font headingFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 14, 
                com.itextpdf.text.Font.BOLD, 
                new com.itextpdf.text.BaseColor(51, 51, 51));
            
            com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 12, 
                com.itextpdf.text.Font.NORMAL, 
                new com.itextpdf.text.BaseColor(0, 0, 0));
            
            com.itextpdf.text.Font highlightFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 12, 
                com.itextpdf.text.Font.BOLD, 
                new com.itextpdf.text.BaseColor(204, 0, 0));
            
            
            com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("用电规律分析报告", titleFont);
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            
            com.itextpdf.text.Font dateFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10, 
                com.itextpdf.text.Font.NORMAL, 
                new com.itextpdf.text.BaseColor(128, 128, 128));
            com.itextpdf.text.Paragraph date = new com.itextpdf.text.Paragraph(
                "生成时间: " + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), dateFont);
            date.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
            date.setSpacingAfter(20);
            document.add(date);
            
            
            com.itextpdf.text.pdf.draw.LineSeparator line = new com.itextpdf.text.pdf.draw.LineSeparator();
            line.setLineColor(new com.itextpdf.text.BaseColor(200, 200, 200));
            document.add(new com.itextpdf.text.Chunk(line));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            
            String[] lines = text.split("\n");
            boolean inSection = false;
            StringBuilder sectionContent = new StringBuilder();
            String currentSection = "";
            
            for (String lineContent : lines) {
                if (lineContent.startsWith("========================================")) {
                    
                    continue;
                } else if (lineContent.startsWith("用电规律分析报告")) {
                    
                    continue;
                } else if (lineContent.startsWith("--- ")) {
                    
                    if (inSection && sectionContent.length() > 0) {
                        addSectionContent(document, currentSection, sectionContent.toString());
                        sectionContent.setLength(0);
                    }
                    
                    
                    currentSection = lineContent.replace("--- ", "").replace(" ---", "");
                    com.itextpdf.text.Paragraph section = new com.itextpdf.text.Paragraph(currentSection, headingFont);
                    section.setSpacingBefore(15);
                    section.setSpacingAfter(10);
                    document.add(section);
                    
                    
                    com.itextpdf.text.pdf.draw.LineSeparator sectionLine = new com.itextpdf.text.pdf.draw.LineSeparator();
                    sectionLine.setLineColor(new com.itextpdf.text.BaseColor(220, 220, 220));
                    document.add(new com.itextpdf.text.Chunk(sectionLine));
                    
                    inSection = true;
                } else if (lineContent.startsWith("> ")) {
                    
                    String suggestion = lineContent.replace("> ", "");
                    com.itextpdf.text.Paragraph suggestionPara = new com.itextpdf.text.Paragraph("  • " + suggestion, highlightFont);
                    suggestionPara.setSpacingBefore(5);
                    suggestionPara.setIndentationLeft(20);
                    document.add(suggestionPara);
                } else if (lineContent.startsWith("- ")) {
                    
                    com.itextpdf.text.Paragraph listItem = new com.itextpdf.text.Paragraph(lineContent, normalFont);
                    listItem.setIndentationLeft(20);
                    listItem.setSpacingBefore(3);
                    document.add(listItem);
                } else if (lineContent.trim().isEmpty()) {
                    
                    document.add(new com.itextpdf.text.Paragraph(" "));
                } else {
                    
                    sectionContent.append(lineContent).append("\n");
                }
            }
            
            
            if (sectionContent.length() > 0) {
                addSectionContent(document, currentSection, sectionContent.toString());
            }
            
            
            addChartSection(document);
            
            
            document.add(new com.itextpdf.text.Paragraph(" "));
            document.add(new com.itextpdf.text.Paragraph(" "));
            com.itextpdf.text.pdf.draw.LineSeparator footerLine = new com.itextpdf.text.pdf.draw.LineSeparator();
            footerLine.setLineColor(new com.itextpdf.text.BaseColor(200, 200, 200));
            document.add(new com.itextpdf.text.Chunk(footerLine));
            
            com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10, 
                com.itextpdf.text.Font.ITALIC, 
                new com.itextpdf.text.BaseColor(128, 128, 128));
            
            com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph(
                "本报告由电力管理系统自动生成 | 生成时间: " + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), footerFont);
            footer.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            document.add(footer);
            
            document.close();
            
            return pdfOutput.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF转换失败", e);
        }
    }
    
    
    private void addSectionContent(com.itextpdf.text.Document document, String sectionTitle, String content) throws Exception {
        
        if ("核心指标".equals(sectionTitle)) {
            
            String[] lines = content.split("\n");
            com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(2);
            table.setWidthPercentage(80);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);
            
            
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10, 
                com.itextpdf.text.Font.BOLD, 
                new com.itextpdf.text.BaseColor(255, 255, 255));
            com.itextpdf.text.Font cellFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10, 
                com.itextpdf.text.Font.NORMAL, 
                new com.itextpdf.text.BaseColor(0, 0, 0));
            
            
            com.itextpdf.text.pdf.PdfPCell headerCell1 = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase("指标名称", headerFont));
            headerCell1.setBackgroundColor(new com.itextpdf.text.BaseColor(70, 130, 180));
            table.addCell(headerCell1);
            
            com.itextpdf.text.pdf.PdfPCell headerCell2 = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase("数值", headerFont));
            headerCell2.setBackgroundColor(new com.itextpdf.text.BaseColor(70, 130, 180));
            table.addCell(headerCell2);
            
            
            for (String line : lines) {
                if (line.trim().length() > 0 && line.contains(":")) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        table.addCell(new com.itextpdf.text.pdf.PdfPCell(
                            new com.itextpdf.text.Phrase(parts[0].trim(), cellFont)));
                        table.addCell(new com.itextpdf.text.pdf.PdfPCell(
                            new com.itextpdf.text.Phrase(parts[1].trim(), cellFont)));
                    }
                }
            }
            
            document.add(table);
        } else {
            
            com.itextpdf.text.Font contentFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10, 
                com.itextpdf.text.Font.NORMAL, 
                new com.itextpdf.text.BaseColor(0, 0, 0));
            com.itextpdf.text.Paragraph contentParagraph = new com.itextpdf.text.Paragraph(content, contentFont);
            contentParagraph.setSpacingBefore(5);
            contentParagraph.setSpacingAfter(10);
            document.add(contentParagraph);
        }
    }
    
    
    private void addChartSection(com.itextpdf.text.Document document) throws Exception {
        
        com.itextpdf.text.Font sectionFont = new com.itextpdf.text.Font(
            com.itextpdf.text.Font.FontFamily.HELVETICA, 14, 
            com.itextpdf.text.Font.BOLD, 
            new com.itextpdf.text.BaseColor(51, 51, 51));
        com.itextpdf.text.Paragraph section = new com.itextpdf.text.Paragraph("图表分析", sectionFont);
        section.setSpacingBefore(20);
        section.setSpacingAfter(10);
        document.add(section);
        
        
        com.itextpdf.text.pdf.draw.LineSeparator sectionLine = new com.itextpdf.text.pdf.draw.LineSeparator();
        sectionLine.setLineColor(new com.itextpdf.text.BaseColor(220, 220, 220));
        document.add(new com.itextpdf.text.Chunk(sectionLine));
        
        
        com.itextpdf.text.Font chartFont = new com.itextpdf.text.Font(
            com.itextpdf.text.Font.FontFamily.HELVETICA, 10, 
            com.itextpdf.text.Font.NORMAL, 
            new com.itextpdf.text.BaseColor(0, 0, 0));
        com.itextpdf.text.Paragraph chartDesc = new com.itextpdf.text.Paragraph(
            "本报告包含以下图表分析:", chartFont);
        chartDesc.setSpacingBefore(10);
        chartDesc.setSpacingAfter(10);
        document.add(chartDesc);
        
        
        com.itextpdf.text.Font listFont = new com.itextpdf.text.Font(
            com.itextpdf.text.Font.FontFamily.HELVETICA, 10, 
            com.itextpdf.text.Font.NORMAL, 
            new com.itextpdf.text.BaseColor(0, 0, 0));
        
        com.itextpdf.text.Paragraph chart1 = new com.itextpdf.text.Paragraph(
            "1. 每日用电量趋势图 - 展示分析周期内每日用电量变化趋势", listFont);
        chart1.setIndentationLeft(20);
        chart1.setSpacingBefore(5);
        chart1.setSpacingAfter(5);
        document.add(chart1);
        
        com.itextpdf.text.Paragraph chart2 = new com.itextpdf.text.Paragraph(
            "2. 24小时用电分布图 - 展示一天中各个时段的平均用电量占比", listFont);
        chart2.setIndentationLeft(20);
        chart2.setSpacingBefore(5);
        chart2.setSpacingAfter(5);
        document.add(chart2);
        
        com.itextpdf.text.Paragraph chart3 = new com.itextpdf.text.Paragraph(
            "3. 用电量对比图 - 展示环比和同比增长情况", listFont);
        chart3.setIndentationLeft(20);
        chart3.setSpacingBefore(5);
        chart3.setSpacingAfter(5);
        document.add(chart3);
        
        
        com.itextpdf.text.Font noteFont = new com.itextpdf.text.Font(
            com.itextpdf.text.Font.FontFamily.HELVETICA, 9, 
            com.itextpdf.text.Font.ITALIC, 
            new com.itextpdf.text.BaseColor(128, 128, 128));
        com.itextpdf.text.Paragraph note = new com.itextpdf.text.Paragraph(
            "注: 详细图表数据请登录系统查看", noteFont);
        note.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        note.setSpacingBefore(15);
        document.add(note);
    }
    
    
    private byte[] convertTextToHtml(byte[] textData) {
        try {
            String text = new String(textData, "UTF-8");
            
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html>\n<head>\n");
            html.append("<meta charset=\"UTF-8\">\n");
            html.append("<title>用电规律分析报告</title>\n");
            html.append("<style>\n");
            html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
            html.append("h1 { color: #333; }\n");
            html.append("pre { background-color: #f5f5f5; padding: 15px; border-radius: 5px; }\n");
            html.append("</style>\n");
            html.append("</head>\n<body>\n");
            html.append("<h1>用电规律分析报告</h1>\n");
            html.append("<pre>").append(text.replace("<", "&lt;").replace(">", "&gt;")).append("</pre>\n");
            html.append("</body>\n</html>");
            
            return html.toString().getBytes("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("HTML转换失败", e);
        }
    }
    
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportData(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false, defaultValue = "CSV") String format) {
        try {
            LocalDateTime start = startTime != null ? 
                LocalDateTime.parse(startTime, DATE_TIME_FORMATTER) : null;
            LocalDateTime end = endTime != null ? 
                LocalDateTime.parse(endTime, DATE_TIME_FORMATTER) : null;
            
            byte[] exportData = electricityDataService.exportData(userId, start, end, format);
            
            String filename = "electricity-data-";
            if (userId != null) {
                filename += "user-" + userId + "-";
            }
            filename += java.time.LocalDate.now().toString() + "." + format.toLowerCase();
            
            org.springframework.http.MediaType mediaType;
            if ("CSV".equalsIgnoreCase(format)) {
                mediaType = new org.springframework.http.MediaType("text", "csv");
            } else if ("EXCEL".equalsIgnoreCase(format)) {
                mediaType = new org.springframework.http.MediaType("application", 
                    "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else {
                mediaType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
            }
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(exportData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    

    @GetMapping("/export-analysis")
    public ResponseEntity<byte[]> exportAnalysisData(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false, defaultValue = "CSV") String format) {
        try {
            byte[] exportData = electricityDataService.exportAnalysisData(userId, period, format);
            
            String filename = "electricity-analysis-";
            if (userId != null) {
                filename += "user-" + userId + "-";
            } else {
                filename += "all-users-";
            }
            filename += (period != null ? period : "30d") + "." + format.toLowerCase();
            
            org.springframework.http.MediaType mediaType;
            if ("CSV".equalsIgnoreCase(format)) {
                mediaType = new org.springframework.http.MediaType("text", "csv");
            } else if ("EXCEL".equalsIgnoreCase(format)) {
                mediaType = new org.springframework.http.MediaType("application", 
                    "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else {
                mediaType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
            }
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(exportData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}