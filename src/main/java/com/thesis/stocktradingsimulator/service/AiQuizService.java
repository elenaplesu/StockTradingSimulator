package com.thesis.stocktradingsimulator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.stocktradingsimulator.model.Holding;
import com.thesis.stocktradingsimulator.model.Portfolio;
import com.thesis.stocktradingsimulator.repository.HoldingRepository;
import com.thesis.stocktradingsimulator.repository.PortfolioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class AiQuizService {

    @Value("${groq.api.key}")
    private String apiKey;

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Reusable client for maximum speed
    private final HttpClient client = HttpClient.newHttpClient();

    public AiQuizService(PortfolioRepository portfolioRepository, HoldingRepository holdingRepository) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
    }

    public String generatePersonalizedQuiz(Long userId) {
        try {
            // 1. Gather the context
            Portfolio portfolio = portfolioRepository.findByUserId(userId).orElse(null);
            if (portfolio == null) return getFallbackQuiz();

            StringBuilder portfolioContext = new StringBuilder();
            portfolioContext.append("User has $").append(portfolio.getUser().getCashBalance()).append(" in cash. ");

            List<Holding> holdings = holdingRepository.findByPortfolioId(portfolio.getId());
            if (holdings != null && !holdings.isEmpty()) {
                portfolioContext.append("They own the following stocks: ");
                for (Holding h : holdings) {
                    portfolioContext.append(h.getQuantity()).append(" shares of ").append(h.getSymbol())
                            .append(" at an average price of $").append(h.getAverageBuyPrice()).append(". ");
                }
            } else {
                portfolioContext.append("They currently own no stocks. ");
            }

            // 2. Build the Groq (Llama 3) Prompt
            String systemPrompt = "You are an expert financial tutor. Respond ONLY with a JSON object containing a 'questions' array. " +
                    "Format strictly like this: {\"questions\": [{\"q\": \"Question text\", \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"], \"answer\": \"Correct Option\"}]}";

            String userPrompt = "The user currently has this portfolio: [" + portfolioContext.toString() + "]. " +
                    "Generate a 5-question multiple-choice educational quiz about general stock market concepts, investing principles, and how trading works based on their experience level. DO NOT ask math questions about their specific shares.";

            // 3. Format the JSON request specifically for Groq's blazing fast API
            Map<String, Object> requestMap = Map.of(
                    "model", "llama-3.1-8b-instant", // THE FIX: Meta's newest active high-speed model
                    "temperature", 0.2, // Low creativity for high speed
                    "response_format", Map.of("type", "json_object"), // Force strict JSON
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );
            String requestBody = objectMapper.writeValueAsString(requestMap);

            // 4. Send the request to Groq
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey) // Groq uses Bearer tokens
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Groq API Failed: " + response.body());
                return getFallbackQuiz();
            }

            // 5. Parse the OpenAI-style response to extract just the text
            JsonNode rootNode = objectMapper.readTree(response.body());
            String aiResponseText = rootNode.path("choices").get(0).path("message").path("content").asText();

            // 6. Extract the actual array from the JSON object so React doesn't break
            JsonNode contentNode = objectMapper.readTree(aiResponseText);
            return contentNode.path("questions").toString(); // Returns just the strict [...] array

        } catch (Exception e) {
            System.err.println("AI Generation Failed. Triggering Fallback: " + e.getMessage());
            return getFallbackQuiz();
        }
    }

    private String getFallbackQuiz() {
        return "[\n" +
                "  {\"q\": \"What is the primary benefit of diversifying a portfolio?\", \"options\": [\"Guaranteed profits\", \"Reducing overall risk\", \"Avoiding all taxes\", \"Increasing dividends\"], \"answer\": \"Reducing overall risk\"},\n" +
                "  {\"q\": \"If inflation is 3% and your cash earns 0%, what happens to your purchasing power?\", \"options\": [\"It stays the same\", \"It increases by 3%\", \"It decreases by 3%\", \"It doubles\"], \"answer\": \"It decreases by 3%\"},\n" +
                "  {\"q\": \"What does a P/E ratio measure?\", \"options\": [\"Price to Earnings\", \"Profit to Equity\", \"Portfolio to Exchange\", \"Price to Execution\"], \"answer\": \"Price to Earnings\"},\n" +
                "  {\"q\": \"Which order type buys a stock instantly at the best available current price?\", \"options\": [\"Limit Order\", \"Stop-Loss Order\", \"Market Order\", \"Trailing Stop\"], \"answer\": \"Market Order\"},\n" +
                "  {\"q\": \"What is a dividend?\", \"options\": [\"A tax on trading\", \"A broker fee\", \"A portion of company profits paid to shareholders\", \"A stock split\"], \"answer\": \"A portion of company profits paid to shareholders\"}\n" +
                "]";
    }
}