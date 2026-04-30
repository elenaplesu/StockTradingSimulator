import { act, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Explore from '../Explore';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('recharts', async (importOriginal) => {
    const actual = await importOriginal();
    return {
        ...actual,
        ResponsiveContainer: ({ children }) => <div>{children}</div>,
        AreaChart: ({ data }) => <div data-testid="mock-chart">{JSON.stringify(data)}</div>,
        Area: () => null,
        XAxis: () => null,
        YAxis: () => null,
        CartesianGrid: () => null,
        Tooltip: () => null,
    };
});

vi.mock('../config', () => ({
    API_BASE_URL: 'http://localhost:8080',
    getCsrfToken: () => 'fake-token'
}));

const mockAddNotification = vi.fn();
const mockRemoveNotification = vi.fn();

vi.mock('../useNotifications', () => ({
    useNotifications: () => ({
        notifications: [],
        addNotification: mockAddNotification,
        removeNotification: mockRemoveNotification
    })
}));

const mockIsHoliday = vi.fn();
const mockGetHolidays = vi.fn();

vi.mock('date-holidays', () => {
    return {
        default: vi.fn().mockImplementation(function() {
            this.isHoliday = mockIsHoliday;
            this.getHolidays = mockGetHolidays;
            return this;
        })
    };
});

const flushPromises = () => new Promise(resolve => queueMicrotask(resolve));

describe('Explore Component - Chart Padding & Polling Logic', () => {

    beforeEach(() => {
        vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] });
        global.fetch = vi.fn();
        mockAddNotification.mockClear();
        mockRemoveNotification.mockClear();
        mockIsHoliday.mockClear();
        mockGetHolidays.mockClear();

        mockIsHoliday.mockReturnValue(false);
        mockGetHolidays.mockReturnValue([
            { name: 'Good Friday', date: new Date(2023, 3, 14) },
            { name: 'Christmas Day', date: new Date(2023, 11, 25) }
        ]);
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    it('pads data correctly at 15:04 and fills the 15:05 slot after 5 seconds', async () => {
        const T_1500 = new Date('2023-10-25T15:00:00.000Z').getTime();
        const T_1504 = new Date('2023-10-25T15:04:00.000Z').getTime();
        const T_1505 = new Date('2023-10-25T15:05:00.000Z').getTime();

        vi.setSystemTime(T_1504);

        global.fetch.mockImplementation((url) => {
            if (url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([{ timestamp: T_1500, price: 150 }])
                });
            }
            if (url.includes('/api/stocks/') && !url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ symbol: 'AAPL', currentPrice: 152 })
                });
            }
            return Promise.reject(new Error('not mocked'));
        });

        render(<MemoryRouter><Explore userId={1} /></MemoryRouter>);

        fireEvent.change(screen.getByPlaceholderText(/Enter ticker/i), { target: { value: 'AAPL' } });

        await act(async () => {
            fireEvent.submit(screen.getByRole('button', { name: 'Search' }));
            await flushPromises();
            await flushPromises();
            await flushPromises();
        });

        const dataAfterSearch = JSON.parse(screen.getByTestId('mock-chart').textContent);
        const p1500 = dataAfterSearch.find(p => p.timestamp === T_1500);
        expect(p1500).toBeDefined();
        expect(p1500.price).toBe(152);

        const p1505initial = dataAfterSearch.find(p => p.timestamp === T_1505);
        expect(p1505initial).toBeDefined();
        expect(p1505initial.price).toBeNull();

        global.fetch.mockImplementation((url) => {
            if (url.includes('/api/stocks/AAPL') && !url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ symbol: 'AAPL', currentPrice: 155 })
                });
            }
            return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
        });

        await act(async () => {
            vi.setSystemTime(T_1505 - 5000);
            vi.advanceTimersByTime(5000);
            await flushPromises();
            await flushPromises();
        });

        const dataAfterPoll = JSON.parse(screen.getByTestId('mock-chart').textContent);

        const p1505filled = dataAfterPoll.find(p => p.timestamp === T_1505);
        expect(p1505filled).toBeDefined();
        expect(p1505filled.price).toBe(155);

        const nullSlots = dataAfterPoll.filter(p => p.price === null);
        expect(nullSlots.length).toBeGreaterThan(0);

        const filledSlots = dataAfterPoll.filter(p => p.price !== null);
        expect(filledSlots.length).toBeGreaterThan(0);
    });
    it('does not poll when market is closed', async () => {
        const T_closed = new Date('2023-10-25T21:00:00.000Z').getTime();
        const T_history = new Date('2023-10-25T19:55:00.000Z').getTime();

        vi.setSystemTime(T_closed);

        global.fetch.mockImplementation((url) => {
            if (url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([{ timestamp: T_history, price: 150 }])
                });
            }
            if (url.includes('/api/stocks/') && !url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ symbol: 'AAPL', currentPrice: 150 })
                });
            }
            return Promise.reject(new Error('not mocked'));
        });

        render(<MemoryRouter><Explore userId={1} /></MemoryRouter>);

        fireEvent.change(screen.getByPlaceholderText(/Enter ticker/i), { target: { value: 'AAPL' } });

        await act(async () => {
            fireEvent.submit(screen.getByRole('button', { name: 'Search' }));
            await flushPromises();
            await flushPromises();
            await flushPromises();
        });

        expect(screen.getByTestId('mock-chart')).toBeTruthy();

        const callCountAfterSearch = global.fetch.mock.calls.length;

        await act(async () => {
            vi.advanceTimersByTime(10000);
            await flushPromises();
        });

        expect(global.fetch.mock.calls.length).toBe(callCountAfterSearch);

        expect(screen.getByText(/MARKET CLOSED/i)).toBeTruthy();
    });

    it('does not poll on a bank holiday (Christmas 2026)', async () => {
        mockIsHoliday.mockReturnValue({ type: 'public', name: 'Christmas Day' });
        mockGetHolidays.mockReturnValue([
            { name: 'Good Friday', date: new Date(2026, 3, 3) },
            { name: 'Christmas Day', date: new Date(2026, 11, 25) }
        ]);

        const T_CHRISTMAS = new Date('2026-12-25T15:00:00.000Z').getTime();
        const T_HISTORY = new Date('2026-12-25T14:00:00.000Z').getTime();

        vi.setSystemTime(T_CHRISTMAS);

        global.fetch.mockImplementation((url) => {
            if (url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([{ timestamp: T_HISTORY, price: 150 }])
                });
            }
            if (url.includes('/api/stocks/') && !url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ symbol: 'AAPL', currentPrice: 152 })
                });
            }
            return Promise.reject(new Error('not mocked'));
        });

        render(<MemoryRouter><Explore userId={1} /></MemoryRouter>);

        fireEvent.change(screen.getByPlaceholderText(/Enter ticker/i), { target: { value: 'AAPL' } });
        await act(async () => {
            fireEvent.submit(screen.getByRole('button', { name: 'Search' }));
            await flushPromises();
            await flushPromises();
            await flushPromises();
        });

        expect(screen.getByText(/MARKET CLOSED/i)).toBeTruthy();

        const callCountAfterSearch = global.fetch.mock.calls.length;

        await act(async () => {
            vi.setSystemTime(T_CHRISTMAS + 30000);
            vi.advanceTimersByTime(30000);
            await flushPromises();
        });

        expect(global.fetch.mock.calls.length).toBe(callCountAfterSearch);
    });

    it('does not poll on weekends (Saturday)', async () => {
        mockIsHoliday.mockReturnValue(false);

        const T_SATURDAY = new Date('2023-10-28T15:00:00.000Z').getTime();
        const T_HISTORY = new Date('2023-10-28T14:00:00.000Z').getTime();

        vi.setSystemTime(T_SATURDAY);

        global.fetch.mockImplementation((url) => {
            if (url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([{ timestamp: T_HISTORY, price: 150 }])
                });
            }
            if (url.includes('/api/stocks/') && !url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ symbol: 'AAPL', currentPrice: 152 })
                });
            }
            return Promise.reject(new Error('not mocked'));
        });

        render(<MemoryRouter><Explore userId={1} /></MemoryRouter>);

        fireEvent.change(screen.getByPlaceholderText(/Enter ticker/i), { target: { value: 'AAPL' } });

        await act(async () => {
            fireEvent.submit(screen.getByRole('button', { name: 'Search' }));
            await flushPromises();
            await flushPromises();
            await flushPromises();
        });

        expect(screen.getByText(/MARKET CLOSED/i)).toBeTruthy();

        const callCountAfterSearch = global.fetch.mock.calls.length;

        await act(async () => {
            vi.advanceTimersByTime(30000);
            await flushPromises();
        });

        expect(global.fetch.mock.calls.length).toBe(callCountAfterSearch);
    });

    it('does not poll on Good Friday (NYSE trading holiday)', async () => {
        mockIsHoliday.mockReturnValue(false);
        mockGetHolidays.mockReturnValue([
            { name: 'Good Friday', date: new Date(2026, 3, 3) },
            { name: 'Christmas Day', date: new Date(2026, 11, 25) }
        ]);

        const T_GOODFRIDAY = new Date('2026-04-03T15:00:00.000Z').getTime();
        const T_HISTORY = new Date('2026-04-03T14:00:00.000Z').getTime();

        vi.setSystemTime(T_GOODFRIDAY);

        global.fetch.mockImplementation((url) => {
            if (url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([{ timestamp: T_HISTORY, price: 150 }])
                });
            }
            if (url.includes('/api/stocks/') && !url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ symbol: 'AAPL', currentPrice: 152 })
                });
            }
            return Promise.reject(new Error('not mocked'));
        });

        render(<MemoryRouter><Explore userId={1} /></MemoryRouter>);

        fireEvent.change(screen.getByPlaceholderText(/Enter ticker/i), { target: { value: 'AAPL' } });

        await act(async () => {
            fireEvent.submit(screen.getByRole('button', { name: 'Search' }));
            await flushPromises();
            await flushPromises();
            await flushPromises();
        });

        expect(screen.getByText(/MARKET CLOSED/i)).toBeTruthy();

        const callCountAfterSearch = global.fetch.mock.calls.length;

        await act(async () => {
            vi.advanceTimersByTime(30000);
            await flushPromises();
        });

        expect(global.fetch.mock.calls.length).toBe(callCountAfterSearch);
    });

    it('pads data correctly and stops polling when market closes', async () => {
        const T_1945 = new Date('2023-10-25T19:45:00.000Z').getTime();
        const T_2000 = new Date('2023-10-25T20:00:00.000Z').getTime();
        const T_2005 = new Date('2023-10-25T20:05:00.000Z').getTime();

        vi.setSystemTime(T_1945);

        global.fetch.mockImplementation((url) => {
            if (url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([{ timestamp: T_1945 - 300000, price: 150 }])
                });
            }
            if (url.includes('/api/stocks/') && !url.includes('/history')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ symbol: 'AAPL', currentPrice: 152 })
                });
            }
            return Promise.reject(new Error('not mocked'));
        });

        render(<MemoryRouter><Explore userId={1} /></MemoryRouter>);

        fireEvent.change(screen.getByPlaceholderText(/Enter ticker/i), { target: { value: 'AAPL' } });

        await act(async () => {
            fireEvent.submit(screen.getByRole('button', { name: 'Search' }));
            await flushPromises();
            await flushPromises();
            await flushPromises();
        });

        const callCountAfterSearch = global.fetch.mock.calls.length;
        expect(callCountAfterSearch).toBe(2);

        await act(async () => {
            vi.setSystemTime(T_1945 + 300000);
            vi.advanceTimersByTime(5000);
            await flushPromises();
            await flushPromises();
        });

        await act(async () => {
            vi.setSystemTime(T_1945 + 600000);
            vi.advanceTimersByTime(5000);
            await flushPromises();
            await flushPromises();
        });

        const callsAfterPolling = global.fetch.mock.calls.length;
        expect(callsAfterPolling).toBeGreaterThan(callCountAfterSearch);

        await act(async () => {
            vi.setSystemTime(T_2005);
            vi.advanceTimersByTime(T_2005 - (T_1945 + 600000));
            await flushPromises();
        });

        const finalCallCount = global.fetch.mock.calls.length;

        expect(finalCallCount).toBeLessThanOrEqual(callsAfterPolling + 1);

        expect(screen.getByText(/MARKET CLOSED/i)).toBeTruthy();
    });
});