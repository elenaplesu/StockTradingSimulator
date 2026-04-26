import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import Portfolio from '../Portfolio';
import { beforeEach, describe, expect, test, vi } from 'vitest';

global.fetch = vi.fn();

vi.mock('../config', () => ({
    API_BASE_URL: 'http://localhost:8080',
    getCsrfToken: () => 'fake-token'
}));

vi.mock('../useNotifications', () => ({
    useNotifications: () => ({
        notifications: [],
        addNotification: vi.fn(),
        removeNotification: vi.fn()
    })
}));

describe('Portfolio Component', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    test('renders the loading spinner initially', () => {
        fetch.mockImplementation(() => new Promise(() => {}));

        render(
            <MemoryRouter>
                <Portfolio userId={1} />
            </MemoryRouter>
        );

        expect(screen.getByText(/Loading Quantitative Analytics/i)).toBeInTheDocument();
    });

    test('fetches and renders portfolio analytics and transactions correctly', async () => {
        const mockAnalytics = {
            cashBalance: 15000.50,
            netWorth: 20000.00,
            aggregateROI: 10.5,
            hhi: 5000,
            crossSectionalVariance: 0.2,
            holdings: [
                {
                    symbol: "AAPL",
                    quantity: 10,
                    averageBuyPrice: 150.00,
                    currentPrice: 175.00,
                    totalValue: 1750.00,
                    returnOnInvestment: 16.66,
                    weightPercentage: 50.0
                }
            ]
        };

        const mockTransactions = [
            {
                id: 1,
                timestamp: "2023-10-27T10:00:00Z",
                type: "BUY",
                symbol: "AAPL",
                quantity: 10,
                executionPrice: 150.00
            }
        ];

        fetch.mockImplementation((url) => {
            if (url.includes('/analytics')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve(mockAnalytics),
                });
            }
            if (url.includes('/transactions')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve(mockTransactions),
                });
            }
            return Promise.reject(new Error('Unknown URL'));
        });

        render(
            <MemoryRouter>
                <Portfolio userId={1} />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText('Portfolio Analytics Terminal')).toBeInTheDocument();
        });

        expect(screen.getByText(/\$15,000\.50/i)).toBeInTheDocument();
        expect(screen.getByText(/\$20,000\.00/i)).toBeInTheDocument();
        expect(screen.getAllByText(/AAPL/i).length).toBeGreaterThan(0);
        expect(screen.getAllByText('10').length).toBeGreaterThan(0);
        expect(screen.getByText(/\$175\.00/i)).toBeInTheDocument();
    });

    test('renders error state when the API fails', async () => {
        const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

        fetch.mockImplementation(() => Promise.resolve({ ok: false }));

        render(
            <MemoryRouter>
                <Portfolio userId={1} />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText(/Error: Could not load data/i)).toBeInTheDocument();
        });

        consoleError.mockRestore();
    });
});