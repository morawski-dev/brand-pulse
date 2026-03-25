/**
 * Unit tests for the centralized API client (error mapping + parsing).
 * global.fetch is mocked so no real network calls are made.
 */

import { apiGet, apiPost, ApiException } from '../client';

describe('apiClient', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
    jest.restoreAllMocks();
  });

  function mockFetchOnce(response: Partial<Response> & { json?: () => Promise<any> }) {
    global.fetch = jest.fn().mockResolvedValue(response as Response);
  }

  it('apiGet returns parsed JSON on success', async () => {
    mockFetchOnce({ ok: true, status: 200, json: async () => ({ hello: 'world' }) });
    const data = await apiGet<{ hello: string }>('/api/test');
    expect(data).toEqual({ hello: 'world' });
  });

  it('apiGet throws ApiException with the backend error body on 404', async () => {
    mockFetchOnce({
      ok: false,
      status: 404,
      json: async () => ({ code: 'BRAND_NOT_FOUND', message: 'Nie znaleziono' }),
    });

    await expect(apiGet('/api/brands/me')).rejects.toBeInstanceOf(ApiException);
    try {
      await apiGet('/api/brands/me');
    } catch (e) {
      const err = e as ApiException;
      expect(err.error.code).toBe('BRAND_NOT_FOUND');
      expect(err.status).toBe(404);
    }
  });

  it('apiGet maps a 401 with a non-JSON body to UNAUTHORIZED', async () => {
    mockFetchOnce({
      ok: false,
      status: 401,
      json: async () => {
        throw new Error('not json');
      },
    });

    try {
      await apiGet('/api/secure');
      throw new Error('expected to throw');
    } catch (e) {
      const err = e as ApiException;
      expect(err).toBeInstanceOf(ApiException);
      expect(err.error.code).toBe('UNAUTHORIZED');
    }
  });

  it('apiPost returns an empty object for 204 No Content', async () => {
    mockFetchOnce({ ok: true, status: 204 });
    const data = await apiPost('/api/auth/logout', {});
    expect(data).toEqual({});
  });

  it('wraps network failures in an ApiException', async () => {
    global.fetch = jest.fn().mockRejectedValue(new TypeError('Failed to fetch'));
    await expect(apiGet('/api/test')).rejects.toBeInstanceOf(ApiException);
  });
});
