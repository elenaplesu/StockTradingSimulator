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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AiQuizService {

    @Value("${groq.api.key}")
    private String apiKey;

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // 1. THE BACKEND FALLBACK POOL
    private static final List<Map<String, Object>> FALLBACK_POOL = List.of(
            Map.of("q", "What is the primary benefit of diversifying a portfolio?", "options", List.of("Guaranteed profits", "Reducing overall risk", "Avoiding all taxes", "Increasing dividends"), "answer", "Reducing overall risk"),
            Map.of("q", "If inflation is 3% and your cash earns 0%, what happens to your purchasing power?", "options", List.of("It stays the same", "It increases by 3%", "It decreases by 3%", "It doubles"), "answer", "It decreases by 3%"),
            Map.of("q", "What does a P/E ratio measure?", "options", List.of("Price to Earnings", "Profit to Equity", "Portfolio to Exchange", "Price to Execution"), "answer", "Price to Earnings"),
            Map.of("q", "Which order type buys a stock instantly at the best available current price?", "options", List.of("Limit Order", "Stop-Loss Order", "Market Order", "Trailing Stop"), "answer", "Market Order"),
            Map.of("q", "What is a dividend?", "options", List.of("A tax on trading", "A broker fee", "A portion of company profits paid to shareholders", "A stock split"), "answer", "A portion of company profits paid to shareholders"),
            Map.of("q", "What is an ETF (Exchange Traded Fund)?", "options", List.of("A single technology stock", "A basket of securities that trades like a stock", "A government bond", "A cryptocurrency"), "answer", "A basket of securities that trades like a stock"),
            Map.of("q", "What characterizes a 'Bull Market'?", "options", List.of("Prices are falling", "High unemployment", "Prices are rising or expected to rise", "Trading is halted"), "answer", "Prices are rising or expected to rise"),
            Map.of("q", "What does 'liquidity' mean in finance?", "options", List.of("How much debt a company has", "How easily an asset can be converted into cash", "The amount of dividends paid", "The volatility of a stock"), "answer", "How easily an asset can be converted into cash"),
            Map.of("q", "What is the main advantage of a Limit Order?", "options", List.of("It executes immediately", "It guarantees the price you pay or better", "It prevents you from paying taxes", "It automatically selects the best stock"), "answer", "It guarantees the price you pay or better"),
            Map.of("q", "What is compound interest?", "options", List.of("Interest calculated only on the principal amount", "Earning interest on both your original money and the interest it has already earned", "A flat fee charged by brokers", "Interest that decreases over time"), "answer", "Earning interest on both your original money and the interest it has already earned"),
            Map.of("q", "What are 'Blue-Chip' stocks?", "options", List.of("Highly speculative penny stocks", "Stocks of large, well-established, and financially sound companies", "Companies that only sell technology", "Stocks that are currently losing money"), "answer", "Stocks of large, well-established, and financially sound companies"),
            Map.of("q", "What does ROI stand for?", "options", List.of("Rate of Inflation", "Return on Investment", "Risk of Insolvency", "Revenue over Income"), "answer", "Return on Investment")
    );

    public AiQuizService(PortfolioRepository portfolioRepository, HoldingRepository holdingRepository,
                         HttpClient httpClient, ObjectMapper objectMapper) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public String generatePersonalizedQuiz(Long userId) {
        try {
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

            String systemPrompt = "You are an expert financial tutor. Respond ONLY with a valid JSON object. " +
                    "Format strictly like this: {\"questions\": [{\"q\": \"Question?\", \"options\": [\"First choice\", \"Second choice\", \"Third choice\", \"Fourth choice\"], \"answer\": \"Exact text of the correct choice\"}]}. " +
                    "CRITICAL RULES: \n" +
                    "1. Do NOT prefix options with letters or numbers (No 'A)', 'B)', '1.', etc.). \n" +
                    "2. The 'answer' string MUST be an exact, character-for-character copy of the correct string from the 'options' array. \n" +
                    "3. Do not include any markdown formatting outside the JSON.";

            String userPrompt = "The user currently has this portfolio: [" + portfolioContext.toString() + "]. " +
                    "Generate a 5-question multiple-choice educational quiz about the stock market, investing principles, and trading concepts. " +
                    "Tailor the difficulty and topics to their specific holdings. Focus primarily on general market knowledge, but consider mixing in a conceptual question or two about quantitative metrics like Return on Investment (ROI), Price-to-Earnings (P/E), or portfolio diversification/HHI if it fits their profile. " +
                    "Make the questions practical and educational. DO NOT ask them to perform complex mental math calculations about their specific shares.";

            Map<String, Object> requestMap = Map.of(
                    "model", "llama-3.1-8b-instant",
                    "temperature", 0.2d,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );
            String requestBody = objectMapper.writeValueAsString(requestMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(5)) // 2. THE JAVA 5-SECOND STOPWATCH
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return getFallbackQuiz();
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            String aiResponseText = rootNode.path("choices").get(0).path("message").path("content").asText();
            JsonNode contentNode = objectMapper.readTree(aiResponseText);
            return contentNode.path("questions").toString();

        } catch (Exception e) {
            return getFallbackQuiz(); // Instantly falls back if taking longer than 5s
        }
    }

    // 3. THE JAVA RANDOMIZER
    public String getFallbackQuiz() {
        try {
            List<Map<String, Object>> shuffledPool = new ArrayList<>(FALLBACK_POOL);
            Collections.shuffle(shuffledPool); // Randomize the 12 questions
            List<Map<String, Object>> selectedQuestions = shuffledPool.subList(0, 5); // Take the first 5
            return objectMapper.writeValueAsString(selectedQuestions); // Send as JSON array string
        } catch (Exception e) {
            return "[]";
        }
    }
}