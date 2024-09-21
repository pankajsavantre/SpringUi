package com.ps.init.controller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ps.init.model.UiModel;

@RestController
public class UiController {
	
	public String getHome() {
		return "/";
	}
	

    @PostMapping("/generate")
    public ResponseEntity<Resource> generateProject(
            @ModelAttribute UiModel projectRequest,
            Model model) {

        String projectName = projectRequest.getProjectName();
        MultipartFile excelFile = projectRequest.getConfigFile();
        Path zipFilePath = Paths.get("projects", projectName + ".zip");

        // Process Excel file and generate project structure
        if (excelFile != null && !excelFile.isEmpty()) {
            try (XSSFWorkbook workbook = new XSSFWorkbook(excelFile.getInputStream())) {
                // Read data from the first sheet
                var sheet = workbook.getSheetAt(0);
                StringBuilder projectData = new StringBuilder();

                for (Row row : sheet) {
                    // Assuming the first column contains dependencies
                    String dependency = row.getCell(0).getStringCellValue();
                    projectData.append(dependency).append("\n");
                }

                // Generate project structure
                generateProjectStructure(projectName, projectData.toString());

                // Zip the project folder
                zipProject(projectName);

                // Prepare the zip file for download
                Resource resource = new FileSystemResource(zipFilePath.toFile());
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + projectRequest.getProjectName());

                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
            } catch (IOException e) {
                model.addAttribute("fileUploadError", "Failed to process the Excel file: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    
    private void generateProjectStructure(String projectName, String dependencies) throws IOException {
	        Path projectPath = Paths.get("projects", projectName);
	        Files.createDirectories(projectPath);

	        String pomContent = "<project>\n" +
	                "<modelVersion>4.0.0</modelVersion>\n" +
	                "<groupId>com.example</groupId>\n" +
	                "<artifactId>" + projectName + "</artifactId>\n" +
	                "<version>0.0.1-SNAPSHOT</version>\n" +
	                "<dependencies>\n" +
	                "    <dependency>\n" +
	                "        <groupId>org.springframework.boot</groupId>\n" +
	                "        <artifactId>spring-boot-starter-web</artifactId>\n" +
	                "    </dependency>\n" +
	                dependencies +
	                "</dependencies>\n" +
	                "</project>";

	        try (FileOutputStream fos = new FileOutputStream(projectPath.resolve("pom.xml").toFile())) {
	            fos.write(pomContent.getBytes());
	        }
	    }

	    private void zipProject(String projectName) throws IOException {
	        Path projectPath = Paths.get("projects", projectName);
	        Path zipFilePath = Paths.get("projects", projectName + ".zip");

	        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
	            Files.walk(projectPath)
	                .filter(path -> !Files.isDirectory(path))
	                .forEach(path -> {
	                    ZipEntry zipEntry = new ZipEntry(projectPath.relativize(path).toString());
	                    try {
	                        zos.putNextEntry(zipEntry);
	                        Files.copy(path, zos);
	                        zos.closeEntry();
	                    } catch (IOException e) {
	                        e.printStackTrace();
	                    }
	                });
	        }
	    }
}
