package com.thesis.stocktradingsimulator.repository;

import com.thesis.stocktradingsimulator.BaseIntegrationTest;
import com.thesis.stocktradingsimulator.model.Transaction;
import com.thesis.stocktradingsimulator.model.Transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void findByPortfolioIdOrderByTimestampDesc_ShouldReturnSortedTransactions() throws InterruptedException {

        Transaction firstTx = new Transaction(defaultPortfolio, TransactionType.BUY, "AAPL", 10, new BigDecimal("150.00"));
        transactionRepository.save(firstTx);

        Thread.sleep(100);

        Transaction secondTx = new Transaction(defaultPortfolio, TransactionType.BUY, "TSLA", 5, new BigDecimal("200.00"));
        transactionRepository.save(secondTx);

        List<Transaction> history = transactionRepository.findByPortfolioIdOrderByTimestampDesc(defaultPortfolio.getId());

        assertEquals(2, history.size(), "Should find exactly 2 transactions");

        assertEquals("TSLA", history.get(0).getSymbol(), "Newest transaction should be first");
        assertEquals("AAPL", history.get(1).getSymbol(), "Older transaction should be second");
    }

    @Test
    void findByPortfolioIdOrderByTimestampDesc_ShouldReturnEmpty_WhenPortfolioHasNoHistory() {

        List<Transaction> history = transactionRepository.findByPortfolioIdOrderByTimestampDesc(999L);

        assertTrue(history.isEmpty(), "Should return an empty list if no transactions exist");
    }
}