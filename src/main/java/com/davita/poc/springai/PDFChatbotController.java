package com.davita.poc.springai;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chatbot")
@Slf4j
public class PDFChatbotController {

	@Autowired
	private OllamaChatModel chatModel;

	@Value("${chatbot.pdf.lib}")
	private String pdfLib;

	private String pdfContent = ""; // Store extracted content for Q&A

	// Upload PDF and extract text
	@PostMapping("/upload-pdf")
	public ResponseEntity<Map<String, String>> uploadPDF(@RequestParam("file") MultipartFile file) {
		try {
			pdfContent = extractTextFromPDF(file);
			Map<String, String> response = new HashMap<>();
			response.put("message", "PDF uploaded and text extracted successfully.");
			response.put("content", pdfContent);
			log.info("pdfContent: {}", pdfContent);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Failed to process the PDF: " + e.getMessage()));
		}
	}

	// Handle Q&A with LLaMA
	@PostMapping("/ask")
	public ResponseEntity<Map<String, String>> askQuestion(@RequestBody Map<String, String> request) {
		String question = request.get("question");
		if (pdfContent.isEmpty()) {
			return ResponseEntity.badRequest()
					.body(Map.of("error", "No PDF content available. Please upload a PDF first."));
		}

		try {
			// Call LLaMA model for the answer
			// ChatResponse chatResponse = chatModel
			// .call(new Prompt(List.of(new SystemMessage(pdfContent), new
			// UserMessage(question))));
			ChatResponse chatResponse = ChatClient.builder(chatModel).defaultSystem(pdfContent).build().prompt()
					.user(question).call().chatResponse();
			String chatResponseStr = chatResponse.getResult().getOutput().getText();
			log.info("Chat Response : {}", chatResponseStr);

			return ResponseEntity.ok(Map.of("answer", chatResponseStr));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Failed to get an answer: " + e.getMessage()));
		}
	}

	// Extract text from PDF
	private String extractTextFromPDF(MultipartFile file) throws IOException {

		switch (PdfLibEnum.valueOf(pdfLib)) {
		case pdfbox:
			try (PDDocument document = PDDocument.load(file.getInputStream())) {
				PDFTextStripper stripper = new PDFTextStripper();
				stripper.setAddMoreFormatting(true);
				return stripper.getText(document);
			}
		case itext:
			try {
				PdfReader reader = new PdfReader(file.getInputStream());
				StringBuffer sbf = new StringBuffer();
				int numberOfPages = reader.getNumberOfPages();
				for (int i = 1; i <= numberOfPages; i++) {
					String text = PdfTextExtractor.getTextFromPage(reader, i);
					sbf.append(text).append("\n");
				}
				reader.close();
				return sbf.toString();
			} catch (Exception e) {
				log.error("Error in reading pdf with iText:", e);
			}
		case openpdf:
			try {
				// Load the PDF document
				com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(file.getInputStream());
				// Loop through all pages
				int numberOfPages = reader.getNumberOfPages();
				StringBuffer sbf = new StringBuffer();
				com.lowagie.text.pdf.parser.PdfTextExtractor openPdfTextExtractor = new com.lowagie.text.pdf.parser.PdfTextExtractor(
						reader);
				for (int i = 1; i <= numberOfPages; i++) {
					// Extract text from each page
					String pageContent = openPdfTextExtractor.getTextFromPage(i);
					sbf.append(pageContent).append(System.lineSeparator());
				}
				// Close the reader
				reader.close();
				return sbf.toString();
			} catch (IOException e) {
				log.error("Error in reading pdf with opepdf:", e);
			}
		}

		return null;
	}

}
