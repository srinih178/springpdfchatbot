package com.davita.poc.springai;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chatbot")
public class PDFChatbotController {

	@Autowired
	private OllamaChatModel chatModel;

	private String pdfContent = ""; // Store extracted content for Q&A

	// Upload PDF and extract text
	@PostMapping("/upload-pdf")
	public ResponseEntity<Map<String, String>> uploadPDF(@RequestParam("file") MultipartFile file) {
		try {
			pdfContent = extractTextFromPDF(file);
			Map<String, String> response = new HashMap<>();
			response.put("message", "PDF uploaded and text extracted successfully.");
			response.put("content", pdfContent);
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
			/*ChatResponse chatResponse = chatModel
					.call(new Prompt(List.of(new SystemMessage(pdfContent), new UserMessage(question)),
							OllamaOptions.builder().withModel(OllamaModel.LLAMA3_2).build()));*/
			
			ChatResponse chatResponse = chatModel
					.call(new Prompt(List.of(new SystemMessage(pdfContent), new UserMessage(question))));

			return ResponseEntity.ok(Map.of("answer", chatResponse.getResult().getOutput().getText()));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Failed to get an answer: " + e.getMessage()));
		}
	}

	// Extract text from PDF
	private String extractTextFromPDF(MultipartFile file) throws IOException {
		try (PDDocument document = PDDocument.load(file.getInputStream())) {
			PDFTextStripper stripper = new PDFTextStripper();
			return stripper.getText(document);
		}
	}
	
}
